package board.jtag.types

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.Cat

/**
  * Virtual JTAG BridgeからR/W要求を出すときとその応答の定義
  * (VJTAG以外から出すことがあれば、この定義は移動したほうが良い)
  */
object DebugAccessCommand {

  /**
    * 要求種類。R/W以外にReset/Echoなどが必要になるケースに備えてenumにしておく
    */
  object Type extends ChiselEnum {
    val invalid  = Value(0x00.U)
    val read     = Value(0x01.U)
    val write    = Value(0x02.U)
    val invalid2 = Value(0xff.U)
  }

  /**
    * 要求定義の補助関数群
    * { reserved 24bit, requestType 8bit, addr 32bit, data 32bit}
    */
  object Request {

    /**
      * Requst自体のサイズ
      */
    val cmdWidth: Int = 96

    /**
    * 要求データの幅
    */
    val dataWidth: Int = 32

    /**
    * 要求アドレス幅
    */
    val addrWidth: Int = 32

    /**
    * RequestTypeの幅
    */
    val requestTypeWidth: Int = Type.getWidth

    /**
      * 空き領域
      */
    val reservedWidth: Int = 24

    /**
      * 要求内容から、UIntのデータを作成する
      * @param request 要求の種類
      * @param addr 要求アドレス
      * @param data データ
      * @param reserved 予約
      * @return { request 8bit, addr 32bit, }
      */
    def encode(request: Type.Type, addr: UInt, data: UInt, reserved: UInt = 0.U(reservedWidth.W)): UInt =
      Cat(reserved(reservedWidth - 1, 0), request.asUInt(requestTypeWidth - 1, 0), addr(addrWidth - 1, 0), data(dataWidth - 1, 0))

    /**
      * Write要求を作成する
      */
    def encodeWriteReq(addr: UInt, data: UInt): UInt = encode(request = Type.write, addr = addr, data = data)

    /**
      * Read要求を作成する
      */
    def encodeReadReq(addr: UInt): UInt = encode(request = Type.read, addr = addr, data = 0.U)

    val dataPosL        = 0
    val dataPosH        = dataPosL + dataWidth - 1
    val addrPosL        = dataPosH + 1
    val addrPosH        = addrPosL + addrWidth - 1
    val requestTypePosL = addrPosH + 1
    val requestTypePosH = requestTypePosL + requestTypeWidth - 1
    val reservedPosL    = requestTypePosH + 1
    val reservedPosH    = reservedPosL + reservedWidth - 1

    /**
      * dataを取り出す
      * @param rawData encodeしたデータ
      */
    def getData(rawData: UInt): UInt = rawData(dataPosH, dataPosL)

    /**
      * アドレスを取り出す
      * @param rawData encodeしたデータ
      */
    def getAddress(rawData: UInt): UInt = rawData(addrPosH, addrPosL)

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
}
