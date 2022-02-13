package cpu

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import cpu.types.Instruction
import cpu.types.Addressing

class InstructionDecodeSpec extends AnyFreeSpec {
  // (別の表を見ながら順に作り直した...)
  val expectTable: Seq[(Int, (Instruction.Type, Addressing.Type))] = Seq(
    0x00 -> (Instruction.brk, Addressing.implied)
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
