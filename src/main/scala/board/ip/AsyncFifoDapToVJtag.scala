package board.ip

import chisel3._

/**
  * async_fifo_dap_to_vjtag.v を chiselで取り扱うために用意した定義
  */
class async_fifo_dap_to_vjtag extends BlackBox {
  val io = IO(new Bundle {
    val aclr    = Input(Bool())     // input	  aclr;
    val data    = Input(UInt(8.W))  // input	[7:0]  data;
    val rdclk   = Input(Clock())    // input	  rdclk;
    val rdreq   = Input(Bool())     // input	  rdreq;
    val wrclk   = Input(Clock())    // input	  wrclk;
    val wrreq   = Input(Bool())     // input	  wrreq;
    val q       = Output(UInt(8.W)) // output	[7:0]  q;
    val rdempty = Output(Bool())    // output	  rdempty;
    val rdusedw = Output(UInt(8.W)) // output	[7:0]  rdusedw;
    val wrfull  = Output(Bool())    // output	  wrfull;
  })
}
