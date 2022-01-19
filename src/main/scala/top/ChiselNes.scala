package top

import chisel3._

/** Top Module
  */
class ChiselNes extends Module {
  val io = IO(new Bundle {})

  // TODO:
}

/** Generate Verilog
  */
object ChiselNesDriver extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new ChiselNes, args)
}
