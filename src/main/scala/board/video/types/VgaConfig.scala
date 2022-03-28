package board.video.types

import chisel3._
import chisel3.util.log2Up

/**
  * VGA SYNC信号の設定
  * @param backPortch syncPulse後の区間
  * @param activeVideo 有効なビデオ領域
  * @param frontPorch  syncPulse前の区間
  * @param syncPulse SYNC=Low区間
  */
case class SyncSignal(
    backPorch: Int,
    activeVideo: Int,
    frontPorch: Int,
    syncPulse: Int,
) {

  /**
    * カウント最大値
    */
  val counterMax = activeVideo + frontPorch + syncPulse + backPorch

  /**
    * カウント最大値を格納するのに必要なビット幅
    */
  val counterWidth = log2Up(counterMax)

  /**
    * Back Porch開始count
    */
  val backPorchStart = 0

  /**
    * Back Portch終了count
    */
  val backPorchEnd = backPorchStart + backPorch

  /**
    * Active Video開始count
    */
  val activeVideoStart = backPorchEnd

  /**
    * Active Video終了count
    */
  val activeVideoEnd = activeVideoStart + activeVideo

  /**
    * Front Porch開始count
    */
  val frontPorchStart = activeVideoEnd

  /**
    * Front Portch終了count
    */
  val frontPorchEnd = frontPorchStart + frontPorch

  /**
    * Sync開始count
    */
  val syncStart = frontPorchEnd

  /**
    * Sync終了count
    */
  val syncEnd = syncStart + syncPulse

  /**
    * これより大きな値は範囲外
    */
  val outOfRangeStart = syncEnd
}

/**
  * VGA信号の設定
  * @param pixelClockFreq Pixel Clock周波数
  * @param horizontalSync HSYNCの設定
  * @param verticalSync VSYNCの設定
  */
case class VgaConfig(
    pixelClockFreq: Double,
    hsync: SyncSignal,
    vsync: SyncSignal,
) {

  /**
      * 映像の横幅
      */
  val width = hsync.activeVideo

  /**
      * 映像の高さ
      */
  val height = vsync.activeVideo
}

object VgaConfig {

  /**
    * 640x480 60Hzの表示設定を返します
    */
  def minConf = VgaConfig(
    pixelClockFreq = 25.175e6,
    hsync = SyncSignal(backPorch = 48, activeVideo = 640, frontPorch = 16, syncPulse = 96),
    vsync = SyncSignal(backPorch = 31, activeVideo = 480, frontPorch = 11, syncPulse = 2),
  )
}
