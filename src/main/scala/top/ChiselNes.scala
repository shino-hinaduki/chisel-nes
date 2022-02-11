package top

import chisel3._
import board.BoardIO
import board.SevenSegmentLed
import chisel3.util.MuxLookup

/** Top Module
  */
class ChiselNes extends Module {
  val io = IO(new Bundle {
    // DE0-CV外部コンポーネント
    val extPort = new BoardIO()
    // TODO: PLLで生成したClock
    // TODO: その他
  })

  // unuse ports
  io.extPort.DRAM_ADDR         := 0.U
  io.extPort.DRAM_BA           := 0.U
  io.extPort.DRAM_CAS_N        := 0.U
  io.extPort.DRAM_CKE          := 0.U
  io.extPort.DRAM_CLK          := 0.U
  io.extPort.DRAM_CS_N         := 0.U
  io.extPort.DRAM_DQ_out.data  := 0.U
  io.extPort.DRAM_DQ_out.oe    := false.B
  io.extPort.DRAM_LDQM         := false.B
  io.extPort.DRAM_RAS_N        := false.B
  io.extPort.DRAM_UDQM         := false.B
  io.extPort.DRAM_WE_N         := false.B
  io.extPort.GPIO_0_out.data   := 0.U
  io.extPort.GPIO_0_out.oe     := false.B
  io.extPort.GPIO_1_out.data   := 0.U
  io.extPort.GPIO_1_out.oe     := false.B
  io.extPort.PS2_CLK_out.data  := false.B
  io.extPort.PS2_CLK_out.oe    := false.B
  io.extPort.PS2_CLK2_out.data := false.B
  io.extPort.PS2_CLK2_out.oe   := false.B
  io.extPort.PS2_DAT_out.data  := false.B
  io.extPort.PS2_DAT_out.oe    := false.B
  io.extPort.PS2_DAT2_out.data := false.B
  io.extPort.PS2_DAT2_out.oe   := false.B
  io.extPort.SD_CLK            := false.B
  io.extPort.SD_CMD_out.data   := false.B
  io.extPort.SD_CMD_out.oe     := false.B
  io.extPort.SD_DATA_out.data  := 0.U
  io.extPort.SD_DATA_out.oe    := false.B
  io.extPort.VGA_B             := 0.U
  io.extPort.VGA_G             := 0.U
  io.extPort.VGA_HS            := false.B
  io.extPort.VGA_R             := 0.U
  io.extPort.VGA_VS            := false.B

  // ボードテスト回路
  withClockAndReset(io.extPort.CLOCK_50, !io.extPort.RESET_N) {
    val counter = RegInit(UInt(64.W), 0.U)
    counter         := counter + 1.U
    io.extPort.LEDR := (1.U(10.W) << counter(23, 20))
    val digits = SevenSegmentLed.decodeNDigits(counter >> 16.U, 6, isActiveLow = true)
    io.extPort.HEX0 := digits(0)
    io.extPort.HEX1 := digits(1)
    io.extPort.HEX2 := digits(2)
    io.extPort.HEX3 := digits(3)
    io.extPort.HEX4 := digits(4)
    io.extPort.HEX5 := digits(5)
  }

  // TODO: エミュレータ自体のImpl
  // TODO: エミュレータと外部コンポーネントの接続
}

/** Generate Verilog
  */
object ChiselNesDriver extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new ChiselNes, args)
}
