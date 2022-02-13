package cpu.addressing

import chisel3._
import chisel3.util.Cat

import cpu.types.Addressing
import cpu.register.CpuRegister

/**
  * ZeroPageのOF実装
  */
class ZeroPageImpl extends AddressingImpl {
  override def addressing: Addressing.Type =
    Addressing.zeroPage
  // OpCode後1byteが実効アドレスLowerなので読み出し
  override def onRequest(reqReadData: Boolean, opcodeAddr: UInt, reg: CpuRegister): Process =
    Process.ReadOperand(opcodeAddr + 1.U, 1.U)
  // opcode後1byteのreadDataが実効アドレスLower, upperは0固定。Dataが必要であればそのアドレスも読み出し
  override def doneReadOperand(reqReadData: Boolean, opcodeAddr: UInt, reg: CpuRegister, readData: UInt): Process =
    if (reqReadData) {
      Process.ReadData(Cat(0.U(8.W), readData(7, 0)), 1.U)
    } else {
      Process.ReportAddr(Cat(0.U(8.W), readData(7, 0)))
    }
  override def doneReadPointer(reqReadData: Boolean, opcodeAddr: UInt, reg: CpuRegister, readData: UInt): Process =
    Process.Clear(isIllegal = true)
  // 読み出し先アドレスと読みだしたデータを報告
  override def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.ReportFull(readAddr, readData)
}
