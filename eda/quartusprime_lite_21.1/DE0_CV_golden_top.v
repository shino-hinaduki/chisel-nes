// ============================================================================
// Copyright (c) 2014 by Terasic Technologies Inc.
// ============================================================================
//
// Permission:
//
//   Terasic grants permission to use and modify this code for use
//   in synthesis for all Terasic Development Boards and Altera Development 
//   Kits made by Terasic.  Other use of this code, including the selling 
//   ,duplication, or modification of any portion is strictly prohibited.
//
// Disclaimer:
//
//   This VHDL/Verilog or C/C++ source code is intended as a design reference
//   which illustrates how these types of functions can be implemented.
//   It is the user's responsibility to verify their design for
//   consistency and functionality through the use of formal
//   verification methods.  Terasic provides no warranty regarding the use 
//   or functionality of this code.
//
// ============================================================================
//           
//  Terasic Technologies Inc
//  9F., No.176, Sec.2, Gongdao 5th Rd, East Dist, Hsinchu City, 30070. Taiwan
//  
//  
//                     web: http://www.terasic.com/  
//                     email: support@terasic.com
//
// ============================================================================
//   Ver  :| Author            :| Mod. Date :| Changes Made:
//   V1.0 :| Yue Yang          :| 08/25/2014:| Initial Revision
// ============================================================================
`define Enable_CLOCK2
`define Enable_CLOCK3
`define Enable_CLOCK4
`define Enable_CLOCK
`define Enable_DRAM
`define Enable_GPIO
`define Enable_HEX0
`define Enable_HEX1
`define Enable_HEX2
`define Enable_HEX3
`define Enable_HEX4
`define Enable_HEX5
`define Enable_KEY
`define Enable_LEDR
`define Enable_PS2
`define Enable_RESET
`define Enable_SD
`define Enable_SW
`define Enable_VGA

module DE0_CV_golden_top (

`ifdef Enable_CLOCK2
      ///////// CLOCK2 "3.3-V LVTTL" /////////
      input              CLOCK2_50,
`endif	  

`ifdef Enable_CLOCK3
      ///////// CLOCK3 "3.3-V LVTTL" /////////
      input              CLOCK3_50,
`endif

`ifdef Enable_CLOCK4
      ///////// CLOCK4  "3.3-V LVTTL"  /////////
      inout              CLOCK4_50,
`endif	  
`ifdef Enable_CLOCK
      ///////// CLOCK  "3.3-V LVTTL" /////////
      input              CLOCK_50,
`endif
`ifdef Enable_DRAM
      ///////// DRAM  "3.3-V LVTTL" /////////
      output      [12:0] DRAM_ADDR,
      output      [1:0]  DRAM_BA,
      output             DRAM_CAS_N,
      output             DRAM_CKE,
      output             DRAM_CLK,
      output             DRAM_CS_N,
      inout       [15:0] DRAM_DQ,
      output             DRAM_LDQM,
      output             DRAM_RAS_N,
      output             DRAM_UDQM,
      output             DRAM_WE_N,
`endif
`ifdef Enable_GPIO
      ///////// GPIO "3.3-V LVTTL" /////////
      inout       [35:0] GPIO_0,
      inout       [35:0] GPIO_1,
`endif
`ifdef Enable_HEX0
      ///////// HEX0  "3.3-V LVTTL" /////////
      output      [6:0]  HEX0,
`endif
`ifdef Enable_HEX1
      ///////// HEX1 "3.3-V LVTTL" /////////
      output      [6:0]  HEX1,
`endif
`ifdef Enable_HEX2
      ///////// HEX2 "3.3-V LVTTL" /////////
      output      [6:0]  HEX2,
`endif
`ifdef Enable_HEX3
      ///////// HEX3 "3.3-V LVTTL" /////////
      output      [6:0]  HEX3,
`endif
`ifdef Enable_HEX4
      ///////// HEX4 "3.3-V LVTTL" /////////
      output      [6:0]  HEX4,
`endif
`ifdef Enable_HEX5
      ///////// HEX5 "3.3-V LVTTL" /////////
      output      [6:0]  HEX5,
`endif
`ifdef Enable_KEY
      ///////// KEY  "3.3-V LVTTL" /////////
      input       [3:0]  KEY,
`endif
`ifdef Enable_LEDR
      ///////// LEDR /////////
      output      [9:0]  LEDR,
`endif
`ifdef Enable_PS2
      ///////// PS2 "3.3-V LVTTL" /////////
      inout              PS2_CLK,
      inout              PS2_CLK2,
      inout              PS2_DAT,
      inout              PS2_DAT2,
`endif
`ifdef Enable_RESET
      ///////// RESET "3.3-V LVTTL" /////////
      input              RESET_N,
`endif
`ifdef Enable_SD
      ///////// SD "3.3-V LVTTL" /////////
      output             SD_CLK,
      inout              SD_CMD,
      inout       [3:0]  SD_DATA,
`endif
`ifdef Enable_SW
      ///////// SW "3.3-V LVTTL"/////////
      input       [9:0]  SW,
`endif
`ifdef Enable_VGA
      ///////// VGA  "3.3-V LVTTL" /////////
      output      [3:0]  VGA_B,
      output      [3:0]  VGA_G,
      output             VGA_HS,
      output      [3:0]  VGA_R,
      output             VGA_VS
