package cpu.types

import chisel3._
import chisel3.experimental.ChiselEnum

/** 命令の定義
  */
object Instruction extends ChiselEnum {
  // invalid(実行しようとした場合HALTさせる)
  val invalid = Value
  // binary op
  val adc, sbc, and, eor, ora = Value
  // shift/rotate
  val asl, lsr, rol, ror = Value
  // inc/dec
  val inc, inx, iny, dec, dex, dey = Value
  // load/store
  val lda, ldx, ldy, sta, stx, sty = Value
  // set/clear
  val sec, sed, sei, clc, cld, cli, clv = Value
  // compare
  val cmp, cpx, cpy = Value
  // jump return
  val jmp, jsr, rti, rts = Value
  // branch
  val bcc, bcs, beq, bmi, bne, bpl, bvc, bvs = Value
  // push/pop
  val pha, php, pla, plp = Value
  // transfer
  val tax, tay, tsx, txa, txs, tya = Value
  // other
  val brk, bit, nop = Value
  // unofficial1
  val alr, anc, anc2, sbc2, arr, xaa, axs, lax, lax2, las, sax, shx, ahx, tas, shy, dcp, isc, rla, rra, slo, sre, halt = Value
}
