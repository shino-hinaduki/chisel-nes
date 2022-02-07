// (C) 2001-2021 Intel Corporation. All rights reserved.
// Your use of Intel Corporation's design tools, logic functions and other 
// software and tools, and its AMPP partner logic functions, and any output 
// files from any of the foregoing (including device programming or simulation 
// files), and any associated documentation or information are expressly subject 
// to the terms and conditions of the Intel Program License Subscription 
// Agreement, Intel FPGA IP License Agreement, or other applicable 
// license agreement, including, without limitation, that your use is for the 
// sole purpose of programming logic devices manufactured by Intel and sold by 
// Intel or its authorized distributors.  Please refer to the applicable 
// agreement for further details.


// THIS FILE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THIS FILE OR THE USE OR OTHER DEALINGS
// IN THIS FILE.

/******************************************************************************
 *                                                                            *
 * This module connects the Avalon Switch Frabic to an External Bus           *
 *                                                                            *
 ******************************************************************************/

module debug_subsystem_cpu_debug_bus_bridge_0 (
	// Inputs
	clk,
	reset,

	avalon_address,
	avalon_byteenable,
	avalon_chipselect,
	avalon_read,
	avalon_write,
	avalon_writedata,

	acknowledge,
	irq,
	read_data,

	// Bidirectionals

	// Outputs
	avalon_irq,
	avalon_readdata,
	avalon_waitrequest,

	address,
	bus_enable,
	byte_enable,
	rw,
	write_data
);


/*****************************************************************************
 *                           Parameter Declarations                          *
 *****************************************************************************/

parameter	AW		= 14;	// Address bits
parameter	EAB	= 1;	// Extra address bits needed for the output address
parameter	OW		= 15;	// Address bits of the output port

parameter	DW		= 15;	// Data bits
parameter	BW		= 1;	// Byte enable bits

/*****************************************************************************
 *                             Port Declarations                             *
 *****************************************************************************/
// Inputs
input						clk;
input						reset;

input			[AW: 0]	avalon_address;
input			[BW: 0]	avalon_byteenable;
input						avalon_chipselect;
input						avalon_read;
input						avalon_write;
input			[DW: 0]	avalon_writedata;

input						acknowledge;
input						irq;
input			[DW: 0]	read_data;

// Bidirectionals

// Outputs
output					avalon_irq;
output reg	[DW: 0]	avalon_readdata;
output					avalon_waitrequest;

output reg	[OW: 0]	address;
output reg				bus_enable;
output reg	[BW: 0]	byte_enable;
output reg				rw;
output reg	[DW: 0]	write_data;

/*****************************************************************************
 *                           Constant Declarations                           *
 *****************************************************************************/

/*****************************************************************************
 *                 Internal Wires and Registers Declarations                 *
 *****************************************************************************/

// Internal Wires

// Internal Registers
reg			[ 7: 0]		time_out_counter;

// State Machine Registers

/*****************************************************************************
 *                         Finite State Machine(s)                           *
 *****************************************************************************/


/*****************************************************************************
 *                             Sequential Logic                              *
 *****************************************************************************/

always @(posedge clk)
begin
	if (reset == 1'b1)
	begin
		avalon_readdata <= 'h0;
	end
	else if (acknowledge | (&(time_out_counter)))
	begin
		avalon_readdata <= read_data;
	end
end

always @(posedge clk)
begin
	if (reset == 1'b1)
	begin
		address		<=  'h0;
		bus_enable	<= 1'b0;
		byte_enable	<=  'h0;
		rw				<= 1'b1;
		write_data	<=  'h0;
	end
	else
	begin
		address		<= {avalon_address, {EAB{1'b0}}};

		if (avalon_chipselect & (|(avalon_byteenable)))
		begin
			bus_enable	<= avalon_waitrequest;
		end
		else
		begin
			bus_enable	<= 1'b0;
		end

		byte_enable	<= avalon_byteenable;
		rw			<= avalon_read | ~avalon_write;
		write_data	<= avalon_writedata;
	end
end

always @(posedge clk)
begin
	if (reset == 1'b1)
	begin
		time_out_counter <= 8'h00;
	end
	else if (avalon_waitrequest == 1'b1)
	begin
		time_out_counter <= time_out_counter + 8'h01;
	end
	else
	begin
		time_out_counter <= 8'h00;
	end
end

/*****************************************************************************
 *                            Combinational Logic                            *
 *****************************************************************************/

assign avalon_irq = irq;
assign avalon_waitrequest =
		avalon_chipselect & (|(avalon_byteenable)) & 
		~acknowledge & ~(&(time_out_counter));

/*****************************************************************************
 *                              Internal Modules                             *
 *****************************************************************************/

endmodule

