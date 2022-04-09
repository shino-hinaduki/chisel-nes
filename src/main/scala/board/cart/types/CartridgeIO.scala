package board.cart.types

import chisel3._
import common.TriState

object CartridgeIO {

  /**
    * CartridgeのCPU Bus関連のIO定義
    */
  class CpuIO extends Bundle {

    /**
      * A14 - A0
      */
    val address = Input(UInt(15.W))

    /**
      * D7 - D0 (エミュ本体からの出力)
      */
    val dataOut = Input(TriState(UInt(8.W)))

    /**
      * D7 - D0 (エミュ本体へ入力)
      */
    val dataIn = Output(UInt(8.W))

    /**
      * R/~W
      */
    val isRead = Input(Bool())

    /**
      * /ROMSEL
      */
    val isNotRomSel = Input(Bool())

    /**
      * φ2
      */
    val o2 = Input(Clock())

    /**
      * /IRQ
      */
    val isNotIrq = Output(Bool())
  }

  /**
    * CartridgeのPPU Bus関連のIO定義
    */
  class PpuIO extends Bundle {

    /**
      * PA13 - PA0
      */
    val address = Input(UInt(14.W))

    /**
      * PD7 - PD0 (エミュ本体からの出力)
      */
    val dataOut = Input(TriState(UInt(8.W)))

    /**
      * PD7 - PD0 (エミュ本体へ入力)
      */
    val dataIn = Output(UInt(8.W))

    /**
      * VRAMA10
      */
    val vrama10 = Input(Bool())

    /**
      * /VRAMCS
      */
    val isNotVramCs = Output(Bool())

    /**
      * /RD
      */
    val isNotRead = Input(Bool())

    /**
      * /WE
      */
    val isNotWrite = Input(Bool())
  }
}

/**
  * CartridgeのIO定義
  */
class CartridgeIO extends Bundle {
  val cpu = new CartridgeIO.CpuIO
  val ppu = new CartridgeIO.PpuIO
}
