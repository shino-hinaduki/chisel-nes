package board.ip

import chisel3._

/**
  * async_fifo_dap_to_vjtag.v を chiselで取り扱うために用意した定義
  */
class AsyncFifoDapToVJtag extends BlackBox {
  override def desiredName: String = "async_fifo_dap_to_vjtag"
  val dataWidth                    = 32
  val userdwWidth                  = 2

  val io = IO(new Bundle {
    val aclr    = Input(Bool())               // input	  aclr;
    val data    = Input(UInt(dataWidth.W))    // input	[31:0]  data;
    val rdclk   = Input(Clock())              // input	  rdclk;
    val rdreq   = Input(Bool())               // input	  rdreq;
    val wrclk   = Input(Clock())              // input	  wrclk;
    val wrreq   = Input(Bool())               // input	  wrreq;
    val q       = Output(UInt(dataWidth.W))   // output	[31:0]  q;
    val rdempty = Output(Bool())              // output	  rdempty;
    val rdusedw = Output(UInt(userdwWidth.W)) // output	[1:0]  rdusedw;
    val wrfull  = Output(Bool())              // output	  wrfull;
  })
}
