package board.ip

import chisel3._
import chisel3.experimental.Analog

/**
  * gpio_bidirectional.v を chiselで取り扱うために用意した定義
  */
class GpioBidirectional extends BlackBox {
  override def desiredName: String = "gpio_bidirectional"

  val io = IO(new Bundle {
    val datain  = Input(UInt(1.W))  // input   [0:0]  datain;
    val dataio  = Analog(1.W)       // inout   [0:0]  dataio;
    val dataout = Output(UInt(1.W)) // output   [0:0]  dataout;
    val oe      = Input(Bool())     // input   [0:0]  oe;
  })
}
