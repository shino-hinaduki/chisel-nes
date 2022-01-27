package cpu.types

import chisel3._
import chisel3.experimental.ChiselEnum

/** 命令の定義
  */
object Instruction extends ChiselEnum {
  // invalid
  val Invalid = Value
  // binary op
  val ADC, SBC, AND, EOR, ORA = Value
  // shift/rotate
  val ASL, LSR, ROL, ROR = Value
  // inc/dec
  val INC, INX, INY, DEC, DEX, DEY = Value
  // load/store
  val LDA, LDX, LDY, STA, STX, STY = Value
  // set/clear
  val SEC, SED, SEI, CLC, CLD, CLI, CLV = Value
  // compare
  val CMP, CPX, CPY = Value
  // jump return
  val JMP, JSR, RTI, RTS = Value
  // branch
  val BCC, BCS, BEQ, BMI, BNE, BPL, BVC, BVS = Value
  // push/pop
  val PHA, PHP, PLA, PLP = Value
  // transfer
  val TAX, TAY, TSX, TXA, TXS, TYA = Value
  // other
  val BRK, BIT, NOP = Value
  // unofficial1
  val ALR, ANC, ARR, AXS, LAX, SAX, DCP, ISC, RLA, RRA, SLO, SRE, SKB, IGN = Value
}
