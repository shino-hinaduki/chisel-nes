package board.video

import chisel3._
import chisel3.internal.firrtl.Width

import board.video.types.FrameBufferIO

/**
  * PPUからの映像出力をFrameBufferにため、FrameBufferから映像を出力する
  * 内蔵clockはPPUからのクロックとして取り扱い、その他pixel clockが別途供給する
  */
class VideoOut extends Module {
  // 映像の幅, 最低サイズで固定
  val width: Int = 640
  // 映像の高さ, 最低サイズで固定
  val height: Int = 480
  // FrameBufferのアドレス空間
  val addrWidth: Width = 18.W
  // Framebuffer内のデータは 24bpprgb固定
  val dataWidth: Width = 24.W

  val io = IO(new Bundle {
    // VGA出力用のpixel clock
    val pixelClock = Input(Clock())
    // pixelClock DomainのReset
    val pixelClockRst = Input(Reset())
    // FrameBufferとの接続
    val frameBuffer = new FrameBufferIO(addrWidth, dataWidth)
  })
}
