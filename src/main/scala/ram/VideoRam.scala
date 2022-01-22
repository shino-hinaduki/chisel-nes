package ram

/** 本体内臓のVideo RAM
  * 0x2000 ~ 0x27ff の 2KiB
  * ミラーリング設定はカードリッジ側の結線次第
  */
class VideoRam extends Ram {}
