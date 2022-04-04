package board.access

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

import board.access.types.InternalAccessCommand

/**
  * VJTAG to DAPの動作確認サンプル
  *
  * @param counterWidth
  */
class DebugAccessTester(val counterWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    // デバッグカウント観測用
    val debugCounter = Output(UInt(counterWidth.W))
    // R/W要求観測用
    val debugLatestOffset = Output(UInt(InternalAccessCommand.Request.offsetWidth.W))
    // デバッグアクセスFIFO
    val debugAccess = new InternalAccessCommand.SlaveIO
  })
  val counterReg = RegInit(UInt(counterWidth.W), 0.U)
  io.debugCounter := counterReg
  val latestOffsetReg = RegInit(UInt(InternalAccessCommand.Request.offsetWidth.W), 0.U)
  io.debugLatestOffset := latestOffsetReg

  val dequeueReg = RegInit(Bool(), false.B)
  io.debugAccess.req.rdclk := clock
  io.debugAccess.req.rdreq := dequeueReg

  val enqueueReg     = RegInit(Bool(), false.B)
  val enqueueDataReg = RegInit(UInt(InternalAccessCommand.Response.cmdWidth.W), 0.U)
  io.debugAccess.resp.wrclk := clock
  io.debugAccess.resp.wrreq := enqueueReg
  io.debugAccess.resp.data  := enqueueDataReg

  // Reqに積まれたコマンドを処理して、次cycでcounterの値を応答を積む
  when(!io.debugAccess.req.rdempty && !dequeueReg) { // DequeueRegが立っているときは1回前のデータが見える
    // Dequeue
    val offset             = InternalAccessCommand.Request.getOffset(io.debugAccess.req.q)
    val data               = InternalAccessCommand.Request.getData(io.debugAccess.req.q)
    val (reqType, isValid) = InternalAccessCommand.Request.getRequestType(io.debugAccess.req.q)

    when(isValid) {
      // Valid CMD
      switch(reqType) {
        is(InternalAccessCommand.Type.read) {
          // Read CMD
          dequeueReg     := true.B
          enqueueReg     := true.B
          enqueueDataReg := Cat(0.U((InternalAccessCommand.Response.cmdWidth - InternalAccessCommand.Request.offsetWidth).W), offset) // resp offset
          // enqueueDataReg := counterReg       // resp Counter
          counterReg := counterReg + 1.U // increment Counter
        }
        is(InternalAccessCommand.Type.write) {
          // Write CMD
          dequeueReg     := true.B
          enqueueReg     := false.B
          enqueueDataReg := DontCare
          counterReg     := data // Write Counter
        }
      }
      // for Debug
      latestOffsetReg := offset
    }.otherwise {
      // Invalid CMD
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
