package gcd

import chisel3._

class VirtualCart extends Module {
  val io = IO(new Bundle {
    val bus0Addr = Input(UInt(8.W))
    val bus0Data = Input(UInt(8.W))
    val bus1Addr = Input(UInt(16.W))
    val bus1Data = Input(UInt(8.W))
  })
}
