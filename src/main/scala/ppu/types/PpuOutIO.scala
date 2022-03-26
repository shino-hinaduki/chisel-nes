package ppu.types

import chisel3._

/**
  * PPUからの映像出力を定義します
  */
class PpuOutIO extends Bundle {
  // falseの場合は書き込まない
  val valid = Output(Bool())
  // 現在のPixelのX座標
  val x = Output(UInt(8.W))
  // 現在のPixelのy座標
  val y = Output(UInt(8.W))
  // 現在のPixelのR値
  val r = Output(UInt(8.W))
  // 現在のPixelのG値
  val g = Output(UInt(8.W))
  // 現在のPixelのB値
  val b = Output(UInt(8.W))
}
