package cpu

import chisel3._

/** DataBusからの命令取得と、その内容をInstruction Registerに保持する役割を持つ
 *  @param isDebug デバッグ用pinを残すならtrue
  */
class InstructionFetch(isDebug: Boolean) extends Module {
  val io = IO(new Bundle {
    // デバッグ用、Fetchしたときのaddr/dataの値を保持
    val debugAddrIn  = if (isDebug) Some(Input(UInt(16.W))) else None
    val debugAddrOut = if (isDebug) Some(Output(UInt(16.W))) else None

    // Fetchを有効にする場合はtrue、trueの間は毎cycle取得する
    val nEn = Input(Bool())
    // DataBusと直結
    val dataIn = Input(UInt(8.W))
    // IRの出力
    val dataOut = Output(UInt(8.W))
    // IRに有効な値が入っていればtrue
    val valid = Output(Bool())
  })

  // nEnが有効なときに命令を取得する
  val instReg  = Reg(UInt(8.W))
  val validReg = Reg(Bool())
  io.dataOut := instReg
  io.valid   := validReg
  when(io.nEn) {
    // disable
    instReg  := instReg
    validReg := false.B
  }.otherwise {
    // enable
    instReg  := io.dataIn
    validReg := true.B
  }

  // デバッグ用にアドレスも取得する
  if (isDebug) {
    val debugAddrReg = Reg(UInt(16.W))
    io.debugAddrOut.get := debugAddrReg
    when(io.nEn) {
      // disable
      debugAddrReg := debugAddrReg
    }.otherwise {
      // enable
      debugAddrReg := io.debugAddrIn.get
    }
  }
}
