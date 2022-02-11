package cpu.types

import chisel3._
import chisel3.experimental.ChiselEnum

/** AddressingModeの定義
  */
object Addressing extends ChiselEnum {
  // invalid(実行しようとした場合HALTさせる)
  val invalid = Value
  // [impl] 何もしない
  val implied = Value
  // [A | A] Aレジスタ参照
  val accumulator = Value
  // [# | #$12] dataをFetch。この値をそのまま使う(ここに書き戻す処理がある命令は存在しない)
  val immediate = Value
  // [abs | $1234] lower, upperをFetch。({upper, lower})が実効アドレス
  val absolute = Value
  // [zpg | $12] lowerをFetch。({0x00, lower})が実効アドレス
  val zeroPage = Value
  // [zpg,X | $12,X] lowerをFetch。({0x00, lower} + (uint8)X)が実効アドレス
  val xIndexedZeroPage = Value
  // [zpg,Y | $12,Y] lowerをFetch。({0x00, lower} + (uint8)Y)が実効アドレス
  val yIndexedZeroPage = Value
  // [abs,X | $1234,X] lower, upperをFetch。({upper, lower} + (uint16)X)が実効アドレス
  val xIndexedAbsolute = Value
  // [abs,Y | $1234,Y] lower, upperをFetch。({upper, lower} + (uint16)Y)が実効アドレス
  val yIndexedAbsolute = Value
  // [rel | $12] offsetをFetch。 (PC + (int8)offset)が実効アドレス
  val relative = Value
  // [ind | ($1234)] lower, upperをFetch。addr={upper, lower}を計算。{*(addr+1), *addr}が実効アドレス
  val indirect = Value
  // [X,ind | ($44,X)] lowerをFetch。addr=({0x00, lower} + (uint8)X)を計算。 {*(addr+1), *addr}が実効アドレス
  val xIndexedIndirect = Value
  // [ind,Y | ($12),Y] lowerをFetch。addr={0x00, lower}を計算。{*(addr+1), *addr} + (uint16)Yが実効アドレス
  val indirectYIndexed = Value
}
