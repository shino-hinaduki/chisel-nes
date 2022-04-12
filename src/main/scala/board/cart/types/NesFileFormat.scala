package board.cart.types

import chisel3._
import chisel3.util.MuxLookup
import chisel3.util.Cat
import chisel3.experimental.ChiselEnum

/** 
 * NameTableMirroringの設定を示す
 */
object NameTableMirror extends ChiselEnum {
  val Unknown      = Value(0x0.U)
  val Horizontal   = Value(0x1.U)
  val Vertical     = Value(0x2.U)
  val SingleScreen = Value(0x3.U)
  val FourScreen   = Value(0x4.U)
}

/** 
 *.NES fileのParse内容を示す
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
object NesFileFormat {

  /**
    * 4byteのVecから指定されてた1byteのデータを取り出す
    *
    * @param wordArr 元データ
    * @param byteIndex index
    * @return 取り出した1byteのデータ
    */
  def getByte(wordArr: Vec[UInt], byteIndex: Int): UInt = {
    val wordIndex  = byteIndex / 4
    val byteOffset = byteIndex % 4
    // 4byte取り出し
    val word = wordArr(wordIndex)
    // 特定byteを取り出し
    val data = word(8 * (1 + byteOffset) - 1, 8 * byteOffset)
    data
  }

  /**
    * .NES先頭4byteを検査
    */
  def isValidHeader(wordArr: Vec[UInt]) = {
    (getByte(wordArr = wordArr, byteIndex = 0) === 0x4e.U) &&
    (getByte(wordArr = wordArr, byteIndex = 1) === 0x45.U) &&
    (getByte(wordArr = wordArr, byteIndex = 2) === 0x53.U) &&
    (getByte(wordArr = wordArr, byteIndex = 3) === 0x1a.U)
  }

  /**
    * PRG ROM Sizeを取得。この値 * 16KiB(0x4000) が実体のサイズ
    */
  def prgRomSize(wordArr: Vec[UInt]) =
    getByte(wordArr = wordArr, byteIndex = 4)

  /**
    * CHR ROM Sizeを取得。この値 * 8KiB(0x2000) が実体のサイズ
    */
  def chrRomSize(wordArr: Vec[UInt]) =
    getByte(wordArr = wordArr, byteIndex = 5)

  /**
    * Mirroring設定を取得
    */
  def nameTableMirror(wordArr: Vec[UInt]) =
    MuxLookup(
      Cat(getByte(wordArr, 6)(3), getByte(wordArr, 6)(0)),
      NameTableMirror.Unknown,
      Seq(
        0.U -> NameTableMirror.Horizontal,
        1.U -> NameTableMirror.Vertical,
        2.U -> NameTableMirror.FourScreen,
        3.U -> NameTableMirror.FourScreen,
      )
    )

  /**
    * CHR ROM Sizeを取得。この値 * 8KiB(0x2000) が実体のサイズ
    */
  def existsSaveRam(wordArr: Vec[UInt]) =
    getByte(wordArr = wordArr, byteIndex = 6)(1)

  /**
    * Saved RAM 0x7000 - 0x71ff のデータが有るならtrue
    */
  def existsTrainer(wordArr: Vec[UInt]) =
    getByte(wordArr = wordArr, byteIndex = 6)(2)

  /**
    * Mapperの種類
    */
  def mapper(wordArr: Vec[UInt]) =
    getByte(wordArr = wordArr, byteIndex = 7)(3, 0) |
      getByte(wordArr = wordArr, byteIndex = 6)(7, 4)

}
