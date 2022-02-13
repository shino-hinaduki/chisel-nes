package cpu.register

import chisel3._

/**
  * X,YいずれかのIndex Registerを示す
  */
sealed trait IndexRegister {}
object IndexRegister {
  case class X() extends IndexRegister
  case class Y() extends IndexRegister
}

/** 
 * CPU Register
 */
class CpuRegister extends Bundle {
  val a  = UInt(8.W)
  val x  = UInt(8.W)
  val y  = UInt(8.W)
  val pc = UInt(16.W)
  val sp = UInt(8.W)
  val p  = new ProcessorStatus
}
