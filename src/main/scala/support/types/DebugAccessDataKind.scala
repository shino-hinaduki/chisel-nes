package support.types

import chisel3._
import chisel3.experimental.ChiselEnum

/**
  * DAPのアクセス対象の切り替え
  */
object DebugAccessDataKind extends ChiselEnum {
  val invalid      = Value(0x0.U)
  val readTest     = Value(0x1.U)
  val info         = Value(0x2.U)
  val screen       = Value(0x3.U)
  val cartridge    = Value(0x4.U)
  val cpuBusMaster = Value(0x5.U)
  val ppuBusMaster = Value(0x6.U)
}
