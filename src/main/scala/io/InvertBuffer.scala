package io

import chisel3._
import common.TriState

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
class InvertBuffer extends Module {
  val io = IO(new Bundle {
    // 0-3の入出力設定
    val nEn0 = Input(Bool())
    // 4-5の入出力設定
    val nEn1 = Input(Bool())
    // 入力
    val a = Input(UInt(6.W))
    // 出力
    val y0 = Output(TriState(UInt(4.W)))
    val y1 = Output(TriState(UInt(2.W)))
  })
  // dataは常に接続しておいて、OEで制御
  io.y0.data := ~(io.a(3, 0))
  io.y1.data := ~(io.a(4, 3))

  // OEでBus切り替え
  io.y0.oe := !io.nEn0;
  io.y1.oe := !io.nEn1;
}
