package board.video.types

import chisel3._

/**
  * VGA出力の定義
  */
class VgaIO(val rWidth: Int, val gWidth: Int, val bWidth: Int) extends Bundle {
  // 赤色
  val r = Output(UInt(rWidth.W))
  // 緑色
  val g = Output(UInt(gWidth.W))
  // 青色
  val b = Output(UInt(bWidth.W))
  // 水平同期信号
  val hsync = Output(Bool())
  // 垂直同期信号
  val vsync = Output(Bool())
}
