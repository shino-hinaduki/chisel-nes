package cpu.addressing

import chisel3._
import cpu.types.Addressing

/**
  * InvalidのOF実装
  */
class InvalidImpl extends OperandFetchImpl {
  override def addressing: Addressing.Type =
    Addressing.invalid
  override def onRequest(opcodeAddr: UInt, reqReadData: Boolean, aReg: UInt): Process =
    Clear(isIllegal = true)
  override def doneReadOperand(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, xReg: UInt, yReg: UInt): Process =
    Clear(isIllegal = true)
  override def doneReadPointer(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, xReg: UInt, yReg: UInt): Process =
    Clear(isIllegal = true)
  override def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, xReg: UInt, yReg: UInt): Process =
    Clear(isIllegal = true)
}
