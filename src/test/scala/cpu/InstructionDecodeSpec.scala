package cpu

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import cpu.types.Instruction
import cpu.types.Addressing

class InstructionDecodeSpec extends AnyFreeSpec {
  // (別の表を見ながら順に作り直した...)
  val expectTable: Seq[(Int, (Instruction.Type, Addressing.Type))] = Seq(
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
