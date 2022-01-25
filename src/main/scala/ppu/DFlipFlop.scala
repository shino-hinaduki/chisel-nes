package ppu

import chisel3._

/** LS377 Enable付きD-FF。PPUのアドレスバスに使用
  * 
  * | /EN | CLK     | DATA | Q     | /Q     |
  * | H   | X       | X    | Qprev | /Qprev |
  * | L   | posedge | H    | H     | L      |
  * | L   | posedge | L    | L     | H      |
  * | X   | L       | X    | Qprev | Qprev  |
  * 
  * posedgeで/ENが有効ならデータを取り込むだけ
  */
class DFlipFlop extends Module {
  val io = IO(new Bundle {
    val nEn = Input(Bool())
    val d   = Input(UInt(8.W))
    val q   = Output(UInt(8.W))
  })

  // FFの実体
  val reg = RegInit(UInt(8.W), 0.U)
  io.q := reg

  when(io.nEn) {
    // disale
    reg := reg
  }.otherwise {
    // enable
    reg := io.d
  }
}
