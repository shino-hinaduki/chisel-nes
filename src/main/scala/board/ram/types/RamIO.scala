package board.ram.types

import chisel3._

/**
  * DualPort RAMのPort1個分の定義
  *
  * @param addrWidth アドレス幅
  * @param dataWidth データ幅
  */
class RamIO(val addrWidth: Int, val dataWidth: Int) extends Bundle {

  /**
      * Access Addr
      */
  val addr = Input(UInt(addrWidth.W))

  /**
    * Access Clock
    */
  val clock = Input(Clock())

  /**
    * Write Data In
    */
  val data = Input(UInt(dataWidth.W))

  /**
    * Read Enable
    */
  val rden = Input(Bool())

  /**
    * Write Enable
    */
  val wren = Input(Bool())

  /**
    * Read Data out
    */
  val q = Output(UInt(dataWidth.W))
}
