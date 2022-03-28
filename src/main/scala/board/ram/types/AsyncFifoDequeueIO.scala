package board.ram.types

import chisel3._

/**
  * Async FIFO の Read Port定義
  *
  * @param dataWidth データ幅
  */
class AsyncFifoDequeueIO(val dataWidth: Int) extends Bundle {

  /**
    * Read Clock
    */
  val rdclk = Input(Clock())

  /**
    * Read Enable
    */
  val rdreq = Input(Bool())

  /**
    * Read Data out
    */
  val q = Output(UInt(dataWidth.W))

  /**
    * Queue empty
    */
  val rdempty = Output(Bool())
}
