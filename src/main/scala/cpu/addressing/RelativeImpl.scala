package cpu.addressing

import chisel3._
import chisel3.util.Cat

import cpu.types.Addressing
import cpu.register.CpuRegister

/**
  * RelativeのOF実装
  */
class RelativeImpl extends OperandFetchImpl {
  override def addressing: Addressing.Type =
    Addressing.relative
  // OpCode後1byteをオフセットとして読み出す
  override def onRequest(opcodeAddr: UInt, reqReadData: Boolean, reg: CpuRegister): Process =
    ReadOperand(opcodeAddr + 1.U, 1.U)
  // opcode後1byteのreadDataが実効アドレスLower, upperは0固定。Dataが必要であればそのアドレスも読み出し
  override def doneReadOperand(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process = {
    // offsetをint8として解釈し、現在のPCとの差分を計算した値を求める
    val offsetSigned = readData(7, 0).asSInt()
    val pc           = opcodeAddr + 2.U // current_opcode, $offset[readAddr], next_op[*PC*]
    // 事前に17bitの符号拡張をして差分を計算
    val pcSigned  = Cat(0.U(1.W), pc(15, 0)).asSInt()
    val dstSigned = pcSigned + offsetSigned
    // 16bitに戻して報告
    val dst = dstSigned.asUInt()(15, 0)
    assert(dstSigned.litValue.toInt >= 0) // 0以上の値にはなっているはず(その前提がなければこのassertは外す)
    ReportAddr(dst)
  }
  override def doneReadPointer(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Clear(isIllegal = true)
  override def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, reg: CpuRegister): Process =
    Clear(isIllegal = true)
}
