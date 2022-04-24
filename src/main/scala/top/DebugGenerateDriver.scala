package top

import chisel3._
import board.access.DebugAccessTester
import board.cart.CartridgeHub
import board.cart.VirtualCartridge
import board.discrete.Debounce
import board.discrete.SevenSegmentLed
import board.gpio.GpioMapping
import board.jtag.VirtualJtagBridge
import board.video.VgaOut
import bus.BusArbiter
import bus.BusSelector
import cpu.InstructionFetch
import cpu.OperandFetch
import logic.Decoder
import logic.DFlipFlop
import logic.InvertBuffer
import logic.ShiftRegister
import ram.VideoRam
import ram.WorkRam

/**
  * デバッグ用にVerilog出力を行うドライバ
  */
object DebugGenerateDriver extends App {
  val chiselStage = (new chisel3.stage.ChiselStage)

  // TODO: メンテナンスが億劫であれば、Reflectionを使って列挙する
  chiselStage.emitVerilog(new DebugAccessTester, args)
  chiselStage.emitVerilog(new CartridgeHub, args)
//   chiselStage.emitVerilog(new VirtualCartridge, args) // Abstract Reset not allowed as top-level input: io.cpuReset/ppuReset
  chiselStage.emitVerilog(new Debounce(inputWidth = 4, averageNum = 4, isActiveLow = true), args)
  chiselStage.emitVerilog(new SevenSegmentLed, args)
//   chiselStage.emitVerilog(new VirtualJtagBridge, args) // Abstract Reset not allowed as top-level input: io.reset
//   chiselStage.emitVerilog(new VgaOut, args) // Abstract Reset not allowed as top-level input: io.pixelClockReset
  chiselStage.emitVerilog(new BusArbiter(n = 4), args)
  chiselStage.emitVerilog(new BusSelector, args)
  chiselStage.emitVerilog(new InstructionFetch, args)
//   chiselStage.emitVerilog(new OperandFetch(resetOnPanic = true), args) // TODO: Build Error
  chiselStage.emitVerilog(new Decoder, args)
  chiselStage.emitVerilog(new DFlipFlop, args)
  chiselStage.emitVerilog(new InvertBuffer, args)
  chiselStage.emitVerilog(new ShiftRegister, args)
  chiselStage.emitVerilog(new VideoRam, args)
  chiselStage.emitVerilog(new WorkRam, args)
}
