package cpu.register

import chisel3._

/** CPU Register
  */
class CpuRegister extends Module {
  val io = IO(new Bundle {
    val a  = UInt(8.W)
    val x  = UInt(8.W)
    val y  = UInt(8.W)
    val pc = UInt(16.W)
    val sp = UInt(16.W)
    val p  = new ProcessorStatus
  })
}
