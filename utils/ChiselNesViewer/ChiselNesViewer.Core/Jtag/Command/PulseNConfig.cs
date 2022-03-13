using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ChiselNesViewer.Core.Jtag.Command {
    /// <summary>
    /// nCONFIGピンにLOWパルスを与える
    /// </summary>
    public static class PulseNConfig{

        /// <summary>
        /// PULSE_NCONFIGコマンドを発行してFPGAを再コンフィグします
        /// </summary>
        /// <param name="jtag"></param>
        /// <returns></returns>

        public static void Do(IJtagCommunicatable jtag) {
            jtag.MoveIdle();
            jtag.MoveIdleToShiftIr();
            jtag.WriteShiftIr(0b0000_0001);
            jtag.MoveShiftIrToShiftDr();
            jtag.DeviceClose();
        }
    }
}
