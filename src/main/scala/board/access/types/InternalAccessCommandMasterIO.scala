package board.access.types

import chisel3._
import board.ram.types.AsyncFifoEnqueueIO
import board.ram.types.AsyncFifoDequeueIO

/**
  * 外部からのデータ読み書き向けの要求/応答Queue制御
  * AsyncFIFOで分断することを想定
  */
class InternalAccessCommandMasterIO extends Bundle {

  /**
    * 要求Queue
    */
  val req = Flipped(new AsyncFifoEnqueueIO(InternalAccessCommand.Request.cmdWidth))

  /**
    * 応答Queue
    */
  val resp = Flipped(new AsyncFifoDequeueIO(InternalAccessCommand.Response.cmdWidth))
}
