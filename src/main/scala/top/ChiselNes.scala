package top

import chisel3._
import chisel3.util.Cat
import chisel3.util.MuxLookup

import board.BoardIO
import board.SevenSegmentLed
import board.VirtualJtagBridge
import board.types.VirtualJtagIO

import support.DebugAccessPort
import board.ip.virtual_jtag

/** 
 * Top Module
 */
class ChiselNes extends RawModule {
  val io = IO(new Bundle {
    // DE0-CV外部コンポーネント
    val extPort = new BoardIO() // TODO: inoutをどうにかして、prefixを消せる...?
  })

  // Board defaultのclock/reset
  val clk50Mhz = io.extPort.CLOCK_50
  val rst      = !io.extPort.RESET_N

  // Virtual JTAG & DebugAccessPort
  val vjtagIp           = Module(new virtual_jtag)
  val virtualJtagBridge = Module(new VirtualJtagBridge)
  virtualJtagBridge.io.rst <> rst
  virtualJtagBridge.io.vjtag <> vjtagIp.io

  // クロック跨いでるけどテスト回路だからいいとする...
  when(io.extPort.SW === 0.U) {
    // ボードテスト回路(7segにVJTAGのVIRの値を出す)
    withClockAndReset(vjtagIp.io.tck, rst) {
      // ir_inの値そのまま
      val digits = SevenSegmentLed.decodeNDigits(vjtagIp.io.ir_in, 6, isActiveLow = true)
      io.extPort.HEX0 := digits(0)
      io.extPort.HEX1 := digits(1)
      io.extPort.HEX2 := digits(2)
      io.extPort.HEX3 := digits(3)
      io.extPort.HEX4 := digits(4)
      io.extPort.HEX5 := digits(5)

      // VJTAGのstate
      io.extPort.LEDR := Cat(
        vjtagIp.io.virtual_state_cdr,
        vjtagIp.io.virtual_state_sdr,
        vjtagIp.io.virtual_state_e1dr,
        vjtagIp.io.virtual_state_pdr,
        vjtagIp.io.virtual_state_e2dr,
        vjtagIp.io.virtual_state_udr,
        vjtagIp.io.virtual_state_cir,
        vjtagIp.io.virtual_state_uir,
        vjtagIp.io.tdi,
        vjtagIp.io.tck.asBool
      )
    }
  }.otherwise {
    // ボードテスト回路(7segにカウンタの値を出す)
    withClockAndReset(io.extPort.CLOCK_50, rst) {
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
  }

  // TODO: エミュレータ自体のImpl
  // TODO: エミュレータと外部コンポーネントの接続

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
}

/** Generate Verilog
  */
object ChiselNesDriver extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new ChiselNes, args)
}
