package board.ip

import chisel3._
import board.ram.types.DualPortRam
import board.ram.types.RamIO

/**
  * dpram_framebuffer.v を chiselで取り扱うために用意した定義
  */
class DualPortRamFrameBuffer extends BlackBox with DualPortRam {
  override def desiredName: String = "dpram_framebuffer"

  val io = IO(new Bundle {
    val address_a = Input(UInt(16.W))  // input	[15:0]  address_a;
    val address_b = Input(UInt(16.W))  // input	[15:0]  address_b;
    val clock_a   = Input(Clock())     // input	  clock_a;
    val clock_b   = Input(Clock())     // input	  clock_b;
    val data_a    = Input(UInt(24.W))  // input	[23:0]  data_a;
    val data_b    = Input(UInt(24.W))  // input	[23:0]  data_b;
    val rden_a    = Input(Bool())      // input	  rden_a;
    val rden_b    = Input(Bool())      // input	  rden_b;
    val wren_a    = Input(Bool())      // input	  wren_a;
    val wren_b    = Input(Bool())      // input	  wren_b;
    val q_a       = Output(UInt(24.W)) // output	[23:0]  q_a;
    val q_b       = Output(UInt(24.W)) // output	[23:0]  q_b;
  })

  override def connectToA(ram: RamIO): Unit = {
    io.address_a <> ram.address
    io.clock_a <> ram.clock
    io.data_a <> ram.data
    io.rden_a <> ram.rden
    io.wren_a <> ram.wren
    io.q_a <> ram.q
  }

  override def connectToB(ram: RamIO): Unit = {
    io.address_b <> ram.address
    io.clock_b <> ram.clock
    io.data_b <> ram.data
    io.rden_b <> ram.rden
    io.wren_b <> ram.wren
    io.q_b <> ram.q
  }

}
