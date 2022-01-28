package cpu.core

import chisel3._
import cpu.types.Addressing
import cpu.types.Instruction
import chisel3.util.MuxLookup

/** DataBusからの命令取得と、その内容をInstruction Registerに保持する役割を持つ
  */
class InstructionFetch extends Module {
  val io = IO(new Bundle {
    // DataBusと直結、入力
    val addrIn = Input(UInt(16.W))
    val dataIn = Input(UInt(8.W))
    // Fetchを有効にする場合はtrue、trueの間は毎cycle取得する
    val nEn = Input(Bool())

    // IRに有効な値が入っていればtrue, nEn設定後の次cycleから有効
    val valid = Output(Bool())
    // Fetchしたときのaddr/dataの値を保持
    val addrOut = Output(UInt(16.W))
    val dataOut = Output(UInt(8.W))
    // 次cycleで使用するInstruction, Addressing Mode
    val inst       = Output(Instruction())
    val addressing = Output(Addressing())
  })

  // nEnが有効なときに命令と命令のおいてあるアドレスを取得する
  val addrReg  = RegInit(UInt(16.W), 0.U)
  val dataReg  = RegInit(UInt(8.W), 0.U)
  val validReg = RegInit(Bool(), false.B)
  io.addrOut := addrReg
  io.dataOut := dataReg
  io.valid   := validReg
  when(io.nEn) {
    // disable
    addrReg  := addrReg
    dataReg  := dataReg
    validReg := false.B
  }.otherwise {
    // enable
    addrReg  := io.addrIn
    dataReg  := io.dataIn
    validReg := true.B
  }

  // decode stage
  // IF,ID,OFは並列化できないため、次サイクルで(Impliedなどを除いて)OFが実行できるようにDecodeまで済ませる
  val instReg       = RegInit(Instruction(), Instruction.not)
  val addressingReg = RegInit(Addressing(), Addressing.implied)
  when(io.nEn) {
    // disable
    instReg       := instReg
    addressingReg := addressingReg
  }.otherwise {
    // enable
    // io.dataInから直接デコードする
    val inst       = MuxLookup(io.dataIn, Instruction.not, InstructionDecode.lookupTable.map(x => x._1 -> x._2._1))
    val addressing = MuxLookup(io.dataIn, Addressing.implied, InstructionDecode.lookupTable.map(x => x._1 -> x._2._2))
    instReg       := inst
    addressingReg := addressing
  }
}
