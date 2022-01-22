package bus

import chisel3._
import chisel3.util.Cat

/**
  * LS139 Decoder
  * 
  * Truth Table
  * | EN | SEL   | OUTPUTS           |
  * | /G | B | A | Y3 | Y2 | Y1 | Y0 |
  * | H  | X | X | H  | H  | H  | H  |
  * | L  | L | L | H  | H  | H  | L  |
  * | L  | L | H | H  | H  | L  | H  |
  * | L  | H | L | H  | L  | H  | H  |
  * | L  | H | H | L  | H  | H  | H  |
  * 
  * { B, A }で選んだ数字がY[x]にデコードされて出てくるイメージ
  */
class Decoder extends Module {
  val io = IO(new Bundle {
    // select A
    val a = Input(Bool())
    // select B
    val b = Input(Bool())
    // Enable /G
    val nEn = Input(Bool())
    // Outputs Y
    val y = Output(UInt(4.W))
  })
  // select A/Bを数値化
  val sel = Cat(io.b, io.a)

  when(io.nEn) {
    // disable
    io.y := "b1111".U
  }.otherwise {
    // enable, a,bで選んだ位置を立てて反転
    io.y := ~(1.U << sel)
  }
}
