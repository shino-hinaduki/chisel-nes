package cpu.types

import chisel3._
import chisel3.experimental.ChiselEnum

/** AddressingModeの定義
  */
object Addressing extends ChiselEnum {
  // invalid
  val invalid = Value
  // other
  val implied     = Value
  val accumulator = Value
  val immediate   = Value
  val absolute    = Value
  val zeroPage    = Value
  val zeroPageX   = Value
  val zeroPageY   = Value
  val absoluteX   = Value
  val absoluteY   = Value
  val relative    = Value
  val indirect    = Value
  val indirectX   = Value
  val indirectY   = Value
}
