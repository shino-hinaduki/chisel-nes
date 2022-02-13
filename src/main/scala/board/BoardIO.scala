package board

import chisel3._
import common.TriState

/** 
  * DE0-CVのボード定義
  * 他のボードへのポーティングが必要であれば、Config等でtop自体を切り替える必要がある
  */
class BoardIO extends Bundle {
  // clock
  val CLOCK_50  = Input(Clock())
  val CLOCK2_50 = Input(Clock())
  val CLOCK3_50 = Input(Clock())
  val CLOCK4_50 = Input(Clock())
// dram
  val DRAM_ADDR   = Output(UInt(13.W))
  val DRAM_BA     = Output(UInt(2.W))
  val DRAM_CAS_N  = Output(Bool())
  val DRAM_CKE    = Output(Bool())
  val DRAM_CLK    = Output(Bool())
  val DRAM_CS_N   = Output(Bool())
  val DRAM_DQ_in  = Input(UInt(16.W))
  val DRAM_DQ_out = Output(TriState(UInt(16.W)))
  val DRAM_LDQM   = Output(Bool())
  val DRAM_RAS_N  = Output(Bool())
  val DRAM_UDQM   = Output(Bool())
  val DRAM_WE_N   = Output(Bool())
  // gpio
  val GPIO_0_in  = Input(UInt(36.W))
  val GPIO_0_out = Output(TriState(UInt(36.W)))
  val GPIO_1_in  = Input(UInt(36.W))
  val GPIO_1_out = Output(TriState(UInt(36.W)))
  // 7-segment led
  val HEX0 = Output(UInt(7.W))
  val HEX1 = Output(UInt(7.W))
  val HEX2 = Output(UInt(7.W))
  val HEX3 = Output(UInt(7.W))
  val HEX4 = Output(UInt(7.W))
  val HEX5 = Output(UInt(7.W))
  // push switch
  val KEY = Input(UInt(4.W))
  // led
  val LEDR = Output(UInt(10.W))
  // PS2
  val PS2_CLK_in   = Input(Bool())
  val PS2_CLK_out  = Output(TriState(Bool()))
  val PS2_CLK2_in  = Input(Bool())
  val PS2_CLK2_out = Output(TriState(Bool()))
  val PS2_DAT_in   = Input(Bool())
  val PS2_DAT_out  = Output(TriState(Bool()))
  val PS2_DAT2_in  = Input(Bool())
  val PS2_DAT2_out = Output(TriState(Bool()))
  // reset
  val RESET_N = Input(Bool())
  // sd
  val SD_CLK      = Output(Bool())
  val SD_CMD_in   = Input(Bool())
  val SD_CMD_out  = Output(TriState(Bool()))
  val SD_DATA_in  = Input(UInt(4.W))
  val SD_DATA_out = Output(TriState(UInt(4.W)))
  // slide sw
  val SW = Input(UInt(10.W))
  // vga
  val VGA_B  = Output(UInt(4.W))
  val VGA_G  = Output(UInt(4.W))
  val VGA_HS = Output(Bool())
  val VGA_R  = Output(UInt(4.W))
  val VGA_VS = Output(Bool())
}
