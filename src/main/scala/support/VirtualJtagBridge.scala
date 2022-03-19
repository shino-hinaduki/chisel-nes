package support

import chisel3._
import chisel3.internal.firrtl.Width
import support.types.VirtualJtagIO
import support.types.DebugAccessDataKind
import support.types.VirtualInstruction
import support.types.DebugAccessIO

/**
  * Virtual JTAG Intel(R) FPGA IP Coreと接続し、DebugAccessPortとの接続を行う
  * 本Module自身はSystem Clock、Virutal JTAG Coreとの連携部分はtckのClock Domainとして扱う
  */
class VirtualJtagBridge extends Module {
  // VIRを24bit想定で解釈するため、固定値とする
  val irWidth: Width = 24.W

  val io = IO(new Bundle {
    // Virtual JTAG IP Coreと接続
    val vjtag = new VirtualJtagIO(irWidth)
    // DebugAccessPortとのI/F  TODO: 非同期クロックの載せ替え...。
    val dap = new DebugAccessIO
  })

  // JTAG Clock Domain
  withClock(io.vjtag.tck) {
    // 現在キャプチャされている命令
    val irInReg = RegInit(UInt(irWidth), 0.U)
    io.vjtag.ir_out := irInReg // そのまま向けておく
    // irInRegを解釈した値
    val isValidIrReg = RegInit(Bool(), false.B)
    val isWriteReg   = RegInit(Bool(), false.B)
    val dataKindReg  = RegInit(DebugAccessDataKind.Type(), DebugAccessDataKind.invalid)
    val baseAddrReg  = RegInit(UInt(16.W), 0.U)
    // 出力するデータ
    val tdoReg = RegInit(Bool(), false.B)
    io.vjtag.tdo := tdoReg

    when(io.vjtag.virtual_state_cdr) {
      // Capture_DR
    }.elsewhen(io.vjtag.virtual_state_sdr) {
      // Shift_DR
      when(isValidIrReg) {
        // TODO: 有効なデータを流す
      }.otherwise {
        // 本IPとは無関係の命令が流れているので、Bypassする
        tdoReg := io.vjtag.tdi
      }
    }.elsewhen(io.vjtag.virtual_state_e1dr) {
      // Exit1_DR
    }.elsewhen(io.vjtag.virtual_state_pdr) {
      // Pause_DR
    }.elsewhen(io.vjtag.virtual_state_e2dr) {
      // Exit2_DR
    }.elsewhen(io.vjtag.virtual_state_udr) {
      // Update_DR
    }.elsewhen(io.vjtag.virtual_state_cir) {
      // Capture_IR
      // (irInRegを見せているものがキャプチャされるだけなのでケア不要)
    }.elsewhen(io.vjtag.virtual_state_uir) {
      // Update_IR
      // VirtualJTAGなので (ShiftIR USER1, ShiftDR XXXX) で流した XXXX が io.vjtag.ir_in に入っている
      irInReg := io.vjtag.ir_in

      val inst = VirtualInstruction.parse(io.vjtag.ir_in)
      isValidIrReg := inst.isValid
      isWriteReg   := inst.isWrite
      dataKindReg  := inst.dataKind
      baseAddrReg  := inst.dataKind
    }.otherwise {
      // NOP
    }
  }
}
