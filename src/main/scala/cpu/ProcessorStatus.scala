package cpu

import chisel3._

/** Processor Status Register
  */
class ProcessorStatus extends Bundle {
  val negative  = Bool()
  val overflow  = Bool()
  val reserved  = Bool()
  val break     = Bool()
  val decimal   = Bool()
  val interrupt = Bool()
  val zero      = Bool()
  val carry     = Bool()
}
