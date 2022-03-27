package top

import chisel3._
import chisel3.util.Cat
import chisel3.util.MuxLookup
import chisel3.experimental.Analog

import board.discrete.SevenSegmentLed
import board.jtag.VirtualJtagBridge
import board.jtag.types.VirtualJtagIO

import board.ip.virtual_jtag
import board.ip.pll_sysclk
import board.ip.pll_vga
import board.ip.dpram_framebuffer
import board.discrete.Debounce
import board.jtag.types.VirtualInstruction
import board.access.DebugAccessTester
import board.ip.async_fifo_vjtag_to_dap
import board.ip.async_fifo_dap_to_vjtag

/** 
 * Top Module
 */
class ChiselNes extends RawModule {
  // clock
  val CLOCK_50  = IO(Input(Clock()))
  val CLOCK2_50 = IO(Input(Clock()))
  val CLOCK3_50 = IO(Input(Clock()))
  val CLOCK4_50 = IO(Input(Clock()))
  // dram
  val DRAM_ADDR  = IO(Output(UInt(13.W)))
  val DRAM_BA    = IO(Output(UInt(2.W)))
  val DRAM_CAS_N = IO(Output(Bool()))
  val DRAM_CKE   = IO(Output(Bool()))
  val DRAM_CLK   = IO(Output(Bool()))
  val DRAM_CS_N  = IO(Output(Bool()))
  val DRAM_DQ    = IO(Analog(16.W))
  val DRAM_LDQM  = IO(Output(Bool()))
  val DRAM_RAS_N = IO(Output(Bool()))
  val DRAM_UDQM  = IO(Output(Bool()))
  val DRAM_WE_N  = IO(Output(Bool()))
  // gpio
  val GPIO_0 = IO(Analog(36.W))
  val GPIO_1 = IO(Analog(36.W))
  // 7-segment led
  val HEX0 = IO(Output(UInt(7.W)))
  val HEX1 = IO(Output(UInt(7.W)))
  val HEX2 = IO(Output(UInt(7.W)))
  val HEX3 = IO(Output(UInt(7.W)))
  val HEX4 = IO(Output(UInt(7.W)))
  val HEX5 = IO(Output(UInt(7.W)))
  // push switch
  val KEY = IO(Input(UInt(4.W)))
  // led
  val LEDR = IO(Output(UInt(10.W)))
  // PS2
  val PS2_CLK  = IO(Analog(1.W))
  val PS2_CLK2 = IO(Analog(1.W))
  val PS2_DAT  = IO(Analog(1.W))
  val PS2_DAT2 = IO(Analog(1.W))
  // reset
  val RESET_N = IO(Input(Bool()))
  // sd
  val SD_CLK  = IO(Output(Bool()))
  val SD_CMD  = IO(Analog(1.W))
  val SD_DATA = IO(Analog(4.W))
  // slide sw
  val SW = IO(Input(UInt(10.W)))
  // vga
  val VGA_B  = IO(Output(UInt(4.W)))
  val VGA_G  = IO(Output(UInt(4.W)))
  val VGA_HS = IO(Output(Bool()))
  val VGA_R  = IO(Output(UInt(4.W)))
  val VGA_VS = IO(Output(Bool()))

  /**********************************************************************/
  /* Board Component & IP                                               */
  val clk50Mhz = CLOCK_50
  val reset    = !RESET_N

  // NES関連のClock
  val pllSysClk = Module(new pll_sysclk)
  val cpuClk    = pllSysClk.io.outclk_0 // 1.789709 MHz
  val ppuClk    = pllSysClk.io.outclk_0 // 5.36127 MHz
  val sysClk    = pllSysClk.io.outclk_0 // 21.616541 MHz
  val sysRst    = !pllSysClk.io.locked

  // VGA出力関連のClock
  val pllVga      = Module(new pll_vga)
  val vgaPixelClk = pllVga.io.outclk_0 // 25.175644 MHz
  val vgaRst      = !pllVga.io.locked

  /**********************************************************************/
  /* 7SEG LED                                                           */
  val sevenSegmentLed = withClockAndReset(CLOCK_50, reset) { Module(new SevenSegmentLed()) }
  val numVisibleReg   = withClockAndReset(CLOCK_50, reset) { RegInit(Bool(), true.B) }
  val numReg          = withClockAndReset(CLOCK_50, reset) { RegInit(UInt(24.W), 0.U) }
  HEX0                         := sevenSegmentLed.io.digitsOut(0)
  HEX1                         := sevenSegmentLed.io.digitsOut(1)
  HEX2                         := sevenSegmentLed.io.digitsOut(2)
  HEX3                         := sevenSegmentLed.io.digitsOut(3)
  HEX4                         := sevenSegmentLed.io.digitsOut(4)
  HEX5                         := sevenSegmentLed.io.digitsOut(5)
  sevenSegmentLed.io.isVisible := numVisibleReg
  sevenSegmentLed.io.dataIn    := numReg

  /**********************************************************************/
  /* LED Array                                                          */
  val ledArrayReg = withClockAndReset(CLOCK_50, reset) { RegInit(UInt(10.W), 0.U) }
  LEDR := ledArrayReg

