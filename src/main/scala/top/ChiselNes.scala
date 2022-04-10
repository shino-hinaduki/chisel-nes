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
import board.gpio.GpioMapping
import board.cart.CartridgeHub
import board.cart.VirtualCartridge

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
  /* Unuse ports                                                        */
  DRAM_ADDR  := 0.U
  DRAM_BA    := 0.U
  DRAM_CAS_N := true.B
  DRAM_CKE   := 0.U
  DRAM_CLK   := 0.U
  DRAM_CS_N  := true.B
  DRAM_DQ    := DontCare
  DRAM_LDQM  := false.B
  DRAM_RAS_N := false.B
  DRAM_UDQM  := false.B
  DRAM_WE_N  := true.B

  PS2_CLK  := DontCare
  PS2_CLK2 := DontCare
  PS2_DAT  := DontCare
  PS2_DAT2 := DontCare

  SD_CLK  := false.B
  SD_CMD  := DontCare
  SD_DATA := DontCare

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

  // 7SEG LED
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

  // LED Array
  val ledArrayReg = withClockAndReset(clk50Mhz, reset) { RegInit(UInt(10.W), 0.U) }
  LEDR := ledArrayReg

  /**********************************************************************/
  /* Virtual JTAG                                                       */
  val vjtag             = Module(new VirtualJtag)
  val virtualJtagBridge = withClockAndReset(vjtag.io.tck, reset) { Module(new VirtualJtagBridge) }
  virtualJtagBridge.io.reset := reset
  virtualJtagBridge.io.vjtag <> vjtag.io

  // TODO: 接続したら削除
  virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.cpu.litValue.toInt) <> DontCare
  virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.ppu.litValue.toInt) <> DontCare
  virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.apu.litValue.toInt) <> DontCare
  virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.cpuBusMaster.litValue.toInt) <> DontCare
  virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.ppuBusMaster.litValue.toInt) <> DontCare
  virtualJtagBridge.io.debugAccessQueues(VirtualInstruction.AccessTarget.audio.litValue.toInt) <> DontCare

  /**********************************************************************/
  /* Virtual Cartridge & Hub                                            */
  // Cartridge切り替え用Hub
  val cartHub = Module(new CartridgeHub)
  cartHub.io.isUseGpio := SW(9) // 一番左端のスライドスイッチ

  // TODO: Emu本体と接続
  cartHub.io.emuToHub.cpu.address      := 0.U
  cartHub.io.emuToHub.cpu.dataOut.data := 0.U
  cartHub.io.emuToHub.cpu.dataOut.oe   := false.B
  cartHub.io.emuToHub.cpu.isRead       := false.B
  cartHub.io.emuToHub.cpu.isNotRomSel  := true.B
  cartHub.io.emuToHub.cpu.o2           := cpuClk // TODO: cpuClkから位相シフトが必要, Mapper次第で調整
  cartHub.io.emuToHub.ppu.address      := 0.U
  cartHub.io.emuToHub.ppu.dataOut.data := 0.U
  cartHub.io.emuToHub.ppu.dataOut.oe   := false.B
  cartHub.io.emuToHub.ppu.vrama10      := false.B
  cartHub.io.emuToHub.ppu.isNotRead    := true.B
  cartHub.io.emuToHub.ppu.isNotWrite   := true.B

  // 仮想Cartridge本体
  val virtualCart = withClockAndReset(sysClk, reset) { Module(new VirtualCartridge(prgRomAddrWidth = 17, chrRomAddrWidth = 17)) }
  virtualCart.io.cart <> cartHub.io.hubToVirtual
  virtualCart.io.isEnable := cartHub.io.isEnableVirtual
  virtualCart.io.ppuClock := ppuClk
  virtualCart.io.ppuReset := reset

  // VJTAG経由で読み書きできるように
  val vjtagToVCartCommonQueue = Module(new AsyncFifoVJtagToDap)
  val vCartCommonToVjtagQueue = Module(new AsyncFifoDapToVJtag)
  virtualJtagBridge.io
    .debugAccessQueues(VirtualInstruction.AccessTarget.cartCommon.litValue.toInt)
    .connect(virtualCart.io.debugAccessCommon, vjtagToVCartCommonQueue, vCartCommonToVjtagQueue)
  val vjtagToVCartPrgQueue = Module(new AsyncFifoVJtagToDap)
  val vCartPrgToVjtagQueue = Module(new AsyncFifoDapToVJtag)
  virtualJtagBridge.io
    .debugAccessQueues(VirtualInstruction.AccessTarget.cartPrg.litValue.toInt)
    .connect(virtualCart.io.debugAccessPrg, vjtagToVCartPrgQueue, vCartPrgToVjtagQueue)
  val vjtagToVCartSaveQueue = Module(new AsyncFifoVJtagToDap)
  val vCartSaveToVjtagQueue = Module(new AsyncFifoDapToVJtag)
  virtualJtagBridge.io
    .debugAccessQueues(VirtualInstruction.AccessTarget.cartSave.litValue.toInt)
    .connect(virtualCart.io.debugAccessSave, vjtagToVCartSaveQueue, vCartSaveToVjtagQueue)
  val vjtagToVCartChrQueue = Module(new AsyncFifoVJtagToDap)
  val vCartChrToVjtagQueue = Module(new AsyncFifoDapToVJtag)
  virtualJtagBridge.io
    .debugAccessQueues(VirtualInstruction.AccessTarget.cartChr.litValue.toInt)
    .connect(virtualCart.io.debugAccessChr, vjtagToVCartChrQueue, vCartChrToVjtagQueue)

  // DPRAMと接続
  val vcartPrgRam = Module(new DualPortRamCartPrg)
  vcartPrgRam.connectToA(virtualCart.io.prgRamEmu)
  vcartPrgRam.connectToB(virtualCart.io.prgRamDebug)

  val vcartSaveRam = Module(new DualPortRamCartSave)
  vcartSaveRam.connectToA(virtualCart.io.saveRamEmu)
  vcartSaveRam.connectToB(virtualCart.io.saveRamDebug)

  val vcartChrRam = Module(new DualPortRamCartChr)
  vcartChrRam.connectToA(virtualCart.io.chrRamEmu)
  vcartChrRam.connectToB(virtualCart.io.chrRamDebug)

  /**********************************************************************/
  /* GPIO Remap                                                         */
  val gpioMapping = Module(new GpioMapping)
  gpioMapping.io.GPIO_0 <> GPIO_0
  gpioMapping.io.GPIO_1 <> GPIO_1
  // Cartridge (GPIO-CartHub)
  gpioMapping.connectToCart(cartHub.io.hubToGpio)
  // Cartridge LevelShift Enable
  gpioMapping.io.cart_oe_in := cartHub.io.isEnableGpio // GPIO側が選ばれていたらHi-Z解除
  // Controller
  gpioMapping.io.joy1_ps  := false.B // TODO: エミュに接続
  gpioMapping.io.joy1_clk := false.B // TODO: エミュに接続
  gpioMapping.io.joy2_ps  := false.B // TODO: エミュに接続
  gpioMapping.io.joy2_clk := false.B // TODO: エミュに接続
  // Audio
  gpioMapping.io.dac_bclk    := false.B // TODO: エミュに接続
  gpioMapping.io.dac_lrclk   := false.B // TODO: エミュに接続
  gpioMapping.io.dac_sd_mode := false.B // TODO: エミュに接続
  gpioMapping.io.dac_din     := false.B // TODO: エミュに接続
  // Debug
  gpioMapping.io.led  := cartHub.io.isEnableGpio // GPIO側が選ばれていたら点灯
  gpioMapping.io.io0  := cpuClk.asBool
  gpioMapping.io.io1  := ppuClk.asBool
  gpioMapping.io.rsv0 := cpuClk.asBool
  gpioMapping.io.rsv1 := ppuClk.asBool

  /**********************************************************************/
  /* Debug Access Tester                                                */
  val debugAccessTester = withClockAndReset(clk50Mhz, reset) { Module(new DebugAccessTester()) }

  val vjtagToDatQueue = Module(new AsyncFifoVJtagToDap)
  val datToVjtagQueue = Module(new AsyncFifoDapToVJtag)
  virtualJtagBridge.io
    .debugAccessQueues(VirtualInstruction.AccessTarget.accessTest.litValue.toInt)
    .connect(debugAccessTester.io.debugAccess, vjtagToDatQueue, datToVjtagQueue)

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
  vgaOut.io.isDebug         := SW(8) // for Debug
  vgaOut.io.pixelClock      := vgaPixelClk
  vgaOut.io.pixelClockReset := reset

  // FrameBufferと接続
  val frameBuffer = Module(new DualPortRamFrameBuffer)
  vgaOut.io.frameBuffer.connect(frameBuffer)

  // TODO: PPUと接続
  vgaOut.io.ppuVideo.valid := false.B
  vgaOut.io.ppuVideo.r     := DontCare
  vgaOut.io.ppuVideo.g     := DontCare
  vgaOut.io.ppuVideo.b     := DontCare
  vgaOut.io.ppuVideo.x     := DontCare
  vgaOut.io.ppuVideo.y     := DontCare

  // VJTAGと接続
  val vjtagToFbQueue = Module(new AsyncFifoVJtagToDap)
  val fbToVjtagQueue = Module(new AsyncFifoDapToVJtag)
  virtualJtagBridge.io
    .debugAccessQueues(VirtualInstruction.AccessTarget.frameBuffer.litValue.toInt)
    .connect(vgaOut.io.debugAccess, vjtagToFbQueue, fbToVjtagQueue)

  /**********************************************************************/
  /* Test                                                               */
  // クロック跨いでるけどテスト回路だからいいとする...
  val pattern = SW(7, 0) // 上位bitは別用途で使っているので削る
  when(pattern === 0.U) {
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
  }.elsewhen(pattern === 1.U) {
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
  }.elsewhen(pattern === 2.U) {
    // 7segにDebugAccessTesterのカウンタ値を出す
    withClockAndReset(clk50Mhz, reset) {
      // ir_inの値そのまま
      numReg        := debugAccessTester.io.debugCounter
      numVisibleReg := true.B
      // 適当に流れるようにする
      ledArrayReg := (1.U(10.W) << debugAccessTester.io.debugCounter(3, 0))
    }
  }.elsewhen(pattern === 3.U) {
    // 7segにDebugAccessTesterの最後のオフセット値を出す
    withClockAndReset(clk50Mhz, reset) {
      // ir_inの値そのまま
      numReg        := debugAccessTester.io.debugLatestOffset
      numVisibleReg := true.B
      // 適当に流れるようにする
      ledArrayReg := (1.U(10.W) << debugAccessTester.io.debugLatestOffset(3, 0))
    }
  }.elsewhen(pattern === 4.U) {
    // 7segにfb -> vjtagの最後の値を出す
    withClockAndReset(vjtag.io.tck, reset) {
      val fbReadDataReg = RegInit(UInt(32.W), 0.U)
      when(!fbToVjtagQueue.io.rdempty) {
        fbReadDataReg := fbToVjtagQueue.io.q
      }
      numReg        := fbReadDataReg(23, 0)
      numVisibleReg := true.B
      // Queueのステータスを表示
      ledArrayReg := Cat(
        // clk
        vjtagToFbQueue.io.wrclk.asBool,
        vjtagToFbQueue.io.rdclk.asBool,
        // vjtag -> fifo
        vjtagToFbQueue.io.wrreq,
        vjtagToFbQueue.io.wrfull,
        // fifo -> fb
        vjtagToFbQueue.io.rdreq,
        vjtagToFbQueue.io.rdempty,
        // fb -> fifo
        fbToVjtagQueue.io.wrreq,
        fbToVjtagQueue.io.wrfull,
        // fifo -> vjtag
        fbToVjtagQueue.io.rdreq,
        fbToVjtagQueue.io.rdempty,
      )
    }
  }.elsewhen(pattern === 5.U) {
    // 7segにGPIOの値を出す
    withClockAndReset(cpuClk, reset) {
      numReg        := Cat(gpioMapping.io.pd_i, 0.U(8.W), gpioMapping.io.d_i)
      numVisibleReg := true.B
      ledArrayReg := Cat(
        gpioMapping.io.irq,
        gpioMapping.io.vramcs,
        gpioMapping.io.joy1_rsv,
        gpioMapping.io.joy1_do,
        gpioMapping.io.joy2_micin,
        gpioMapping.io.joy2_do,
        gpioMapping.io.rsv1,
        gpioMapping.io.rsv0,
        gpioMapping.io.cart_oe_in,
        gpioMapping.io.led,
      )
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

}

/** Generate Verilog
  */
object ChiselNesDriver extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new ChiselNes, args)
}
