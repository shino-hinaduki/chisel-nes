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
    // 3x
    0x30 -> (Instruction.bmi, Addressing.relative),
    0x31 -> (Instruction.and, Addressing.indirectYIndexed),
    0x32 -> (Instruction.jam, Addressing.invalid),
    0x33 -> (Instruction.rla, Addressing.indirectYIndexed),
    0x34 -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0x35 -> (Instruction.and, Addressing.xIndexedZeroPage),
    0x36 -> (Instruction.rol, Addressing.xIndexedZeroPage),
    0x37 -> (Instruction.rla, Addressing.xIndexedZeroPage),
    0x38 -> (Instruction.sec, Addressing.implied),
    0x39 -> (Instruction.and, Addressing.yIndexedAbsolute),
    0x3a -> (Instruction.nop, Addressing.implied),
    0x3b -> (Instruction.rla, Addressing.yIndexedAbsolute),
    0x3c -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0x3d -> (Instruction.and, Addressing.xIndexedAbsolute),
    0x3e -> (Instruction.rol, Addressing.xIndexedAbsolute),
    0x3f -> (Instruction.rla, Addressing.xIndexedAbsolute),
    // 4x
    0x40 -> (Instruction.rti, Addressing.implied),
    0x41 -> (Instruction.eor, Addressing.xIndexedIndirect),
    0x42 -> (Instruction.jam, Addressing.invalid),
    0x43 -> (Instruction.sre, Addressing.xIndexedIndirect),
    0x44 -> (Instruction.nop, Addressing.zeroPage),
    0x45 -> (Instruction.eor, Addressing.zeroPage),
    0x46 -> (Instruction.lsr, Addressing.zeroPage),
    0x47 -> (Instruction.sre, Addressing.zeroPage),
    0x48 -> (Instruction.pha, Addressing.implied),
    0x49 -> (Instruction.eor, Addressing.immediate),
    0x4a -> (Instruction.lsr, Addressing.accumulator),
    0x4b -> (Instruction.alr, Addressing.immediate),
    0x4c -> (Instruction.jmp, Addressing.absolute),
    0x4d -> (Instruction.eor, Addressing.absolute),
    0x4e -> (Instruction.lsr, Addressing.absolute),
    0x4f -> (Instruction.sre, Addressing.absolute),
    // 5x
    0x50 -> (Instruction.bvc, Addressing.relative),
    0x51 -> (Instruction.eor, Addressing.indirectYIndexed),
    0x52 -> (Instruction.jam, Addressing.invalid),
    0x53 -> (Instruction.sre, Addressing.indirectYIndexed),
    0x54 -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0x55 -> (Instruction.eor, Addressing.xIndexedZeroPage),
    0x56 -> (Instruction.lsr, Addressing.xIndexedZeroPage),
    0x57 -> (Instruction.sre, Addressing.xIndexedZeroPage),
    0x58 -> (Instruction.cli, Addressing.implied),
    0x59 -> (Instruction.eor, Addressing.yIndexedAbsolute),
    0x5a -> (Instruction.nop, Addressing.implied),
    0x5b -> (Instruction.sre, Addressing.yIndexedAbsolute),
    0x5c -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0x5d -> (Instruction.eor, Addressing.xIndexedAbsolute),
    0x5e -> (Instruction.lsr, Addressing.xIndexedAbsolute),
    0x5f -> (Instruction.sre, Addressing.xIndexedAbsolute),
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
