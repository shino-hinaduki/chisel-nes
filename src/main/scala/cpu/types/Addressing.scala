package cpu.types

import chisel3._
import chisel3.experimental.ChiselEnum

/** AddressingModeの定義
  */
object Addressing extends ChiselEnum {
  // invalid(実行しようとした場合HALTさせる)
  val invalid = Value
  // 何もしない
  val implied = Value
  // Aレジスタ参照
  val accumulator = Value
  // dataをFetch。この値をそのまま使う(ここに書き戻す処理がある命令は存在しない)
  val immediate = Value
  // lower, upperをFetch。({upper, lower})が実効アドレス
  val absolute = Value
  // lowerをFetch。({0x00, lower})が実効アドレス
  val zeroPage = Value
  // lowerをFetch。({0x00, lower} + (uint8)X)が実効アドレス
  val indexedZeroPageX = Value
  // lowerをFetch。({0x00, lower} + (uint8)Y)が実効アドレス
  val indexedZeroPageY = Value
  // lower, upperをFetch。({upper, lower} + (uint16)X)が実効アドレス
  val indexedAbsoluteX = Value
  // lower, upperをFetch。({upper, lower} + (uint16)Y)が実効アドレス
  val indexedAbsoluteY = Value
  // offsetをFetch。 (PC + (int8)offset)が実効アドレス
  val relative = Value
  // lower, upperをFetch。addr={upper, lower}を計算。{*(addr+1), *addr}が実効アドレス
  val indirectAbsolute = Value
  // lowerをFetch。addr=({0x00, lower} + (uint8)X)を計算。 {*(addr+1), *addr}が実効アドレス
  val indexedIndirectX = Value
  // lowerをFetch。addr={0x00, lower}を計算。{*(addr+1), *addr} + (uint16)Yが実効アドレス
  val indirectIndexedY = Value
}
