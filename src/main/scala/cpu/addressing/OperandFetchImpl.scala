package cpu.addressing

import chisel3._
import _root_.cpu.types.Addressing

/**
  * OperandFetchImplが行う処理を定義する
  */
trait Process {}

/**
  * OFを初期状態に戻す
  * @param isIllegal 想定外の挙動でResetする場合はtrue
  */
case class Clear(isIllegal: Boolean) extends Process

/**
  * BusMasterを使ってReadを要求する, 要求するのは命令直後のデータ
  * @param addr Read先アドレス
  * @param length Read byte数
  */
case class ReadOperand(addr: UInt, length: UInt) extends Process

/**
  * BusMasterを使ってReadを要求する, 対象はアドレス(Indirect向け)
  * @param addr Read先アドレス
  * @param length Read byte数
  */
case class ReadPointer(addr: UInt, length: UInt) extends Process

/**
  * BusMasterを使ってReadを要求する, 対象はデータ
  * @param addr Read先アドレス
  * @param length Read byte数
  */
case class ReadData(addr: UInt, length: UInt) extends Process

/**
  * データを読み出さず、アドレスだけを結果として報告する
  * @param addr 解決したアドレス
  */
case class ReportAddr(addr: UInt) extends Process

/**
  * データのみを報告する
  * @param data 報告するデータ
  */
case class ReportData(data: UInt) extends Process

/**
  * データを読み出し、アドレスとともに報告する
  * @param addr 解決したアドレス
  * @param data 読みだしたデータ
  */
case class ReportFull(addr: UInt, data: UInt) extends Process

/**
  * 完了したことだけ報告する(Impliedなど)
  */
case class ReportNone() extends Process

/**
 * AddressingModeごとのOperandFetchするときの挙動を定義する
 */
trait OperandFetchImpl {

  /**
   * 対象のAddressingModeを返す
   */
  def addressing: Addressing.Type

  /**
    * Fetch要求を受けたときの処理
    * @param opcodeAddr OpCodeが配置されていたアドレスUInt(16.W)
    * @param reqReadData Dataの読み出しを要求されていればtrue
    * @param aReg Aレジスタの値
    * @return 次の処理
    */
  def onRequest(opcodeAddr: UInt, reqReadData: Boolean, aReg: UInt): Process

  /**
    * OP直後のデータを読みだしたあとの処理
    *
    * @param opcodeAddr OpCodeが配置されていたアドレスUInt(16.W)
    * @param reqReadData Dataの読み出しを要求されていればtrue
    * @param readAddr 最後に読みだしたアドレス
    * @param readData Readしたデータ。複数byte Readした場合は結合済
    * @param xReg Xレジスタの値
    * @param yReg Yレジスタの値
    * @return 次の処理
    */
  def doneReadOperand(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, xReg: UInt, yReg: UInt): Process

  /**
    * (Indirect限定) 解決先アドレス算出に必要なReadが完了したときの処理
    *
    * @param opcodeAddr OpCodeが配置されていたアドレスUInt(16.W)
    * @param reqReadData Dataの読み出しを要求されていればtrue
    * @param readAddr 最後に読みだしたアドレス
    * @param readData Readしたデータ。複数byte Readした場合は結合済
    * @param xReg Xレジスタの値
    * @param yReg Yレジスタの値
    * @return 次の処理
    */
  def doneReadPointer(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, xReg: UInt, yReg: UInt): Process

  /**
    * データを読みだしたあとの処理
    *
    * @param opcodeAddr OpCodeが配置されていたアドレスUInt(16.W)
    * @param readAddr 最後に読みだしたアドレス
    * @param readData Readしたデータ。複数byte Readした場合は結合済
    * @param xReg Xレジスタの値
    * @param yReg Yレジスタの値
    * @return 次の処理
    */
  def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, xReg: UInt, yReg: UInt): Process
}
