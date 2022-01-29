package cpu.bus

import chisel3.Bundle

import chisel3._

/** Arbiterの受け側, Master側はFlipped()で定義する
  */
class BusSlave extends Bundle {
  // アクセス先のアドレス
  val addr = Input(UInt(16.W))
  // R/W要求している場合はtrue
  val nRequest = Input(Bool())
  // 要求内容がWriteが有効ならtrue、Readが有効ならfalse
  val nWriteEnable = Input(Bool())
  // (要求後次Cycleで出力) バス調停が通り要求内容の通りにBusAddr/Dataが設定できていればtrue
  val nValid = Output(Bool())

  // (Writeする場合)対象のデータ
  val dataIn  = Input(UInt(8.W))
  val dataOut = Output(UInt(8.W))
}

/** Address Bus/Data Busの調停を行う
  */
class Arbiter extends Module {
  val io = IO(new Bundle {})
}
