package board.gpio

import chisel3._
import chisel3.experimental.Analog
import chisel3.util.Cat
import chisel3.util.HasBlackBoxInline

import board.ip.GpioBidirectional

/**
  * eda/kicad_6.0.2/nes_peripheral のpin assignに変換する
  */
class GpioMapping extends BlackBox with HasBlackBoxInline {
  // GPIOごとの有効ピン数
  val gpioWidth: Int = 36

  val io = IO(new Bundle {
    // DE0-CV GPIO0
    val GPIO_0 = Analog(gpioWidth.W)
    // DE0-CV GPIO1
    val GPIO_1 = Analog(gpioWidth.W)

    // CPU (CartridgeIO.CpuIO と同じ定義だが、BlackBoxなので名称一致優先)
    val a      = Input(UInt(15.W))
    val d_o    = Input(UInt(8.W))
    val d_i    = Output(UInt(8.W))
    val d_oe   = Input(Bool())
    val rw     = Input(Bool())
    val romsel = Input(Bool())
    val o2     = Input(Clock())
    val irq    = Output(Bool())

    // PPU (CartridgeIO.PpuIO と同じ定義だが、BlackBoxなので名称一致優先)
    val pa      = Input(UInt(14.W))
    val pd_o    = Input(UInt(8.W))
    val pd_i    = Output(UInt(8.W))
    val pd_oe   = Input(Bool())
    val vrama10 = Input(Bool())
    val vramcs  = Output(Bool())
    val rd      = Input(Bool())
    val we      = Input(Bool())

    // Level-Shift Enable
    val cart_oe_in = Input(Bool())

    // JOY1
    val joy1_ps  = Input(Bool())
    val joy1_rsv = Output(Bool())
    val joy1_do  = Output(Bool())
    val joy1_clk = Input(Bool())

    // JOY2
    val joy2_ps    = Input(Bool())
    val joy2_micin = Output(Bool())
    val joy2_do    = Output(Bool())
    val joy2_clk   = Input(Bool())

    // I2S
    val dac_bclk    = Input(Bool())
    val dac_lrclk   = Input(Bool())
    val dac_sd_mode = Input(Bool())
    val dac_din     = Input(Bool())

    // Debug
    val led  = Input(Bool())
    val io0  = Input(Bool())
    val io1  = Input(Bool())
    val rsv0 = Input(Bool())
    val rsv1 = Input(Bool())
  })

