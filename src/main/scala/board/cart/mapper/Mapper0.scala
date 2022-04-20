package board.cart.mapper

import chisel3._
import board.ram.types.RamIO
import board.cart.types.CartridgeIO
import board.cart.types.NameTableMirror
import board.cart.types.NesFileFormat

/**
  * シンプルなNROMの実装
  */
class Mapper0 extends MapperImpl {

  override def assignCpu(
      cart: CartridgeIO,
      cpuClock: Clock,
      cpuReset: Reset,
      prgRam: RamIO,
      saveRam: RamIO,
      inesHeader: Vec[UInt]
  ): Unit = {
    // IRQなし
    cart.cpu.nIrq := true.B

    // PRG ROM固定 /ROMSEL時にデータが出力されるようにする
    val nOePrg = cart.cpu.nRomSel
    cart.cpu.dataIn := Mux(!nOePrg, prgRam.q, DontCare)
    prgRam.address  := cart.cpu.address // TODO: inesHeaderからミラーするアドレス範囲を作る
    prgRam.data     := cart.cpu.dataOut.getData()
    prgRam.rden     := !nOePrg
    prgRam.wren     := false.B          // Writeしない

    // Save RAMはない
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
    // /VRAMCS は /PA13直結
    cart.ppu.nVramCs := !cart.ppu.address(13)
    // Nametable MirrorはHeaderから決定
    cart.ppu.vrama10 := Mux(
      NesFileFormat.nameTableMirror(inesHeader) === NameTableMirror.Horizontal,
      cart.ppu.address(10),
      cart.ppu.address(11)
    )
    // CHR-ROM PA13で/CS, /RDで/OE。 PA10/11でVRAMA10切り替え
    val nCsChr = cart.ppu.address(13)
    val nOeChr = cart.ppu.nRd

    cart.ppu.dataIn := Mux(!nCsChr && !nOeChr, chrRam.q, DontCare)
    chrRam.address  := cart.ppu.address // TODO: inesHeaderからミラーするアドレス範囲を作る
    chrRam.data     := cart.ppu.dataOut.getData()
    chrRam.rden     := !nCsChr && !nOeChr
    chrRam.wren     := false.B
  }

}
