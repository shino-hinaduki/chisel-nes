package board.access

import chisel3._
import chisel3.experimental.ChiselEnum

import board.access.types.InternalAccessCommand

object TestMode extends ChiselEnum {
  // カウンターの値を返す
  val counter = Value(0x00000000.U)
  // アクセスされたアドレスの値を返す
  val address = Value(0x00000001.U)
}

/**
  * VJTAG to DAPの最低限の動作確認サンプル。Readするたびにインクリするカウンタの値を返し、Writeで上書き可能
  *
  * @param counterWidth
  */
class DebugAccessTester(val counterWidth: Int = 32) extends Module {
  // このアドレスへのWriteはTestMode切り替えとみなす
  val modeOffset = 0xffffffff.U
  // 適切なモードを指定せずにReadした場合の値
  val invalidData = 0x01234567.U

  val io = IO(new Bundle {
    // デバッグ出力観測用
    val debugCounter = Output(UInt(counterWidth.W))
    // デバッグアクセスFIFO
    val debugAccess = new InternalAccessCommand.SlaveIO
  })
  val testModeReg = RegInit(TestMode.Type(), TestMode.counter)
  val counterReg  = RegInit(UInt(counterWidth.W), 0.U)
  io.debugCounter := counterReg

  val dequeueReg = RegInit(Bool(), false.B)
  io.debugAccess.req.rdclk := clock
  io.debugAccess.req.rdreq := dequeueReg

  val enqueueReg     = RegInit(Bool(), false.B)
  val enqueueDataReg = RegInit(UInt(InternalAccessCommand.Response.cmdWidth.W), 0.U)
  io.debugAccess.resp.wrclk := clock
  io.debugAccess.resp.wrreq := enqueueReg
  io.debugAccess.resp.data  := enqueueDataReg

  // Reqに積まれたコマンドを処理して、処理したcyc中に応答を積む
  // Write時/Read時の処理はmodeに委ねられていて、counterの値を返す、要求アドレスをそのまま返す。など
  // 0xffffffff への Write だけは特別扱いになっており、modeの値を上書きする
  when(!io.debugAccess.req.rdempty) {
    // Dequeue
    val offset             = InternalAccessCommand.Request.getOffset(io.debugAccess.req.q)
    val data               = InternalAccessCommand.Request.getData(io.debugAccess.req.q)
    val (reqType, isValid) = InternalAccessCommand.Request.getRequestType(io.debugAccess.req.q)

    when(!isValid) {
      // Invalid CMD
      dequeueReg     := true.B
      enqueueReg     := false.B
      enqueueDataReg := DontCare
      counterReg     := counterReg
    }.elsewhen(reqType === InternalAccessCommand.Type.read) {
      // Read CMD
      when(testModeReg === TestMode.counter) {
        dequeueReg     := true.B
        enqueueReg     := true.B
        enqueueDataReg := counterReg       // resp Counter
        counterReg     := counterReg + 1.U // increment Counter
      }.elsewhen(testModeReg === TestMode.address) {
        dequeueReg     := true.B
        enqueueReg     := true.B
        enqueueDataReg := offset // resp Read Address
        counterReg     := counterReg
      }.otherwise {
        dequeueReg     := true.B
        enqueueReg     := true.B
        enqueueDataReg := invalidData // resp Invalid Data
        counterReg     := counterReg
      }
    }.elsewhen(reqType === InternalAccessCommand.Type.write) {
      // Write CMD
      when(offset === modeOffset) {
        testModeReg    := data // Update Mode
        dequeueReg     := true.B
        enqueueReg     := false.B
        enqueueDataReg := DontCare
        counterReg     := counterReg
      }.elsewhen(testModeReg === TestMode.counter) {
        dequeueReg     := true.B
        enqueueReg     := false.B
        enqueueDataReg := DontCare
        counterReg     := data // Write Counter
      }.otherwise {
        // NOP or Not Implemented
        dequeueReg     := true.B
        enqueueReg     := false.B
        enqueueDataReg := DontCare
        counterReg     := counterReg
      }
    }.otherwise {
      // Not Implemented CMD
      dequeueReg     := true.B
      enqueueReg     := false.B
      enqueueDataReg := DontCare
      counterReg     := counterReg
    }
  }.otherwise {
    // NOP
    dequeueReg     := false.B
    enqueueReg     := false.B
    enqueueDataReg := DontCare
    counterReg     := counterReg
  }
}
