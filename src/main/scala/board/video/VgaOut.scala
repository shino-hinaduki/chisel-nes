package board.video

import chisel3._
import chisel3.internal.firrtl.Width

import board.video.types.FrameBufferIO
import ppu.types.PpuOutIO
import chisel3.util.log2Up
import board.video.types.VgaConfig
import board.video.types.PpuImageConfig
import chisel3.util.Cat
import board.video.types.VgaIO

/**
  * PPUからの映像出力をFrameBufferにため、FrameBufferから映像を出力する
  * 内蔵clockはPPUからのクロックとして取り扱い、その他pixel clockが別途供給する
  * @param vgaConfig 出力設定。初期値は 640x480 60Hz
  * @param ppuConfig PPUの映像配置設定。初期値は中央に等倍
  */
class VgaOut(
    val vgaConfig: VgaConfig = VgaConfig.minConf,
    val ppuConfig: PpuImageConfig = PpuImageConfig(VgaConfig.minConf.width / 2, VgaConfig.minConf.height / 2, 1)
) extends Module {
  // FrameBufferのアクセス範囲 256 * 256 = 65536word => 16bit
  val fbAddrWidth = 16
  // FrameBufferのWord辺のデータ幅 24bpprgb
  val fbDataWidth = 24
  // R,G,Bで3色
  val channelNum = 3
  // R,G,Bそれぞれのデータ幅
  val channelDataWidth = (fbDataWidth / channelNum)

  val io = IO(new Bundle {
    // PPUからの映像出力を受け取る
    val ppuOut = Flipped(new PpuOutIO())

    // trueならテスト信号を出力する。pixelClockに同期して読み出される
    val isDebug = Input(Bool())
    // VGA出力用のpixel clock
    val pixelClock = Input(Clock())
    // pixelClock DomainのReset
    val pixelClockReset = Input(Reset())
    // FrameBufferとの接続, _aがPPU, _bがVGAとして扱う
    val frameBuffer = new FrameBufferIO(fbAddrWidth.W, fbDataWidth.W)
    // 映像出力
    val vgaOut = new VgaIO(fbDataWidth)
  })

  /*********************************************************************/
  /* Common                                                            */
  // X,Y座標から、DPRAMのアドレスに変換する
  def posToFbAddr(x: UInt, y: UInt): UInt = Cat(y(7, 0), x(7, 0))
  // DPRAMのアドレスからX,Y座標に変換する
  def fbAddrToPos(data: UInt): (UInt, UInt) = (data(7, 0), data(15, 8))
  // RGB値からDPRAMのデータに変換する
  def rgbToFbData(r: UInt, g: UInt, b: UInt): UInt = Cat(b(7, 0), g(7, 0), r(7, 0))
  // DPRAMのデータからRGB値に変換する
  def fbDataToRgb(data: UInt): (UInt, UInt, UInt) = (data(7, 0), data(15, 8), data(23, 16))

  /*********************************************************************/
  /* PPU VideoOut -> DualPort RAM                                      */
  val writeFbAddrReg = RegInit(UInt(fbAddrWidth.W), 0.U)
  val writeFbDataReg = RegInit(UInt(fbDataWidth.W), 0.U)
  val writeFbEnReg   = RegInit(Bool(), false.B)
  io.frameBuffer.address_a := writeFbAddrReg
  io.frameBuffer.clock_a   := clock   // VgaOut自体のClockはPPU Clockで駆動する
  io.frameBuffer.data_a    := writeFbDataReg
  io.frameBuffer.rden_a    := false.B // Readはしない
  io.frameBuffer.wren_a    := writeFbEnReg

  // 毎cycチェックして、DPRAMへの書き込み信号を作る
  when(io.ppuOut.valid) {
    writeFbAddrReg := posToFbAddr(io.ppuOut.x, io.ppuOut.y)
    when(io.ppuOut.visible) {
      writeFbDataReg := rgbToFbData(io.ppuOut.r, io.ppuOut.g, io.ppuOut.b)
    }.otherwise {
      writeFbDataReg := rgbToFbData(0.U, 0.U, 0.U)
    }
    writeFbEnReg := true.B
  }.otherwise {
    writeFbAddrReg := DontCare
    writeFbDataReg := DontCare
    writeFbEnReg   := false.B
  }

  /*********************************************************************/
  /* DualPort RAM -> VGA out                                           */
  withClockAndReset(io.pixelClock, io.pixelClockReset) {
    // Dual Port RAM読み出し用
    val readFbAddrReg = RegInit(UInt(fbAddrWidth.W), 0.U)
    val readFbEnReg   = RegInit(Bool(), false.B)
    io.frameBuffer.address_b := readFbAddrReg
    io.frameBuffer.clock_b   := io.pixelClock
    io.frameBuffer.data_b    := DontCare // Pixel Clock DomainからはWriteはしない
    io.frameBuffer.rden_b    := readFbEnReg
    io.frameBuffer.wren_b    := false.B  // Writeはしない
    // DPRAM読み出し要求と同じタイミングでセットして使う
    val hsyncPrefetchReg = RegInit(Bool(), false.B)
    val vsyncPrefetchReg = RegInit(Bool(), false.B)

    // VGA信号
    val rOutReg  = RegInit(UInt(channelDataWidth.W), 0.U)
    val gOutReg  = RegInit(UInt(channelDataWidth.W), 0.U)
    val bOutReg  = RegInit(UInt(channelDataWidth.W), 0.U)
    val hsyncReg = RegInit(Bool(), true.B)
    val vsyncReg = RegInit(Bool(), true.B)
    io.vgaOut.r     := rOutReg
    io.vgaOut.g     := gOutReg
    io.vgaOut.b     := bOutReg
    io.vgaOut.hsync := hsyncReg
    io.vgaOut.vsync := vsyncReg

    // カウント最大値とそのbit幅
    val xCounterMax   = vgaConfig.hsync.counterMax
    val xCounterWidth = vgaConfig.hsync.counterWidth
    val yCounterMax   = vgaConfig.vsync.counterMax
    val yCounterWidth = vgaConfig.vsync.counterWidth

    // X,Y方向のpixel counter
    val xCounter = RegInit(UInt(xCounterWidth.W), 0.U)
    val yCounter = RegInit(UInt(yCounterWidth.W), 0.U)

    // Xカウント -> Yカウント
    when(xCounter < xCounterMax.U) {
      xCounter := xCounter + 1.U
    }.otherwise {
      xCounter := 0.U
      // Xが1順したらYを足す
      when(yCounter < yCounterMax.U) {
        yCounter := yCounter + 1.U
      }.otherwise {
        yCounter := 0.U
      }
    }

    // X,Y位置を確認して, DPRAMの読み出しを行う
    val isActiveX =
      (vgaConfig.hsync.activeVideoStart.U <= xCounter) && (xCounter < vgaConfig.hsync.activeVideoEnd.U)
    val isActiveY =
      (vgaConfig.vsync.activeVideoStart.U <= yCounter) && (yCounter < vgaConfig.vsync.activeVideoEnd.U)
    val isSyncX =
      (vgaConfig.hsync.syncStart.U <= xCounter) && (xCounter < vgaConfig.hsync.syncEnd.U)
    val isSyncY =
      (vgaConfig.vsync.syncStart.U <= yCounter) && (yCounter < vgaConfig.vsync.syncEnd.U)

    when(isActiveX && isActiveY) {
      // 有効な領域なので座標を取得する
      val offsetX = xCounter - vgaConfig.hsync.activeVideoStart.U
      val offsetY = yCounter - vgaConfig.vsync.activeVideoStart.U

      // FrameBufferの描画領域内に位置するか確認
      val isDrawX =
        (ppuConfig.leftX.U <= offsetX) && (offsetX < ppuConfig.rightX.U)
      val isDrawY =
        (ppuConfig.topY.U <= offsetY) && (offsetY < ppuConfig.bottomY.U)

      when(isDrawX && isDrawY) {
        // FBから読み出して表示するので、更にFB上の座標に変換。scaleが指定されているときは引き伸ばす
        val fbX = (offsetX - ppuConfig.leftX.U) >> (ppuConfig.scale - 1).U
        val fbY = (offsetY - ppuConfig.topY.U) >> (ppuConfig.scale - 1).U
        readFbAddrReg    := posToFbAddr(fbX, fbY)
        readFbEnReg      := true.B
        hsyncPrefetchReg := true.B
        vsyncPrefetchReg := true.B
      }.otherwise {
        // 画面範囲にはあるが、FB範囲外
        readFbAddrReg    := posToFbAddr(0.U, 0.U)
        readFbEnReg      := false.B // DPRAMからは読み出さない
        hsyncPrefetchReg := true.B
        vsyncPrefetchReg := true.B
      }
    }.otherwise {
      // 有効な領域ではない。Front/Back PorchもしくはSync
      readFbAddrReg    := posToFbAddr(0.U, 0.U)
      readFbEnReg      := false.B
      hsyncPrefetchReg := !isSyncX // SyncはActiveLowなので注意
      vsyncPrefetchReg := !isSyncY // SyncはActiveLowなので注意
    }

    // 現在の読み出し結果から出力信号を決定する
    when(readFbEnReg) {
      when(io.isDebug) {
        // デバッグ用に読み出しアドレスからパターンを作る
        val (x, y) = fbAddrToPos(readFbAddrReg)
        rOutReg := x
        gOutReg := y
        bOutReg := (x >> 1) + y
      }.otherwise {
        // 有効なデータを読みだした後
        val (r, g, b) = fbDataToRgb(io.frameBuffer.q_b)
        rOutReg := r
        gOutReg := g
        bOutReg := b
      }
      // Active Areaなのでtrue固定
      hsyncReg := true.B
      vsyncReg := true.B
    }.otherwise {
      rOutReg  := 0.U // ActiveAreaでもFB外のケースがあるので黒にしておく
      gOutReg  := 0.U // ActiveAreaでもFB外のケースがあるので黒にしておく
      bOutReg  := 0.U // ActiveAreaでもFB外のケースがあるので黒にしておく
      hsyncReg := hsyncPrefetchReg
      vsyncReg := vsyncPrefetchReg
    }
  }
}
