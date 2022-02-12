package cpu.addressing

import chisel3._
import cpu.types.Addressing
import cpu.register.CpuRegister

/**
  * IndirectのOF実装
  */
class IndirectImpl extends OperandFetchImpl {
  override def addressing: Addressing.Type =
    Addressing.indirect
  // OpCode後2byteが実効アドレスへのポインタなので読み出し
  override def onRequest(opcodeAddr: UInt, reqReadData: Boolean, reg: CpuRegister): Process =
    Process.ReadOperand(opcodeAddr + 1.U, 2.U)
  // opcode後2byteのreadDataにあるアドレスを読み出す
  override def doneReadOperand(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.ReadPointer(readData, 2.U)
  // 読みだしたアドレスがreadDataにあるのでこれを返すか、更にこのアドレスにあるデータを読み出して返す
  override def doneReadPointer(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    if (reqReadData) {
      Process.ReadData(readData, 1.U)
    } else {
      Process.ReportAddr(readData)
    }
  // 読み出し先アドレスと読みだしたデータを報告
  override def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.ReportFull(readAddr, readData)
}
