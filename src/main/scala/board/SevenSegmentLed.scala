package board

import chisel3._
import chisel3.util.MuxLookup

/** ボード上に乗っている7seg decoderを使う用
 * @param isActiveLog 負論理ならtrue
  */
object SevenSegmentLed {
  // gfedcbaの並び順
  val lookupTable: Seq[(UInt, UInt)] = Seq(
    0x0.U -> "0b0111111".asUInt(7.W),
    0x1.U -> "0b0000110".asUInt(7.W),
    0x2.U -> "0b1011011".asUInt(7.W),
    0x3.U -> "0b1001111".asUInt(7.W),
    0x4.U -> "0b1100110".asUInt(7.W),
    0x5.U -> "0b1101101".asUInt(7.W),
    0x6.U -> "0b1111101".asUInt(7.W),
    0x7.U -> "0b0100111".asUInt(7.W),
    0x8.U -> "0b1111111".asUInt(7.W),
    0x9.U -> "0b1101111".asUInt(7.W),
    0xa.U -> "0b1110111".asUInt(7.W),
    0xb.U -> "0b1111100".asUInt(7.W),
    0xc.U -> "0b0111001".asUInt(7.W),
    0xd.U -> "0b1011110".asUInt(7.W),
    0xe.U -> "0b1111001".asUInt(7.W),
    0xf.U -> "0b1110001".asUInt(7.W)
  )
  // 全消灯
  val offPattern = "0b0000000".asUInt(7.W)

  /** 1桁の16進数の数値を点灯パターンに変換します
    * @param num 表示する数値
    * @param isActiveLow 負論理ならtrue
    * @return
    */
  def decode(num: UInt, isActiveLow: Boolean = false): UInt =
    if (isActiveLow) ~MuxLookup(num, offPattern, lookupTable)
    else MuxLookup(num, offPattern, lookupTable)

  /** 複数桁の16進数の数値を点灯パターンに変換します
    * @param num 表示する数値
    * @param ledNum 7segの個数。1個あたり4bit
    * @param isActiveLow 負論理ならtrue
    * @return
    */
  def decodeNDigits(num: UInt, ledNum: Int, isActiveLow: Boolean = false): Seq[UInt] =
    (0 until ledNum).map(i => {
      val widthPerDigit = 4
      val lower         = i * widthPerDigit
      val upper         = ((i + 1) * widthPerDigit) - 1 // bitposなので-1
      val digit         = num(upper, lower)
      decode(digit, isActiveLow)
    })
}
