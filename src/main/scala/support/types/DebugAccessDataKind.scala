package support.types

import chisel3._
import chisel3.experimental.ChiselEnum

/**
  * DAPのアクセス対象の切り替え
  */
object DebugAccessDataKind extends ChiselEnum {
  val invalid      = Value(0x00.U)
  val info         = Value(0x01.U)
  val debug        = Value(0x02.U)
  val cpuBusMaster = Value(0x03.U)
  val ppuBusMaster = Value(0x04.U)
  val screen       = Value(0x10.U)
  val audio        = Value(0x20.U)
  val cartInfo     = Value(0x30.U)
  val cartPrg      = Value(0x31.U)
  val cartBattt    = Value(0x32.U)
  val cartChr      = Value(0x33.U)
  val readTest     = Value(0x7e.U)
  val invalid2     = Value(0x7f.U)
}
