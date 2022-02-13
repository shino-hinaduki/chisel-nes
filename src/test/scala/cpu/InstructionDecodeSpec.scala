package cpu

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import cpu.types.Instruction
import cpu.types.Addressing

class InstructionDecodeSpec extends AnyFreeSpec {
  // (別の表を見ながら順に作り直した...)
  val expectTable: Seq[(Int, (Instruction.Type, Addressing.Type))] = Seq(
    // 0x
    0x00 -> (Instruction.brk, Addressing.implied),
    0x01 -> (Instruction.ora, Addressing.xIndexedIndirect),
    0x02 -> (Instruction.jam, Addressing.invalid),
    0x03 -> (Instruction.slo, Addressing.xIndexedIndirect),
    0x04 -> (Instruction.nop, Addressing.zeroPage),
    0x05 -> (Instruction.ora, Addressing.zeroPage),
    0x06 -> (Instruction.asl, Addressing.zeroPage),
    0x07 -> (Instruction.slo, Addressing.zeroPage),
    0x08 -> (Instruction.php, Addressing.implied),
    0x09 -> (Instruction.ora, Addressing.immediate),
    0x0a -> (Instruction.asl, Addressing.accumulator),
    0x0b -> (Instruction.anc, Addressing.immediate),
    0x0c -> (Instruction.nop, Addressing.absolute),
    0x0d -> (Instruction.ora, Addressing.absolute),
    0x0e -> (Instruction.asl, Addressing.absolute),
    0x0f -> (Instruction.slo, Addressing.absolute),
    // 1x
    0x10 -> (Instruction.bpl, Addressing.relative),
    0x11 -> (Instruction.ora, Addressing.indirectYIndexed),
    0x12 -> (Instruction.jam, Addressing.invalid),
    0x13 -> (Instruction.slo, Addressing.indirectYIndexed),
    0x14 -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0x15 -> (Instruction.ora, Addressing.xIndexedZeroPage),
    0x16 -> (Instruction.asl, Addressing.xIndexedZeroPage),
    0x17 -> (Instruction.slo, Addressing.xIndexedZeroPage),
    0x18 -> (Instruction.clc, Addressing.implied),
    0x19 -> (Instruction.ora, Addressing.yIndexedAbsolute),
    0x1a -> (Instruction.nop, Addressing.implied),
    0x1b -> (Instruction.slo, Addressing.yIndexedAbsolute),
    0x1c -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0x1d -> (Instruction.ora, Addressing.xIndexedAbsolute),
    0x1e -> (Instruction.asl, Addressing.xIndexedAbsolute),
    0x1f -> (Instruction.slo, Addressing.xIndexedAbsolute),
    // 2x
    0x20 -> (Instruction.jsr, Addressing.absolute),
    0x21 -> (Instruction.and, Addressing.xIndexedIndirect),
    0x22 -> (Instruction.jam, Addressing.invalid),
    0x23 -> (Instruction.rla, Addressing.xIndexedIndirect),
    0x24 -> (Instruction.bit, Addressing.zeroPage),
    0x25 -> (Instruction.and, Addressing.zeroPage),
    0x26 -> (Instruction.rol, Addressing.zeroPage),
    0x27 -> (Instruction.rla, Addressing.zeroPage),
    0x28 -> (Instruction.plp, Addressing.implied),
    0x29 -> (Instruction.and, Addressing.immediate),
    0x2a -> (Instruction.rol, Addressing.accumulator),
    0x2b -> (Instruction.anc, Addressing.immediate),
    0x2c -> (Instruction.bit, Addressing.absolute),
    0x2d -> (Instruction.and, Addressing.absolute),
    0x2e -> (Instruction.rol, Addressing.absolute),
    0x2f -> (Instruction.rla, Addressing.absolute),
  )
  // 本家で定義されているテーブル(UIntでMapにすると、論理環境でうまくKeyが見つからなかったので数値に変換)
  val decode: Map[Int, (Instruction.Type, Addressing.Type)] =
    InstructionDecode.lookUpTable.map { case (opcode, data) =>
      (opcode.litValue.toInt -> data)
    }.toMap

  expectTable foreach {
    case (opcode, (expectInst, expectAddressing)) => {
      f"$opcode%02x is ($expectInst, $expectAddressing)" in {
        val (inst, addressing) = decode(opcode)
        assert(inst == expectInst)
        assert(addressing == expectAddressing)
      }

    }
  }
}
