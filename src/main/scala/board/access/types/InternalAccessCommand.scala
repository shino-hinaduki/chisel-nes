package board.access.types

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.Cat

import board.ram.types.AsyncFifoEnqueueIO
import board.ram.types.AsyncFifoDequeueIO
import board.ip.AsyncFifoVJtagToDap
import board.ip.AsyncFifoDapToVJtag

/**
  * Virtual JTAG Bridgeなどから、R/W要求を出すときとその応答の定義
  */
object InternalAccessCommand {

  /**
    * 要求種類。R/W以外にReset/Echoなどが必要になるケースに備えてenumにしておく
    */
  object Type extends ChiselEnum {
    val read  = Value(0x01.U)
    val write = Value(0x02.U)
  }

  /**
    * 要求定義の補助関数群
    * { requestType 8bit, offset 24bit, data 32bit}
    */
  object Request {

    /**
      * Requst自体のサイズ
      */
    val cmdWidth: Int = 64

    /**
    * 要求データの幅
    */
    val dataWidth: Int = 32

    /**
    * 要求アドレス幅
    */
    val offsetWidth: Int = 24

    /**
    * RequestTypeの幅
    */
    val requestTypeWidth: Int = Type.getWidth

    /**
      * 要求内容から、UIntのデータを作成する
      * @param request 要求の種類
      * @param offset 要求アドレス
      * @param data データ
      * @return { request 8bit, offset 24bit, data 32bit }
      */
    def encode(request: Type.Type, offset: UInt, data: UInt): UInt =
      Cat(request.asUInt, offset(offsetWidth - 1, 0), data(dataWidth - 1, 0))

    /**
      * Write要求を作成する
      */
    def encodeWriteReq(offset: UInt, data: UInt): UInt = encode(request = Type.write, offset = offset, data = data)

    /**
      * Read要求を作成する
      */
    def encodeReadReq(offset: UInt): UInt = encode(request = Type.read, offset = offset, data = 0.U)

    val dataPosL        = 0
    val dataPosH        = dataPosL + dataWidth - 1
    val offsetPosL      = dataPosH + 1
    val offsetPosH      = offsetPosL + offsetWidth - 1
    val requestTypePosL = offsetPosH + 1
    val requestTypePosH = requestTypePosL + requestTypeWidth - 1

    /**
      * dataを取り出す
      * @param rawData encodeしたデータ
      */
    def getData(rawData: UInt): UInt = rawData(dataPosH, dataPosL)

    /**
      * アドレスを取り出す
      * @param rawData encodeしたデータ
      */
    def getOffset(rawData: UInt): UInt = rawData(offsetPosH, offsetPosL)

    /**
      * requestTypeを取り出す
      * @param rawData encodeしたデータ
      */
    def getRequestType(rawData: UInt): (Type.Type, Bool) = Type.safe(rawData(requestTypePosH, requestTypePosL))
  }

  /**
    * 応答定義の補助関数群. 
    * { data 32bit }
    */
  object Response {

    /**
      * Response自体のサイズ
      */
    val cmdWidth: Int = 32

    /**
    * 要求データの幅
    */
    val dataWidth: Int = 32

    /**
      * 要求内容から、UIntのデータを作成する
      * @param data データ
      */
    def encode(data: UInt): UInt = data(dataWidth - 1, 0)

    val dataPosL = 0
    val dataPosH = dataPosL + dataWidth - 1

    /**
      * dataを取り出す
      * @param rawData encodeしたデータ
      */
    def getData(rawData: UInt): UInt = rawData(dataPosH, dataPosL)
  }

  /**
  * 外部からのデータ読み書き向けの要求/応答Queue制御
  * AsyncFIFOで分断することを想定
  */
  class MasterIO extends Bundle {

    /**
    * 要求Queue
    */
    val req = Flipped(new AsyncFifoEnqueueIO(InternalAccessCommand.Request.cmdWidth))

    /**
    * 応答Queue
    */
    val resp = Flipped(new AsyncFifoDequeueIO(InternalAccessCommand.Response.cmdWidth))

    /**
      * SlaveIOと接続します
      *
      * @param slave 接続先のI/F
      * @param mosiFifo Master->Slave間のFIFO
      * @param misoFifo Master<-Slave間のFIFO
      */
    def connect(slave: SlaveIO, mosiFifo: AsyncFifoVJtagToDap, misoFifo: AsyncFifoDapToVJtag) = {
      // req: master -> fifo
      mosiFifo.io.data <> req.data
      mosiFifo.io.wrclk <> req.wrclk
      mosiFifo.io.wrreq <> req.wrreq
      mosiFifo.io.wrfull <> req.wrfull
      // req: fifo -> slave
      mosiFifo.io.rdclk <> slave.req.rdclk
      mosiFifo.io.rdreq <> slave.req.rdreq
      mosiFifo.io.q <> slave.req.q
      mosiFifo.io.rdempty <> slave.req.rdempty
      // resp: slave -> fifo
      misoFifo.io.data <> slave.resp.data
      misoFifo.io.wrclk <> slave.resp.wrclk
      misoFifo.io.wrreq <> slave.resp.wrreq
      misoFifo.io.wrfull <> slave.resp.wrfull
      // resp: fifo -> master
      misoFifo.io.rdclk <> resp.rdclk
      misoFifo.io.rdreq <> resp.rdreq
      misoFifo.io.q <> resp.q
      misoFifo.io.rdempty <> resp.rdempty
    }
  }

  /**
  * 外部からのデータ読み書き向けの要求受信/応答返却Queue制御
  * AsyncFIFOで分断することを想定
  */
  class SlaveIO extends Bundle {

    /**
    * 要求Queue
    */
    val req = Flipped(new AsyncFifoDequeueIO(InternalAccessCommand.Request.cmdWidth))

    /**
    * 応答Queue
    */
    val resp = Flipped(new AsyncFifoEnqueueIO(InternalAccessCommand.Response.cmdWidth))

    /**
      * MasterIOと接続します
      *
      * @param master 接続先のI/F
      * @param mosiFifo Master->Slave間のFIFO
      * @param misoFifo Master<-Slave間のFIFO
      */
    def connect(master: MasterIO, mosiFifo: AsyncFifoVJtagToDap, misoFifo: AsyncFifoDapToVJtag) = {
      master.connect(this, mosiFifo, misoFifo)
    }
  }

}
