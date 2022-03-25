package board.video.types

import chisel3._

/**
  * VGA出力の定義
  * @param bpp pixelあたりのデータ幅, alphaを除く
  */
class VgaIO(val bpp: Int) extends Bundle {
  // RGBで3色
  val channelNum = 3
  // 赤色
  val r = Output(UInt((bpp / channelNum).W))
  // 緑色
  val g = Output(UInt((bpp / channelNum).W))
  // 青色
  val b = Output(UInt((bpp / channelNum).W))
  // 水平同期信号
  val hsync = Output(Bool())
  // 垂直同期信号
  val vsync = Output(Bool())
}
