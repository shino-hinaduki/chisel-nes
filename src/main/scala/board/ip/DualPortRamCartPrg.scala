package board.ip

import chisel3._

/**
  * dpram_cart_prt.v を chiselで取り扱うために用意した定義
  */
class dpram_cart_prg extends BlackBox {
  val io = IO(new Bundle {
    val address_a = Input(UInt(17.W)) // input	[16:0]  address_a;
    val address_b = Input(UInt(17.W)) // input	[16:0]  address_b;
    val clock_a   = Input(Clock())    // input	  clock_a;
    val clock_b   = Input(Clock())    // input	  clock_b;
    val data_a    = Input(UInt(8.W))  // input	[7:0]  data_a;
    val data_b    = Input(UInt(8.W))  // input	[7:0]  data_b;
    val rden_a    = Input(Bool())     // input	  rden_a;
    val rden_b    = Input(Bool())     // input	  rden_b;
    val wren_a    = Input(Bool())     // input	  wren_a;
    val wren_b    = Input(Bool())     // input	  wren_b;
    val q_a       = Output(UInt(8.W)) // output	[7:0]  q_a;
    val q_b       = Output(UInt(8.W)) // output	[7:0]  q_b;
  })
}
