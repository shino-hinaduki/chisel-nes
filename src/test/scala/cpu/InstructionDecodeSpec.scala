package cpu

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import cpu.types.Instruction
import cpu.types.Addressing

class InstructionDecodeSpec extends AnyFreeSpec {
  // testの期待値
  case class TestExpect(inst: Instruction.Type, addressing: Addressing.Type, needDataRead: Boolean)
  // (別の表を見ながら順に作り直した...)
  val expectTable: Seq[(Int, TestExpect)] = Seq(
    // 0x
    0x00 -> TestExpect(Instruction.brk, Addressing.implied, false),
    0x01 -> TestExpect(Instruction.ora, Addressing.xIndexedIndirect, true),
    0x02 -> TestExpect(Instruction.halt, Addressing.invalid, false),
    0x03 -> TestExpect(Instruction.slo, Addressing.xIndexedIndirect, true),
    0x04 -> TestExpect(Instruction.nop, Addressing.zeroPage, true),
    0x05 -> TestExpect(Instruction.ora, Addressing.zeroPage, true),
    0x06 -> TestExpect(Instruction.asl, Addressing.zeroPage, true),
    0x07 -> TestExpect(Instruction.slo, Addressing.zeroPage, true),
    0x08 -> TestExpect(Instruction.php, Addressing.implied, false),
    0x09 -> TestExpect(Instruction.ora, Addressing.immediate, false),
    0x0a -> TestExpect(Instruction.asl, Addressing.accumulator, false),
    0x0b -> TestExpect(Instruction.anc, Addressing.immediate, false),
    0x0c -> TestExpect(Instruction.nop, Addressing.absolute, true),
    0x0d -> TestExpect(Instruction.ora, Addressing.absolute, true),
    0x0e -> TestExpect(Instruction.asl, Addressing.absolute, true),
    0x0f -> TestExpect(Instruction.slo, Addressing.absolute, true),
    // 1x
    0x10 -> TestExpect(Instruction.bpl, Addressing.relative, false),
    0x11 -> TestExpect(Instruction.ora, Addressing.indirectYIndexed, true),
    0x12 -> TestExpect(Instruction.halt, Addressing.invalid, false),
    0x13 -> TestExpect(Instruction.slo, Addressing.indirectYIndexed, true),
    0x14 -> TestExpect(Instruction.nop, Addressing.xIndexedZeroPage, true),
    0x15 -> TestExpect(Instruction.ora, Addressing.xIndexedZeroPage, true),
    0x16 -> TestExpect(Instruction.asl, Addressing.xIndexedZeroPage, true),
    0x17 -> TestExpect(Instruction.slo, Addressing.xIndexedZeroPage, true),
    0x18 -> TestExpect(Instruction.clc, Addressing.implied, false),
    0x19 -> TestExpect(Instruction.ora, Addressing.yIndexedAbsolute, true),
    0x1a -> TestExpect(Instruction.nop, Addressing.implied, false),
    0x1b -> TestExpect(Instruction.slo, Addressing.yIndexedAbsolute, true),
    0x1c -> TestExpect(Instruction.nop, Addressing.xIndexedAbsolute, true),
    0x1d -> TestExpect(Instruction.ora, Addressing.xIndexedAbsolute, true),
    0x1e -> TestExpect(Instruction.asl, Addressing.xIndexedAbsolute, true),
    0x1f -> TestExpect(Instruction.slo, Addressing.xIndexedAbsolute, true),
    // 2x
    0x20 -> TestExpect(Instruction.jsr, Addressing.absolute, true),
    0x21 -> TestExpect(Instruction.and, Addressing.xIndexedIndirect, true),
    0x22 -> TestExpect(Instruction.halt, Addressing.invalid, false),
    0x23 -> TestExpect(Instruction.rla, Addressing.xIndexedIndirect, true),
    0x24 -> TestExpect(Instruction.bit, Addressing.zeroPage, true),
    0x25 -> TestExpect(Instruction.and, Addressing.zeroPage, true),
    0x26 -> TestExpect(Instruction.rol, Addressing.zeroPage, true),
    0x27 -> TestExpect(Instruction.rla, Addressing.zeroPage, true),
    0x28 -> TestExpect(Instruction.plp, Addressing.implied, false),
    0x29 -> TestExpect(Instruction.and, Addressing.immediate, false),
    0x2a -> TestExpect(Instruction.rol, Addressing.accumulator, false),
    0x2b -> TestExpect(Instruction.anc2, Addressing.immediate, false),
    0x2c -> TestExpect(Instruction.bit, Addressing.absolute, true),
    0x2d -> TestExpect(Instruction.and, Addressing.absolute, true),
    0x2e -> TestExpect(Instruction.rol, Addressing.absolute, true),
    0x2f -> TestExpect(Instruction.rla, Addressing.absolute, true),
    // 3x
    0x30 -> TestExpect(Instruction.bmi, Addressing.relative, false),
    0x31 -> TestExpect(Instruction.and, Addressing.indirectYIndexed, true),
    0x32 -> TestExpect(Instruction.halt, Addressing.invalid, false),
    0x33 -> TestExpect(Instruction.rla, Addressing.indirectYIndexed, true),
    0x34 -> TestExpect(Instruction.nop, Addressing.xIndexedZeroPage, true),
    0x35 -> TestExpect(Instruction.and, Addressing.xIndexedZeroPage, true),
    0x36 -> TestExpect(Instruction.rol, Addressing.xIndexedZeroPage, true),
    0x37 -> TestExpect(Instruction.rla, Addressing.xIndexedZeroPage, true),
    0x38 -> TestExpect(Instruction.sec, Addressing.implied, false),
    0x39 -> TestExpect(Instruction.and, Addressing.yIndexedAbsolute, true),
    0x3a -> TestExpect(Instruction.nop, Addressing.implied, false),
    0x3b -> TestExpect(Instruction.rla, Addressing.yIndexedAbsolute, true),
    0x3c -> TestExpect(Instruction.nop, Addressing.xIndexedAbsolute, true),
    0x3d -> TestExpect(Instruction.and, Addressing.xIndexedAbsolute, true),
    0x3e -> TestExpect(Instruction.rol, Addressing.xIndexedAbsolute, true),
    0x3f -> TestExpect(Instruction.rla, Addressing.xIndexedAbsolute, true),
    // 4x
    0x40 -> TestExpect(Instruction.rti, Addressing.implied, false),
    0x41 -> TestExpect(Instruction.eor, Addressing.xIndexedIndirect, true),
    0x42 -> TestExpect(Instruction.halt, Addressing.invalid, false),
    0x43 -> TestExpect(Instruction.sre, Addressing.xIndexedIndirect, true),
    0x44 -> TestExpect(Instruction.nop, Addressing.zeroPage, true),
    0x45 -> TestExpect(Instruction.eor, Addressing.zeroPage, true),
    0x46 -> TestExpect(Instruction.lsr, Addressing.zeroPage, true),
    0x47 -> TestExpect(Instruction.sre, Addressing.zeroPage, true),
    0x48 -> TestExpect(Instruction.pha, Addressing.implied, false),
    0x49 -> TestExpect(Instruction.eor, Addressing.immediate, false),
    0x4a -> TestExpect(Instruction.lsr, Addressing.accumulator, false),
    0x4b -> TestExpect(Instruction.alr, Addressing.immediate, false),
    0x4c -> TestExpect(Instruction.jmp, Addressing.absolute, false),
    0x4d -> TestExpect(Instruction.eor, Addressing.absolute, true),
    0x4e -> TestExpect(Instruction.lsr, Addressing.absolute, true),
    0x4f -> TestExpect(Instruction.sre, Addressing.absolute, true),
    // 5x
    0x50 -> TestExpect(Instruction.bvc, Addressing.relative, false),
    0x51 -> TestExpect(Instruction.eor, Addressing.indirectYIndexed, true),
    0x52 -> TestExpect(Instruction.halt, Addressing.invalid, false),
    0x53 -> TestExpect(Instruction.sre, Addressing.indirectYIndexed, true),
    0x54 -> TestExpect(Instruction.nop, Addressing.xIndexedZeroPage, true),
    0x55 -> TestExpect(Instruction.eor, Addressing.xIndexedZeroPage, true),
    0x56 -> TestExpect(Instruction.lsr, Addressing.xIndexedZeroPage, true),
    0x57 -> TestExpect(Instruction.sre, Addressing.xIndexedZeroPage, true),
    0x58 -> TestExpect(Instruction.cli, Addressing.implied, false),
    0x59 -> TestExpect(Instruction.eor, Addressing.yIndexedAbsolute, true),
    0x5a -> TestExpect(Instruction.nop, Addressing.implied, false),
    0x5b -> TestExpect(Instruction.sre, Addressing.yIndexedAbsolute, true),
    0x5c -> TestExpect(Instruction.nop, Addressing.xIndexedAbsolute, true),
    0x5d -> TestExpect(Instruction.eor, Addressing.xIndexedAbsolute, true),
    0x5e -> TestExpect(Instruction.lsr, Addressing.xIndexedAbsolute, true),
    0x5f -> TestExpect(Instruction.sre, Addressing.xIndexedAbsolute, true),
    // 6x
    0x60 -> TestExpect(Instruction.rts, Addressing.implied, false),
    0x61 -> TestExpect(Instruction.adc, Addressing.xIndexedIndirect, true),
    0x62 -> TestExpect(Instruction.halt, Addressing.invalid, false),
    0x63 -> TestExpect(Instruction.rra, Addressing.xIndexedIndirect, true),
    0x64 -> TestExpect(Instruction.nop, Addressing.zeroPage, true),
    0x65 -> TestExpect(Instruction.adc, Addressing.zeroPage, true),
    0x66 -> TestExpect(Instruction.ror, Addressing.zeroPage, true),
    0x67 -> TestExpect(Instruction.rra, Addressing.zeroPage, true),
    0x68 -> TestExpect(Instruction.pla, Addressing.implied, false),
    0x69 -> TestExpect(Instruction.adc, Addressing.immediate, false),
    0x6a -> TestExpect(Instruction.ror, Addressing.accumulator, false),
    0x6b -> TestExpect(Instruction.arr, Addressing.immediate, false),
    0x6c -> TestExpect(Instruction.jmp, Addressing.indirect, false),
    0x6d -> TestExpect(Instruction.adc, Addressing.absolute, true),
    0x6e -> TestExpect(Instruction.ror, Addressing.absolute, true),
    0x6f -> TestExpect(Instruction.rra, Addressing.absolute, true),
    // 7x
    0x70 -> TestExpect(Instruction.bvs, Addressing.relative, false),
    0x71 -> TestExpect(Instruction.adc, Addressing.indirectYIndexed, true),
    0x72 -> TestExpect(Instruction.halt, Addressing.invalid, false),
    0x73 -> TestExpect(Instruction.rra, Addressing.indirectYIndexed, true),
    0x74 -> TestExpect(Instruction.nop, Addressing.xIndexedZeroPage, true),
    0x75 -> TestExpect(Instruction.adc, Addressing.xIndexedZeroPage, true),
    0x76 -> TestExpect(Instruction.ror, Addressing.xIndexedZeroPage, true),
    0x77 -> TestExpect(Instruction.rra, Addressing.xIndexedZeroPage, true),
    0x78 -> TestExpect(Instruction.sei, Addressing.implied, false),
    0x79 -> TestExpect(Instruction.adc, Addressing.yIndexedAbsolute, true),
    0x7a -> TestExpect(Instruction.nop, Addressing.implied, false),
    0x7b -> TestExpect(Instruction.rra, Addressing.yIndexedAbsolute, true),
    0x7c -> TestExpect(Instruction.nop, Addressing.xIndexedAbsolute, true),
    0x7d -> TestExpect(Instruction.adc, Addressing.xIndexedAbsolute, true),
    0x7e -> TestExpect(Instruction.ror, Addressing.xIndexedAbsolute, true),
    0x7f -> TestExpect(Instruction.rra, Addressing.xIndexedAbsolute, true),
    // 8x
    0x80 -> TestExpect(Instruction.nop, Addressing.immediate, false),
    0x81 -> TestExpect(Instruction.sta, Addressing.xIndexedIndirect, false),
    0x82 -> TestExpect(Instruction.nop, Addressing.immediate, false),
    0x83 -> TestExpect(Instruction.sax, Addressing.xIndexedIndirect, false),
    0x84 -> TestExpect(Instruction.sty, Addressing.zeroPage, false),
    0x85 -> TestExpect(Instruction.sta, Addressing.zeroPage, false),
    0x86 -> TestExpect(Instruction.stx, Addressing.zeroPage, false),
    0x87 -> TestExpect(Instruction.sax, Addressing.zeroPage, false),
    0x88 -> TestExpect(Instruction.dey, Addressing.implied, false),
    0x89 -> TestExpect(Instruction.nop, Addressing.immediate, false),
    0x8a -> TestExpect(Instruction.txa, Addressing.implied, false),
    0x8b -> TestExpect(Instruction.xaa, Addressing.immediate, false),
    0x8c -> TestExpect(Instruction.sty, Addressing.absolute, false),
    0x8d -> TestExpect(Instruction.sta, Addressing.absolute, false),
    0x8e -> TestExpect(Instruction.stx, Addressing.absolute, false),
    0x8f -> TestExpect(Instruction.sax, Addressing.absolute, false),
    // 9x
    0x90 -> TestExpect(Instruction.bcc, Addressing.relative, false),
    0x91 -> TestExpect(Instruction.sta, Addressing.indirectYIndexed, false),
    0x92 -> TestExpect(Instruction.halt, Addressing.invalid, false),
    0x93 -> TestExpect(Instruction.ahx, Addressing.indirectYIndexed, false),
    0x94 -> TestExpect(Instruction.sty, Addressing.xIndexedZeroPage, false),
    0x95 -> TestExpect(Instruction.sta, Addressing.xIndexedZeroPage, false),
    0x96 -> TestExpect(Instruction.stx, Addressing.yIndexedZeroPage, false),
    0x97 -> TestExpect(Instruction.sax, Addressing.yIndexedZeroPage, false),
    0x98 -> TestExpect(Instruction.tya, Addressing.implied, false),
    0x99 -> TestExpect(Instruction.sta, Addressing.yIndexedAbsolute, false),
    0x9a -> TestExpect(Instruction.txs, Addressing.implied, false),
    0x9b -> TestExpect(Instruction.tas, Addressing.yIndexedAbsolute, false),
    0x9c -> TestExpect(Instruction.shy, Addressing.xIndexedAbsolute, false),
    0x9d -> TestExpect(Instruction.sta, Addressing.xIndexedAbsolute, false),
    0x9e -> TestExpect(Instruction.shx, Addressing.yIndexedAbsolute, false),
    0x9f -> TestExpect(Instruction.ahx, Addressing.yIndexedAbsolute, false),
    // ax
    0xa0 -> TestExpect(Instruction.ldy, Addressing.immediate, false),
    0xa1 -> TestExpect(Instruction.lda, Addressing.xIndexedIndirect, true),
    0xa2 -> TestExpect(Instruction.ldx, Addressing.immediate, false),
    0xa3 -> TestExpect(Instruction.lax, Addressing.xIndexedIndirect, true),
    0xa4 -> TestExpect(Instruction.ldy, Addressing.zeroPage, true),
    0xa5 -> TestExpect(Instruction.lda, Addressing.zeroPage, true),
    0xa6 -> TestExpect(Instruction.ldx, Addressing.zeroPage, true),
    0xa7 -> TestExpect(Instruction.lax, Addressing.zeroPage, true),
    0xa8 -> TestExpect(Instruction.tay, Addressing.implied, false),
    0xa9 -> TestExpect(Instruction.lda, Addressing.immediate, false),
    0xaa -> TestExpect(Instruction.tax, Addressing.implied, false),
    0xab -> TestExpect(Instruction.lax2, Addressing.immediate, false),
    0xac -> TestExpect(Instruction.ldy, Addressing.absolute, true),
    0xad -> TestExpect(Instruction.lda, Addressing.absolute, true),
    0xae -> TestExpect(Instruction.ldx, Addressing.absolute, true),
    0xaf -> TestExpect(Instruction.lax, Addressing.absolute, true),
    // bx
    0xb0 -> TestExpect(Instruction.bcs, Addressing.relative, false),
    0xb1 -> TestExpect(Instruction.lda, Addressing.indirectYIndexed, true),
    0xb2 -> TestExpect(Instruction.halt, Addressing.invalid, false),
    0xb3 -> TestExpect(Instruction.lax, Addressing.indirectYIndexed, true),
    0xb4 -> TestExpect(Instruction.ldy, Addressing.xIndexedZeroPage, true),
    0xb5 -> TestExpect(Instruction.lda, Addressing.xIndexedZeroPage, true),
    0xb6 -> TestExpect(Instruction.ldx, Addressing.yIndexedZeroPage, true),
    0xb7 -> TestExpect(Instruction.lax, Addressing.yIndexedZeroPage, true),
    0xb8 -> TestExpect(Instruction.clv, Addressing.implied, false),
    0xb9 -> TestExpect(Instruction.lda, Addressing.yIndexedAbsolute, true),
    0xba -> TestExpect(Instruction.tsx, Addressing.implied, false),
    0xbb -> TestExpect(Instruction.las, Addressing.yIndexedAbsolute, true),
    0xbc -> TestExpect(Instruction.ldy, Addressing.xIndexedAbsolute, true),
    0xbd -> TestExpect(Instruction.lda, Addressing.xIndexedAbsolute, true),
    0xbe -> TestExpect(Instruction.ldx, Addressing.yIndexedAbsolute, true),
    0xbf -> TestExpect(Instruction.lax, Addressing.yIndexedAbsolute, true),
    // cx
    0xc0 -> TestExpect(Instruction.cpy, Addressing.immediate, false),
    0xc1 -> TestExpect(Instruction.cmp, Addressing.xIndexedIndirect, true),
    0xc2 -> TestExpect(Instruction.nop, Addressing.immediate, false),
    0xc3 -> TestExpect(Instruction.dcp, Addressing.xIndexedIndirect, true),
    0xc4 -> TestExpect(Instruction.cpy, Addressing.zeroPage, true),
    0xc5 -> TestExpect(Instruction.cmp, Addressing.zeroPage, true),
    0xc6 -> TestExpect(Instruction.dec, Addressing.zeroPage, true),
    0xc7 -> TestExpect(Instruction.dcp, Addressing.zeroPage, true),
    0xc8 -> TestExpect(Instruction.iny, Addressing.implied, false),
    0xc9 -> TestExpect(Instruction.cmp, Addressing.immediate, false),
    0xca -> TestExpect(Instruction.dex, Addressing.implied, false),
    0xcb -> TestExpect(Instruction.axs, Addressing.immediate, false),
    0xcc -> TestExpect(Instruction.cpy, Addressing.absolute, true),
    0xcd -> TestExpect(Instruction.cmp, Addressing.absolute, true),
    0xce -> TestExpect(Instruction.dec, Addressing.absolute, true),
    0xcf -> TestExpect(Instruction.dcp, Addressing.absolute, true),
    // dx
    0xd0 -> TestExpect(Instruction.bne, Addressing.relative, false),
    0xd1 -> TestExpect(Instruction.cmp, Addressing.indirectYIndexed, true),
    0xd2 -> TestExpect(Instruction.halt, Addressing.invalid, false),
    0xd3 -> TestExpect(Instruction.dcp, Addressing.indirectYIndexed, true),
    0xd4 -> TestExpect(Instruction.nop, Addressing.xIndexedZeroPage, true),
    0xd5 -> TestExpect(Instruction.cmp, Addressing.xIndexedZeroPage, true),
    0xd6 -> TestExpect(Instruction.dec, Addressing.xIndexedZeroPage, true),
    0xd7 -> TestExpect(Instruction.dcp, Addressing.xIndexedZeroPage, true),
    0xd8 -> TestExpect(Instruction.cld, Addressing.implied, false),
    0xd9 -> TestExpect(Instruction.cmp, Addressing.yIndexedAbsolute, true),
    0xda -> TestExpect(Instruction.nop, Addressing.implied, false),
    0xdb -> TestExpect(Instruction.dcp, Addressing.yIndexedAbsolute, true),
    0xdc -> TestExpect(Instruction.nop, Addressing.xIndexedAbsolute, true),
    0xdd -> TestExpect(Instruction.cmp, Addressing.xIndexedAbsolute, true),
    0xde -> TestExpect(Instruction.dec, Addressing.xIndexedAbsolute, true),
    0xdf -> TestExpect(Instruction.dcp, Addressing.xIndexedAbsolute, true),
    // ex
    0xe0 -> TestExpect(Instruction.cpx, Addressing.immediate, false),
    0xe1 -> TestExpect(Instruction.sbc, Addressing.xIndexedIndirect, true),
    0xe2 -> TestExpect(Instruction.nop, Addressing.immediate, false),
    0xe3 -> TestExpect(Instruction.isc, Addressing.xIndexedIndirect, true),
    0xe4 -> TestExpect(Instruction.cpx, Addressing.zeroPage, true),
    0xe5 -> TestExpect(Instruction.sbc, Addressing.zeroPage, true),
    0xe6 -> TestExpect(Instruction.inc, Addressing.zeroPage, true),
    0xe7 -> TestExpect(Instruction.isc, Addressing.zeroPage, true),
    0xe8 -> TestExpect(Instruction.inx, Addressing.implied, false),
    0xe9 -> TestExpect(Instruction.sbc, Addressing.immediate, false),
    0xea -> TestExpect(Instruction.nop, Addressing.implied, false),
    0xeb -> TestExpect(Instruction.sbc2, Addressing.immediate, false),
    0xec -> TestExpect(Instruction.cpx, Addressing.absolute, true),
    0xed -> TestExpect(Instruction.sbc, Addressing.absolute, true),
    0xee -> TestExpect(Instruction.inc, Addressing.absolute, true),
    0xef -> TestExpect(Instruction.isc, Addressing.absolute, true),
    // fx
    0xf0 -> TestExpect(Instruction.beq, Addressing.relative, false),
    0xf1 -> TestExpect(Instruction.sbc, Addressing.indirectYIndexed, true),
    0xf2 -> TestExpect(Instruction.halt, Addressing.invalid, false),
    0xf3 -> TestExpect(Instruction.isc, Addressing.indirectYIndexed, true),
    0xf4 -> TestExpect(Instruction.nop, Addressing.xIndexedZeroPage, true),
    0xf5 -> TestExpect(Instruction.sbc, Addressing.xIndexedZeroPage, true),
    0xf6 -> TestExpect(Instruction.inc, Addressing.xIndexedZeroPage, true),
    0xf7 -> TestExpect(Instruction.isc, Addressing.xIndexedZeroPage, true),
    0xf8 -> TestExpect(Instruction.sed, Addressing.implied, false),
    0xf9 -> TestExpect(Instruction.sbc, Addressing.yIndexedAbsolute, true),
    0xfa -> TestExpect(Instruction.nop, Addressing.implied, false),
    0xfb -> TestExpect(Instruction.isc, Addressing.yIndexedAbsolute, true),
    0xfc -> TestExpect(Instruction.nop, Addressing.xIndexedAbsolute, true),
    0xfd -> TestExpect(Instruction.sbc, Addressing.xIndexedAbsolute, true),
    0xfe -> TestExpect(Instruction.inc, Addressing.xIndexedAbsolute, true),
    0xff -> TestExpect(Instruction.isc, Addressing.xIndexedAbsolute, true),
  )
  // 本家で定義されているテーブル(UIntでMapにすると、論理環境でうまくKeyが見つからなかったので数値に変換)
  val decode: Map[Int, (Instruction.Type, Addressing.Type)] =
    InstructionDecode.lookUpTable.map { case (opcode, data) =>
      (opcode.litValue.toInt -> data)
    }.toMap

  expectTable foreach {
    case (opcode, TestExpect(expectInst, expectAddressing, expectNeedRead)) => {
      val (inst, addressing) = decode(opcode)
      f"opcode[$opcode%02x] is ($expectInst, $expectAddressing)" in {
        assert(inst == expectInst)
        assert(addressing == expectAddressing)
      }
      f"opcode[$opcode%02x] requires Read($expectNeedRead)" in {
        val reqRead = InstructionDecode.needDataRead(inst, addressing)
        assert(reqRead == expectNeedRead)
      }
    }
  }
}
