package cpu

import chisel3._
import cpu.types.Addressing
import cpu.types.Instruction

/** 
 * IFで取得した命令をDecodeしてOFへのOperand取得依頼、ALUで実行する命令の決定を行う...が
 * 命令が可変長で8bit BusでOFで複数回Readが必要なことから IF->ID->OFはpipeline化しない。
 */
object InstructionDecode {
  // opcode -> Instructionの対応を取得する
  def lookUpTableForInstruction(): Seq[(UInt, Instruction.Type)] = InstructionDecode.lookUpTable.map { case (opcode, (instruction, addressing)) => opcode -> instruction }
  // opcode -> Addressingの対応を取得する
  def lookUpTableForAddressing(): Seq[(UInt, Addressing.Type)] = InstructionDecode.lookUpTable.map { case (opcode, (instruction, addressing)) => opcode -> addressing }
  // Opcodeと命令/アドレッシングモードの対応
  def lookUpTable: Seq[(UInt, (Instruction.Type, Addressing.Type))] = Seq(
    // binary
    0x69.U -> (Instruction.adc, Addressing.immediate),
    0x65.U -> (Instruction.adc, Addressing.zeroPage),
    0x75.U -> (Instruction.adc, Addressing.xIndexedZeroPage),
    0x6d.U -> (Instruction.adc, Addressing.absolute),
    0x7d.U -> (Instruction.adc, Addressing.xIndexedAbsolute),
    0x79.U -> (Instruction.adc, Addressing.yIndexedAbsolute),
    0x61.U -> (Instruction.adc, Addressing.xIndexedIndirect),
    0x71.U -> (Instruction.adc, Addressing.indirectYIndexed),
    0xe9.U -> (Instruction.sbc, Addressing.immediate),
    0xe5.U -> (Instruction.sbc, Addressing.zeroPage),
    0xf5.U -> (Instruction.sbc, Addressing.xIndexedZeroPage),
    0xed.U -> (Instruction.sbc, Addressing.absolute),
    0xfd.U -> (Instruction.sbc, Addressing.xIndexedAbsolute),
    0xf9.U -> (Instruction.sbc, Addressing.yIndexedAbsolute),
    0xe1.U -> (Instruction.sbc, Addressing.xIndexedIndirect),
    0xf1.U -> (Instruction.sbc, Addressing.indirectYIndexed),
    0x29.U -> (Instruction.and, Addressing.immediate),
    0x25.U -> (Instruction.and, Addressing.zeroPage),
    0x35.U -> (Instruction.and, Addressing.xIndexedZeroPage),
    0x2d.U -> (Instruction.and, Addressing.absolute),
    0x3d.U -> (Instruction.and, Addressing.xIndexedAbsolute),
    0x39.U -> (Instruction.and, Addressing.yIndexedAbsolute),
    0x21.U -> (Instruction.and, Addressing.xIndexedIndirect),
    0x31.U -> (Instruction.and, Addressing.indirectYIndexed),
    0x49.U -> (Instruction.eor, Addressing.immediate),
    0x45.U -> (Instruction.eor, Addressing.zeroPage),
    0x55.U -> (Instruction.eor, Addressing.xIndexedZeroPage),
    0x4d.U -> (Instruction.eor, Addressing.absolute),
    0x5d.U -> (Instruction.eor, Addressing.xIndexedAbsolute),
    0x59.U -> (Instruction.eor, Addressing.yIndexedAbsolute),
    0x41.U -> (Instruction.eor, Addressing.xIndexedIndirect),
    0x51.U -> (Instruction.eor, Addressing.indirectYIndexed),
    0x09.U -> (Instruction.ora, Addressing.immediate),
    0x05.U -> (Instruction.ora, Addressing.zeroPage),
    0x15.U -> (Instruction.ora, Addressing.xIndexedZeroPage),
    0x0d.U -> (Instruction.ora, Addressing.absolute),
    0x1d.U -> (Instruction.ora, Addressing.xIndexedAbsolute),
    0x19.U -> (Instruction.ora, Addressing.yIndexedAbsolute),
    0x01.U -> (Instruction.ora, Addressing.xIndexedIndirect),
    0x11.U -> (Instruction.ora, Addressing.indirectYIndexed),

    // shift, rotate
    0x0a.U -> (Instruction.asl, Addressing.accumulator),
    0x06.U -> (Instruction.asl, Addressing.zeroPage),
    0x16.U -> (Instruction.asl, Addressing.xIndexedZeroPage),
    0x0e.U -> (Instruction.asl, Addressing.absolute),
    0x1e.U -> (Instruction.asl, Addressing.xIndexedAbsolute),
    0x4a.U -> (Instruction.lsr, Addressing.accumulator),
    0x46.U -> (Instruction.lsr, Addressing.zeroPage),
    0x56.U -> (Instruction.lsr, Addressing.xIndexedZeroPage),
    0x4e.U -> (Instruction.lsr, Addressing.absolute),
    0x5e.U -> (Instruction.lsr, Addressing.xIndexedAbsolute),
    0x2a.U -> (Instruction.rol, Addressing.accumulator),
    0x26.U -> (Instruction.rol, Addressing.zeroPage),
    0x36.U -> (Instruction.rol, Addressing.xIndexedZeroPage),
    0x2e.U -> (Instruction.rol, Addressing.absolute),
    0x3e.U -> (Instruction.rol, Addressing.xIndexedAbsolute),
    0x6a.U -> (Instruction.ror, Addressing.accumulator),
    0x66.U -> (Instruction.ror, Addressing.zeroPage),
    0x76.U -> (Instruction.ror, Addressing.xIndexedZeroPage),
    0x6e.U -> (Instruction.ror, Addressing.absolute),
    0x7e.U -> (Instruction.ror, Addressing.xIndexedAbsolute),

    // inc,dec
    0xe6.U -> (Instruction.inc, Addressing.zeroPage),
    0xf6.U -> (Instruction.inc, Addressing.xIndexedZeroPage),
    0xee.U -> (Instruction.inc, Addressing.absolute),
    0xfe.U -> (Instruction.inc, Addressing.xIndexedAbsolute),
    0xe8.U -> (Instruction.inx, Addressing.implied),
    0xc8.U -> (Instruction.iny, Addressing.implied),
    0xc6.U -> (Instruction.dec, Addressing.zeroPage),
    0xd6.U -> (Instruction.dec, Addressing.xIndexedZeroPage),
    0xce.U -> (Instruction.dec, Addressing.absolute),
    0xde.U -> (Instruction.dec, Addressing.xIndexedAbsolute),
    0xca.U -> (Instruction.dex, Addressing.implied),
    0x88.U -> (Instruction.dey, Addressing.implied),

    // load/store
    0xa9.U -> (Instruction.lda, Addressing.immediate),
    0xa5.U -> (Instruction.lda, Addressing.zeroPage),
    0xb5.U -> (Instruction.lda, Addressing.xIndexedZeroPage),
    0xad.U -> (Instruction.lda, Addressing.absolute),
    0xbd.U -> (Instruction.lda, Addressing.xIndexedAbsolute),
    0xb9.U -> (Instruction.lda, Addressing.yIndexedAbsolute),
    0xa1.U -> (Instruction.lda, Addressing.xIndexedIndirect),
    0xb1.U -> (Instruction.lda, Addressing.indirectYIndexed),
    0xa2.U -> (Instruction.ldx, Addressing.immediate),
    0xa6.U -> (Instruction.ldx, Addressing.zeroPage),
    0xb6.U -> (Instruction.ldx, Addressing.yIndexedZeroPage),
    0xae.U -> (Instruction.ldx, Addressing.absolute),
    0xbe.U -> (Instruction.ldx, Addressing.yIndexedAbsolute),
    0xa0.U -> (Instruction.ldy, Addressing.immediate),
    0xa4.U -> (Instruction.ldy, Addressing.zeroPage),
    0xb4.U -> (Instruction.ldy, Addressing.xIndexedZeroPage),
    0xac.U -> (Instruction.ldy, Addressing.absolute),
    0xbc.U -> (Instruction.ldy, Addressing.xIndexedAbsolute),
    0x85.U -> (Instruction.sta, Addressing.zeroPage),
    0x95.U -> (Instruction.sta, Addressing.xIndexedZeroPage),
    0x8d.U -> (Instruction.sta, Addressing.absolute),
    0x9d.U -> (Instruction.sta, Addressing.xIndexedAbsolute),
    0x99.U -> (Instruction.sta, Addressing.yIndexedAbsolute),
    0x81.U -> (Instruction.sta, Addressing.xIndexedIndirect),
    0x91.U -> (Instruction.sta, Addressing.indirectYIndexed),
    0x86.U -> (Instruction.stx, Addressing.zeroPage),
    0x96.U -> (Instruction.stx, Addressing.yIndexedZeroPage),
    0x8e.U -> (Instruction.stx, Addressing.absolute),
    0x84.U -> (Instruction.sty, Addressing.zeroPage),
    0x94.U -> (Instruction.sty, Addressing.xIndexedZeroPage),
    0x8c.U -> (Instruction.sty, Addressing.absolute),

    // set,clear
    0x38.U -> (Instruction.sec, Addressing.implied),
    0xf8.U -> (Instruction.sed, Addressing.implied),
    0x78.U -> (Instruction.sei, Addressing.implied),
    0x18.U -> (Instruction.clc, Addressing.implied),
    0xd8.U -> (Instruction.cld, Addressing.implied),
    0x58.U -> (Instruction.cli, Addressing.implied),
    0xb8.U -> (Instruction.clv, Addressing.implied),

    // compare
    0xc9.U -> (Instruction.cmp, Addressing.immediate),
    0xc5.U -> (Instruction.cmp, Addressing.zeroPage),
    0xd5.U -> (Instruction.cmp, Addressing.xIndexedZeroPage),
    0xcd.U -> (Instruction.cmp, Addressing.absolute),
    0xdd.U -> (Instruction.cmp, Addressing.xIndexedAbsolute),
    0xd9.U -> (Instruction.cmp, Addressing.yIndexedAbsolute),
    0xc1.U -> (Instruction.cmp, Addressing.xIndexedIndirect),
    0xd1.U -> (Instruction.cmp, Addressing.indirectYIndexed),
    0xe0.U -> (Instruction.cpx, Addressing.immediate),
    0xe4.U -> (Instruction.cpx, Addressing.zeroPage),
    0xec.U -> (Instruction.cpx, Addressing.absolute),
    0xc0.U -> (Instruction.cpy, Addressing.immediate),
    0xc4.U -> (Instruction.cpy, Addressing.zeroPage),
    0xcc.U -> (Instruction.cpy, Addressing.absolute),

    // jump,return
    0x4c.U -> (Instruction.jmp, Addressing.absolute),
    0x6c.U -> (Instruction.jmp, Addressing.indirect),
    0x20.U -> (Instruction.jsr, Addressing.absolute),
    0x40.U -> (Instruction.rti, Addressing.implied),
    0x60.U -> (Instruction.rts, Addressing.implied),

    // branch
    0x90.U -> (Instruction.bcc, Addressing.relative),
    0xb0.U -> (Instruction.bcs, Addressing.relative),
    0xf0.U -> (Instruction.beq, Addressing.relative),
    0xd0.U -> (Instruction.bne, Addressing.relative),
    0x30.U -> (Instruction.bmi, Addressing.relative),
    0x10.U -> (Instruction.bpl, Addressing.relative),
    0x50.U -> (Instruction.bvc, Addressing.relative),
    0x70.U -> (Instruction.bvs, Addressing.relative),

    // push, pop
    0x48.U -> (Instruction.pha, Addressing.implied),
    0x08.U -> (Instruction.php, Addressing.implied),
    0x68.U -> (Instruction.pla, Addressing.implied),
    0x28.U -> (Instruction.plp, Addressing.implied),

    // data transfer
    0xaa.U -> (Instruction.tax, Addressing.implied),
    0xa8.U -> (Instruction.tay, Addressing.implied),
    0xba.U -> (Instruction.tsx, Addressing.implied),
    0x8a.U -> (Instruction.txa, Addressing.implied),
    0x9a.U -> (Instruction.txs, Addressing.implied),
    0x98.U -> (Instruction.tya, Addressing.implied),

    // other
    0x00.U -> (Instruction.brk, Addressing.implied),
    0x24.U -> (Instruction.bit, Addressing.zeroPage),
    0x2c.U -> (Instruction.bit, Addressing.absolute),
    0xea.U -> (Instruction.nop, Addressing.implied),

    // undocumented
    0x4b.U -> (Instruction.alr, Addressing.immediate),
    0x0b.U -> (Instruction.anc, Addressing.immediate),
    0x6b.U -> (Instruction.arr, Addressing.immediate),
    0xcb.U -> (Instruction.sbx, Addressing.immediate),
    0xa3.U -> (Instruction.lax, Addressing.xIndexedIndirect),
    0xa7.U -> (Instruction.lax, Addressing.zeroPage),
    0xaf.U -> (Instruction.lax, Addressing.absolute),
    0xb3.U -> (Instruction.lax, Addressing.indirectYIndexed),
    0xb7.U -> (Instruction.lax, Addressing.yIndexedZeroPage),
    0xbf.U -> (Instruction.lax, Addressing.yIndexedAbsolute),
    0x83.U -> (Instruction.sax, Addressing.xIndexedIndirect),
    0x87.U -> (Instruction.sax, Addressing.zeroPage),
    0x8f.U -> (Instruction.sax, Addressing.absolute),
    0x97.U -> (Instruction.sax, Addressing.yIndexedZeroPage),
    0xc3.U -> (Instruction.dcp, Addressing.xIndexedIndirect),
    0xc7.U -> (Instruction.dcp, Addressing.zeroPage),
    0xcf.U -> (Instruction.dcp, Addressing.absolute),
    0xd3.U -> (Instruction.dcp, Addressing.indirectYIndexed),
    0xd7.U -> (Instruction.dcp, Addressing.xIndexedZeroPage),
    0xdb.U -> (Instruction.dcp, Addressing.yIndexedAbsolute),
    0xdf.U -> (Instruction.dcp, Addressing.xIndexedAbsolute),
    0xe3.U -> (Instruction.isc, Addressing.xIndexedIndirect),
    0xe7.U -> (Instruction.isc, Addressing.zeroPage),
    0xef.U -> (Instruction.isc, Addressing.absolute),
    0xf3.U -> (Instruction.isc, Addressing.indirectYIndexed),
    0xf7.U -> (Instruction.isc, Addressing.xIndexedZeroPage),
    0xfb.U -> (Instruction.isc, Addressing.yIndexedAbsolute),
    0xff.U -> (Instruction.isc, Addressing.xIndexedAbsolute),
    0x80.U -> (Instruction.nop, Addressing.immediate),
    0x82.U -> (Instruction.nop, Addressing.immediate),
    0x89.U -> (Instruction.nop, Addressing.immediate),
    0xc2.U -> (Instruction.nop, Addressing.immediate),
    0xe2.U -> (Instruction.nop, Addressing.immediate),
    0x04.U -> (Instruction.nop, Addressing.zeroPage),
    0x44.U -> (Instruction.nop, Addressing.zeroPage),
    0x64.U -> (Instruction.nop, Addressing.zeroPage),
    0x14.U -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0x34.U -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0x54.U -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0x74.U -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0xd4.U -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0xf4.U -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0x0c.U -> (Instruction.nop, Addressing.absolute),
    0x1c.U -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0x3c.U -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0x5c.U -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0x7c.U -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0xdc.U -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0xfc.U -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0x1a.U -> (Instruction.nop, Addressing.implied),
    0x3a.U -> (Instruction.nop, Addressing.implied),
    0x5a.U -> (Instruction.nop, Addressing.implied),
    0x7a.U -> (Instruction.nop, Addressing.implied),
    0xda.U -> (Instruction.nop, Addressing.implied),
    0xfa.U -> (Instruction.nop, Addressing.implied),
    0x23.U -> (Instruction.rla, Addressing.xIndexedIndirect),
    0x27.U -> (Instruction.rla, Addressing.zeroPage),
    0x2f.U -> (Instruction.rla, Addressing.absolute),
    0x33.U -> (Instruction.rla, Addressing.indirectYIndexed),
    0x37.U -> (Instruction.rla, Addressing.xIndexedZeroPage),
    0x3b.U -> (Instruction.rla, Addressing.yIndexedAbsolute),
    0x3f.U -> (Instruction.rla, Addressing.xIndexedAbsolute),
    0x63.U -> (Instruction.rra, Addressing.xIndexedIndirect),
    0x67.U -> (Instruction.rra, Addressing.zeroPage),
    0x6f.U -> (Instruction.rra, Addressing.absolute),
    0x73.U -> (Instruction.rra, Addressing.indirectYIndexed),
    0x77.U -> (Instruction.rra, Addressing.xIndexedZeroPage),
    0x7b.U -> (Instruction.rra, Addressing.yIndexedAbsolute),
    0x7f.U -> (Instruction.rra, Addressing.xIndexedAbsolute),
    0x03.U -> (Instruction.slo, Addressing.xIndexedIndirect),
    0x07.U -> (Instruction.slo, Addressing.zeroPage),
    0x0f.U -> (Instruction.slo, Addressing.absolute),
    0x13.U -> (Instruction.slo, Addressing.indirectYIndexed),
    0x17.U -> (Instruction.slo, Addressing.xIndexedZeroPage),
    0x1b.U -> (Instruction.slo, Addressing.yIndexedAbsolute),
    0x1f.U -> (Instruction.slo, Addressing.xIndexedAbsolute),
    0x43.U -> (Instruction.sre, Addressing.xIndexedIndirect),
    0x47.U -> (Instruction.sre, Addressing.zeroPage),
    0x4f.U -> (Instruction.sre, Addressing.absolute),
    0x53.U -> (Instruction.sre, Addressing.indirectYIndexed),
    0x57.U -> (Instruction.sre, Addressing.xIndexedZeroPage),
    0x5b.U -> (Instruction.sre, Addressing.yIndexedAbsolute),
    0x5f.U -> (Instruction.sre, Addressing.xIndexedAbsolute),
    0xeb.U -> (Instruction.usbc, Addressing.immediate),

    // illegal
    0x02.U -> (Instruction.jam, Addressing.invalid),
    0x12.U -> (Instruction.jam, Addressing.invalid),
    0x22.U -> (Instruction.jam, Addressing.invalid),
    0x32.U -> (Instruction.jam, Addressing.invalid),
    0x42.U -> (Instruction.jam, Addressing.invalid),
    0x52.U -> (Instruction.jam, Addressing.invalid),
    0x62.U -> (Instruction.jam, Addressing.invalid),
    0x72.U -> (Instruction.jam, Addressing.invalid),
    0x92.U -> (Instruction.jam, Addressing.invalid),
    0xb2.U -> (Instruction.jam, Addressing.invalid),
    0xd2.U -> (Instruction.jam, Addressing.invalid),
    0xf2.U -> (Instruction.jam, Addressing.invalid)
  )
}
