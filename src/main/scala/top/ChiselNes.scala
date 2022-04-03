package top

import chisel3._
import chisel3.util.Cat
import chisel3.util.MuxLookup
import chisel3.experimental.Analog

import board.ip._

import board.discrete.SevenSegmentLed
import board.jtag.VirtualJtagBridge
import board.jtag.types.VirtualJtagIO

import board.discrete.Debounce
import board.jtag.types.VirtualInstruction
import board.access.DebugAccessTester
import board.video.VgaOut

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
  val clk50Mhz  = CLOCK_50  // Bank 3B
  val clk50Mhz2 = CLOCK2_50 // Bank 7A
  val clk50Mhz3 = CLOCK3_50 // Bank 8A
  val clk50Mhz4 = CLOCK4_50 // Bank 4A
  val reset     = !RESET_N

  // NES関連のClock
  val pllSysClk = Module(new PllSystemClock)
  val cpuClk    = pllSysClk.io.outclk_0 // 1.789709 MHz
  val ppuClk    = pllSysClk.io.outclk_0 // 5.36127 MHz
  val sysClk    = pllSysClk.io.outclk_0 // 21.616541 MHz
  pllSysClk.io.refclk := clk50Mhz
  pllSysClk.io.rst    := reset

  // VGA出力関連のClock
  val pllVga      = Module(new PllVga)
  val vgaPixelClk = pllVga.io.outclk_0 // 25.175644 MHz
  pllVga.io.refclk := clk50Mhz3 // VGA関連の端子が8Aなので、近いCLK入力を使う
  pllVga.io.rst    := reset

  /**********************************************************************/
  /* 7SEG LED                                                           */
  val sevenSegmentLed = withClockAndReset(clk50Mhz, reset) { Module(new SevenSegmentLed()) }
  val numVisibleReg   = withClockAndReset(clk50Mhz, reset) { RegInit(Bool(), true.B) }
  val numReg          = withClockAndReset(clk50Mhz, reset) { RegInit(UInt(24.W), 0.U) }
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
  val ledArrayReg = withClockAndReset(clk50Mhz, reset) { RegInit(UInt(10.W), 0.U) }
  LEDR := ledArrayReg

  /**********************************************************************/
  /* Virtual JTAG                                                       */
  val vjtag             = Module(new VirtualJtag)
  val virtualJtagBridge = withClockAndReset(vjtag.io.tck, reset) { Module(new VirtualJtagBridge) }
  virtualJtagBridge.io.reset <> reset
  virtualJtagBridge.io.vjtag <> vjtag.io
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
  val debugAccessTester = withClockAndReset(clk50Mhz, reset) { Module(new DebugAccessTester()) }

  // TODO: 接続しやすいUtilを用意する...。
  val vjtagToDatQueue = Module(new AsyncFifoVJtagToDap)
  val datToVjtagQueue = Module(new AsyncFifoDapToVJtag)
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
  val vgaOut = withClockAndReset(ppuClk, reset) { Module(new VgaOut()) }

  // 端子出力
  VGA_R  := vgaOut.io.videoOut.r(7, 4)
  VGA_G  := vgaOut.io.videoOut.g(7, 4)
  VGA_B  := vgaOut.io.videoOut.b(7, 4)
  VGA_HS := vgaOut.io.videoOut.hsync
  VGA_VS := vgaOut.io.videoOut.vsync

  // 制御関連
  vgaOut.io.isDebug         := SW(9) // for Debug
  vgaOut.io.pixelClock      := vgaPixelClk
  vgaOut.io.pixelClockReset := reset

  // FrameBufferと接続
  val frameBuffer = Module(new DualPortRamFrameBuffer)
  frameBuffer.io.address_a <> vgaOut.io.frameBuffer.ppu.address
  frameBuffer.io.clock_a <> vgaOut.io.frameBuffer.ppu.clock
  frameBuffer.io.data_a <> vgaOut.io.frameBuffer.ppu.data
  frameBuffer.io.rden_a <> vgaOut.io.frameBuffer.ppu.rden
  frameBuffer.io.wren_a <> vgaOut.io.frameBuffer.ppu.wren
  frameBuffer.io.q_a <> vgaOut.io.frameBuffer.ppu.q
  frameBuffer.io.address_b <> vgaOut.io.frameBuffer.vga.address
  frameBuffer.io.clock_b <> vgaOut.io.frameBuffer.vga.clock
  frameBuffer.io.data_b <> vgaOut.io.frameBuffer.vga.data
  frameBuffer.io.rden_b <> vgaOut.io.frameBuffer.vga.rden
  frameBuffer.io.wren_b <> vgaOut.io.frameBuffer.vga.wren
  frameBuffer.io.q_b <> vgaOut.io.frameBuffer.vga.q

  // TODO: PPUと接続
  vgaOut.io.ppuVideo.valid := false.B
  vgaOut.io.ppuVideo.r     := DontCare
  vgaOut.io.ppuVideo.g     := DontCare
  vgaOut.io.ppuVideo.b     := DontCare
  vgaOut.io.ppuVideo.x     := DontCare
  vgaOut.io.ppuVideo.y     := DontCare

  // TODO: 接続しやすいUtilを用意する...。
  val vjtagToFbQueue = Module(new AsyncFifoVJtagToDap)
  val fbToVjtagQueue = Module(new AsyncFifoDapToVJtag)

  // debug: queue remain
  // vjtagToFbQueue.io.wrusedw <> DontCare
  // fbToVjtagQueue.io.rdusedw <> DontCare
  // req: vjtag -> fifo
  vjtagToFbQueue.io.data <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.frameBuffer.litValue.toInt).req.data
  vjtagToFbQueue.io.wrclk <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.frameBuffer.litValue.toInt).req.wrclk
  vjtagToFbQueue.io.wrreq <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.frameBuffer.litValue.toInt).req.wrreq
  vjtagToFbQueue.io.wrfull <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.frameBuffer.litValue.toInt).req.wrfull
  // req: fifo -> fb
  vjtagToFbQueue.io.rdclk <> vgaOut.io.debugAccess.req.rdclk
  vjtagToFbQueue.io.rdreq <> vgaOut.io.debugAccess.req.rdreq
  vjtagToFbQueue.io.q <> vgaOut.io.debugAccess.req.q
  vjtagToFbQueue.io.rdempty <> vgaOut.io.debugAccess.req.rdempty
  // resp: fb -> fifo
  fbToVjtagQueue.io.data <> vgaOut.io.debugAccess.resp.data
  fbToVjtagQueue.io.wrclk <> vgaOut.io.debugAccess.resp.wrclk
  fbToVjtagQueue.io.wrreq <> vgaOut.io.debugAccess.resp.wrreq
  fbToVjtagQueue.io.wrfull <> vgaOut.io.debugAccess.resp.wrfull
  // resp: fifo -> vjtag
  fbToVjtagQueue.io.rdclk <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.frameBuffer.litValue.toInt).resp.rdclk
  fbToVjtagQueue.io.rdreq <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.frameBuffer.litValue.toInt).resp.rdreq
  fbToVjtagQueue.io.q <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.frameBuffer.litValue.toInt).resp.q
  fbToVjtagQueue.io.rdempty <> virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.frameBuffer.litValue.toInt).resp.rdempty

  /**********************************************************************/
  /* Test                                                               */
  // クロック跨いでるけどテスト回路だからいいとする...
  when(SW === 0.U) {
    //7segにカウンタの値を出す
    withClockAndReset(clk50Mhz, reset) {
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
    withClockAndReset(vjtag.io.tck, reset) {
      // ir_inの値そのまま
      numReg        := vjtag.io.ir_in
      numVisibleReg := true.B
      // VJTAGのstate
      ledArrayReg := Cat(
        vjtag.io.virtual_state_cdr,
        vjtag.io.virtual_state_sdr,
        vjtag.io.virtual_state_e1dr,
        vjtag.io.virtual_state_pdr,
        vjtag.io.virtual_state_e2dr,
        vjtag.io.virtual_state_udr,
        vjtag.io.virtual_state_cir,
        vjtag.io.virtual_state_uir,
        vjtag.io.tdi,
        vjtag.io.tck.asBool
      )
    }
  }.elsewhen(SW === 2.U) {
    // 7segにDebugAccessTesterのカウンタ値を出す
    withClockAndReset(clk50Mhz, reset) {
      // ir_inの値そのまま
      numReg        := debugAccessTester.io.debugCounter
      numVisibleReg := true.B
      // 適当に流れるようにする
      ledArrayReg := (1.U(10.W) << debugAccessTester.io.debugCounter(3, 0))
    }
  }.elsewhen(SW === 3.U) {
    // 7segにDebugAccessTesterの最後のオフセット値を出す
    withClockAndReset(clk50Mhz, reset) {
      // ir_inの値そのまま
      numReg        := debugAccessTester.io.debugLatestOffset
      numVisibleReg := true.B
      // 適当に流れるようにする
      ledArrayReg := (1.U(10.W) << debugAccessTester.io.debugLatestOffset(3, 0))
    }
  }.otherwise {
    // 7seg消灯。SWの値をそのままLEDRに出す
    withClockAndReset(clk50Mhz, reset) {
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
}

/** Generate Verilog
  */
object ChiselNesDriver extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new ChiselNes, args)
}
