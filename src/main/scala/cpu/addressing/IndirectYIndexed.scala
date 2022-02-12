package cpu.addressing

import chisel3._
import chisel3.util.Cat

import cpu.types.Addressing
import cpu.register.CpuRegister

/**
  * IndirectIndexedのOF実装
  */
class IndirectYIndexedImpl extends OperandFetchImpl {
  override def addressing: Addressing.Type =
    Addressing.indirectYIndexed
  // OpCode後1byteが実効アドレスへのポインタなので読み出し
  override def onRequest(opcodeAddr: UInt, reqReadData: Boolean, reg: CpuRegister): Process =
    Process.ReadOperand(opcodeAddr + 1.U, 1.U)
  // opcode後1byteのreadDataにあるアドレス(上位は0固定)を読み出す
  override def doneReadOperand(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.ReadPointer(readData, 2.U) // (onRequestで1byte歯科要求していないので、readData上位は0)
  // 読みだしたアドレスがreadDataにY分をオフセットした分が目的のアドレス。これを返すか、更にこのアドレスにあるデータを読み出して返す
  override def doneReadPointer(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process = {
    val addr = (readData + Cat(0.U(8.W), reg.y))(15, 0)
    if (reqReadData) {
      Process.ReadData(addr, 1.U)
    } else {
      Process.ReportAddr(addr)
    }
  }
  // 読み出し先アドレスと読みだしたデータを報告
  override def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Process.ReportFull(readAddr, readData)
}
