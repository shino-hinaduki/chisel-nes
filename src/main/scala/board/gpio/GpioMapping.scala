package board.gpio

import chisel3._
import chisel3.experimental.Analog

/**
  * eda/kicad_6.0.2/nes_peripheral の信号に変換する
  */
class GpioMapping extends RawModule {
  val io = IO(new Bundle {
    // DE0-CV GPIO0
    val GPIO_0 = IO(Analog(36.W))
    // DE0-CV GPIO1
    val GPIO_1 = IO(Analog(36.W))
  })
}
