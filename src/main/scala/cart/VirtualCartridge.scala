package cart

import chisel3._

import cart.types.CartridgeIO
import board.access.types.InternalAccessCommand
import board.ram.types.RamIO

/**
  * Cartridge実機を模倣したモジュール定義. PPU Clockかそれより早いクロックで動かすことを推奨
  * 
  * @param prgRomAddrWidth PRG-ROMのアドレス空間
  * @param chrRomAddrWidth PRG-ROMのアドレス空間
  * @param emuDataWidth エミュレータからアクセスする時のデータ幅、変更不要のはず
  * @param debugDataWidth DebugAccess経由でアクセスする時のデータ幅
  */
class VirtualCartridge(
    val prgRomAddrWidth: Int = 17,
    val chrRomAddrWidth: Int = 17,
    val emuDataWidth: Int = 8,
    val debugDataWidth: Int = 32,
) extends Module {

  val io = IO(new Bundle {
    // 本物のCartridgeに入れる信号と同じもの
    val cart = new CartridgeIO
    // VirtualCartridgeを利用する場合はtrue
    val isEnable = Input(Bool())

    // DPRAMをPRG-ROM, CHR-ROMの2種類置く。VJTAGから書き込む側を別ポートにする
    val prgRamEmu   = Flipped(new RamIO(addrWidth = prgRomAddrWidth, dataWidth = emuDataWidth))
    val chrRamEmu   = Flipped(new RamIO(addrWidth = chrRomAddrWidth, dataWidth = emuDataWidth))
    val prgRamDebug = Flipped(new RamIO(addrWidth = prgRomAddrWidth, dataWidth = debugDataWidth))
    val chrRamDebug = Flipped(new RamIO(addrWidth = chrRomAddrWidth, dataWidth = debugDataWidth))

    // VJTAGからデータの読み書きを実現する
    val debugAccess = new InternalAccessCommand.SlaveIO
  })

  // TODO: 実装する
  io.cart.cpu.dataIn      := 0x39.U
  io.cart.cpu.isNotIrq    := true.B
  io.cart.ppu.dataIn      := 0x39.U
  io.cart.ppu.isNotVramCs := true.B

  io.prgRamEmu.address := 0.U
  io.prgRamEmu.clock   := clock
  io.prgRamEmu.data    := 0.U
  io.prgRamEmu.rden    := false.B
  io.prgRamEmu.wren    := false.B

  io.chrRamEmu.address := 0.U
  io.chrRamEmu.clock   := clock
  io.chrRamEmu.data    := 0.U
  io.chrRamEmu.rden    := false.B
  io.chrRamEmu.wren    := false.B

  io.prgRamDebug.address := 0.U
  io.prgRamDebug.clock   := clock
  io.prgRamDebug.data    := 0.U
  io.prgRamDebug.rden    := false.B
  io.prgRamDebug.wren    := false.B

  io.chrRamDebug.address := 0.U
  io.chrRamDebug.clock   := clock
  io.chrRamDebug.data    := 0.U
  io.chrRamDebug.rden    := false.B
  io.chrRamDebug.wren    := false.B

  io.debugAccess.req.rdclk  := clock
  io.debugAccess.req.rdreq  := false.B
  io.debugAccess.resp.data  := 0.U
  io.debugAccess.resp.wrclk := clock
  io.debugAccess.resp.wrreq := false.B
}
