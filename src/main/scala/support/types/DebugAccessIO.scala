package support.types

import chisel3._

/**
 * DebugAccessPortに要求するI/F定義。使う側はFlippedして使う
 */
class DebugAccessIO extends Bundle {
  // 読み出し対象の選択
  val dataKind = Input(new DebugAccessDataKind.Type)
  // 読み出し先のアドレス
  val addr = Input(UInt(16.W))
  // 書き込むデータ, isWrite=true時のみ参照
  val writeData = Input(UInt(8.W))
  // R/Wどちらを要求するか
  val isWrite = Input(Bool())
  // 立ち上がりで要求
  val reqStrobe = Input(Bool())

  // 処理中であればtrue。この間はreqStrobeを受け付けない
  val busy = Output(Bool())
  // 処理環境時にfalse
  val done = Output(Bool())
  // 読みだしたデータ。isWrite=false & done時のみ有効
  val readData = Output(UInt(8.W))
  // Read要求をやり終わったときのみtrue
  val readDataValid = Output(Bool())
}