  /**********************************************************************/
  /* Virtual JTAG                                                       */
  val vjtagIp           = Module(new virtual_jtag)
  val virtualJtagBridge = withClockAndReset(vjtagIp.io.tck, reset) { Module(new VirtualJtagBridge) }
  virtualJtagBridge.io.reset <> reset
  virtualJtagBridge.io.vjtag <> vjtagIp.io
  // virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.accessTest.litValue.toInt) <> DontCare
  virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.cpu.litValue.toInt) <> DontCare
  virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.ppu.litValue.toInt) <> DontCare
  virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.apu.litValue.toInt) <> DontCare
  virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.cart.litValue.toInt) <> DontCare
  virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.frameBuffer.litValue.toInt) <> DontCare
  virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.cpuBusMaster.litValue.toInt) <> DontCare
  virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.ppuBusMaster.litValue.toInt) <> DontCare
  virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.audio.litValue.toInt) <> DontCare

  /**********************************************************************/
  /* Debug Access Tester                                                */
  val debugAccessTester = withClockAndReset(CLOCK_50, reset) { Module(new DebugAccessTester()) }

  // TODO: 接続しやすいUtilを用意する...。
  val vjtagToDatQueue = Module(new async_fifo_vjtag_to_dap)
  val datToVjtagQueue = Module(new async_fifo_dap_to_vjtag)
  // debug: queue remain
  // vjtagToDatQueue.io.wrusedw <> DontCare
  // datToVjtagQueue.io.rdusedw <> DontCare
  // req: vjtag -> fifo
  vjtagToDatQueue.io.data <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.accessTest.litValue.toInt).req.data
  vjtagToDatQueue.io.wrclk <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.accessTest.litValue.toInt).req.wrclk
  vjtagToDatQueue.io.wrreq <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.accessTest.litValue.toInt).req.wrreq
  vjtagToDatQueue.io.wrfull <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.accessTest.litValue.toInt).req.wrfull
  // req: fifo -> dat
  vjtagToDatQueue.io.rdclk <> debugAccessTester.io.debugAccess.req.rdclk
  vjtagToDatQueue.io.rdreq <> debugAccessTester.io.debugAccess.req.rdreq
  vjtagToDatQueue.io.q <> debugAccessTester.io.debugAccess.req.q
  vjtagToDatQueue.io.rdempty <> debugAccessTester.io.debugAccess.req.rdempty
  // resp: dat -> fifo
  datToVjtagQueue.io.data <> debugAccessTester.io.debugAccess.resp.data
  datToVjtagQueue.io.wrclk <> debugAccessTester.io.debugAccess.resp.wrclk
  datToVjtagQueue.io.wrreq <> debugAccessTester.io.debugAccess.resp.wrreq
  datToVjtagQueue.io.wrfull <> debugAccessTester.io.debugAccess.resp.wrfull
  // resp: fifo -> vjtag
  datToVjtagQueue.io.rdclk <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.accessTest.litValue.toInt).resp.rdclk
  datToVjtagQueue.io.rdreq <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.accessTest.litValue.toInt).resp.rdreq
  datToVjtagQueue.io.q <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.accessTest.litValue.toInt).resp.q
  datToVjtagQueue.io.rdempty <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.accessTest.litValue.toInt).resp.rdempty

  /**********************************************************************/
  /* VGA Output                                                         */
  // Framebuffer, 本家仕様の通りDoubleBufferにはしない
  // val frameBuffer = Module(new dpram_framebuffer)

  /**********************************************************************/
  /* Test                                                               */
  // クロック跨いでるけどテスト回路だからいいとする...
  when(SW === 0.U) {
    //7segにカウンタの値を出す
    withClockAndReset(CLOCK_50, reset) {
      val counter = RegInit(UInt(64.W), 0.U)
      counter := counter + 1.U
      // カウンタの値そのまま
      numReg        := (counter >> 16.U)
      numVisibleReg := true.B
      // 流れる感じにする
      ledArrayReg := (1.U(10.W) << counter(23, 20))
    }
  }.elsewhen(SW === 1.U) {
    // 7segにVJTAGのVIRの値を出す
    withClockAndReset(vjtagIp.io.tck, reset) {
      // ir_inの値そのまま
      numReg        := vjtagIp.io.ir_in
      numVisibleReg := true.B
      // VJTAGのstate
      ledArrayReg := Cat(
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
  }.elsewhen(SW === 2.U) {
    // 7segにDebugAccessTesterのカウンタ値を出す
    withClockAndReset(CLOCK_50, reset) {
      // ir_inの値そのまま
      numReg        := debugAccessTester.io.debugCounter
      numVisibleReg := true.B
      // 適当に流れるようにする
      ledArrayReg := (1.U(10.W) << debugAccessTester.io.debugCounter(3, 0))
    }
  }.otherwise {
    // 7seg消灯。SWの値をそのままLEDRに出す
    withClockAndReset(CLOCK_50, reset) {
      // 消灯
      numReg        := 0x123456.U
      numVisibleReg := false.B
      // SlideSWの値そのまま
      ledArrayReg := SW
    }
  }

  // TODO: エミュレータ自体のImpl
  // TODO: エミュレータと外部コンポーネントの接続

  // unuse ports
  DRAM_ADDR  := 0.U
  DRAM_BA    := 0.U
  DRAM_CAS_N := 0.U
  DRAM_CKE   := 0.U
  DRAM_CLK   := 0.U
  DRAM_CS_N  := 0.U
  DRAM_DQ    := DontCare
  DRAM_LDQM  := false.B
  DRAM_RAS_N := false.B
  DRAM_UDQM  := false.B
  DRAM_WE_N  := false.B
  GPIO_0     := DontCare
  GPIO_1     := DontCare
  PS2_CLK    := DontCare
  PS2_CLK2   := DontCare
  PS2_DAT    := DontCare
  PS2_DAT2   := DontCare
  SD_CLK     := false.B
  SD_CMD     := DontCare
  SD_DATA    := DontCare
  VGA_B      := 0.U
  VGA_G      := 0.U
  VGA_HS     := false.B
  VGA_R      := 0.U
  VGA_VS     := false.B
}

/** Generate Verilog
  */
object ChiselNesDriver extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new ChiselNes, args)
}
