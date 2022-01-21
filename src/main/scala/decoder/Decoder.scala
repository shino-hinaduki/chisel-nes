package decoder

import chisel3._
import chisel3.util.switch
import chisel3.util.Cat
import chisel3.util.is

/**
  * LS139, CPU Busのアドレス線から /RAMSEL, /DBE, /ROMSEL 信号を生成する
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
    val nG = Input(Bool())
    // Outputs Y
    val y = Output(UInt(4.W))
  })
  // select A/Bを数値化
  val input = Cat(io.b, io.a)

  when(io.nG) {
    // /G=H
    io.y := "b1111".U
  }.otherwise {
    switch(input) {
      is("b00".U) {
        io.y := "b1110".U
      }
      is("b01".U) {
        io.y := "b1101".U
      }
      is("b10".U) {
        io.y := "b1011".U
      }
      is("b11".U) {
        io.y := "b0111".U
      }
    }
  }
}
