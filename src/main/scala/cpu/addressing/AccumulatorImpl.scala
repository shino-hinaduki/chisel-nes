package cpu.addressing

import chisel3._
import cpu.types.Addressing
import cpu.register.CpuRegister

/**
  * AccumulatorのOF実装
  */
class AccumulatorImpl extends OperandFetchImpl {
  override def addressing: Addressing.Type =
    Addressing.accumulator
  override def onRequest(opcodeAddr: UInt, reqReadData: Boolean, reg: CpuRegister): Process =
    ReportData(reg.a)
  override def doneReadOperand(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Clear(isIllegal = true)
  override def doneReadPointer(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Clear(isIllegal = true)
  override def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Clear(isIllegal = true)
}
