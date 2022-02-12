package bus.types

import chisel3._

/** Arbiterの受け側, Master側はFlipped()で定義する
  */
class BusIO extends Bundle {
  // アクセス先のアドレス
  val addr = Input(UInt(16.W))
  // R/W要求している場合はfalse
  val req = Input(Bool())
  // 要求内容がWriteが有効ならtrue、Readが有効ならfalse
  val writeEnable = Input(Bool())
  // (要求後次Cycleで出力) バス調停が通り要求内容の通りにBusAddr/Dataが設定できていればtrue
  val valid = Output(Bool())

  // (Writeする場合)対象のデータ, nWriteEnable=trueの場合はDon't care
  val dataIn = Input(UInt(8.W))
  // (Readする場合)対象のデータ
  val dataOut = Output(UInt(8.W))
}
