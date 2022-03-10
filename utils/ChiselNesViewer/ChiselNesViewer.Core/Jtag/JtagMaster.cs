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
        /// Latencyの初期設定。通常16msがデフォルトだが短くできるデバイスもある
        /// </summary>
        private const int Latency = 16;

        /// <summary>
        /// BaudRateの初期設定。FT245のApplication Notesおり 3MB/sが最高だが実効は1MB/sほど
        /// </summary>
        private const int BaudRate = 3000000;

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
            Debug.Assert(this._device == null);

            // Close済想定だが、念の為あるなら閉じておく
            this._device?.Close();
            this._device = null;

            // SerialNumberを引数に開く
            var device = new FTDI();
            if (device.OpenBySerialNumber(targetInfo.SerialNumber) != FTDI.FT_STATUS.FT_OK) {
                return false;
            }

            // ローカルにセット
            this._device = device;

            // JTAGで利用するための設定を施す
            Debug.Assert(_device?.IsOpen ?? false);
            Configure();

            // 成功
            return true;
        }

        /// <summary>
        /// デバイスを初期化して、必要な設定を施します
        /// </summary>
        private void Configure() {
            Debug.Assert(this._device?.IsOpen ?? false);

            _device?.ResetDevice();
            _device?.SetLatency(Latency);
            _device?.SetBaudRate(BaudRate);
        }

        /// <summary>
        /// デバイスを切断します
        /// </summary>
        /// <returns></returns>
        public bool Close() {
            Debug.Assert(this._device != null);
            return _device?.Close() == FTDI.FT_STATUS.FT_OK;
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

        public byte[] ReadU8(int readReqBytes) {
            Debug.Assert(_device?.IsOpen ?? false);
            Debug.Assert(0 <= readReqBytes && readReqBytes < IJtagCommunicatable.ReadUnitSize);

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

        public bool MoveIdle() =>
            WriteU16(TMS, TMS, TMS, TMS, TMS, L);

        public bool MoveIdleToShiftIr() =>
            WriteU16(TMS, TMS, L, L);

        public bool WriteShiftDr(byte data) =>
            WriteU8(WR, data);

        public bool WriteShiftIr(byte data) =>
            WriteU16((ushort)((ushort)WR | ((ushort)data) << 8), L);


        public bool MoveShiftIrToShiftDr() =>
            WriteU16(TMS, TMS, TMS, L, L);


        public bool MoveShiftDrToShiftIr() =>
            WriteU16(TMS_H, TMS, TMS, TMS, L, L);

        public bool DeviceClose() =>
            WriteU16(TMS_H, TMS, OFF);

        public bool WriteShiftDr(IEnumerable<byte> src) {
            Debug.Assert(src != null);

            // (u8) WR, data[0], WR, data[1], ... の送信データを作って送信する
            var writeDatas = src.SelectMany(x => new byte[] { WR, x }).ToArray();
            return (writeDatas.Length == 0) ? true : WriteU8(writeDatas);
        }

        public byte[] ReadShiftDr(uint dataSize) {
            // サイズ指定がないときは処理不要
            if (dataSize == 0) {
                return new byte[] { };
            }

            // FTD2XX都合で64byteごと処理する
            var dst = new List<byte>(capacity: (int)dataSize);
            var remainSize = dataSize;
            while (remainSize > 0) {
                // 63byteもしくは残りの端数
                var readBytes = Math.Min(remainSize, IJtagCommunicatable.ReadUnitSize);
                remainSize -= readBytes;
                Debug.Assert(0 < readBytes && readBytes <= IJtagCommunicatable.ReadUnitSize);

                // (u16) RD | readBytes, 0 | L, 0 | L, 0 | L, ....のデータを作って送る
                var sendDataU16Length = (int)(readBytes + 1) / 2; // 先頭の命令分を追加、2byteごとなので半分
                var sendDataU16 = Enumerable.Repeat(L, sendDataU16Length).ToArray();
                sendDataU16[0] = (ushort)(RD | readBytes);
                WriteU16(sendDataU16);

                // 0 | L, 0 | L, ... の部分を受け取る。データ分のみなのでreadByteそのまま
                var readDataU8 = ReadU8((int)readBytes);
                dst.AddRange(readDataU8);
            }
            return dst.ToArray();

        }
        #endregion
    }
}
