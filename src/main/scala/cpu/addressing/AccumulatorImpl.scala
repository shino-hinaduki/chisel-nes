package cpu.addressing

import chisel3._
import cpu.types.Addressing
import cpu.register.CpuRegister

/**
  * AccumulatorのOF実装
  */
class AccumulatorImpl extends AddressingImpl {
  override def addressing: Addressing.Type =
    Addressing.accumulator
  override def onRequest(reqReadData: Boolean, opcodeAddr: UInt, reg: CpuRegister): Process =
    Process.ReportData(reg.a)
  override def doneReadOperand(reqReadData: Boolean, opcodeAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.Clear(isIllegal = true)
  override def doneReadPointer(reqReadData: Boolean, opcodeAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.Clear(isIllegal = true)
  override def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.Clear(isIllegal = true)
}
