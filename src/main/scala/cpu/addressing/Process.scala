package cpu.addressing

import chisel3._
import _root_.cpu.types.Addressing
import cpu.register.CpuRegister

/**
  * AddressingImplが行う処理を定義する
  */
sealed trait Process {}

object Process {

  /**
  * OFを初期状態に戻す
  * @param isIllegal 想定外の挙動でResetする場合はtrue
  */
  case class Clear(isIllegal: Boolean) extends Process

  /**
  * BusMasterを使ってReadを要求する, 要求するのは命令直後のデータ
  * @param addr Read先アドレス, 2byte
  * @param length Read byte数
  */
  case class ReadOperand(addr: UInt, length: UInt) extends Process

  /**
  * BusMasterを使ってReadを要求する, 対象はアドレス(Indirect向け)
  * @param addr Read先アドレス, 2byte
  * @param length Read byte数
  */
  case class ReadPointer(addr: UInt, length: UInt) extends Process

  /**
  * BusMasterを使ってReadを要求する, 対象はデータ
  * @param addr Read先アドレス, 2byte
  * @param length Read byte数
  */
  case class ReadData(addr: UInt, length: UInt) extends Process

  /**
  * データを読み出さず、アドレスだけを結果として報告する
  * @param addr 解決したアドレス, 2byte
  */
  case class ReportAddr(addr: UInt) extends Process

  /**
  * データのみを報告する
  * @param data 報告するデータ, 1byte
  */
  case class ReportData(data: UInt) extends Process

  /**
  * データを読み出し、アドレスとともに報告する
  * @param addr 解決したアドレス, 2byte
  * @param data 読みだしたデータ, 1byte
  */
  case class ReportFull(addr: UInt, data: UInt) extends Process

  /**
  * データを読み出し、アドレスとともに報告するが、Validにするまでに1cyc遅らせる
  * @param addr 解決したアドレス, 2byte
  * @param data 読みだしたデータ, 1byte
  */
  case class ReportFullWithDelay(addr: UInt, data: UInt) extends Process

  /**
  * 完了したことだけ報告する(Impliedなど)
  */
  case class ReportNone() extends Process
}
