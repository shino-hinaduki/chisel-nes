package cpu.addressing

import chisel3._
import chisel3.util.Cat

import cpu.types.Addressing
import cpu.register.CpuRegister

/**
  * IndexedIndirectのOF実装
  */
class XIndexedIndirectImpl extends AddressingImpl {
  override def addressing: Addressing.Type =
    Addressing.xIndexedIndirect
  // OpCode後1byteが実効アドレスへのポインタのLowerなので読み出し
  override def onRequest(reqReadData: Boolean, opcodeAddr: UInt, reg: CpuRegister): Process =
    Process.ReadOperand(opcodeAddr + 1.U, 1.U)
  // opcode後1byteのreadDataに、Xregを加えたアドレスを読み出す。上位8bitは0固定
  override def doneReadOperand(reqReadData: Boolean, opcodeAddr: UInt, readData: UInt, reg: CpuRegister): Process = {
    val addr = Cat(0.U(8.W), (readData(7, 0) + reg.x)(7, 0))
    Process.ReadPointer(addr, 2.U)
  }
  // 読みだしたアドレスがreadDataにあるのでこれを返すか、更にこのアドレスにあるデータを読み出して返す
  override def doneReadPointer(reqReadData: Boolean, opcodeAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    if (reqReadData) {
      Process.ReadData(readData, 1.U)
    } else {
      Process.ReportAddr(readData)
    }
  // 読み出し先アドレスと読みだしたデータを報告
  override def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.ReportFull(readAddr, readData)
}
