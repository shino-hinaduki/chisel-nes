using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ChiselNesViewer.Core.Controls
{
    /// <summary>
    /// JTAGのTAPコントローラの状態遷移をサポートします
    /// </summary>
    internal interface IJtagCommunicatable
    {
        bool MoveIdle();
        bool MoveIdleToShiftIr();
        bool WriteShiftDr(byte data);
        bool WriteShiftIr(byte data);
        bool MoveShiftIrToShiftDr();
        bool MoveShiftDrToShiftIr();
        bool WriteShiftDr(byte[] datas);
        byte[] ReadShiftDr(UInt32 dataSize);
    }
}