  setInline(
    "GpioMapping.v",
    """module GpioMapping(
      |  inout  [35: 0] GPIO_0,
      |  inout  [35: 0] GPIO_1,
      | 
      |  input  [14: 0] a,
      |  input  [ 7: 0] d_o,
      |  output [ 7: 0] d_i,
      |  input          d_oe,
      |  input          rw,
      |  input          romsel,
      |  input          o2,
      |  output         irq,
      | 
      |  input  [13: 0] pa,
      |  input  [14: 0] pd_o,
      |  output [14: 0] pd_i,
      |  input          pd_oe,
      |  input          vrama10,
      |  output         vramcs,
      |  input          rd,
      |  input          we,
      | 
      |  input          cart_oe_in,
      | 
      |  input          joy1_ps,
      |  output         joy1_rsv,
      |  output         joy1_do,
      |  input          joy1_clk,
      |
      |  input          joy2_ps,
      |  output         joy2_micin,
      |  output         joy2_do,
      |  input          joy2_clk,
      |
      |  input          dac_bclk,
      |  input          dac_lrclk,
      |  input          dac_sd_mode,
      |  input          dac_din,
      |
      |  input          led,
      |  input          io0,
      |  input          io1,
      |  input          rsv0,
      |  input          rsv1
      |);
      |
      |assign GPIO_1[ 3] = a[14];
      |assign GPIO_1[ 1] = a[13];
      |assign GPIO_0[ 3] = a[12];
      |assign GPIO_0[ 0] = a[11];
      |assign GPIO_0[ 2] = a[10];
      |assign GPIO_1[ 0] = a[ 9];
      |assign GPIO_1[ 2] = a[ 8];
      |assign GPIO_0[ 4] = a[ 7];
      |assign GPIO_0[ 6] = a[ 6];
      |assign GPIO_1[ 4] = a[ 5];
      |assign GPIO_1[ 6] = a[ 4];
      |assign GPIO_0[ 8] = a[ 3];
      |assign GPIO_0[10] = a[ 2];
      |assign GPIO_1[ 8] = a[ 1];
      |assign GPIO_1[10] = a[ 0];
      |
      |assign GPIO_0[ 5] = d_oe ? d_o[7] : 1'bz;
      |assign GPIO_0[ 7] = d_oe ? d_o[6] : 1'bz;
      |assign GPIO_1[ 5] = d_oe ? d_o[5] : 1'bz;
      |assign GPIO_1[ 7] = d_oe ? d_o[4] : 1'bz;
      |assign GPIO_0[ 9] = d_oe ? d_o[3] : 1'bz;
      |assign GPIO_0[11] = d_oe ? d_o[2] : 1'bz;
      |assign GPIO_1[ 9] = d_oe ? d_o[1] : 1'bz;
      |assign GPIO_1[11] = d_oe ? d_o[0] : 1'bz;
      |
      |assign d_i[7]     = GPIO_0[ 5];
      |assign d_i[6]     = GPIO_0[ 7];
      |assign d_i[5]     = GPIO_1[ 5];
      |assign d_i[4]     = GPIO_1[ 7];
      |assign d_i[3]     = GPIO_0[ 9];
      |assign d_i[2]     = GPIO_0[11];
      |assign d_i[1]     = GPIO_1[ 9];
      |assign d_i[0]     = GPIO_1[11];
      |
      |assign GPIO_0[12] = rw;
      |assign GPIO_0[13] = romsel;
      |assign GPIO_0[ 1] = o2;
      |assign irq        = GPIO_0[14];
      |
      |assign GPIO_1[21] = pa[13];
      |assign GPIO_0[23] = pa[12];
      |assign GPIO_0[21] = pa[11];
      |assign GPIO_1[19] = pa[10];
      |assign GPIO_1[17] = pa[ 9];
      |assign GPIO_0[19] = pa[ 8];
      |assign GPIO_0[17] = pa[ 7];
      |assign GPIO_0[16] = pa[ 6];
      |assign GPIO_0[18] = pa[ 5];
      |assign GPIO_1[16] = pa[ 4];
      |assign GPIO_1[18] = pa[ 3];
      |assign GPIO_0[20] = pa[ 2];
      |assign GPIO_0[22] = pa[ 1];
      |assign GPIO_1[20] = pa[ 0];
      |
      |assign GPIO_1[23] = pd_oe ? pd_o[7] : 1'bz;
      |assign GPIO_0[25] = pd_oe ? pd_o[6] : 1'bz;
      |assign GPIO_0[27] = pd_oe ? pd_o[5] : 1'bz;
      |assign GPIO_1[25] = pd_oe ? pd_o[4] : 1'bz;
      |assign GPIO_1[24] = pd_oe ? pd_o[3] : 1'bz;
      |assign GPIO_0[26] = pd_oe ? pd_o[2] : 1'bz;
      |assign GPIO_0[24] = pd_oe ? pd_o[1] : 1'bz;
      |assign GPIO_1[22] = pd_oe ? pd_o[0] : 1'bz;
      |
      |assign pd_i[7]    = GPIO_1[23];
      |assign pd_i[6]    = GPIO_0[25];
      |assign pd_i[5]    = GPIO_0[27];
      |assign pd_i[4]    = GPIO_1[25];
      |assign pd_i[3]    = GPIO_1[24];
      |assign pd_i[2]    = GPIO_0[26];
      |assign pd_i[1]    = GPIO_0[24];
      |assign pd_i[0]    = GPIO_1[22];
      |
      |assign GPIO_1[15] = ~pa[13]; // ~PA13
      |assign GPIO_1[14] = vrama10;
      |assign vramcs     = GPIO_1[13];
      |assign GPIO_1[12] = rd;
      |assign GPIO_0[15] = we;
      |
      |assign GPIO_1[35] = cart_oe_in;
      |
      |assign GPIO_0[28] = joy1_ps;
      |assign joy1_rsv   = GPIO_0[30];
      |assign joy1_do    = GPIO_0[29];
      |assign GPIO_0[31] = joy1_clk;
      |
      |assign GPIO_1[34] = joy2_ps;
      |assign joy2_micin = GPIO_1[30];
      |assign joy2_do    = GPIO_1[32];
      |assign GPIO_1[33] = joy2_clk;
      |
      |assign GPIO_0[32] = dac_bclk;
      |assign GPIO_0[33] = dac_lrclk;
      |assign GPIO_0[34] = dac_sd_mode;
      |assign GPIO_0[35] = dac_din;
      |
      |assign GPIO_1[29] = led;
      |assign GPIO_1[31] = io0;
      |assign GPIO_1[28] = io1;
      |assign GPIO_1[26] = rsv0;
      |assign GPIO_1[27] = rsv1;
      |
      |endmodule
    """.stripMargin
  )
}
