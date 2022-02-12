package cpu.addressing

import chisel3._
import cpu.types.Addressing
import cpu.register.CpuRegister
import cpu.register.IndexRegister
import cpu.register.IndexRegister.X
import cpu.register.IndexRegister.Y

/**
  * IndexedAbsoluteのOF実装
  * @param indexReg 対象のIndex Register
  */
class IndexedAbsoluteImpl(val indexReg: IndexRegister) extends OperandFetchImpl {
  // 対象のIndex Registerの値を取得する
  def getIndexRegData(reg: CpuRegister) = indexReg match {
    case X() => reg.x
    case Y() => reg.y
  }

  override def addressing: Addressing.Type =
    Addressing.absolute
  // OpCode後2byteが実効アドレスなので読み出し
  override def onRequest(opcodeAddr: UInt, reqReadData: Boolean, reg: CpuRegister): Process =
    Process.ReadOperand(opcodeAddr + 1.U, 2.U)
  // opcode後2byteのreadDataが実効アドレス。Dataが必要であればそのアドレスも読み出し
  override def doneReadOperand(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process = {
    // 読みだしたアドレス + (X or Y) reg した値をアドレスとして扱う。Overflow分は捨てる
    val addr = (readData + getIndexRegData(reg))(15, 0)
    if (reqReadData) {
      Process.ReadData(addr, 1.U)
    } else {
      Process.ReportAddr(addr)
    }
  }
  override def doneReadPointer(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.Clear(isIllegal = true)
  // 読み出し先アドレスと読みだしたデータを報告
  override def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.ReportFull(readAddr, readData)
}
