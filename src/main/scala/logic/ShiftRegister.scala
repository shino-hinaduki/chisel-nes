package logic

import chisel3._
import chisel3.util.Cat

/**
  * C4021 Shift Register
  * 
  * Truth Table (Serial In/Serial Out)
  * | CLK | P / /S | PI[0] | PI[n] | SI | Q[0]      | Q[n]       |
  * | pos | L      | -     | -     | L  | L         | Q[n-1]     |
  * | pos | L      | -     | -     | H  | H         | Q[n-1]     |
  * | neg | L      | -     | -     | -  | no change | no change  |
  *
  * Truth Table (Parallel In/Serial Out)
  * | CLK | P / /S | PI[0] | PI[n] | SI | Q[0]      | Q[n]       |
  * | -   | H      | L     | L     | -  | L         | L          |
  * | -   | H      | L     | H     | -  | L         | H          |
  * | -   | H      | H     | L     | -  | H         | L          |
  * | -   | H      | H     | H     | -  | H         | H          |
  * 
  * P/ /S pinで動作が決まっている。
  *  * Serialにすると、clkごとにシフトする。Q0に次はいる値はSIの現在地
  *  * ParallelにするとQ0~Q7はPI0~PI7の値をラッチする
  * 用途としてはParallelでまとめて値を取り込んで、Serialに切り替えシフトしつつQ7あたりで順番に受ける感じ
  * 
  */
class ShiftRegister extends Module {
  val io = IO(new Bundle {
    val pi      = Input(UInt(8.W))
    val si      = Input(Bool())
    val nSerial = Input(Bool())
    val q5      = Output(Bool())
    val q6      = Output(Bool())
    val q7      = Output(Bool())
  })

  // 保持しているデータの実体
  val shiftReg = RegInit(UInt(8.W), 0.U)
  when(io.nSerial) {
    // パラレル入力を取り込み
    shiftReg := io.pi
  }.otherwise {
    // 左1シフト, SIの内容を最下位に取り込み
    shiftReg := Cat(shiftReg(6, 0), io.si)
  }

  // pin assign
  io.q5 := shiftReg(5)
  io.q6 := shiftReg(6)
  io.q7 := shiftReg(7)
}
