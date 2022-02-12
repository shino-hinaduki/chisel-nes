package cpu.addressing

import chisel3._
import cpu.types.Addressing

/**
  * ImpliedのOF実装
  */
class ImpliedImpl extends OperandFetchImpl {
  override def addressing: Addressing.Type                                                                                              = Addressing.invalid
  override def onRequest(opcodeAddr: UInt, reqReadData: Boolean): Process                                                               = ReportNone()
  override def doneReadOperand(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, xReg: UInt, yReg: UInt): Process = Clear(isIllegal = true)
  override def doneReadPointer(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, xReg: UInt, yReg: UInt): Process = Clear(isIllegal = true)
  override def doneReadData(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, xReg: UInt, yReg: UInt): Process    = Clear(isIllegal = true)
}
