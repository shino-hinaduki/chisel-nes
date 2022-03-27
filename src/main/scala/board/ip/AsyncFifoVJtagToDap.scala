package board.ip

import chisel3._

/**
  * async_fifo_vjtag_to_dap.v を chiselで取り扱うために用意した定義
  */
class async_fifo_vjtag_to_dap extends BlackBox {
  val dataWidth   = 64
  val userdwWidth = 2

  val io = IO(new Bundle {
    val aclr    = Input(Bool())               // input	  aclr;
    val data    = Input(UInt(dataWidth.W))    // input	[63:0]  data;
    val rdclk   = Input(Clock())              // input	  rdclk;
    val rdreq   = Input(Bool())               // input	  rdreq;
    val wrclk   = Input(Clock())              // input	  wrclk;
    val wrreq   = Input(Bool())               // input	  wrreq;
    val q       = Output(UInt(dataWidth.W))   // output	[63:0]  q;
    val rdempty = Output(Bool())              // output	  rdempty;
    val wrfull  = Output(Bool())              // output	  wrfull;
    val wrusedw = Output(UInt(userdwWidth.W)) // output	[1:0]  wrusedw;
  })
}
