package cpu

import chisel3._

/** DataBusからの命令取得と、その内容をInstruction Registerに保持する役割を持つ
  */
class InstructionFetch extends Module {
  val io = IO(new Bundle {
    // DataBusと直結、入力
    val addrIn = Input(UInt(16.W))
    val dataIn = Input(UInt(8.W))
    // Fetchしたときのaddr/dataの値を保持
    val addrOut = Output(UInt(16.W))
    val dataOut = Output(UInt(8.W))
    // Fetchを有効にする場合はtrue、trueの間は毎cycle取得する
    val nEn = Input(Bool())
    // IRに有効な値が入っていればtrue, nEn設定後の次cycleから有効
    val valid = Output(Bool())
  })

  // nEnが有効なときに命令を取得する
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
}
