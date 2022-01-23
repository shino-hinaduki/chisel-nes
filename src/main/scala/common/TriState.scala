package common

import chisel3._

object TriState {
  def apply[T <: Data](data: T) = new TriState(data)
}

/** 双方向バス、もしくはHi-Zの定義が必要なときに必要
 */
class TriState[T <: Data](source: T) extends Bundle {
  val data = source
  val oe   = Bool()
}
