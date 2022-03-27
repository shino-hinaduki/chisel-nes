package board.access.types

import chisel3._
import board.ram.types.AsyncFifoEnqueueIO
import board.ram.types.AsyncFifoDequeueIO

/**
  * 外部からのデータ読み書き向けの要求受信/応答返却Queue制御
  * AsyncFIFOで分断することを想定
  */
class InternalAccessCommandSlaveIO extends Bundle {

  /**
    * 要求Queue
    */
  val req = new AsyncFifoDequeueIO(InternalAccessCommand.Request.cmdWidth)

  /**
    * 応答Queue
    */
  val resp = new AsyncFifoEnqueueIO(InternalAccessCommand.Response.cmdWidth)
}
