package board.video.types

import chisel3._
import chisel3.internal.firrtl.Width

/**
  * FrameBuffer用DPRAMに接続可能なI/F定義
  * @param addrWidth アドレス線のビット幅
  * @param dataWidth データ線のビット幅
  */
class FrameBufferIO(val addrWidth: Width, val dataWidth: Width) extends Bundle {
  val address_a = Output(UInt(addrWidth))
  val clock_a   = Output(Clock())
  val data_a    = Output(UInt(dataWidth))
  val rden_a    = Output(Bool())
  val wren_a    = Output(Bool())
  val q_a       = Input(UInt(dataWidth))

  val address_b = Output(UInt(addrWidth))
  val clock_b   = Output(Clock())
  val data_b    = Output(UInt(dataWidth))
  val rden_b    = Output(Bool())
  val wren_b    = Output(Bool())
  val q_b       = Input(UInt(dataWidth))
}
