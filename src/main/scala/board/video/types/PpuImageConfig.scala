package board.video.types

/**
  * PPUの映像を配置する際の設定
  * ガンマ補正やX/Y Flipなどが必要であればこれに定義する
  * 
  * @param centerX 中心X座標
  * @param centerY 中心Y座標
  * @param scale 拡大率
  */
case class PpuImageConfig(
    centerX: Int,
    centerY: Int,
    scale: Int,
) {

  /**
    * 映像幅
    */
  val width = 256 * scale

  /**
    * 映像高さ
    */
  val height = 240 * scale

  /**
    * 左座標
    */
  val leftX = centerX - width / 2

  /**
    * 右座標
    */
  val rightX = centerX + width / 2

  /**
    * 上座標
    */
  val topY = centerY - height / 2

  /**
    * 下座標
    */
  val bottomY = centerY + height / 2

}
