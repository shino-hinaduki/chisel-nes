package gcd

import chisel3._
import bus.CpuBus
import bus.PpuBus

/** Mapper機能をサポートしない、単純なカードリッジの模倣IP PRG ROM, CHR ROM, Battery-Backed
  *
  * @param isDebugEnable
  *   有効にするとROMに対しても読み書き可能になる
  * @param initNesFilePath
  *   初期値として読み込む.NES File
  */
class VirtualCart(isDebugEnable: Bool, initNesFilePath: String) extends Module {
  def this(isDebugEnable: Bool) = {
    this(isDebugEnable, null)
  }

  val io = IO(new Bundle {
    val cpuBus = new CpuBus
    val ppuBus = new PpuBus
  })

  val PrgRomBaseOffset = 0x8000;
  val PrgRomSize = 0x8000;

  val BatteryBackedRamBaseOffset = 0x6000;
  val BatteryBackedRamSize = 0x2000;

  val ChrRomBaseOffset = 0x0000;
  val ChrRomSize = 0x2000;

  val PrgRom = SyncReadMem(PrgRomSize, UInt(8.W))
  val ChrRom = SyncReadMem(ChrRomSize, UInt(8.W))
  val BatteryBackedRam = SyncReadMem(BatteryBackedRamSize, UInt(8.W))

  // CpuBus
  // 0x0000 - 0x5fff: Don't care
  // 0x6000 - 0x7fff: Battery-Backed RAM
  // 0x8000 - 0xffff: PRG ROM
  when(io.cpuBus.addr < BatteryBackedRamBaseOffset.U) {
    io.cpuBus.dataOut := DontCare
  }.elsewhen(io.cpuBus.addr < PrgRomBaseOffset.U) {
    when(io.cpuBus.wen) {
      BatteryBackedRam.write(io.cpuBus.addr, io.cpuBus.dataIn)
      io.cpuBus.dataOut := DontCare
    }.otherwise {
      io.cpuBus.dataOut := BatteryBackedRam.read(io.cpuBus.addr)
    }
  }.otherwise {
    when(io.cpuBus.wen) {
      // ROMなので本来はWriteできない
      when(isDebugEnable) {
        PrgRom.write(io.cpuBus.addr, io.cpuBus.dataIn)
      }
      io.cpuBus.dataOut := DontCare
    }.otherwise {
      io.cpuBus.dataOut := PrgRom.read(io.cpuBus.addr)
    }
  }

  // PpuBus
  // 0x0000_0000 - 0x0000_1fff: CHR ROM
  // 0x0000_2000 - 0x0000_2eff: NameTable Mirring(CHR ROM)
  // 0x0000_2f00 - 0xffff_ffff: Don't care
  io.ppuBus.dataOut := DontCare
  // TODO:

  // .NES Fileを初期値としてロード
  if (initNesFilePath != null) {
    // TODO
  }
}
