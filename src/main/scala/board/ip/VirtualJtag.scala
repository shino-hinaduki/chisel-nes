package board.ip

import chisel3._

/**
  * virtual_jtag.v を chiselで取り扱うために用意した定義
  */
class VirtualJtag extends BlackBox {
  override def desiredName: String = "virtual_jtag"

  val io = IO(new Bundle {
    val tdi                = Output(Bool())     // output wire        tdi,                // jtag.tdi
    val tdo                = Input(Bool())      // input  wire        tdo,                //     .tdo
    val ir_in              = Output(UInt(24.W)) // output wire [23:0] ir_in,              //     .ir_in
    val ir_out             = Input(UInt(24.W))  // input  wire [23:0] ir_out,             //     .ir_out
    val virtual_state_cdr  = Output(Bool())     // output wire        virtual_state_cdr,  //     .virtual_state_cdr
    val virtual_state_sdr  = Output(Bool())     // output wire        virtual_state_sdr,  //     .virtual_state_sdr
    val virtual_state_e1dr = Output(Bool())     // output wire        virtual_state_e1dr, //     .virtual_state_e1dr
    val virtual_state_pdr  = Output(Bool())     // output wire        virtual_state_pdr,  //     .virtual_state_pdr
    val virtual_state_e2dr = Output(Bool())     // output wire        virtual_state_e2dr, //     .virtual_state_e2dr
    val virtual_state_udr  = Output(Bool())     // output wire        virtual_state_udr,  //     .virtual_state_udr
    val virtual_state_cir  = Output(Bool())     // output wire        virtual_state_cir,  //     .virtual_state_cir
    val virtual_state_uir  = Output(Bool())     // output wire        virtual_state_uir,  //     .virtual_state_uir
    val tck                = Output(Clock())    // output wire        tck                 //  tck.clk
  })
}
