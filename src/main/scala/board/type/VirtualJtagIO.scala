package board.types

import chisel3._
import chisel3.internal.firrtl.Width

/**
  * Virtual JTAG Intel(R) FPGA IP Coreと接続可能なModuleのIO Port定義
  * 
  * @param irWidth SLD_IR_WIDTH の設定値
  */
class VirtualJtagIO(val irWidth: Width) extends Bundle {
  // Input Ports for Virtual JTAG Core
  // (Required) Writes to the TDO pin on the device
  val tdo = Output(Bool())
  // Virtual JTAG instruction register output. The value is captured whenever virtual_state_cir is high.
  val ir_out = Output(UInt(irWidth))

  // Output Ports for Virtual JTAG Core
  // (Required) TDI input data on the device
  val tdi = Input(Bool())
  // (Required) JTAG test clock
  val tck = Input(Clock())
  // Virtual JTAG instruction register data. The value is available and latched when virtual_state_uir is high.
  val ir_in = Input(UInt(irWidth))

  // High-Level Virtual JTAG State Signals
  // Indicates that virtual JTAG is in Capture_DR state.
  val virtual_state_cdr = Input(Bool())
  // (Required) Indicates that virtual JTAG is in Shift_DR state.
  val virtual_state_sdr = Input(Bool())
  // Indicates that virtual JTAG is in Exit1_DR state.
  val virtual_state_e1dr = Input(Bool())
  // Indicates that virtual JTAG is in Pause_DR state.
  val virtual_state_pdr = Input(Bool())
  // Indicates that virtual JTAG is in Exit2_DR state.
  val virtual_state_e2dr = Input(Bool())
  // Indicates that virtual JTAG is in Update_DR state.
  val virtual_state_udr = Input(Bool())
  // Indicates that virtual JTAG is in Capture_IR state.
  val virtual_state_cir = Input(Bool())
  // Indicates that virtual JTAG is in Update_IR state.
  val virtual_state_uir = Input(Bool())
}
