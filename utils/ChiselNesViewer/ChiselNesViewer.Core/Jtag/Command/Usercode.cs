using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ChiselNesViewer.Core.Jtag.Command {
    /// <summary>
    /// デバイス固有コードの定義と取得関数
    /// </summary>
    public class Usercode {
        public uint Raw { get; internal set; }

        /// <summary>
        /// IDCODEコマンドを発行して、データを取得します
        /// </summary>
        /// <param name="jtag"></param>
        /// <returns></returns>

        public static Usercode Read(IJtagCommunicatable jtag) {
            jtag.ClearWriteBuffer();
            jtag.ClearReadBuffer();

            jtag.MoveIdle();
            jtag.MoveIdleToShiftIr();
            jtag.WriteShiftIr(0b0000_0111);
            jtag.MoveShiftIrToShiftDr();
            var readDatas = jtag.ReadShiftDr(4);
            jtag.DeviceClose();

            var raw = BitConverter.ToUInt32(readDatas.ToArray());

            var dst = new Usercode() {
                Raw = raw,
            };
            return dst;
        }
    }
}
