package board.ram.types

import board.ram.types.RamIO

/**
  * IPで生成する2-Port RAM関連の機能定義
  */
trait DualPortRam {

  /**
      * PortAと接続します
      *
      * @param ram DPRAMと接続可能なI/F
      */
  def connectToA(ram: RamIO): Unit

  /**
      * PortBと接続します
      *
      * @param ram DPRAMと接続可能なI/F
      */
  def connectToB(ram: RamIO): Unit
}
