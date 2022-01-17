package debug

import chisel3._
import chisel3.experimental.ChiselEnum
import bus.CpuBus
import bus.PpuBus
import java.nio.file.Files
import java.nio.file.Paths

/** NameTableMirroringの設定を示す
  */
object NameTableMirror extends ChiselEnum {
  val Unknown      = Value(0x0.U)
  val Horizontal   = Value(0x1.U)
  val Vertical     = Value(0x2.U)
  val SingleScreen = Value(0x3.U)
  val FourScreen   = Value(0x4.U)
}

/** .NES fileの解析に必要な機能を提供します
  */
object NesFile {
  // .NES先頭4byteを検査します
  def isValidHeader(byteArr: Array[Byte]) = {
    (byteArr(0) == 0x4e) && (byteArr(1) == 0x45) && (byteArr(2) == 0x53) && (byteArr(3) == 0x1a)
  }
}

/** .NES fileのParse内容を示す
 * 
  * - 16byte           : header
  *   - 0~3 : identify "0x4e, 0x45, 0x53, 0x1a"
  *   - 4   : PRG ROM Bank数
  *   - 5   : CHR ROM Bank数
  *   - 6,7 : config
  *     - 0    : Mirroring
  *     - 1    : Battery-Back RAM有無
  *     - 2    : 512byte trainer有無
  *     - 3    : Mirroring無効化して4画面VRAMを利用
  *     - 4~15 : Mapper
  *   -  8    : PRG-RAM Bank数
  *   -  9    : TV System
  *   - 10    : TV System PRG-RAM
  *   - 11~15 : reserved
  * - 0byte or 512byte : Trainer Data
  * - 16KiB * N byte   : PRG ROM Data
  * -  8KiB * N byte   : CHR ROM Data
  * - 0 or 8KiB        : INST-ROM    
  * - 16byte           : PROM        
  */
class NesFile(srcBytes: Array[Byte]) {
  def this(filePath: String) = {
    this(Files.readAllBytes(Paths.get(filePath)))
  }

  /* check header */
  val headerSize = 16
  if (!NesFile.isValidHeader(srcBytes)) {
    throw new Exception(s"Invalid Format")
  }
  // PRG ROMのサイズ。16KiB単位で定義。少ない場合は後半はミラー
  val prgRomSize = srcBytes(4) * 0x4000
  // CHR ROMのサイズ。8KiB単位で定義
  val chrRomSize = srcBytes(5) * 0x2000 // 8KiB単位
  // Nametable Mirrorの設定
  val nameTableMirror = (((srcBytes(6) & 0x08) != 0x0), ((srcBytes(6) & 0x01) != 0x0)) match {
    case (true, _)  => NameTableMirror.FourScreen
    case (_, true)  => NameTableMirror.Vertical
    case (_, false) => NameTableMirror.Horizontal
    case _          => NameTableMirror.Unknown
  }
  // Batteryで不揮発化相当の扱いを受けているRAMが存在するか
  val isExistsBatteryBackedRam = ((srcBytes(6) & 0x02) != 0x0)
  // .iNES File Battery Backed RAM trainer領域サイズ
  val trainerSize = if ((srcBytes(6) & 0x04) != 0x0) 0x200 else 0
  // Mapper number
  val mapper = (srcBytes(7) << 4) | (srcBytes(6) >> 4)

  /* binaryから各ROM/RAM領域を切り出し */
  val trainerOffset = headerSize
  val prgRomOffset  = trainerOffset + trainerSize
  val chrRomOffset  = prgRomOffset + prgRomSize

  def TrainerBytes = if (trainerSize > 0) srcBytes.drop(trainerOffset).take(trainerSize) else Array.empty
  def PrgRomBytes  = srcBytes.drop(prgRomOffset).take(prgRomSize)
  def ChrRomBytes  = srcBytes.drop(chrRomOffset).take(chrRomSize)
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
  val ExpansionAreaRamSize       = 0x2000 - 0x18;
  val ExpansionAreaRam           = SyncReadMem(ExpansionAreaRamSize, UInt(8.W))

  val BatteryBackedRamBaseOffset = 0x6000;
  val BatteryBackedRamSize       = 0x2000;
  val BatteryBackedRam           = SyncReadMem(BatteryBackedRamSize, UInt(8.W))

  val PrgRomBaseOffset = 0x8000;
  val PrgRomSize       = 0x8000;
  val PrgRom           = SyncReadMem(PrgRomSize, UInt(8.W))

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
  val ChrRomSize       = 0x2000;
  val ChrRom           = SyncReadMem(ChrRomSize, UInt(8.W))

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
  val nesFile = new NesFile(initNesFilePath)

}
