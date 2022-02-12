package cpu.addressing

import chisel3._
import cpu.types.Addressing
import cpu.register.CpuRegister

/**
  * AbsoluteのOF実装
  */
class AbsoluteImpl extends AddressingImpl {
  override def addressing: Addressing.Type =
    Addressing.absolute
  // OpCode後2byteが実効アドレスなので読み出し
  override def onRequest(reqReadData: Boolean, opcodeAddr: UInt, reg: CpuRegister): Process =
    Process.ReadOperand(opcodeAddr + 1.U, 2.U)
  // opcode後2byteのreadDataが実効アドレス。Dataが必要であればそのアドレスも読み出し
  override def doneReadOperand(reqReadData: Boolean, opcodeAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    if (reqReadData) {
      Process.ReadData(readData, 1.U)
    } else {
      Process.ReportAddr(readData)
    }
  override def doneReadPointer(reqReadData: Boolean, opcodeAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.Clear(isIllegal = true)
  // 読み出し先アドレスと読みだしたデータを報告
  override def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.ReportFull(readAddr, readData)
}
