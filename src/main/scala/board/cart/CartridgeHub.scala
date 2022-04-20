package board.cart

import chisel3._
import board.cart.types.CartridgeIO

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
    master.cpu.address <> slave.cpu.address
    master.cpu.dataOut <> slave.cpu.dataOut
    master.cpu.dataIn <> slave.cpu.dataIn
    master.cpu.rNW <> slave.cpu.rNW
    master.cpu.nRomSel <> slave.cpu.nRomSel
    master.cpu.o2 <> slave.cpu.o2
    master.cpu.nIrq <> slave.cpu.nIrq

    // ppu
    master.ppu.address <> slave.ppu.address
    master.ppu.dataOut <> slave.ppu.dataOut
    master.ppu.dataIn <> slave.ppu.dataIn
    master.ppu.vrama10 <> slave.ppu.vrama10
    master.ppu.nVramCs <> slave.ppu.nVramCs
    master.ppu.nRd <> slave.ppu.nRd
    master.ppu.nWe <> slave.ppu.nWe
  }

  // master-slaveの信号を転送しません
  def disconnect(master: CartridgeIO, slave: CartridgeIO) = {
    // cpu
    slave.cpu.address      := 0.U
    slave.cpu.dataOut.data := 0.U
    slave.cpu.dataOut.oe   := false.B
    // master.cpu.dataIn      := slave.cpu.dataIn // slave->master向けは切り離す
    slave.cpu.rNW     := true.B
    slave.cpu.nRomSel := true.B
    slave.cpu.o2      := master.cpu.o2 // clockだけは通す
    // master.cpu.irq         := slave.cpu.irq // slave->master向けは切り離す

    // ppu
    slave.ppu.address      := 0.U
    slave.ppu.dataOut.data := 0.U
    slave.ppu.dataOut.oe   := false.B
    // master.ppu.dataIn := slave.ppu.dataIn // slave->master向けは切り離す
    // master.ppu.vrama10 := slave.ppu.vrama10 // slave->master向けは切り離す
    // master.ppu.nVramCs := slave.ppu.nVramCs // slave->master向けは切り離す
    slave.ppu.nRd := true.B
    slave.ppu.nWe := true.B
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
