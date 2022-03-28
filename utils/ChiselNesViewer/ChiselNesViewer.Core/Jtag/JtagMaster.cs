using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using FTD2XX_NET;

namespace ChiselNesViewer.Core.Jtag {
    /// <summary>
    /// FT245でJTAGを扱うためのクラスです。以下を参考に実装しています
    /// * NES on FPGA pgate1  - https://pgate1.at-ninja.jp/NES_on_FPGA/dev_other.htm
    /// * kazu_1995's diary   - https://kazu1995.hatenablog.jp/entry/2017/11/18/202718
    /// * relm.info           - http://relm.info/?x=entry:entry130319-165251
    /// </summary>
    public class JtagMaster : IDisposable, IJtagCommunicatable {
        /// <summary>
        /// 内部的に持っておくデバイスハンドラ
        /// </summary>
        protected FTDI? _device = null;
        private bool disposedValue;

        #region FTD2XXのセットアップ関連
        /// <summary>
        /// デバイス一覧をリセットし、デバイス数を取得します
        /// </summary>
        /// <returns></returns>
        public static uint GetNumOfDevices() {
            uint numDevices = 0;
            var device = new FTDI();
            device.Rescan();
            device.GetNumberOfDevices(ref numDevices);
            return numDevices;

        }

        /// <summary>
        /// 接続されているFTXXXの一覧を取得します
        /// </summary>
        /// <returns>デバイス一覧。エラーが発生した場合も空の配列を返す</returns>
        public static FTDI.FT_DEVICE_INFO_NODE[] GetDevices() {
            // デバイスがない場合は何もしない
            var numOfDevices = GetNumOfDevices();
            if (numOfDevices == 0) { return new FTDI.FT_DEVICE_INFO_NODE[] { }; }

            // デバイスが存在する場合は一覧を問い合わせる
            var device = new FTDI();
            var deviceList = new FTDI.FT_DEVICE_INFO_NODE[numOfDevices];
            device.Rescan();
            if (device.GetDeviceList(deviceList) == FTDI.FT_STATUS.FT_OK) {
                return deviceList;

            } else {
                return new FTDI.FT_DEVICE_INFO_NODE[] { };
            }
        }

        /// <summary>
        /// FT2XXをJTAG用に使用します
        /// </summary>
        /// <returns></returns>
        /// <exception cref="NotImplementedException"></exception>
        public bool Open(FTDI.FT_DEVICE_INFO_NODE targetInfo) {
            Debug.Assert(targetInfo != null);

            // Close済想定だが、念の為あるなら閉じておく
            this._device?.Close();

            // SerialNumberを引数に開く
            var device = new FTDI();
            if (device.OpenByDescription(targetInfo.Description) != FTDI.FT_STATUS.FT_OK) {
                return false;
            }
            // ローカルにセット
            this._device = device;
            Debug.Assert(_device?.IsOpen ?? false);

            // 初期状態が不明なので、Test/Logic Resetに入れておく
            MoveTestLogicReset();

            // 成功
            return true;
        }
        /// <summary>
        /// デバイスを切断します
        /// </summary>
        /// <returns></returns>
        public bool Close() {
            Debug.Assert(this._device != null);

            this?.MoveTestLogicReset();
            var result = _device?.Close();
            _device = null;

            return result == FTDI.FT_STATUS.FT_OK;
        }

        protected virtual void Dispose(bool disposing) {
            if (!disposedValue) {
                if (disposing) {
                    this._device?.Close();
                    this._device = null;
                }

                disposedValue = true;
            }
        }

        public void Dispose() {
            Dispose(disposing: true);
            GC.SuppressFinalize(this);
        }

        #endregion

        #region JTAG制御関連

        private const ushort L = 0x2d2c;
        private const ushort H = L | 0x1010;
        private const ushort TMS = L | 0x0202;
        private const ushort TMS_H = TMS | H;
        private const ushort OFF = 0x0d0c;
        private const byte WR = 0x81;
        private const byte RD = 0xc0;

