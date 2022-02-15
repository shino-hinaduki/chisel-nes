package cpu

import chisel3._
import cpu.types.Addressing
import cpu.types.Instruction

/** 
 * IFで取得した命令をDecodeしてOFへのOperand取得依頼、ALUで実行する命令の決定を行う...が
 * 命令が可変長で8bit BusでOFで複数回Readが必要なことから IF->ID->OFはpipeline化しない。
 */
object InstructionDecode {
  /* opcode -> Instructionの対応を取得 */
  def lookUpTableForInstruction(): Seq[(UInt, Instruction.Type)] = InstructionDecode.lookUpTable.map { case (opcode, (instruction, addressing)) => opcode -> instruction }

  /* opcode -> Addressingの対応を取得 */
  def lookUpTableForAddressing(): Seq[(UInt, Addressing.Type)] = InstructionDecode.lookUpTable.map { case (opcode, (instruction, addressing)) => opcode -> addressing }

  /* OperandFetchで求めたアドレスに対する事前のDataReadが必要であればtrueを返す */
  def needDataRead(inst: Instruction.Type, addressing: Addressing.Type): Boolean = (inst, addressing) match {
    // 不要なもの、Aregを使うものなどは事前に除外
    case (_, Addressing.invalid)     => false
    case (_, Addressing.implied)     => false
    case (_, Addressing.accumulator) => false // A regの値を使うので不要
    case (_, Addressing.immediate)   => false // opcodeの後ろの値
    case (_, Addressing.relative)    => false // PC := PC+offsetなので不要
    // Logical, Arithmetic
    case (Instruction.ora, _) => true  // A := A | (*addr)
    case (Instruction.and, _) => true  // A := A & (*addr)
    case (Instruction.eor, _) => true  // A := A ^ (*addr)
    case (Instruction.adc, _) => true  // A := A + (*addr)
    case (Instruction.sbc, _) => true  // A := A - (*addr)
    case (Instruction.cmp, _) => true  // A == (*addr)
    case (Instruction.cpx, _) => true  // X == (*addr)
    case (Instruction.cpy, _) => true  // Y == (*addr)
    case (Instruction.dec, _) => true  // (*addr) := (*addr) - 1
    case (Instruction.dex, _) => false // X       := X       - 1
    case (Instruction.dey, _) => false // Y       := Y       - 1
    case (Instruction.inc, _) => true  // (*addr) := (*addr) + 1
    case (Instruction.inx, _) => false // X       := X       + 1
    case (Instruction.iny, _) => false // Y       := Y       + 1
    case (Instruction.asl, _) => true  // (*addr) := (*addr) << 1
    case (Instruction.rol, _) => true  // (*addr) := ((*addr) << 1) | (carry ? 0x01 : 0x00)
    case (Instruction.lsr, _) => true  // (*addr) := (*addr) >> 1
    case (Instruction.ror, _) => true  // (*addr) := ((*addr) >> 1) | (carry ? 0x80 : 0x00)
    // Move
    case (Instruction.lda, _) => true  // A       := (*addr)
    case (Instruction.sta, _) => false // (*addr) := A
    case (Instruction.ldx, _) => true  // X       := (*addr)
    case (Instruction.stx, _) => false // (*addr) := X
    case (Instruction.ldy, _) => true  // X       := (*addr)
    case (Instruction.sty, _) => false // (*addr) := X
    case (Instruction.tax, _) => false // X       := A
    case (Instruction.txa, _) => false // A       := X
    case (Instruction.tay, _) => false // Y       := A
    case (Instruction.tya, _) => false // A       := Y
    case (Instruction.tsx, _) => false // X       := S
    case (Instruction.txs, _) => false // S       := X
    case (Instruction.pla, _) => false // sp++, A := (*sp)
    case (Instruction.pha, _) => false // (*sp)   := A    , sp--
    case (Instruction.plp, _) => false // sp++, P := (*sp)
    case (Instruction.php, _) => false // (*sp)   := P    , sp--
    // Jump, Flag
    case (Instruction.bpl, _) => false // branch on negative==0
    case (Instruction.bmi, _) => false // branch on negative==1
    case (Instruction.bvc, _) => false // branch on overflow==0
    case (Instruction.bvs, _) => false // branch on overflow==1
    case (Instruction.bcc, _) => false // branch on carry==0
    case (Instruction.bcs, _) => false // branch on carry==1
    case (Instruction.bne, _) => false // branch on zero==0
    case (Instruction.beq, _) => false // branch on zero==1
    case (Instruction.brk, _) => false // (*sp) := pc, sp--, (*sp) := p, pc := 0xfffe
    case (Instruction.rti, _) => false // p := (*sp), sp++, pc := (*sp), sp++
    case (Instruction.jsr, _) => true  // (*sp) := pc, sp--, pc := (*addr)
    case (Instruction.rts, _) => false // sp++, pc := (*sp)
    case (Instruction.jmp, _) => false // pc := addr
    case (Instruction.bit, _) => true  // negative := ((*addr) >> 7) & 0x1, overflow := ((*addr) >> 6) & 0x1, zero := (A & (*addr)) != 0
    case (Instruction.clc, _) => false // carry=0
    case (Instruction.sec, _) => false // carry=1
    case (Instruction.cld, _) => false // decimal=0
    case (Instruction.sed, _) => false // decimal=1
    case (Instruction.cli, _) => false // interrupt=0
    case (Instruction.sei, _) => false // interrupt=1
    case (Instruction.clv, _) => false // overflow=0
    // 0xea nop は (_, Implied) で事前に除外済。 Illegal opcodeのnopはfetchして捨てるケースが有るため明示しない

    // Illegal
    case (Instruction.slo, _)  => true  // ASL + ORA
    case (Instruction.rla, _)  => true  // ROL + AND
    case (Instruction.sre, _)  => true  // LSR + EOR
    case (Instruction.rra, _)  => true  // ROR + ADC
    case (Instruction.sax, _)  => false // (*addr) := A & X
    case (Instruction.lax, _)  => true  // LDA + LDX
    case (Instruction.dcp, _)  => true  // DEC + CMP
    case (Instruction.isc, _)  => true  // INC + SBC
    case (Instruction.anc, _)  => false // AND + ASL          (Imm Only)
    case (Instruction.anc2, _) => false // AND + ROL          (Imm Only)
    case (Instruction.alr, _)  => false // AND + LSR          (Imm Only)
    case (Instruction.arr, _)  => false // AND + ROR          (Imm Only)
    case (Instruction.xaa, _)  => false // Imm & X + AND      (Imm Only)
    case (Instruction.lax2, _) => false // A := Imm, X := Imm (Imm Only)
    case (Instruction.axs, _)  => false // X := Imm & X & A   (Imm Only)
    case (Instruction.sbc2, _) => false // SBC + NOP          (Imm Only)
    case (Instruction.ahx, _)  => false // (*addr) := A & X & (addr >> 8)
    case (Instruction.shy, _)  => false // (*addr) := Y     & (addr >> 8)
    case (Instruction.shx, _)  => false // (*addr) := X     & (addr >> 8)
    case (Instruction.tas, _)  => false // (*sp) := A & X, (*addr) := A & X * (addr >> 8)
    case (Instruction.las, _)  => true  // LDA + TSX
    // default(nopかつFetchするケースなどは読み捨てるためtrue)
    case _ => true
  }

