package cart

import chisel3._
import cart.types.CartridgeIO

/**
  * Cartridge配線のセレクタ
  */
class CartridgeHub extends RawModule {
  val io = IO(new Bundle {
    // emuとの接続
    val emuToHub = new CartridgeIO
    // GPIOとの接続
    val hubToGpio    = Flipped(new CartridgeIO)
    val isEnableGpio = Output(Bool())
    // vcartとの接続
    val hubToVirtual    = Flipped(new CartridgeIO)
    val isEnableVirtual = Output(Bool())

    // GPIOの物理出力を使うならtrue
    val isUseGpio = Input(Bool())
  })

  // master-slaveの信号を転送します
  def connect(master: CartridgeIO, slave: CartridgeIO) = {
    // cpu
    slave.cpu.address     := master.cpu.address
    slave.cpu.dataOut     := master.cpu.dataOut
    master.cpu.dataIn     := slave.cpu.dataIn
    slave.cpu.isRead      := master.cpu.isRead
    slave.cpu.isNotRomSel := master.cpu.isNotRomSel
    slave.cpu.o2          := master.cpu.o2
    master.cpu.isNotIrq   := slave.cpu.isNotIrq

    // ppu
    slave.ppu.address      := master.ppu.address
    slave.ppu.dataOut      := master.ppu.dataOut
    master.ppu.dataIn      := slave.ppu.dataIn
    slave.ppu.vrama10      := master.ppu.vrama10
    master.ppu.isNotVramCs := slave.ppu.isNotVramCs
    slave.ppu.isNotRead    := master.ppu.isNotRead
    slave.ppu.isNotWrite   := master.ppu.isNotWrite
  }

  // master-slaveの信号を転送しません
  def disconnect(master: CartridgeIO, slave: CartridgeIO) = {
    // cpu
    slave.cpu.address      := 0.U
    slave.cpu.dataOut.data := 0.U
    slave.cpu.dataOut.oe   := false.B
    // master.cpu.dataIn      := slave.cpu.dataIn // slave->master向けは切り離す
    slave.cpu.isRead      := false.B
    slave.cpu.isNotRomSel := false.B
    slave.cpu.o2          := master.cpu.o2 // clockだけは通す
    // master.cpu.irq         := slave.cpu.irq // slave->master向けは切り離す

    // ppu
    slave.ppu.address      := 0.U
    slave.ppu.dataOut.data := 0.U
    slave.ppu.dataOut.oe   := false.B
    // master.ppu.dataIn := slave.ppu.dataIn // slave->master向けは切り離す
    slave.ppu.vrama10 := false.B
    // master.ppu.vramcs := slave.ppu.vramcs // slave->master向けは切り離す
    slave.ppu.isNotRead  := false.B
    slave.ppu.isNotWrite := false.B
  }

  when(io.isUseGpio) {
    // GPIOと接続
    io.isEnableGpio    := true.B
    io.isEnableVirtual := false.B
    connect(io.emuToHub, io.hubToGpio)
    disconnect(io.emuToHub, io.hubToVirtual)
  }.otherwise {
    // VirtualCartと接続
    io.isEnableGpio    := false.B
    io.isEnableVirtual := true.B
    connect(io.emuToHub, io.hubToVirtual)
    disconnect(io.emuToHub, io.hubToGpio)
  }
}
