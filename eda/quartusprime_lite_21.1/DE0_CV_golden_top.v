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
// test
reg [63:0] counter;
always @(posedge CLOCK_50) begin
	if (!RESET_N) begin
		counter <= 64'd0;
	end else begin
		counter <= counter + 64'd1;
	end
end

// 流れる感じのなにか
assign LEDR = (10'd1 << counter[23:20]);

// 7segの出力を作る。回路上は負論理
function [6:0] decode_for_7seg;
	input [3:0] num;
	case (num)                  // gfedcba
		4'h0: decode_for_7seg = ~7'b0111111;
		4'h1: decode_for_7seg = ~7'b0000110;
		4'h2: decode_for_7seg = ~7'b1011011;
		4'h3: decode_for_7seg = ~7'b1001111;
		4'h4: decode_for_7seg = ~7'b1100110;
		4'h5: decode_for_7seg = ~7'b1101101;
		4'h6: decode_for_7seg = ~7'b1111101;
		4'h7: decode_for_7seg = ~7'b0100111;
		4'h8: decode_for_7seg = ~7'b1111111;
		4'h9: decode_for_7seg = ~7'b1101111;
		4'ha: decode_for_7seg = ~7'b1110111;
		4'hb: decode_for_7seg = ~7'b1111100;
		4'hc: decode_for_7seg = ~7'b0111001;
		4'hd: decode_for_7seg = ~7'b1011110;
		4'he: decode_for_7seg = ~7'b1111001;
		4'hf: decode_for_7seg = ~7'b1110001;
	endcase
endfunction

assign HEX0 = decode_for_7seg(counter[19:16]);
assign HEX1 = decode_for_7seg(counter[23:20]);
assign HEX2 = decode_for_7seg(counter[27:24]);
assign HEX3 = decode_for_7seg(counter[31:28]);
assign HEX4 = decode_for_7seg(counter[35:32]);
assign HEX5 = decode_for_7seg(counter[39:36]);


endmodule 

