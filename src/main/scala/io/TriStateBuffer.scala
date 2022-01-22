package io

import chisel3._

/**
  * HC368 3-state inverter
  * 
  * Truth Table
  * | /OE | A[n] | Y[n] |
  * | H   | X    | Z    |
  * | L   | L    | L    |
  * | L   | H    | H    |
  * 
  * /OEが無効ならHi-Z, /OEが有効ならAを出力するだけ
  */
class TriStateBuffer extends Module {
  val io = IO(new Bundle {
    // 0-3の入出力設定
    val nEn0 = Input(Bool())
    // 4-5の入出力設定
    val nEn1 = Input(Bool())
    // 入力
    val a = Input(UInt(6.W))
    // 出力
    val y = Output(UInt(6.W))
  })
  // 0-3
  when(io.nEn0) {
    io.y(0) := io.a(0)
    io.y(1) := io.a(1)
    io.y(2) := io.a(2)
    io.y(3) := io.a(3)
  }.otherwise {
    io.y(0) := DontCare
    io.y(1) := DontCare
    io.y(2) := DontCare
    io.y(3) := DontCare
  }

  // 4-5
  when(io.nEn0) {
    io.y(4) := io.a(4)
    io.y(5) := io.a(5)
  }.otherwise {
    io.y(4) := DontCare
    io.y(5) := DontCare
  }
}
