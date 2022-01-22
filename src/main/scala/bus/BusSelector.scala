package bus

import chisel3._

/**
  * CPU Busのアドレス線とPHI2から /RAMSEL, /DBE, /ROMSEL 信号を生成する 
  * PHI2: 2a03 31pinの信号。1.79MHzの12分周されたDuty=62.5%を入力する想定
  */
class BusSelector extends Module {
  val io = IO(new Bundle {
    val phi2 = Input(Bool())
    val a13  = Input(Bool())
    val a14  = Input(Bool())
    val a15  = Input(Bool())

    val nRomSel        = Output(Bool()) // $8000 ~
    val nDataBusEnable = Output(Bool()) // $2000 ~
    val nRamSel        = Output(Bool()) // $0000 ~
  })
  // LS139 2回路で合成する
  val decoder0 = new Decoder
  decoder0.io.b  := io.a15
  decoder0.io.a  := io.phi2 // 1/12 MCLK, 62.5% duty cycle
  decoder0.io.nG := false.B // always enabled

  val decoder1 = new Decoder
  decoder1.io.b  := io.a14
  decoder1.io.a  := io.a13
  decoder1.io.nG := decoder0.io.y(1) // decoder1でPHI2=L, A15=Lのときにのみ有効

  // 出力
  io.nRomSel        := decoder0.io.y(3) // PHI2=H, A15=H, A14=X, A13=X
  io.nDataBusEnable := decoder1.io.y(1) // PHI2=H, A15=L, A14=H, A13=L
  io.nRamSel        := decoder1.io.y(0) // PHI2=H, A15=L, A14=L, A13=L
}
