package cpu.types

import chisel3._
import chisel3.experimental.ChiselEnum

/** ハザード回避用のpileline interlock定義
  */
object Interlock extends ChiselEnum {
  val disable = Value(false.B)
  val enable  = Value(true.B)

  // EX,WBでメモリ空間へのアクセスが発生するものをtrueとして返す
  // EX,WBは同一stageとして扱うため、レジスタは除外
  def isWriteMem(inst: Instruction.Type, addressing: Addressing.Type) = (inst, addressing) match {
    case (Instruction.sta, _)                      => true
    case (Instruction.stx, _)                      => true
    case (Instruction.sty, _)                      => true
    case (Instruction.dec, _)                      => true
    case (Instruction.inc, _)                      => true
    case (Instruction.asl, Addressing.accumulator) => false
    case (Instruction.asl, _)                      => true
    case (Instruction.lsr, Addressing.accumulator) => false
    case (Instruction.lsr, _)                      => true
    case (Instruction.rol, Addressing.accumulator) => false
    case (Instruction.rol, _)                      => true
    case (Instruction.ror, Addressing.accumulator) => false
    case (Instruction.ror, _)                      => true
    case (Instruction.pha, _)                      => true
    case (Instruction.php, _)                      => true
    case (Instruction.pla, _)                      => true
    case _                                         => false
  }
}
