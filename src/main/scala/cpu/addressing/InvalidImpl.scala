package cpu.addressing

import chisel3._
import cpu.types.Addressing
import cpu.register.CpuRegister

/**
  * InvalidのOF実装
  */
class InvalidImpl extends AddressingImpl {
  override def addressing: Addressing.Type =
    Addressing.invalid
  override def onRequest(reqReadData: Boolean, opcodeAddr: UInt, reg: CpuRegister): Process =
    Process.Clear(isIllegal = true)
  override def doneReadOperand(reqReadData: Boolean, opcodeAddr: UInt, reg: CpuRegister, readData: UInt): Process =
    Process.Clear(isIllegal = true)
  override def doneReadPointer(reqReadData: Boolean, opcodeAddr: UInt, reg: CpuRegister, readData: UInt): Process =
    Process.Clear(isIllegal = true)
  override def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.Clear(isIllegal = true)
}
