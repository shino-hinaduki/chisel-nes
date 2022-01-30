package cpu.types

import chisel3._
import chisel3.experimental.ChiselEnum

/** Stage間で伝達するときの属性
  */
class Attribute extends Bundle {
  // 有効ならtrue
  val isValid = Bool()
  // Fetchしたアドレス
  val fetchAddr = UInt(16.W)
  // Fetchしたデータ
  val fetchData = UInt(8.W)
  // Fetchした命令
  val instruction = Instruction()
  // アドレッシングモード
  val addressing = Addressing()
  // Operand0（取得していなければ無効値)
  val operand0 = UInt(8.W)
  // Operand1（取得していなければ無効値)
  val operand1 = UInt(8.W)
}
