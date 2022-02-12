package cpu.addressing

import chisel3._
import cpu.types.Addressing

/**
  * AbsoluteのOF実装
  */
class AbsoluteImpl extends OperandFetchImpl {
  override def addressing: Addressing.Type =
    Addressing.absolute
  // OpCode後2byteが実効アドレスなので読み出し
  override def onRequest(opcodeAddr: UInt, reqReadData: Boolean, aReg: UInt): Process =
    ReadOperand(opcodeAddr + 1.U, 2.U)
  // opcode後2byteのreadDataが実効アドレス。Dataが必要であればそのアドレスも読み出し
  override def doneReadOperand(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, xReg: UInt, yReg: UInt): Process =
    if (reqReadData) {
      ReadData(readData, 1.U)
    } else {
      ReportAddr(readData)
    }
  override def doneReadPointer(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, xReg: UInt, yReg: UInt): Process =
    Clear(isIllegal = true)
  // 読み出し先アドレスと読みだしたデータを報告
  override def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, xReg: UInt, yReg: UInt): Process =
    ReportFull(readAddr, readData)
}
