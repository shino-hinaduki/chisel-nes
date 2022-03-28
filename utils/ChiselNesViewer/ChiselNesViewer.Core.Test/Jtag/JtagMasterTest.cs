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
            var testReadData = jtag.ReadShiftDr((uint)testWriteData.Length, removeSurplus: false);
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

            // USER1 0x00007f(dataKind = invalid)
            const byte VJTAG_USER1 = 0x0e;
            var testVirtualInst = new byte[] { 0x00, 0x00, 0x7f }.Reverse().ToArray();

            jtag.WriteShiftIr(VJTAG_USER1);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(testVirtualInst);
            jtag.MoveShiftDrToShiftIr();

            // USER0 Read 16byte
            const byte VJTAG_USER0 = 0x0c;
            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            var testReadData = jtag.ReadShiftDr(16, removeSurplus: false);
            jtag.MoveShiftDrToShiftIr();

            // test終了
            Assert.IsTrue(jtag.Close());

            // 期待値確認
            Assert.AreEqual(testReadData[0], (byte)0xaa);
            Assert.AreEqual(testReadData[1], (byte)0x99);
            Assert.AreEqual(testReadData[2], (byte)0x55);
            Assert.AreEqual(testReadData[3], (byte)0x66);
            Assert.AreEqual(testReadData[4], (byte)0xaa);
            Assert.AreEqual(testReadData[5], (byte)0x99);
            Assert.AreEqual(testReadData[6], (byte)0x55);
            Assert.AreEqual(testReadData[7], (byte)0x66);
            Assert.AreEqual(testReadData[8], (byte)0xaa);
            Assert.AreEqual(testReadData[9], (byte)0x99);
            Assert.AreEqual(testReadData[10], (byte)0x55);
            Assert.AreEqual(testReadData[11], (byte)0x66);
            Assert.AreEqual(testReadData[12], (byte)0xaa);
            Assert.AreEqual(testReadData[13], (byte)0x99);
            Assert.AreEqual(testReadData[14], (byte)0x55);
            Assert.AreEqual(testReadData[15], (byte)0x66);

        }


        /// <summary>
        /// DebugAccessTesterのカウンターの動作確認を行う
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void TestDebugAccessTesterCounter() {
            const byte VJTAG_USER1 = 0x0e;
            const byte VJTAG_USER0 = 0x0c;

            var jtag = new JtagMaster();
            var devices = JtagMaster.GetDevices();
            var device = devices.First(x => x.Description == DeviceDescription);
            Assert.IsTrue(jtag.Open(device));

            jtag.MoveIdle();
            jtag.MoveIdleToShiftIr();

            // USER1 0x000080(isWrite=true, dataKind = accessTest)
            jtag.WriteShiftIr(VJTAG_USER1);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(new byte[] { 0xab, 0xcd, 0x80 }.Reverse());
            jtag.MoveShiftDrToShiftIr();

            // USER0 Write 4byte, カウント値を 0xffffff00 にする
            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(new byte[] { 0xff, 0xff, 0xff, 0x00, }.Reverse());
            jtag.WriteShiftDr(new byte[] { 0xff, 0xff, 0xff, 0x00, }.Reverse());
            jtag.MoveShiftDrToShiftIr();

            // USER1 0x000000(isWrite=false, dataKind = accessTest)
            jtag.WriteShiftIr(VJTAG_USER1);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(new byte[] { 0x00, 0x00, 0x00 }.Reverse());
            jtag.MoveShiftDrToShiftIr();

            // USER0 Read 16byte
            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            var testReadData = jtag.ReadShiftDr(16, removeSurplus: false);
            jtag.MoveShiftDrToShiftIr();

            // test終了
            Assert.IsTrue(jtag.Close());

            // 期待値確認
            Assert.AreEqual(testReadData[0], (byte)0x00);
            Assert.AreEqual(testReadData[1], (byte)0xff);
            Assert.AreEqual(testReadData[2], (byte)0xff);
            Assert.AreEqual(testReadData[3], (byte)0xff);
            Assert.AreEqual(testReadData[4], (byte)0x01);
            Assert.AreEqual(testReadData[5], (byte)0xff);
            Assert.AreEqual(testReadData[6], (byte)0xff);
            Assert.AreEqual(testReadData[7], (byte)0xff);
            Assert.AreEqual(testReadData[8], (byte)0x02);
            Assert.AreEqual(testReadData[9], (byte)0xff);
            Assert.AreEqual(testReadData[10], (byte)0xff);
            Assert.AreEqual(testReadData[11], (byte)0xff);
            Assert.AreEqual(testReadData[12], (byte)0x03);
            Assert.AreEqual(testReadData[13], (byte)0xff);
            Assert.AreEqual(testReadData[14], (byte)0xff);
            Assert.AreEqual(testReadData[15], (byte)0xff);

        }
    }
}