using System;
using System.Linq;
using ChiselNesViewer.Core.Jtag;
using ChiselNesViewer.Core.Jtag.Command;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace ChiselNesViewer.Core.Test.Jtag {
    [TestClass]
    [DoNotParallelize]
    public class JtagMasterTest {
        readonly string DeviceDescription = "USB-Blaster";

        /// <summary>
        /// デバイスの検索と接続
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void Connect() {
            var jtag = new JtagMaster();
            var devices = JtagMaster.GetDevices();
            var device = devices.First(x => x.Description == DeviceDescription);

            Assert.IsTrue(jtag.Open(device));
            Assert.IsTrue(jtag.Close());
        }

        /// <summary>
        /// IDCODEがDE0-CVに乗っているCyclone V E A4に一致しているか確認
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void TestReadIdcode() {
            var jtag = new JtagMaster();
            var devices = JtagMaster.GetDevices();
            var device = devices.First(x => x.Description == DeviceDescription);
            Assert.IsTrue(jtag.Open(device));
            var idcode = Idcode.Read(jtag);
            Assert.IsTrue(jtag.Close());

            // Cyclone V E A4
            //https://www.intel.co.jp/content/dam/altera-www/global/ja_JP/pdfs/literature/hb/cyclone-v/cv_52009_j.pdf
            Assert.AreEqual(idcode.Version, 0b0000);
            Assert.AreEqual(idcode.PartNumber, 0b0010_1011_0000_0101);
            Assert.AreEqual(idcode.MakerId, 0b000_0110_1110);
        }

        /// <summary>
        /// PULSE_NCONFIGコマンドを発行。デバイスがリコンフィリュレーションされていれば成功
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void TestPulseNConfig() {
            var jtag = new JtagMaster();
            var devices = JtagMaster.GetDevices();
            var device = devices.First(x => x.Description == DeviceDescription);

            Assert.IsTrue(jtag.Open(device));
            PulseNConfig.Do(jtag);
        }

        /// <summary>
        /// USERCODEコマンドを発行。期待値はQuartus Prime付属ツールで読み取った値
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void TestUsercode() {
            var jtag = new JtagMaster();
            var devices = JtagMaster.GetDevices();
            var device = devices.First(x => x.Description == DeviceDescription);
            Assert.IsTrue(jtag.Open(device));
            var usercode = Usercode.Read(jtag);
            Assert.IsTrue(jtag.Close());

            Assert.AreEqual(usercode.Raw, (uint)0x04b56019);
        }

        /// <summary>
        /// 複数のコマンドを受け付けるか確認
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void TestUseMultiCommand() {
            var jtag = new JtagMaster();
            var devices = JtagMaster.GetDevices();
            var device = devices.First(x => x.Description == DeviceDescription);

            Assert.IsTrue(jtag.Open(device));
            var idcode = Idcode.Read(jtag);
            var usercode = Usercode.Read(jtag);
            PulseNConfig.Do(jtag);
            Assert.IsTrue(jtag.Close());

            // Cyclone V E A4
            //https://www.intel.co.jp/content/dam/altera-www/global/ja_JP/pdfs/literature/hb/cyclone-v/cv_52009_j.pdf
            Assert.AreEqual(idcode.Raw, (uint)0x02b050dd);
            Assert.AreEqual(usercode.Raw, (uint)0x04b56019);
        }
    }
}