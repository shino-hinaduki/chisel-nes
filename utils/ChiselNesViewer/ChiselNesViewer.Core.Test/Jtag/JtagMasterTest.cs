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


        /// <summary>
        /// VIRへの書き込みと、適当なデータの流し込みテスト
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void TestVirtualJtagWriteRead() {
            var jtag = new JtagMaster();
            var devices = JtagMaster.GetDevices();
            var device = devices.First(x => x.Description == DeviceDescription);
            Assert.IsTrue(jtag.Open(device));

            jtag.MoveIdle();
            jtag.MoveIdleToShiftIr();

            // USER1 testVirtualInst
            // お試し命令, e6b954b3cc のデザインを焼いていればLEDに出るはず
            const byte VJTAG_USER1 = 0x0e;
            var testVirtualInst = new byte[] { 0x12, 0x34, 0x56 }.Reverse().ToArray();

            jtag.WriteShiftIr(VJTAG_USER1);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(testVirtualInst);
            jtag.MoveShiftDrToShiftIr();

            // USER0 Write testData
            // お試しデータ。こちらはVDRに流しているので何も起きない
            const byte VJTAG_USER0 = 0x0c;
            var testWriteData = new byte[] { 0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef }.Reverse().ToArray();

            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(testWriteData);
            jtag.MoveShiftDrToShiftIr();

            // USER0 Read testData
            // お試しデータの読み出し。 e6b954b3cc のデザインだとbypassされているはず
            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            var testReadData = jtag.ReadShiftDr((uint)testWriteData.Length);
            jtag.MoveShiftDrToShiftIr();

            // test終了
            Assert.IsTrue(jtag.Close());

        }

        /// <summary>
        /// 無効な命令を投げてVirtualJtagBridge.invalidDataが読み出せるか確認する
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void TestInvalidUserInst() {
            var jtag = new JtagMaster();
            var devices = JtagMaster.GetDevices();
            var device = devices.First(x => x.Description == DeviceDescription);
            Assert.IsTrue(jtag.Open(device));

            jtag.MoveIdle();
            jtag.MoveIdleToShiftIr();

            // USER1 0x000000(dataKind = invalid)
            const byte VJTAG_USER1 = 0x0e;
            var testVirtualInst = new byte[] { 0x00, 0x00, 0x00 }.Reverse().ToArray();

            jtag.WriteShiftIr(VJTAG_USER1);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(testVirtualInst);
            jtag.MoveShiftDrToShiftIr();

            // USER0 Read 1byte
            const byte VJTAG_USER0 = 0x0c;
            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            var testReadData = jtag.ReadShiftDr(1);
            jtag.MoveShiftDrToShiftIr();

            // test終了
            Assert.IsTrue(jtag.Close());

            // 期待値確認
            Assert.AreEqual(testReadData[0], (byte)0xa5);

        }

        /// <summary>
        /// 無効な命令を投げてVirtualJtagBridge.invalidDataが読み出せるか確認する
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void TestInvalid2UserInst() {
            var jtag = new JtagMaster();
            var devices = JtagMaster.GetDevices();
            var device = devices.First(x => x.Description == DeviceDescription);
            Assert.IsTrue(jtag.Open(device));

            jtag.MoveIdle();
            jtag.MoveIdleToShiftIr();

            // USER1 0x00007f(dataKind = invalid2)
            const byte VJTAG_USER1 = 0x0e;
            var testVirtualInst = new byte[] { 0x00, 0x00, 0x7f }.Reverse().ToArray();

            jtag.WriteShiftIr(VJTAG_USER1);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(testVirtualInst);
            jtag.MoveShiftDrToShiftIr();

            // USER0 Read 1byte
            const byte VJTAG_USER0 = 0x0c;
            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            var testReadData = jtag.ReadShiftDr(1);
            jtag.MoveShiftDrToShiftIr();

            // test終了
            Assert.IsTrue(jtag.Close());

            // 期待値確認
            Assert.AreEqual(testReadData[0], (byte)0xa5);

        }
    }
}