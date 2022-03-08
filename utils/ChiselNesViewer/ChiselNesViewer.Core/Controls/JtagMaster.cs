using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using FTD2XX_NET;

namespace ChiselNesViewer.Core.Controls
{
    /// <summary>
    /// FT245でJTAGを扱うためのクラスです。以下を参考に実装しています
    /// * NES on FPGA pgate1  - https://pgate1.at-ninja.jp/NES_on_FPGA/dev_other.htm
    /// * kazu_1995's diary   - https://kazu1995.hatenablog.jp/entry/2017/11/18/202718
    /// * relm.info           - http://relm.info/?x=entry:entry130319-165251
    /// </summary>
    internal class JtagMaster: IDisposable, IJtagCommunicatable
    {
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

        /// <summary>
        /// デバイス一覧をリセットし、デバイス数を取得します
        /// </summary>
        /// <returns></returns>
        public static UInt32 GetNumOfDevices()
        {
            UInt32 numDevices = 0;
            var device = new FTDI();
            device.Rescan();
            device.GetNumberOfDevices(ref numDevices);
            return numDevices;

        }

        /// <summary>
        /// 接続されているFTXXXの一覧を取得します
        /// </summary>
        /// <returns>デバイス一覧。エラーが発生した場合も空の配列を返す</returns>
        public static FTDI.FT_DEVICE_INFO_NODE[] GetDevices()
        {
            // デバイスがない場合は何もしない
            var numOfDevices = GetNumOfDevices();
            if (numOfDevices == 0) { return new FTDI.FT_DEVICE_INFO_NODE[] { }; }

            // デバイスが存在する場合は一覧を問い合わせる
            var device = new FTDI();
            var deviceList = new FTDI.FT_DEVICE_INFO_NODE[numOfDevices];
            device.Rescan();
            if (device.GetDeviceList(deviceList) == FTDI.FT_STATUS.FT_OK)
            {
                return deviceList;

            } else
            {
                return new FTDI.FT_DEVICE_INFO_NODE[] { };
            }
        }

        /// <summary>
        /// FT2XXをJTAG用に使用します
        /// </summary>
        /// <returns></returns>
        /// <exception cref="NotImplementedException"></exception>
        public bool Open(FTDI.FT_DEVICE_INFO_NODE targetInfo)
        {
            Debug.Assert(targetInfo != null);
            Debug.Assert(this._device == null);

            // Close済想定だが、念の為あるなら閉じておく
            this._device?.Close();
            this._device = null;

            // SerialNumberを引数に開く
            var device = new FTDI();
            if (device.OpenBySerialNumber(targetInfo.SerialNumber) != FTDI.FT_STATUS.FT_OK)
            {
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
        private void Configure()
        {
            Debug.Assert(this._device?.IsOpen ?? false);

            _device?.ResetDevice();
            _device?.SetLatency(Latency);
            _device?.SetBaudRate(BaudRate);
        }

        /// <summary>
        /// デバイスを切断します
        /// </summary>
        /// <returns></returns>
        public bool Close()
        {
            Debug.Assert(this._device != null);
            return _device?.Close() == FTDI.FT_STATUS.FT_OK;
        }

        protected virtual void Dispose(bool disposing)
        {
            if (!disposedValue)
            {
                if (disposing)
                {
                    this._device?.Close();
                    this._device =null;
                }

                disposedValue = true;
            }
        }

        public void Dispose()
        {
            Dispose(disposing: true);
            GC.SuppressFinalize(this);
        }

        bool IJtagCommunicatable.MoveIdle()
        {
            throw new NotImplementedException();
        }

        bool IJtagCommunicatable.MoveIdleToShiftIr()
        {
            throw new NotImplementedException();
        }

        bool IJtagCommunicatable.WriteShiftDr(byte data)
        {
            throw new NotImplementedException();
        }

        bool IJtagCommunicatable.WriteShiftIr(byte data)
        {
            throw new NotImplementedException();
        }

        bool IJtagCommunicatable.MoveShiftIrToShiftDr()
        {
            throw new NotImplementedException();
        }

        bool IJtagCommunicatable.MoveShiftDrToShiftIr()
        {
            throw new NotImplementedException();
        }

        bool IJtagCommunicatable.WriteShiftDr(byte[] datas)
        {
            throw new NotImplementedException();
        }

        byte[] IJtagCommunicatable.ReadShiftDr(uint dataSize)
        {
            throw new NotImplementedException();
        }
    }
}