`endif	 
);



/*****************************************************************/
// virtualjtag test
//wire [23:0] vjtag_ir_in; // vjtag0.ir_in
//wire vjtag_state_uir;    // Hのときにvjtag_ir_inを取り込む
//virtual_jtag vjtag0 (
//	  .tdi                (<connected-to-tdi>),                // jtag.tdi
//	  .tdo                (<connected-to-tdo>),                //     .tdo
//	  .ir_in              (vjtag_ir_in),                       //     .ir_in
//	  .ir_out             (<connected-to-ir_out>),             //     .ir_out
//	  .virtual_state_cdr  (<connected-to-virtual_state_cdr>),  //     .virtual_state_cdr
//	  .virtual_state_sdr  (<connected-to-virtual_state_sdr>),  //     .virtual_state_sdr
//   .virtual_state_e1dr (<connected-to-virtual_state_e1dr>), //     .virtual_state_e1dr
//	  .virtual_state_pdr  (<connected-to-virtual_state_pdr>),  //     .virtual_state_pdr
//	  .virtual_state_e2dr (<connected-to-virtual_state_e2dr>), //     .virtual_state_e2dr
//	  .virtual_state_udr  (<connected-to-virtual_state_udr>),  //     .virtual_state_udr
//	  .virtual_state_cir  (<connected-to-virtual_state_cir>),  //     .virtual_state_cir
//	  .virtual_state_uir  (vjtag_state_uir),                   //     .virtual_state_uir
//	  .tck                (<connected-to-tck>)                 //  tck.clk
// );

// TODO: inout兼用ピンが未定義のwireのまま
ChiselNes chiselNes0(
//  .clock(clock),
//  .reset(reset),
  .io_extPort_CLOCK_50(CLOCK_50),
  .io_extPort_CLOCK2_50(CLOCK2_50),
  .io_extPort_CLOCK3_50(CLOCK3_50),
  .io_extPort_CLOCK4_50(CLOCK4_50),
  .io_extPort_DRAM_ADDR(DRAM_ADDR),
  .io_extPort_DRAM_BA(DRAM_BA),
  .io_extPort_DRAM_CAS_N(DRAM_CAS_N),
  .io_extPort_DRAM_CKE(DRAM_CKE),
  .io_extPort_DRAM_CLK(DRAM_CLK),
  .io_extPort_DRAM_CS_N(DRAM_CS_N),
  .io_extPort_DRAM_DQ_in(DRAM_DQ_in),
  .io_extPort_DRAM_DQ_out_data(DRAM_DQ_out_data),
  .io_extPort_DRAM_DQ_out_oe(DRAM_DQ_out_oe),
  .io_extPort_DRAM_LDQM(DRAM_LDQM),
  .io_extPort_DRAM_RAS_N(DRAM_RAS_N),
  .io_extPort_DRAM_UDQM(DRAM_UDQM),
  .io_extPort_DRAM_WE_N(DRAM_WE_N),
  .io_extPort_GPIO_0_in(GPIO_0_in),
  .io_extPort_GPIO_0_out_data(GPIO_0_out_data),
  .io_extPort_GPIO_0_out_oe(GPIO_0_out_oe),
  .io_extPort_GPIO_1_in(GPIO_1_in),
  .io_extPort_GPIO_1_out_data(GPIO_1_out_data),
  .io_extPort_GPIO_1_out_oe(GPIO_1_out_oe),
  .io_extPort_HEX0(HEX0),
  .io_extPort_HEX1(HEX1),
  .io_extPort_HEX2(HEX2),
  .io_extPort_HEX3(HEX3),
  .io_extPort_HEX4(HEX4),
  .io_extPort_HEX5(HEX5),
  .io_extPort_KEY(KEY),
  .io_extPort_LEDR(LEDR),
  .io_extPort_PS2_CLK_in(PS2_CLK_in),
  .io_extPort_PS2_CLK_out_data(PS2_CLK_out_data),
  .io_extPort_PS2_CLK_out_oe(PS2_CLK_out_oe),
  .io_extPort_PS2_CLK2_in(PS2_CLK2_in),
  .io_extPort_PS2_CLK2_out_data(PS2_CLK2_out_data),
  .io_extPort_PS2_CLK2_out_oe(PS2_CLK2_out_oe),
  .io_extPort_PS2_DAT_in(PS2_DAT_in),
  .io_extPort_PS2_DAT_out_data(PS2_DAT_out_data),
  .io_extPort_PS2_DAT_out_oe(PS2_DAT_out_oe),
  .io_extPort_PS2_DAT2_in(PS2_DAT2_in),
  .io_extPort_PS2_DAT2_out_data(PS2_DAT2_out_data),
  .io_extPort_PS2_DAT2_out_oe(PS2_DAT2_out_oe),
  .io_extPort_RESET_N(RESET_N),
  .io_extPort_SD_CLK(SD_CLK),
  .io_extPort_SD_CMD_in(SD_CMD_in),
  .io_extPort_SD_CMD_out_data(SD_CMD_out_data),
  .io_extPort_SD_CMD_out_oe(SD_CMD_out_oe),
  .io_extPort_SD_DATA_in(SD_DATA_in),
  .io_extPort_SD_DATA_out_data(SD_DATA_out_data),
  .io_extPort_SD_DATA_out_oe(SD_DATA_out_oe),
  .io_extPort_SW(SW),
  .io_extPort_VGA_B(VGA_B),
  .io_extPort_VGA_G(VGA_G),
  .io_extPort_VGA_HS(VGA_HS),
  .io_extPort_VGA_R(VGA_R),
  .io_extPort_VGA_VS(VGA_VS)
);

endmodule 

