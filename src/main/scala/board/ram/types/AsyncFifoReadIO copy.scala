package board.ram.types

import chisel3._

/**
  * Async FIFO の Read Port定義
  *
  * @param dataWidth データ幅
  */
class AsyncFifoReadIO(val dataWidth: Int) extends Bundle {

  /**
    * Asyncronouse Clear
    */
  val aclr = Input(Bool())

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
  val q = Output(UInt(64.W))

  /**
    * Queue empty
    */
  val rdempty = Output(Bool())
}
