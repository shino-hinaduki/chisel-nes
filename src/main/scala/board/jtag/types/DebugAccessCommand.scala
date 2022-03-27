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
      * @return { request 8bit, offset 32bit, }
      */
    def encode(request: Type.Type, offset: UInt, data: UInt): UInt =
      Cat(request.asUInt(requestTypeWidth - 1, 0), offset(offsetWidth - 1, 0), data(dataWidth - 1, 0))

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
}
