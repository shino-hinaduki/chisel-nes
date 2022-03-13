using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ChiselNesViewer.Core.Jtag {
    /// <summary>
    /// JTAGのTAPコントローラの状態遷移をサポートします
    /// </summary>
    public interface IJtagCommunicatable {
        /// <summary>
        /// FT232都合だが、ReadU8の成約になっているのでこちらに定義しておく
        /// </summary>
        const uint ReadUnitSize = 63;

        #region デバイスの操作

        /// <summary>
        /// WriteBufferの内容をクリアします
        /// </summary>
        /// <returns></returns>
        public bool ClearWriteBuffer();
        /// <summary>
        /// ReadBufferの内容をクリアします
        /// </summary>
        /// <returns></returns>
        public bool ClearReadBuffer();

        /// <summary>
        /// Readを実行します
        /// </summary>
        /// <param name="readReqBytes">読み出すデータサイズ。FT232都合で一度に63byteまで</param>
        /// <returns>読みだしたデータ</returns>
        public byte[] ReadU8(int readReqBytes);

        /// <summary>
        /// Writeを実行します
        /// </summary>
        /// <param name="data">書き込むデータ。この配列の中身はすべてWriteする</param>
        /// <returns></returns>
        public bool WriteU8(params byte[] data);

        /// <summary>
        /// Writeを実行します
        /// </summary>
        /// <param name="data">書き込むデータ。この配列の中身はすべてWriteする</param>
        /// <returns></returns>
        public bool WriteU16(params ushort[] data);
        #endregion

        #region JTAG TAP操作
        /// <summary>
        /// TAPコントローラの操作。TMSを保って5回クロックを入れReset→Idle遷移する
        /// </summary>
        /// <returns></returns>
        public bool MoveIdle();
        /// <summary>
        /// TAPコントローラの操作。Shift-IR遷移する
        /// </summary>
        /// <returns></returns>
        public bool MoveIdleToShiftIr();
        /// <summary>
        /// TAPコントローラの操作。DRにデータを流す
        /// </summary>
        /// <returns></returns>
        public bool WriteShiftDr(byte data);

        /// <summary>
        /// TAPコントローラの操作。DRのデータを受け取る
        /// </summary>
        /// <returns>ReadUnitSize byte分のReadData</returns>
        public byte[] ReadShiftDr();

        /// <summary>
        /// TAPコントローラの操作。IRにデータを流す
        /// </summary>
        /// <returns></returns>
        public bool WriteShiftIr(byte data);
        /// <summary>
        /// TAPコントローラの操作。Shift-IRからShift-DRに遷移する
        /// </summary>
        /// <returns></returns>
        public bool MoveShiftIrToShiftDr();
        /// <summary>
        /// TAPコントローラの操作。Shift-DRからShift-IRに遷移する
        /// </summary>
        /// <returns></returns>
        public bool MoveShiftDrToShiftIr();
        /// <summary>
        /// TAPコントローラの操作
        /// </summary>
        /// <returns></returns>
        public bool DeviceClose();

        /// <summary>
        /// DataRegにデータを流し込みます
        /// </summary>
        /// <param name="src">流すデータ。制限なし</param>
        /// <returns></returns>
        public bool WriteShiftDr(IEnumerable<byte> src);

        /// <summary>
        /// DataRegの内容を読み取ります
        /// </summary>
        /// <param name="dataSize">読み取りデータ数</param>
        /// <returns></returns>
        public byte[] ReadShiftDr(uint dataSize);
        #endregion
    }
}
