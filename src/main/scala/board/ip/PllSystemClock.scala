package board.ip

import chisel3._

/**
  * pll_sysclk.v を chiselで取り扱うために用意した定義
  */
class pll_sysclk extends BlackBox {
  val io = IO(new Bundle {
    val refclk   = Input(Clock())  // input  wire  refclk,   //  refclk.clk // 50.0MHz
    val rst      = Input(Bool())   // input  wire  rst,      //   reset.reset
    val outclk_0 = Output(Clock()) // output wire  outclk_0, // outclk0.clk // 1.789709 MHz
    val outclk_1 = Output(Clock()) // output wire  outclk_1, // outclk1.clk // 5.36127 MHz
    val outclk_2 = Output(Clock()) // output wire  outclk_2, // outclk2.clk // 21.616541MHz
    val locked   = Output(Bool())  // output wire  locked    //  locked.export
  })
}
