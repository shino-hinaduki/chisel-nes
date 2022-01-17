package bus

import chisel3._

class CpuBus extends Bundle {
  val addr    = Input(UInt(8.W))
  val dataIn  = Input(UInt(8.W))
  val dataOut = Output(UInt(8.W))
  val wen     = Input(Bool())
}
