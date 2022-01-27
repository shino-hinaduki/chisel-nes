package cpu.types

import chisel3._
import chisel3.experimental.ChiselEnum

/** AddressingModeの定義
  */
object Addressing extends ChiselEnum {
  // invalid
  val Invalid = Value
  // other
  val Implied     = Value
  val Accumulator = Value
  val Immediate   = Value
  val Absolute    = Value
  val ZeroPage    = Value
  val ZeroPageX   = Value
  val ZeroPageY   = Value
  val AbsoluteX   = Value
  val AbsoluteY   = Value
  val Relative    = Value
  val Indirect    = Value
  val IndirectX   = Value
  val IndirectY   = Value
}
