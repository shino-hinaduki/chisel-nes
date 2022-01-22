package debug

import chisel3._
import chisel3.experimental.ChiselEnum
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
class NesFile(val srcBytes: Array[Byte]) {
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

  def trainerBytes = if (trainerSize > 0) srcBytes.drop(trainerOffset).take(trainerSize) else Array.empty
  def prgRomBytes  = srcBytes.drop(prgRomOffset).take(prgRomSize)
  def chrRomBytes  = srcBytes.drop(chrRomOffset).take(chrRomSize)
}
