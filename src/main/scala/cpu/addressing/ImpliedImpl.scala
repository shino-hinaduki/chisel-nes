package cpu.addressing

import chisel3._
import cpu.types.Addressing
import cpu.register.CpuRegister

/**
  * ImpliedのOF実装
  */
class ImpliedImpl extends AddressingImpl {
  override def addressing: Addressing.Type =
    Addressing.implied
  override def onRequest(opcodeAddr: UInt, reqReadData: Boolean, reg: CpuRegister): Process =
    Process.ReportNone()
  override def doneReadOperand(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.Clear(isIllegal = true)
  override def doneReadPointer(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.Clear(isIllegal = true)
  override def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.Clear(isIllegal = true)
}
