package board.cart.mapper

import chisel3._
import board.ram.types.RamIO
import board.cart.types.CartridgeIO
import board.cart.types.NameTableMirror
import board.cart.types.NesFileFormat

/**
  * 有効なMapperが指定されなかったときの挙動
  */
class InvalidMapper extends MapperImpl {

  override def assignCpu(
      cart: CartridgeIO,
      cpuClock: Clock,
      cpuReset: Reset,
      prgRam: RamIO,
      saveRam: RamIO,
      inesHeader: Vec[UInt]
  ): Unit = {
    // CartridgeHubで切り離される想定なので、値は不定
    cart.cpu.nIrq   := true.B
    cart.cpu.dataIn := DontCare

    prgRam.address := DontCare
    prgRam.data    := DontCare
    prgRam.rden    := false.B
    prgRam.wren    := false.B

    saveRam.address := DontCare
    saveRam.data    := DontCare
    saveRam.rden    := false.B
    saveRam.wren    := false.B
  }

  override def assignPpu(
      cart: CartridgeIO,
      ppuClock: Clock,
      ppuReset: Reset,
      chrRam: RamIO,
      inesHeader: Vec[UInt]
  ): Unit = {
    cart.ppu.nVramCs := true.B
    cart.ppu.vrama10 := false.B // Hi-Zを用意してないがfalse固定でH/Vミラーが効かないようにする
    cart.ppu.dataIn  := DontCare

    chrRam.address := DontCare
    chrRam.data    := DontCare
    chrRam.rden    := false.B
    chrRam.wren    := false.B

  }

}
