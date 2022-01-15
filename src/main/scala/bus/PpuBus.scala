package bus

import chisel3._

class PpuBus extends Bundle {
  val addr = Input(UInt(16.W))
  val dataIn = Input(UInt(8.W))
  val dataOut = Output(UInt(8.W))
  val wen = Input(Bool())
}
