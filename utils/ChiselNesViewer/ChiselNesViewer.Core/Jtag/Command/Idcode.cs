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
    public class Idcode {
        public byte Version { get; internal set; }
        public ushort PartNumber { get; internal set; }
        public ushort MakerId { get; internal set; }
        public uint Raw { get; internal set; }

        /// <summary>
        /// IDCODEコマンドを発行して、データを取得します
        /// </summary>
        /// <param name="jtag"></param>
        /// <returns></returns>

        public static Idcode Read(IJtagCommunicatable jtag) {
            jtag.ClearWriteBuffer();
            jtag.ClearReadBuffer();

            jtag.MoveIdle();
            jtag.MoveIdleToShiftIr();
            jtag.WriteShiftIr(0b0000_0110);
            jtag.MoveShiftIrToShiftDr();
            var readDatas = jtag.ReadShiftDr(4, removeSurplus: true);
            jtag.DeviceClose();

            // データの復元
            var raw = BitConverter.ToUInt32(readDatas.ToArray());

            var dst = new Idcode() {
                Version = (byte)((raw >> 28) & 0xf),
                PartNumber = (ushort)((raw >> 12) & 0xffff),
                MakerId = (ushort)((raw >> 1) & 0x7ff),
                Raw = raw,
            };
            return dst;
        }
    }
}