  /* Opcodeと命令/アドレッシングモードの対応 */
  def lookUpTable: Seq[(UInt, (Instruction.Type, Addressing.Type))] = Seq(
    // Logical, Arithmetic
    0x09.U(8.W) -> (Instruction.ora, Addressing.immediate),
    0x05.U(8.W) -> (Instruction.ora, Addressing.zeroPage),
    0x15.U(8.W) -> (Instruction.ora, Addressing.xIndexedZeroPage),
    0x0d.U(8.W) -> (Instruction.ora, Addressing.absolute),
    0x1d.U(8.W) -> (Instruction.ora, Addressing.xIndexedAbsolute),
    0x19.U(8.W) -> (Instruction.ora, Addressing.yIndexedAbsolute),
    0x01.U(8.W) -> (Instruction.ora, Addressing.xIndexedIndirect),
    0x11.U(8.W) -> (Instruction.ora, Addressing.indirectYIndexed),
    0x29.U(8.W) -> (Instruction.and, Addressing.immediate),
    0x25.U(8.W) -> (Instruction.and, Addressing.zeroPage),
    0x35.U(8.W) -> (Instruction.and, Addressing.xIndexedZeroPage),
    0x2d.U(8.W) -> (Instruction.and, Addressing.absolute),
    0x3d.U(8.W) -> (Instruction.and, Addressing.xIndexedAbsolute),
    0x39.U(8.W) -> (Instruction.and, Addressing.yIndexedAbsolute),
    0x21.U(8.W) -> (Instruction.and, Addressing.xIndexedIndirect),
    0x31.U(8.W) -> (Instruction.and, Addressing.indirectYIndexed),
    0x49.U(8.W) -> (Instruction.eor, Addressing.immediate),
    0x45.U(8.W) -> (Instruction.eor, Addressing.zeroPage),
    0x55.U(8.W) -> (Instruction.eor, Addressing.xIndexedZeroPage),
    0x4d.U(8.W) -> (Instruction.eor, Addressing.absolute),
    0x5d.U(8.W) -> (Instruction.eor, Addressing.xIndexedAbsolute),
    0x59.U(8.W) -> (Instruction.eor, Addressing.yIndexedAbsolute),
    0x41.U(8.W) -> (Instruction.eor, Addressing.xIndexedIndirect),
    0x51.U(8.W) -> (Instruction.eor, Addressing.indirectYIndexed),
    0x69.U(8.W) -> (Instruction.adc, Addressing.immediate),
    0x65.U(8.W) -> (Instruction.adc, Addressing.zeroPage),
    0x75.U(8.W) -> (Instruction.adc, Addressing.xIndexedZeroPage),
    0x6d.U(8.W) -> (Instruction.adc, Addressing.absolute),
    0x7d.U(8.W) -> (Instruction.adc, Addressing.xIndexedAbsolute),
    0x79.U(8.W) -> (Instruction.adc, Addressing.yIndexedAbsolute),
    0x61.U(8.W) -> (Instruction.adc, Addressing.xIndexedIndirect),
    0x71.U(8.W) -> (Instruction.adc, Addressing.indirectYIndexed),
    0xe9.U(8.W) -> (Instruction.sbc, Addressing.immediate),
    0xe5.U(8.W) -> (Instruction.sbc, Addressing.zeroPage),
    0xf5.U(8.W) -> (Instruction.sbc, Addressing.xIndexedZeroPage),
    0xed.U(8.W) -> (Instruction.sbc, Addressing.absolute),
    0xfd.U(8.W) -> (Instruction.sbc, Addressing.xIndexedAbsolute),
    0xf9.U(8.W) -> (Instruction.sbc, Addressing.yIndexedAbsolute),
    0xe1.U(8.W) -> (Instruction.sbc, Addressing.xIndexedIndirect),
    0xf1.U(8.W) -> (Instruction.sbc, Addressing.indirectYIndexed),
    0xc9.U(8.W) -> (Instruction.cmp, Addressing.immediate),
    0xc5.U(8.W) -> (Instruction.cmp, Addressing.zeroPage),
    0xd5.U(8.W) -> (Instruction.cmp, Addressing.xIndexedZeroPage),
    0xcd.U(8.W) -> (Instruction.cmp, Addressing.absolute),
    0xdd.U(8.W) -> (Instruction.cmp, Addressing.xIndexedAbsolute),
    0xd9.U(8.W) -> (Instruction.cmp, Addressing.yIndexedAbsolute),
    0xc1.U(8.W) -> (Instruction.cmp, Addressing.xIndexedIndirect),
    0xd1.U(8.W) -> (Instruction.cmp, Addressing.indirectYIndexed),
    0xe0.U(8.W) -> (Instruction.cpx, Addressing.immediate),
    0xe4.U(8.W) -> (Instruction.cpx, Addressing.zeroPage),
    0xec.U(8.W) -> (Instruction.cpx, Addressing.absolute),
    0xc0.U(8.W) -> (Instruction.cpy, Addressing.immediate),
    0xc4.U(8.W) -> (Instruction.cpy, Addressing.zeroPage),
    0xcc.U(8.W) -> (Instruction.cpy, Addressing.absolute),
    0xc6.U(8.W) -> (Instruction.dec, Addressing.zeroPage),
    0xd6.U(8.W) -> (Instruction.dec, Addressing.xIndexedZeroPage),
    0xce.U(8.W) -> (Instruction.dec, Addressing.absolute),
    0xde.U(8.W) -> (Instruction.dec, Addressing.xIndexedAbsolute),
    0xca.U(8.W) -> (Instruction.dex, Addressing.implied),
    0x88.U(8.W) -> (Instruction.dey, Addressing.implied),
    0xe6.U(8.W) -> (Instruction.inc, Addressing.zeroPage),
    0xf6.U(8.W) -> (Instruction.inc, Addressing.xIndexedZeroPage),
    0xee.U(8.W) -> (Instruction.inc, Addressing.absolute),
    0xfe.U(8.W) -> (Instruction.inc, Addressing.xIndexedAbsolute),
    0xe8.U(8.W) -> (Instruction.inx, Addressing.implied),
    0xc8.U(8.W) -> (Instruction.iny, Addressing.implied),
    0x0a.U(8.W) -> (Instruction.asl, Addressing.accumulator),
    0x06.U(8.W) -> (Instruction.asl, Addressing.zeroPage),
    0x16.U(8.W) -> (Instruction.asl, Addressing.xIndexedZeroPage),
    0x0e.U(8.W) -> (Instruction.asl, Addressing.absolute),
    0x1e.U(8.W) -> (Instruction.asl, Addressing.xIndexedAbsolute),
    0x2a.U(8.W) -> (Instruction.rol, Addressing.accumulator),
    0x26.U(8.W) -> (Instruction.rol, Addressing.zeroPage),
    0x36.U(8.W) -> (Instruction.rol, Addressing.xIndexedZeroPage),
    0x2e.U(8.W) -> (Instruction.rol, Addressing.absolute),
    0x3e.U(8.W) -> (Instruction.rol, Addressing.xIndexedAbsolute),
    0x4a.U(8.W) -> (Instruction.lsr, Addressing.accumulator),
    0x46.U(8.W) -> (Instruction.lsr, Addressing.zeroPage),
    0x56.U(8.W) -> (Instruction.lsr, Addressing.xIndexedZeroPage),
    0x4e.U(8.W) -> (Instruction.lsr, Addressing.absolute),
    0x5e.U(8.W) -> (Instruction.lsr, Addressing.xIndexedAbsolute),
    0x6a.U(8.W) -> (Instruction.ror, Addressing.accumulator),
    0x66.U(8.W) -> (Instruction.ror, Addressing.zeroPage),
    0x76.U(8.W) -> (Instruction.ror, Addressing.xIndexedZeroPage),
    0x6e.U(8.W) -> (Instruction.ror, Addressing.absolute),
    0x7e.U(8.W) -> (Instruction.ror, Addressing.xIndexedAbsolute),
    // Move
    0xa9.U(8.W) -> (Instruction.lda, Addressing.immediate),
    0xa5.U(8.W) -> (Instruction.lda, Addressing.zeroPage),
    0xb5.U(8.W) -> (Instruction.lda, Addressing.xIndexedZeroPage),
    0xad.U(8.W) -> (Instruction.lda, Addressing.absolute),
    0xbd.U(8.W) -> (Instruction.lda, Addressing.xIndexedAbsolute),
    0xb9.U(8.W) -> (Instruction.lda, Addressing.yIndexedAbsolute),
    0xa1.U(8.W) -> (Instruction.lda, Addressing.xIndexedIndirect),
    0xb1.U(8.W) -> (Instruction.lda, Addressing.indirectYIndexed),
    0x85.U(8.W) -> (Instruction.sta, Addressing.zeroPage),
    0x95.U(8.W) -> (Instruction.sta, Addressing.xIndexedZeroPage),
    0x8d.U(8.W) -> (Instruction.sta, Addressing.absolute),
    0x9d.U(8.W) -> (Instruction.sta, Addressing.xIndexedAbsolute),
    0x99.U(8.W) -> (Instruction.sta, Addressing.yIndexedAbsolute),
    0x81.U(8.W) -> (Instruction.sta, Addressing.xIndexedIndirect),
    0x91.U(8.W) -> (Instruction.sta, Addressing.indirectYIndexed),
    0xa2.U(8.W) -> (Instruction.ldx, Addressing.immediate),
    0xa6.U(8.W) -> (Instruction.ldx, Addressing.zeroPage),
    0xb6.U(8.W) -> (Instruction.ldx, Addressing.yIndexedZeroPage),
    0xae.U(8.W) -> (Instruction.ldx, Addressing.absolute),
    0xbe.U(8.W) -> (Instruction.ldx, Addressing.yIndexedAbsolute),
    0x86.U(8.W) -> (Instruction.stx, Addressing.zeroPage),
    0x96.U(8.W) -> (Instruction.stx, Addressing.yIndexedZeroPage),
    0x8e.U(8.W) -> (Instruction.stx, Addressing.absolute),
    0xa0.U(8.W) -> (Instruction.ldy, Addressing.immediate),
    0xa4.U(8.W) -> (Instruction.ldy, Addressing.zeroPage),
    0xb4.U(8.W) -> (Instruction.ldy, Addressing.xIndexedZeroPage),
    0xac.U(8.W) -> (Instruction.ldy, Addressing.absolute),
    0xbc.U(8.W) -> (Instruction.ldy, Addressing.xIndexedAbsolute),
    0x84.U(8.W) -> (Instruction.sty, Addressing.zeroPage),
    0x94.U(8.W) -> (Instruction.sty, Addressing.xIndexedZeroPage),
    0x8c.U(8.W) -> (Instruction.sty, Addressing.absolute),
    0xaa.U(8.W) -> (Instruction.tax, Addressing.implied),
    0x8a.U(8.W) -> (Instruction.txa, Addressing.implied),
    0xa8.U(8.W) -> (Instruction.tay, Addressing.implied),
    0x98.U(8.W) -> (Instruction.tya, Addressing.implied),
    0xba.U(8.W) -> (Instruction.tsx, Addressing.implied),
    0x9a.U(8.W) -> (Instruction.txs, Addressing.implied),
    0x68.U(8.W) -> (Instruction.pla, Addressing.implied),
    0x48.U(8.W) -> (Instruction.pha, Addressing.implied),
    0x28.U(8.W) -> (Instruction.plp, Addressing.implied),
    0x08.U(8.W) -> (Instruction.php, Addressing.implied),
    // Jump, Flag
    0x10.U(8.W) -> (Instruction.bpl, Addressing.relative),
    0x30.U(8.W) -> (Instruction.bmi, Addressing.relative),
    0x50.U(8.W) -> (Instruction.bvc, Addressing.relative),
    0x70.U(8.W) -> (Instruction.bvs, Addressing.relative),
    0x90.U(8.W) -> (Instruction.bcc, Addressing.relative),
    0xb0.U(8.W) -> (Instruction.bcs, Addressing.relative),
    0xd0.U(8.W) -> (Instruction.bne, Addressing.relative),
    0xf0.U(8.W) -> (Instruction.beq, Addressing.relative),
    0x00.U(8.W) -> (Instruction.brk, Addressing.implied),
    0x40.U(8.W) -> (Instruction.rti, Addressing.implied),
    0x20.U(8.W) -> (Instruction.jsr, Addressing.absolute),
    0x60.U(8.W) -> (Instruction.rts, Addressing.implied),
    0x4c.U(8.W) -> (Instruction.jmp, Addressing.absolute),
    0x6c.U(8.W) -> (Instruction.jmp, Addressing.indirect),
    0x24.U(8.W) -> (Instruction.bit, Addressing.zeroPage),
    0x2c.U(8.W) -> (Instruction.bit, Addressing.absolute),
    0x18.U(8.W) -> (Instruction.clc, Addressing.implied),
    0x38.U(8.W) -> (Instruction.sec, Addressing.implied),
    0xd8.U(8.W) -> (Instruction.cld, Addressing.implied),
    0xf8.U(8.W) -> (Instruction.sed, Addressing.implied),
    0x58.U(8.W) -> (Instruction.cli, Addressing.implied),
    0x78.U(8.W) -> (Instruction.sei, Addressing.implied),
    0xb8.U(8.W) -> (Instruction.clv, Addressing.implied),
    0xea.U(8.W) -> (Instruction.nop, Addressing.implied),
    // Illegal
    0x03.U(8.W) -> (Instruction.slo, Addressing.xIndexedIndirect),
    0x07.U(8.W) -> (Instruction.slo, Addressing.zeroPage),
    0x0f.U(8.W) -> (Instruction.slo, Addressing.absolute),
    0x13.U(8.W) -> (Instruction.slo, Addressing.indirectYIndexed),
    0x17.U(8.W) -> (Instruction.slo, Addressing.xIndexedZeroPage),
    0x1b.U(8.W) -> (Instruction.slo, Addressing.yIndexedAbsolute),
    0x1f.U(8.W) -> (Instruction.slo, Addressing.xIndexedAbsolute),
    0x23.U(8.W) -> (Instruction.rla, Addressing.xIndexedIndirect),
    0x27.U(8.W) -> (Instruction.rla, Addressing.zeroPage),
    0x2f.U(8.W) -> (Instruction.rla, Addressing.absolute),
    0x33.U(8.W) -> (Instruction.rla, Addressing.indirectYIndexed),
    0x37.U(8.W) -> (Instruction.rla, Addressing.xIndexedZeroPage),
    0x3b.U(8.W) -> (Instruction.rla, Addressing.yIndexedAbsolute),
    0x3f.U(8.W) -> (Instruction.rla, Addressing.xIndexedAbsolute),
    0x43.U(8.W) -> (Instruction.sre, Addressing.xIndexedIndirect),
    0x47.U(8.W) -> (Instruction.sre, Addressing.zeroPage),
    0x4f.U(8.W) -> (Instruction.sre, Addressing.absolute),
    0x53.U(8.W) -> (Instruction.sre, Addressing.indirectYIndexed),
    0x57.U(8.W) -> (Instruction.sre, Addressing.xIndexedZeroPage),
    0x5b.U(8.W) -> (Instruction.sre, Addressing.yIndexedAbsolute),
    0x5f.U(8.W) -> (Instruction.sre, Addressing.xIndexedAbsolute),
    0x63.U(8.W) -> (Instruction.rra, Addressing.xIndexedIndirect),
    0x67.U(8.W) -> (Instruction.rra, Addressing.zeroPage),
    0x6f.U(8.W) -> (Instruction.rra, Addressing.absolute),
    0x73.U(8.W) -> (Instruction.rra, Addressing.indirectYIndexed),
    0x77.U(8.W) -> (Instruction.rra, Addressing.xIndexedZeroPage),
    0x7b.U(8.W) -> (Instruction.rra, Addressing.yIndexedAbsolute),
    0x7f.U(8.W) -> (Instruction.rra, Addressing.xIndexedAbsolute),
    0x83.U(8.W) -> (Instruction.sax, Addressing.xIndexedIndirect),
    0x87.U(8.W) -> (Instruction.sax, Addressing.zeroPage),
    0x8f.U(8.W) -> (Instruction.sax, Addressing.absolute),
    0x97.U(8.W) -> (Instruction.sax, Addressing.yIndexedZeroPage),
    0xa3.U(8.W) -> (Instruction.lax, Addressing.xIndexedIndirect),
    0xa7.U(8.W) -> (Instruction.lax, Addressing.zeroPage),
    0xaf.U(8.W) -> (Instruction.lax, Addressing.absolute),
    0xb3.U(8.W) -> (Instruction.lax, Addressing.indirectYIndexed),
    0xb7.U(8.W) -> (Instruction.lax, Addressing.yIndexedZeroPage),
    0xbf.U(8.W) -> (Instruction.lax, Addressing.yIndexedAbsolute),
    0xc3.U(8.W) -> (Instruction.dcp, Addressing.xIndexedIndirect),
    0xc7.U(8.W) -> (Instruction.dcp, Addressing.zeroPage),
    0xcf.U(8.W) -> (Instruction.dcp, Addressing.absolute),
    0xd3.U(8.W) -> (Instruction.dcp, Addressing.indirectYIndexed),
    0xd7.U(8.W) -> (Instruction.dcp, Addressing.xIndexedZeroPage),
    0xdb.U(8.W) -> (Instruction.dcp, Addressing.yIndexedAbsolute),
    0xdf.U(8.W) -> (Instruction.dcp, Addressing.xIndexedAbsolute),
    0xe3.U(8.W) -> (Instruction.isc, Addressing.xIndexedIndirect),
    0xe7.U(8.W) -> (Instruction.isc, Addressing.zeroPage),
    0xef.U(8.W) -> (Instruction.isc, Addressing.absolute),
    0xf3.U(8.W) -> (Instruction.isc, Addressing.indirectYIndexed),
    0xf7.U(8.W) -> (Instruction.isc, Addressing.xIndexedZeroPage),
    0xfb.U(8.W) -> (Instruction.isc, Addressing.yIndexedAbsolute),
    0xff.U(8.W) -> (Instruction.isc, Addressing.xIndexedAbsolute),
    0x0b.U(8.W) -> (Instruction.anc, Addressing.immediate),
    0x2b.U(8.W) -> (Instruction.anc2, Addressing.immediate),
    0x4b.U(8.W) -> (Instruction.alr, Addressing.immediate),
    0x6b.U(8.W) -> (Instruction.arr, Addressing.immediate),
    0x8b.U(8.W) -> (Instruction.xaa, Addressing.immediate),
    0xab.U(8.W) -> (Instruction.lax2, Addressing.immediate),
    0xcb.U(8.W) -> (Instruction.axs, Addressing.immediate),
    0xeb.U(8.W) -> (Instruction.sbc2, Addressing.immediate),
    0x93.U(8.W) -> (Instruction.ahx, Addressing.indirectYIndexed),
    0x9f.U(8.W) -> (Instruction.ahx, Addressing.yIndexedAbsolute),
    0x9c.U(8.W) -> (Instruction.shy, Addressing.xIndexedAbsolute),
    0x9e.U(8.W) -> (Instruction.shx, Addressing.yIndexedAbsolute),
    0x9b.U(8.W) -> (Instruction.tas, Addressing.yIndexedAbsolute),
    0xbb.U(8.W) -> (Instruction.las, Addressing.yIndexedAbsolute),
    // No Operation
    0x80.U(8.W) -> (Instruction.nop, Addressing.immediate),
    0x82.U(8.W) -> (Instruction.nop, Addressing.immediate),
    0x89.U(8.W) -> (Instruction.nop, Addressing.immediate),
    0xc2.U(8.W) -> (Instruction.nop, Addressing.immediate),
    0xe2.U(8.W) -> (Instruction.nop, Addressing.immediate),
    0x04.U(8.W) -> (Instruction.nop, Addressing.zeroPage),
    0x44.U(8.W) -> (Instruction.nop, Addressing.zeroPage),
    0x64.U(8.W) -> (Instruction.nop, Addressing.zeroPage),
    0x14.U(8.W) -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0x34.U(8.W) -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0x54.U(8.W) -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0x74.U(8.W) -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0xd4.U(8.W) -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0xf4.U(8.W) -> (Instruction.nop, Addressing.xIndexedZeroPage),
    0x0c.U(8.W) -> (Instruction.nop, Addressing.absolute),
    0x1c.U(8.W) -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0x3c.U(8.W) -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0x5c.U(8.W) -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0x7c.U(8.W) -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0xdc.U(8.W) -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0xfc.U(8.W) -> (Instruction.nop, Addressing.xIndexedAbsolute),
    0x1a.U(8.W) -> (Instruction.nop, Addressing.implied),
    0x3a.U(8.W) -> (Instruction.nop, Addressing.implied),
    0x5a.U(8.W) -> (Instruction.nop, Addressing.implied),
    0x7a.U(8.W) -> (Instruction.nop, Addressing.implied),
    0xda.U(8.W) -> (Instruction.nop, Addressing.implied),
    0xfa.U(8.W) -> (Instruction.nop, Addressing.implied),
    // Halt
    0x02.U(8.W) -> (Instruction.halt, Addressing.invalid),
    0x12.U(8.W) -> (Instruction.halt, Addressing.invalid),
    0x22.U(8.W) -> (Instruction.halt, Addressing.invalid),
    0x32.U(8.W) -> (Instruction.halt, Addressing.invalid),
    0x42.U(8.W) -> (Instruction.halt, Addressing.invalid),
    0x52.U(8.W) -> (Instruction.halt, Addressing.invalid),
    0x62.U(8.W) -> (Instruction.halt, Addressing.invalid),
    0x72.U(8.W) -> (Instruction.halt, Addressing.invalid),
    0x92.U(8.W) -> (Instruction.halt, Addressing.invalid),
    0xb2.U(8.W) -> (Instruction.halt, Addressing.invalid),
    0xd2.U(8.W) -> (Instruction.halt, Addressing.invalid),
    0xf2.U(8.W) -> (Instruction.halt, Addressing.invalid)
  )
}
