package cpu.addressing

import chisel3._
import cpu.types.Addressing
import cpu.register.CpuRegister

/**
  * ImmediateのOF実装
  */
class ImmediateImpl extends OperandFetchImpl {
  override def addressing: Addressing.Type =
    Addressing.immediate
  // OpCode後1byteが即値なので読み出し
  override def onRequest(opcodeAddr: UInt, reqReadData: Boolean, reg: CpuRegister): Process =
    ReadOperand(opcodeAddr + 1.U, 1.U)
  // 読みだしたデータを報告(アドレスは使わなそうだが一応報告)
  override def doneReadOperand(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    ReportFull(readAddr, readData)
  override def doneReadPointer(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Clear(isIllegal = true)
  override def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Clear(isIllegal = true)
}