        public bool ClearWriteBuffer() {
            Debug.Assert(_device?.IsOpen ?? false);
            return _device?.Purge(FTDI.FT_PURGE.FT_PURGE_TX) == FTDI.FT_STATUS.FT_OK;
        }
        public bool ClearReadBuffer() {
            Debug.Assert(_device?.IsOpen ?? false);
            return _device?.Purge(FTDI.FT_PURGE.FT_PURGE_RX) == FTDI.FT_STATUS.FT_OK;
        }
        public byte[] ReadU8(int readReqBytes) {
            Debug.Assert(_device?.IsOpen ?? false);
            Debug.Assert(0 <= readReqBytes && readReqBytes <= IJtagCommunicatable.ReadUnitSize);

            var readData = new byte[readReqBytes];
            uint readBytes = 0;
            var result = _device?.Read(readData, (uint)readData.Length, ref readBytes);

            Debug.Assert(readReqBytes == readBytes);
            return (result == FTDI.FT_STATUS.FT_OK) ? readData : new byte[] { };
        }

        public bool WriteU8(params byte[] data) {
            Debug.Assert(_device?.IsOpen ?? false);
            Debug.Assert(data != null);

            // 転送不要
            if (data.Length == 0) {
                return true;
            }

            uint writtenBytes = 0;
            var result = _device?.Write(data, data.Length, ref writtenBytes);

            Debug.Assert(writtenBytes == data.Length);
            return result == FTDI.FT_STATUS.FT_OK;
        }

        public bool WriteU16(params ushort[] data) {
            Debug.Assert(_device?.IsOpen ?? false);
            Debug.Assert(data != null);

            // FT_Writeでそのまま送るがbyte[]を求められているので変換する
            // 速度が気になるようであれば、unsafeでポインタだけ読み替える
            var dataBytes = data.SelectMany(x => BitConverter.GetBytes(x)).ToArray();
            return WriteU8(dataBytes);
        }

        public bool MoveTestLogicReset() =>
            WriteU16(TMS, TMS, TMS, TMS, TMS);

        public bool MoveIdle() =>
            WriteU16(TMS, TMS, TMS, TMS, TMS, L);

        public bool MoveIdleToShiftIr() =>
            WriteU16(TMS, TMS, L, L);

        public bool WriteShiftDr(byte data) =>
            WriteU8(WR, data);

        public byte[] ReadShiftDr() {
            ClearReadBuffer();
            WriteU16((ushort)(RD | IJtagCommunicatable.ReadUnitSize));

            // dummy write
            WriteU16(Enumerable.Repeat(L, (int)IJtagCommunicatable.ReadUnitSize).ToArray());
            // read
            var d = ReadU8((int)IJtagCommunicatable.ReadUnitSize);

            return d;
        }

        public bool WriteShiftIr(byte data) =>
            WriteU16(BitConverter.ToUInt16(new byte[] { WR, data }), L);


        public bool MoveShiftIrToShiftDr() =>
            WriteU16(TMS, TMS, TMS, L, L);


        public bool MoveShiftDrToShiftIr() =>
            WriteU16(TMS_H, TMS, TMS, TMS, L, L);

        public bool DeviceClose() =>
            WriteU16(TMS_H, TMS, OFF);

        public bool WriteShiftDr(IEnumerable<byte> src) {
            foreach (var d in src) {
                var result = WriteShiftDr(d);
                if (!result) {
                    Debug.Fail("WriteShiftDr failed");
                    return false;
                }
            }
            return true;
        }

        public byte[] ReadShiftDr(uint dataSize, bool removeSurplus = true) {
            var result = new List<byte>((int)dataSize);
            // 63byteで通信する分
            var maxTransferCount = dataSize / IJtagCommunicatable.ReadUnitSize;
            for (var i = 0; i < maxTransferCount; i++) {
                var datas = ReadShiftDr();
                result.AddRange(datas);
            }
            // 最後のあまり
            var remainBytes = dataSize % IJtagCommunicatable.ReadUnitSize;
            if (remainBytes > 0) {
                var datas = ReadShiftDr();
                result.AddRange(removeSurplus ? datas.Take((int)remainBytes) : datas);
            }
            // 連結して返す
            return result.ToArray();
        }
        #endregion
    }
}
