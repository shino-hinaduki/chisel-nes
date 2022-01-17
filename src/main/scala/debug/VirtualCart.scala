package gcd

import chisel3._
import chisel3.experimental.ChiselEnum
import bus.CpuBus
import bus.PpuBus

/** NameTableMirroringの設定を示す
  */
object NameTableMirror extends ChiselEnum {
  val Unknown = Value(0x0.U)
  val Horizontal = Value(0x1.U)
  val Vertical = Value(0x2.U)
  val SingleScreen = Value(0x3.U)
  val FourScreen = Value(0x4.U)
}

/** Mapper機能をサポートしない、単純なカードリッジの模倣IP
  * @param initNesFilePath
  *   初期値として読み込む.NES File
  */
class VirtualCart(initNesFilePath: String) extends Module {
  val io = IO(new Bundle {
    val cpuBus = new CpuBus
    val ppuBus = new PpuBus
    val debugConfig = new Bundle {
      val isWriteRom = Input(Bool()) // ROMへの書き込みを許可するならtrue
    }
  })

  val ExpansionAreaRamBaseOffset = 0x4018;
  val ExpansionAreaRamSize = 0x2000 - 0x18;
  val ExpansionAreaRam = SyncReadMem(ExpansionAreaRamSize, UInt(8.W))

  val BatteryBackedRamBaseOffset = 0x6000;
  val BatteryBackedRamSize = 0x2000;
  val BatteryBackedRam = SyncReadMem(BatteryBackedRamSize, UInt(8.W))

  val PrgRomBaseOffset = 0x8000;
  val PrgRomSize = 0x8000;
  val PrgRom = SyncReadMem(PrgRomSize, UInt(8.W))

  // CpuBus
  // 0x0000 - 0x4017: Don't care
  // 0x4018 - 0x5fff: Expansion RAM
  // 0x6000 - 0x7fff: Battery-Backed RAM
  // 0x8000 - 0xffff: PRG ROM
  when(io.cpuBus.addr < BatteryBackedRamBaseOffset.U) {
    io.cpuBus.dataOut := DontCare
  }.elsewhen(io.cpuBus.addr < BatteryBackedRamBaseOffset.U) {
    when(io.cpuBus.wen) {
      ExpansionAreaRam.write(io.cpuBus.addr, io.cpuBus.dataIn)
      io.cpuBus.dataOut := DontCare
    }.otherwise {
      io.cpuBus.dataOut := ExpansionAreaRam.read(io.cpuBus.addr)
    }
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
      when(io.debugConfig.isWriteRom) {
        PrgRom.write(io.cpuBus.addr, io.cpuBus.dataIn)
      }
      io.cpuBus.dataOut := DontCare
    }.otherwise {
      io.cpuBus.dataOut := PrgRom.read(io.cpuBus.addr)
    }
  }

  val ChrRomBaseOffset = 0x0000;
  val ChrRomSize = 0x2000;
  val ChrRom = SyncReadMem(ChrRomSize, UInt(8.W))

  // PpuBus
  // 0x0000_0000 - 0x0000_1fff: CHR ROM/RAM(Pattern Table 0/1)
  // 0x0000_2000 - 0xffff_ffff: Don't Care
  when(io.ppuBus.addr < ChrRomSize.U) {
    when(io.ppuBus.wen) {
      ChrRom.write(io.ppuBus.addr, io.ppuBus.dataIn)
      io.ppuBus.dataOut := DontCare
    }.otherwise {
      io.ppuBus.dataOut := ChrRom.read(io.ppuBus.addr)
    }
  }.otherwise {
    io.ppuBus.dataOut := DontCare
  }

  // .NES Fileを初期値としてロード
  if (initNesFilePath != null) {
    // TODO
  }
}
