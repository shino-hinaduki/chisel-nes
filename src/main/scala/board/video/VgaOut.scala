package board.video

import chisel3._
import chisel3.util.log2Up
import chisel3.util.Cat
import chisel3.util.switch
import chisel3.internal.firrtl.Width

import ppu.types.PpuOutIO
import board.video.types.FrameBufferIO
import board.video.types.VgaConfig
import board.video.types.PpuImageConfig
import board.video.types.VgaIO
import board.ram.types.AsyncFifoDequeueIO
import board.ram.types.AsyncFifoEnqueueIO
import board.access.types.InternalAccessCommand

/**
  * PPUからの映像出力をFrameBufferにため、FrameBufferから映像を出力する
  * Module自体はPPUからのクロックで駆動し、映像出力部分はio.pxelClockで供給したclockで駆動する
  * @param vgaConfig 出力設定。初期値は 640x480 60Hz
  * @param ppuConfig PPUの映像配置設定。初期値は中央に等倍、背景色は黒
  */
class VgaOut(
    val vgaConfig: VgaConfig = VgaConfig.minConf,
    val ppuConfig: PpuImageConfig = PpuImageConfig(
      centerX = VgaConfig.minConf.width / 2,
      centerY = VgaConfig.minConf.height / 2,
      scale = 1,
      bgColorR = 0,
      bgColorG = 0,
      bgColorB = 0
    ),
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
    val ppuVideoOut = Flipped(new PpuOutIO())
    // 外部デバッグ用のRAM Control命令。ppuClock Domainで駆動する。 ppuVideoOutより優先される
    val debugAccess = new InternalAccessCommand.SlaveIO

    // trueならテスト信号を出力する。pixelClockに同期して読み出される
    val isDebug = Input(Bool())
    // VGA出力用のpixel clock
    val pixelClock = Input(Clock())
    // pixelClock DomainのReset
    val pixelClockReset = Input(Reset())
    // FrameBuffer用RAMとの接続
    val frameBuffer = new FrameBufferIO(fbAddrWidth, fbDataWidth)
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
  /* PPU VideoOut or DebugAccessReq(AsyncFIFO) -> DualPort RAM         */
  // PPU -> DPRAM
  val ppuFbAddrReg = RegInit(UInt(fbAddrWidth.W), 0.U)
  val ppuFbDataReg = RegInit(UInt(fbDataWidth.W), 0.U)
  val ppuFbWrEnReg = RegInit(Bool(), false.B)
  val ppuFbRdEnReg = RegInit(Bool(), false.B) // DebugAccess用限定なので、これが立っていたら応答を返す
  io.frameBuffer.ppu.address := ppuFbAddrReg
  io.frameBuffer.ppu.clock   := clock // VgaOut自体のClockはPPU Clockで駆動する
  io.frameBuffer.ppu.data    := ppuFbDataReg
  io.frameBuffer.ppu.rden    := ppuFbRdEnReg
  io.frameBuffer.ppu.wren    := ppuFbWrEnReg

  // ppu clock domainからのR/Wは出さない
  def fbByPpuNop() = {
    ppuFbAddrReg := DontCare
    ppuFbDataReg := DontCare
    ppuFbWrEnReg := false.B
    ppuFbRdEnReg := false.B
  }
  // ppu clock domainからWrite
  def fbByPpuWrite(addr: UInt, data: UInt) = {
    ppuFbAddrReg := addr
    ppuFbDataReg := data
    ppuFbWrEnReg := true.B
    ppuFbRdEnReg := false.B
  }
  // ppu clock domainからRead
  def fbByPpuRead(addr: UInt) = {
    ppuFbAddrReg := addr
    ppuFbDataReg := DontCare
    ppuFbWrEnReg := false.B
    ppuFbRdEnReg := true.B
  }

  // DebugAccessReq(AsyncFIFO) -> DPRAM
  val debugAccessReqDequeueReg = RegInit(Bool(), false.B)
  io.debugAccess.req.rdclk := clock
  io.debugAccess.req.rdreq := debugAccessReqDequeueReg

  // Dequeueしない
  def debugReqNop() = {
    debugAccessReqDequeueReg := false.B
  }
  // Dequeueする
  def debugReqDequeue() = {
    debugAccessReqDequeueReg := true.B
  }

  // Debug AccessはPPUのDataWriteより優先される。Dequeue中でなく新規Msgがあれば処理
  when(!io.debugAccess.req.rdempty && !debugAccessReqDequeueReg) {
    // 命令をデコードして、Read/WriteをDPRAMに投げる
    val addr               = InternalAccessCommand.Request.getOffset(io.debugAccess.req.q)
    val data               = InternalAccessCommand.Request.getData(io.debugAccess.req.q)
    val (reqType, isValid) = InternalAccessCommand.Request.getRequestType(io.debugAccess.req.q)

    when(isValid) {
      when(reqType === InternalAccessCommand.Type.read) {
        // Read & Dequeue
        fbByPpuRead(addr)
        debugReqDequeue()
      }.elsewhen(reqType === InternalAccessCommand.Type.write) {
        // Write & Dequeue
        fbByPpuWrite(addr, data)
        debugReqDequeue()
      }.otherwise {
        // 未対応、Dequeueだけ実施
        fbByPpuNop()
        debugReqDequeue()
      }
    }.otherwise {
      // Dequeuだけ実施
      fbByPpuNop()
      debugReqDequeue()
    }
  }.elsewhen(io.ppuVideoOut.valid) {
    // PPUが出力している現在の値をDPRAMへWrite
    val addr = posToFbAddr(io.ppuVideoOut.x, io.ppuVideoOut.y)
    val data = rgbToFbData(io.ppuVideoOut.r, io.ppuVideoOut.g, io.ppuVideoOut.b)
    fbByPpuWrite(addr, data)
    // 処理してないのでDequeueしない
    debugReqNop()
  }.otherwise {
    // 要求は出さない, 処理してないのでDequeueしない
    fbByPpuNop()
    debugReqNop()
  }

  // DPRAM -> DebugAccessResp(AsyncFIFO)
  val debugAccessRespDataReg    = RegInit(UInt(InternalAccessCommand.Response.cmdWidth.W), 0.U)
  val debugAccessRespEnqueueReg = RegInit(Bool(), false.B)
  io.debugAccess.resp.wrclk := clock
  io.debugAccess.resp.data  := debugAccessRespDataReg
  io.debugAccess.resp.wrreq := debugAccessRespEnqueueReg

  // Enqueueしない
  def debugRespNop() = {
    debugAccessRespDataReg    := DontCare
    debugAccessRespEnqueueReg := false.B
  }
  // Enqueueする
  def debugRespEnqueue(data: UInt) = {
    debugAccessRespDataReg    := data
    debugAccessRespEnqueueReg := false.B
  }

  // 本cycでReadしていたら、その結果をEnqueueする
  when(ppuFbRdEnReg && !io.debugAccess.resp.wrfull) {
    // 応答をQueueに乗せる
    val data = InternalAccessCommand.Response.encode(io.frameBuffer.ppu.q) // 上位1byteは未使用だが詰めない
    debugRespEnqueue(data)
  }.otherwise {
    // 応答しない。QueueFullなら結果は捨てる
    debugRespNop()
  }

  /*********************************************************************/
  /* DualPort RAM -> VGA out                                           */
  withClockAndReset(io.pixelClock, io.pixelClockReset) {
    // VGAタイミング制御用カウント
    val xCounterMax   = vgaConfig.hsync.counterMax
    val xCounterWidth = vgaConfig.hsync.counterWidth
    val yCounterMax   = vgaConfig.vsync.counterMax
    val yCounterWidth = vgaConfig.vsync.counterWidth

    // X,Y方向のpixel counter
    val xCounter = RegInit(UInt(xCounterWidth.W), 0.U)
    val yCounter = RegInit(UInt(yCounterWidth.W), 0.U)

    // Xカウント -> Yカウント
    when(xCounter < (xCounterMax - 1).U) {
      xCounter := xCounter + 1.U
    }.otherwise {
      xCounter := 0.U
      // Xが1順したらYを足す
      when(yCounter < (yCounterMax - 1).U) {
        yCounter := yCounter + 1.U
      }.otherwise {
        yCounter := 0.U
      }
    }

    // Dual Port RAM読み出し用
    val vgaFbAddrReg = RegInit(UInt(fbAddrWidth.W), 0.U)
    val vgaFbRdEnReg = RegInit(Bool(), false.B)
    io.frameBuffer.vga.address := vgaFbAddrReg
    io.frameBuffer.vga.clock   := io.pixelClock
    io.frameBuffer.vga.data    := DontCare // Pixel Clock DomainからはWriteはしない
    io.frameBuffer.vga.rden    := vgaFbRdEnReg
    io.frameBuffer.vga.wren    := false.B  // Writeはしない
    // DPRAM読み出し要求と同じタイミングでセットして使う
    val hsyncPrefetchReg = RegInit(Bool(), true.B) // active low
    val vsyncPrefetchReg = RegInit(Bool(), true.B) // active low

    // vga clock domainからのR/Wは出さない
    def fbByVgaNop(isNeedHsync: Bool, isNeedVSync: Bool) = {
      vgaFbAddrReg     := DontCare
      vgaFbRdEnReg     := false.B
      hsyncPrefetchReg := !isNeedHsync // activeLowなのでPrefetch regに入れる時点で論理を戻す
      vsyncPrefetchReg := !isNeedVSync // activeLowなのでPrefetch regに入れる時点で論理を戻す
    }
    // vga clock domainからReadしつつ、HSYNC,VSYNC要否も同じタイミングで保持
    def fbByVgaRead(addr: UInt, isNeedHsync: Bool, isNeedVSync: Bool) = {
      vgaFbAddrReg     := addr
      vgaFbRdEnReg     := true.B
      hsyncPrefetchReg := !isNeedHsync // activeLowなのでPrefetch regに入れる時点で論理を戻す
      vsyncPrefetchReg := !isNeedVSync // activeLowなのでPrefetch regに入れる時点で論理を戻す
    }

    // X,Y位置を確認して, DPRAMの読み出しを行う
    val isActiveX =
      (vgaConfig.hsync.activeVideoStart.U <= xCounter) && (xCounter < vgaConfig.hsync.activeVideoEnd.U)
    val isActiveY =
      (vgaConfig.vsync.activeVideoStart.U <= yCounter) && (yCounter < vgaConfig.vsync.activeVideoEnd.U)

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
        val fbX  = (offsetX - ppuConfig.leftX.U) >> (ppuConfig.scale - 1).U
        val fbY  = (offsetY - ppuConfig.topY.U) >> (ppuConfig.scale - 1).U
        val addr = posToFbAddr(fbX, fbY)

        fbByVgaRead(addr, false.B, false.B) // ActiveVideo内なのでSync不要
      }.otherwise {
        // 画面範囲にはあるが、FB範囲外
        fbByVgaNop(false.B, false.B) // ActiveVideo内なのでSync不要
      }
    }.otherwise {
      // 有効な領域ではない。Front/Back PorchもしくはSync
      val isNeedHSync =
        (vgaConfig.hsync.syncStart.U <= xCounter) && (xCounter < vgaConfig.hsync.syncEnd.U)
      val isNeedVSync =
        (vgaConfig.vsync.syncStart.U <= yCounter) && (yCounter < vgaConfig.vsync.syncEnd.U)

      fbByVgaNop(isNeedHSync, isNeedVSync)
    }

    // VGA信号生成
    val rOutReg  = RegInit(UInt(channelDataWidth.W), 0.U)
    val gOutReg  = RegInit(UInt(channelDataWidth.W), 0.U)
    val bOutReg  = RegInit(UInt(channelDataWidth.W), 0.U)
    val hsyncReg = RegInit(Bool(), true.B) // active low
    val vsyncReg = RegInit(Bool(), true.B) // active low
    io.vgaOut.r     := rOutReg
    io.vgaOut.g     := gOutReg
    io.vgaOut.b     := bOutReg
    io.vgaOut.hsync := hsyncReg
    io.vgaOut.vsync := vsyncReg
    // ビデオ出力信号を設定する
    def setVideoOut(r: UInt, g: UInt, b: UInt, hsync: Bool, vsync: Bool) = {
      rOutReg  := r
      gOutReg  := g
      bOutReg  := b
      hsyncReg := hsync
      vsyncReg := vsync
    }

    // 現在の読み出し結果から出力信号を決定する
    when(vgaFbRdEnReg) {
      // Active Video Area
      when(io.isDebug) {
        // デバッグ用に読み出しアドレスからパターンを作る
        val (x, y) = fbAddrToPos(vgaFbAddrReg)
        setVideoOut(x, y, (x >> 1) + y, true.B, true.B) // ActiveAreaなのでtrue固定
      }.otherwise {
        // 有効なデータを読みだした後
        val (r, g, b) = fbDataToRgb(io.frameBuffer.vga.q)
        setVideoOut(r, g, b, true.B, true.B) // ActiveAreaなのでtrue固定
      }
    }.otherwise {
      // FrontPorch, BackPorch, Sync。ActiveVideoだがFB範囲外の場合。最後のケースに備えて背景色を見せる
      setVideoOut(
        ppuConfig.bgColorR.U(channelDataWidth.W),
        ppuConfig.bgColorG.U(channelDataWidth.W),
        ppuConfig.bgColorB.U(channelDataWidth.W),
        hsyncPrefetchReg,
        vsyncPrefetchReg
      )
    }
  }
}
