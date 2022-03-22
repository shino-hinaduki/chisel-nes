set_global_assignment -entity "pll_vga" -library "lib_pll_vga" -name IP_TOOL_NAME "altera_pll"
set_global_assignment -entity "pll_vga" -library "lib_pll_vga" -name IP_TOOL_VERSION "21.1"
set_global_assignment -entity "pll_vga" -library "lib_pll_vga" -name IP_TOOL_ENV "mwpim"
set_global_assignment -library "lib_pll_vga" -name SPD_FILE [file join $::quartus(sip_path) "pll_vga.spd"]

set_global_assignment -library "lib_pll_vga" -name MISC_FILE [file join $::quartus(sip_path) "pll_vga_sim/pll_vga.vo"]
