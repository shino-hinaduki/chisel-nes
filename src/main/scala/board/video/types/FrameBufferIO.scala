package board.video.types

import chisel3._
import chisel3.internal.firrtl.Width
import board.ram.types.RamIO

/**
  * FrameBuffer用DPRAMに接続可能なI/F定義
  * @param addrWidth アドレス線のビット幅
  * @param dataWidth データ線のビット幅
  */
class FrameBufferIO(val addrWidth: Int, val dataWidth: Int) extends Bundle {

  /**
    * PPU Clock Domainからのアクセス
    */
  val ppu = Flipped(new RamIO(addrWidth = addrWidth, dataWidth = dataWidth))

  /**
    * VGA Pixel Clock Domainからのアクセス
    */
  val vga = Flipped(new RamIO(addrWidth = addrWidth, dataWidth = dataWidth))
}
