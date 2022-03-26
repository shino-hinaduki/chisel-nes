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
    * { addr 32bit, reserved 8bit, count 8bit, data 8bit, requestType 8bit}
    * 
    * @remark 余裕がなかったらcount/dataを兼用としてargsとしてもいいかもしれない
    */
  object Request {

    /**
      * Requst自体のサイズ
      */
    val cmdWidth: Int = 64

    /**
    * 要求アドレス幅
    */
    val addrWidth: Int = 32

    /**
    * 要求データの幅
    */
    val dataWidth: Int = 8

    /**
    * RequestTypeの幅
    */
    val requestTypeWidth: Int = Type.getWidth

    /**
    * ReadCountの幅
    */
    val countWidth: Int = 8

    /**
      * 空き領域
      */
    val reservedWidth: Int = 8

    /**
      * 要求内容から、UIntのデータを作成する
      * @param request 要求の種類
      * @param addr 要求アドレス
      * @param data データ
      * @param count カウント(Readのみ有効. 0,1は等価に扱う)
      * @param reserved 予約
      * @return { request 8bit, addr 32bit, }
      */
    def encode(request: Type.Type, addr: UInt, data: UInt, count: UInt, reserved: UInt = 0.U(reservedWidth.W)): UInt =
      Cat(addr(addrWidth - 1, 0), reserved(reservedWidth - 1, 0), count(countWidth - 1, 0), data(dataWidth - 1, 0), request.asUInt(requestTypeWidth - 1, 0))

    /**
      * Write要求を作成する
      */
    def encodeWriteReq(addr: UInt, data: UInt): UInt = encode(request = Type.write, addr = addr, data = data, count = 0.U)

    /**
      * Read要求を作成する
      */
    def encodeReadReq(addr: UInt, count: UInt): UInt = encode(request = Type.read, addr = addr, data = 0.U, count = count)

    /**
      * アドレスを取り出す
      * @param rawData encodeしたデータ
      */
    def getAddress(rawData: UInt): UInt = rawData(63, 32)

    /**
      * reservedの値を取り出す
      * @param rawData encodeしたデータ
      */
    def getReserved(rawData: UInt): UInt = rawData(31, 24)

    /**
      * countを取り出す
      * @param rawData encodeしたデータ
      */
    def getCount(rawData: UInt): UInt = rawData(23, 16)

    /**
      * dataを取り出す
      * @param rawData encodeしたデータ
      */
    def getData(rawData: UInt): UInt = rawData(15, 8)

    /**
      * requestTypeを取り出す
      * @param rawData encodeしたデータ
      */
    def getRequestType(rawData: UInt): (Type.Type, Bool) = Type.safe(rawData)
  }

  /**
    * 応答定義の補助関数群. 
    * { data 8bit }
    * 
    * @ remark Readでしか使っていないのでdataだけだが、以下の具合でもいいかも 
       { reqCount 8bit, currentCount 8bit, data 8bit, requestType 8bit}
    */
  object Response {

    /**
      * Response自体のサイズ
      */
    val cmdWidth: Int = 8

    /**
    * 要求データの幅
    */
    val dataWidth: Int = 8

    /**
      * 要求内容から、UIntのデータを作成する
      * @param data データ
      */
    def encode(data: UInt): UInt = data(7, 0)

    /**
      * dataを取り出す
      * @param rawData encodeしたデータ
      */
    def getData(rawData: UInt): UInt = rawData(7, 0)
  }
}
