package board.ip

import chisel3._

/**
  * pll_vga.v を chiselで取り扱うために用意した定義
  */
class PllVga extends BlackBox {
  override def desiredName: String = "pll_vga"

  val io = IO(new Bundle {
    val refclk   = Input(Clock())  // input  wire  refclk,   //  refclk.clk // 50.0MHz
    val rst      = Input(Bool())   // input  wire  rst,      //   reset.reset
    val outclk_0 = Output(Clock()) // output wire  outclk_0, // outclk0.clk // 25.175644 MHz
    val locked   = Output(Bool())  // output wire  locked    //  locked.export
  })
}
