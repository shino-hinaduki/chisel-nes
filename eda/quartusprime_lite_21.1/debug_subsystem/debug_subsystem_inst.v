	debug_subsystem u0 (
		.clk_clk                                                 (<connected-to-clk_clk>),                                                 //                                         clk.clk
		.cpu_debug_bus_bridge_0_interrupt_irq                    (<connected-to-cpu_debug_bus_bridge_0_interrupt_irq>),                    //            cpu_debug_bus_bridge_0_interrupt.irq
		.ppu_debug_bus_bridge_0_interrupt_irq                    (<connected-to-ppu_debug_bus_bridge_0_interrupt_irq>),                    //            ppu_debug_bus_bridge_0_interrupt.irq
		.reset_reset_n                                           (<connected-to-reset_reset_n>),                                           //                                       reset.reset_n
		.to_external_bus_bridge_0_external_interface_acknowledge (<connected-to-to_external_bus_bridge_0_external_interface_acknowledge>), // to_external_bus_bridge_0_external_interface.acknowledge
		.to_external_bus_bridge_0_external_interface_irq         (<connected-to-to_external_bus_bridge_0_external_interface_irq>),         //                                            .irq
		.to_external_bus_bridge_0_external_interface_address     (<connected-to-to_external_bus_bridge_0_external_interface_address>),     //                                            .address
		.to_external_bus_bridge_0_external_interface_bus_enable  (<connected-to-to_external_bus_bridge_0_external_interface_bus_enable>),  //                                            .bus_enable
		.to_external_bus_bridge_0_external_interface_byte_enable (<connected-to-to_external_bus_bridge_0_external_interface_byte_enable>), //                                            .byte_enable
		.to_external_bus_bridge_0_external_interface_rw          (<connected-to-to_external_bus_bridge_0_external_interface_rw>),          //                                            .rw
		.to_external_bus_bridge_0_external_interface_write_data  (<connected-to-to_external_bus_bridge_0_external_interface_write_data>),  //                                            .write_data
		.to_external_bus_bridge_0_external_interface_read_data   (<connected-to-to_external_bus_bridge_0_external_interface_read_data>),   //                                            .read_data
		.to_external_bus_bridge_1_external_interface_acknowledge (<connected-to-to_external_bus_bridge_1_external_interface_acknowledge>), // to_external_bus_bridge_1_external_interface.acknowledge
		.to_external_bus_bridge_1_external_interface_irq         (<connected-to-to_external_bus_bridge_1_external_interface_irq>),         //                                            .irq
		.to_external_bus_bridge_1_external_interface_address     (<connected-to-to_external_bus_bridge_1_external_interface_address>),     //                                            .address
		.to_external_bus_bridge_1_external_interface_bus_enable  (<connected-to-to_external_bus_bridge_1_external_interface_bus_enable>),  //                                            .bus_enable
		.to_external_bus_bridge_1_external_interface_byte_enable (<connected-to-to_external_bus_bridge_1_external_interface_byte_enable>), //                                            .byte_enable
		.to_external_bus_bridge_1_external_interface_rw          (<connected-to-to_external_bus_bridge_1_external_interface_rw>),          //                                            .rw
		.to_external_bus_bridge_1_external_interface_write_data  (<connected-to-to_external_bus_bridge_1_external_interface_write_data>),  //                                            .write_data
		.to_external_bus_bridge_1_external_interface_read_data   (<connected-to-to_external_bus_bridge_1_external_interface_read_data>),   //                                            .read_data
		.clk_1_clk_clk                                           (<connected-to-clk_1_clk_clk>),                                           //                                   clk_1_clk.clk
		.clk_1_clk_reset_reset_n                                 (<connected-to-clk_1_clk_reset_reset_n>),                                 //                             clk_1_clk_reset.reset_n
		.clk_ppu_0_clk_clk                                       (<connected-to-clk_ppu_0_clk_clk>),                                       //                               clk_ppu_0_clk.clk
		.clk_ppu_0_clk_reset_reset_n                             (<connected-to-clk_ppu_0_clk_reset_reset_n>)                              //                         clk_ppu_0_clk_reset.reset_n
	);

