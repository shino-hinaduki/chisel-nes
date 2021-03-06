package board.ram.types

import chisel3._

/**
  * Async FIFO の Write Port定義
  *
  * @param dataWidth データ幅
  */
class AsyncFifoEnqueueIO(val dataWidth: Int) extends Bundle {

  /**
    * Write Data in
    */
  val data = Input(UInt(dataWidth.W))

  /**
    * Write Clock
    */
  val wrclk = Input(Clock())

  /**
    * Write Enable
    */
  val wrreq = Input(Bool())

  /**
    * Queue Full
    */
  val wrfull = Output(Bool())
}
