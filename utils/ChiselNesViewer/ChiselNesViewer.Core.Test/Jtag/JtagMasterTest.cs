using System;
using System.Collections.Generic;
using System.Linq;
using ChiselNesViewer.Core.Jtag;
using ChiselNesViewer.Core.Jtag.Command;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace ChiselNesViewer.Core.Test.Jtag {
    [TestClass]
    [DoNotParallelize]
    public class JtagMasterTest {
        readonly string DeviceDescription = "USB-Blaster";

        #region JTAGコマンド開通

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

        #endregion

        #region Virtual JTAG開通

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
            var testReadData = jtag.ReadShiftDr();
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
            var testReadData = jtag.ReadShiftDr();
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
            jtag.WriteShiftDr(new byte[] { 0xab, 0xcd, 0x80 }.Reverse()); // Address部分はDon't careなのでデバッグで見やすい値にした
            jtag.MoveShiftDrToShiftIr();

            // USER0 Write 4byte, カウント値を 0xffffff00 にする
            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(new byte[] { 0xff, 0xff, 0xff, 0x00, }.Reverse()); // カウント値の流し込み
            jtag.MoveShiftDrToShiftIr();

            // USER1 0x000000(isWrite=false, dataKind = accessTest) 
            jtag.WriteShiftIr(VJTAG_USER1);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(new byte[] { 0xfe, 0xdc, 0x00 }.Reverse()); // Address部分はDon't careなのでデバッグで見やすい値にした
            jtag.MoveShiftDrToShiftIr();

            // USER0 Read 16byte
            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            var testReadData = jtag.ReadShiftDr();
            jtag.MoveShiftDrToShiftIr();

            // test終了
            Assert.IsTrue(jtag.Close());

            // 期待値確認(最初8byteは捨てる)
            Assert.AreEqual(testReadData[8], (byte)0x00);
            Assert.AreEqual(testReadData[9], (byte)0xff);
            Assert.AreEqual(testReadData[10], (byte)0xff);
            Assert.AreEqual(testReadData[11], (byte)0xff);
            Assert.AreEqual(testReadData[12], (byte)0x01);
            Assert.AreEqual(testReadData[13], (byte)0xff);
            Assert.AreEqual(testReadData[14], (byte)0xff);
            Assert.AreEqual(testReadData[15], (byte)0xff);
            Assert.AreEqual(testReadData[16], (byte)0x02);
            Assert.AreEqual(testReadData[17], (byte)0xff);
            Assert.AreEqual(testReadData[18], (byte)0xff);
            Assert.AreEqual(testReadData[19], (byte)0xff);
            Assert.AreEqual(testReadData[20], (byte)0x03);
            Assert.AreEqual(testReadData[21], (byte)0xff);
            Assert.AreEqual(testReadData[22], (byte)0xff);
            Assert.AreEqual(testReadData[23], (byte)0xff);
        }


        /// <summary>
        /// DebugAccessTesterでアドレス出力させた動作確認を行う。ReadUnitSize以上一度に読みだした場合
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void TestDebugAccessTesterAddressOut() {
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
            jtag.WriteShiftDr(new byte[] { 0xab, 0xcd, 0x80 }.Reverse()); // Address部分はDon't careなのでデバッグで見やすい値にした
            jtag.MoveShiftDrToShiftIr();

            // USER0 Write 4byte, カウント値を 0x00000000 にする
            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(new byte[] { 0x00, 0x00, 0x00, 0x00, }.Reverse()); // カウント値の流し込み
            jtag.MoveShiftDrToShiftIr();


            // 数回に分けて読み出す
            uint totalRead = 4096;
            uint readUnitBytes = 32; // 1回あたり読むRead数
            uint dummyBytes = 8; // 前回Prefetchしたデータがいるので、最初8byteを捨てる

            Func<uint, uint, IEnumerable<byte>> doRead = (offset, size) => {
                // USER1 0x000000(isWrite=false, dataKind = accessTest) 
                jtag.WriteShiftIr(VJTAG_USER1);
                jtag.MoveShiftIrToShiftDr();
                jtag.WriteShiftDr(new byte[] { (byte)((offset >> 8) & 0xff), (byte)((offset >> 0) & 0xff), 0x00 }.Reverse()); // Address部分はDon't careなのでデバッグで見やすい値にした
                jtag.MoveShiftDrToShiftIr();

                // USER0 Read
                jtag.WriteShiftIr(VJTAG_USER0);
                jtag.MoveShiftIrToShiftDr();
                var readData = jtag.ReadShiftDr(size + dummyBytes);
                jtag.MoveShiftDrToShiftIr();
                return readData.Skip((int)dummyBytes);
            };

            /* アドレス不問で32byteずつ読み出す */
            var dst = new List<byte>(4096);
            while (dst.Count < totalRead) {
                var data = doRead((uint)(dst.Count / 4), readUnitBytes);
                dst.AddRange(data);
            }
            // test終了
            Assert.IsTrue(jtag.Close());
        }

        #endregion

        // TODO: ChiselNes独自コマンドなので、実装が落ち着いたら Jtag.Command.XXXXに新設したクラスに移管する
        #region ChiselNesデバッグコマンド

        const byte VJTAG_USER1 = 0x0e;
        const byte VJTAG_USER0 = 0x0c;

        public enum ChiselNesAccessTarget : byte {
            AccessTest = 0x00,
            FrameBuffer,
            CartCommon,
            CartPrg,
            CartSave,
            CartChr,
            CpuBusMaster,
            PpuBusMaster,
            Cpu,
            Ppu,
            Apu,
            Audio,
        }
        public IEnumerable<byte> CreateVir(uint offset, bool isWrite, ChiselNesAccessTarget target)
                => new byte[] { (byte)((offset >> 8) & 0xff), (byte)((offset >> 0) & 0xff), (byte)(((isWrite ? 0x01 : 0x00) << 7) | ((byte)target)) }.Reverse();

        public void WriteToChiselNes(JtagMaster jtag, ChiselNesAccessTarget target, uint offset, IEnumerable<uint> src) {
            jtag.MoveIdle();
            jtag.MoveIdleToShiftIr();

            // USER1
            var vir = CreateVir(offset, true, target).ToArray(); // 3byte, { offset[15:0], isWrite[1], target[6:0] }

            jtag.WriteShiftIr(VJTAG_USER1);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(vir);
            jtag.MoveShiftDrToShiftIr();

            // USER0 Write 4byte
            var writeData = src.SelectMany(x => BitConverter.GetBytes(x)).ToArray(); // 4byteの内容を1byteずつにバラす

            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(writeData);
            jtag.MoveShiftDrToShiftIr();
        }
        public uint[] ReadFromChiselNes(JtagMaster jtag, ChiselNesAccessTarget target, uint offset, uint size) {
            uint readUnitBytes = 32;
            uint dummyBytes = 24;
            var dst = new List<uint>((int)size);

            // 前処理
            jtag.MoveIdle();
            jtag.MoveIdleToShiftIr();


            // USER1
            var vir = CreateVir(offset, false, target).ToArray(); // 3byte, { offset[15:0], isWrite[1], target[6:0] }

            jtag.WriteShiftIr(VJTAG_USER1);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(vir);
            jtag.MoveShiftDrToShiftIr();

            // USER0 Read
            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            while (dst.Count < size + dummyBytes) {
                var rawData = jtag.ReadShiftDr(readUnitBytes);
                var dstData = rawData.Chunk(4).Select(raw => BitConverter.ToUInt32(raw)).ToArray();
                dst.AddRange(dstData);
            }
            jtag.MoveShiftDrToShiftIr();

            return dst.Skip((int)dummyBytes / 8).Take((int)size).ToArray(); // 先頭と最後のあまりを捨てる
        }

        /// <summary>
        /// FrameBufferへのデータ書き込みテスト
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void TestWriteToFrameBuffer() {
            var jtag = new JtagMaster();
            var devices = JtagMaster.GetDevices();
            var device = devices.First(x => x.Description == DeviceDescription);
            Assert.IsTrue(jtag.Open(device));

            int FB_WIDTH = 256;
            int FB_HEIGHT = 256;
            var testData = Enumerable.Repeat(0, FB_HEIGHT).SelectMany((_, y) =>
                Enumerable.Repeat(0, FB_WIDTH).Select((_, x) =>
                    (uint)((((x) & 0x1f) << 11) | (((y) & 0x3f) << 5) | (((0) & 0x1f) << 0))
                )
            ).ToArray();

            WriteToChiselNes(jtag, ChiselNesAccessTarget.FrameBuffer, 0x00000000, testData);

            Assert.IsTrue(jtag.Close());
        }

        /// <summary>
        /// FrameBufferの読みテスト
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void TestWriteReadToFrameBuffer() {
            var jtag = new JtagMaster();
            var devices = JtagMaster.GetDevices();
            var device = devices.First(x => x.Description == DeviceDescription);
            Assert.IsTrue(jtag.Open(device));

            int FB_WIDTH = 256;
            int FB_HEIGHT = 256;
            var writeData = Enumerable.Repeat(0, FB_HEIGHT).SelectMany((_, y) =>
                Enumerable.Repeat(0, FB_WIDTH).Select((_, x) =>
                    (uint)((((x) & 0x1f) << 11) | (((y) & 0x3f) << 5) | ((((x + y)) & 0x1f) << 0))
                )
            ).ToArray();

            WriteToChiselNes(jtag, ChiselNesAccessTarget.FrameBuffer, 0x00000000, writeData);
            var readData = ReadFromChiselNes(jtag, ChiselNesAccessTarget.FrameBuffer, 0x00000000, (uint)writeData.Length);

            Assert.IsTrue(jtag.Close());
            Assert.IsTrue(Enumerable.SequenceEqual(writeData, readData));
        }

        /// <summary>
        /// VirtualCartridgeの読み書きテスト
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void TestWriteNesHeader() {
            var jtag = new JtagMaster();
            var devices = JtagMaster.GetDevices();
            var device = devices.First(x => x.Description == DeviceDescription);

            // テスト用関数, WriteReadTest
            Action<ChiselNesAccessTarget, uint[]> writeReadTest = (target, writeData) => {
                Assert.IsTrue(jtag.Open(device));

                WriteToChiselNes(jtag, target, 0x00000000, writeData);
                var readData = ReadFromChiselNes(jtag, target, 0x00000000, (uint)writeData.Length);

                Assert.IsTrue(jtag.Close());
            };

            var commonRegWordSize = 32 / 4;
            var commonRegWriteData = Enumerable.Range(0, commonRegWordSize).Select(x => (uint)0xffffffff).ToArray();
            commonRegWriteData[0] = 0x1a53454e; //iNES Header "0x4e,0x45,0x53,0x1a"
            commonRegWriteData[1] = 0x00000101; // PRG ROM=CHR ROM=1Bank, Mapper0, Mirror Horizontal,
            commonRegWriteData[2] = 0x00000000;
            commonRegWriteData[3] = 0x00000000;

            writeReadTest(ChiselNesAccessTarget.CartCommon, commonRegWriteData);
        }

        /// <summary>
        /// VirtualCartridgeの読み書きテスト
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void TestWriteReadToVirtualCartridge() {
            var jtag = new JtagMaster();
            var devices = JtagMaster.GetDevices();
            var device = devices.First(x => x.Description == DeviceDescription);

            // テスト用関数, WriteReadTest
            Action<ChiselNesAccessTarget, uint[]> writeReadTest = (target, writeData) => {
                Assert.IsTrue(jtag.Open(device));

                WriteToChiselNes(jtag, target, 0x00000000, writeData);
                var readData = ReadFromChiselNes(jtag, target, 0x00000000, (uint)writeData.Length);

                Assert.IsTrue(jtag.Close());
                Assert.IsTrue(Enumerable.SequenceEqual(writeData, readData));
            };

            // 4[byte/entry] で転送データ数を求める
            var commonRegWordSize = 32 / 4; 
            var prgRomWordSize = 0x1_0000 / 4;
            var saveRamWordSize = 0x1000 / 4;
            var chrRomWordSize = 0x1_0000 / 4;

            var commonRegWriteData = Enumerable.Range(0, commonRegWordSize).Select(x => (uint)x).ToArray();
            commonRegWriteData[0] = 0x1a53454e; //iNES Header "0x4e, 0x45,0x53, 0x1a"
            var prgRomWriteData = Enumerable.Range(0, prgRomWordSize).Select(x => (uint)x * 2 + 0x12345670).ToArray();
            var saveRamWriteData = Enumerable.Range(0, saveRamWordSize).Select(x => (uint)x * 4 + 0x89abcde0).ToArray();
            var chrRomWriteData = Enumerable.Range(0, chrRomWordSize).Select(x => (uint)x * 8 + 0xf02468a0).ToArray();

            //writeReadTest(ChiselNesAccessTarget.CartCommon, commonRegWriteData); // 最適化で未使用要素が消えるので、Verifyしない
            writeReadTest(ChiselNesAccessTarget.CartPrg, prgRomWriteData);
            writeReadTest(ChiselNesAccessTarget.CartSave, saveRamWriteData);
            writeReadTest(ChiselNesAccessTarget.CartChr, chrRomWriteData);
        }

        /// <summary>
        /// VirtualCartridgeの読み書きテスト. まとめて書く
        /// </summary>
        [TestMethod]
        [DoNotParallelize]
        public void TestWriteReadToVirtualCartridge2() {
            var jtag = new JtagMaster();
            var devices = JtagMaster.GetDevices();
            var device = devices.First(x => x.Description == DeviceDescription);

            // テスト用関数
            Action<ChiselNesAccessTarget, uint[]> writeTest = (target, writeData) => {
                Assert.IsTrue(jtag.Open(device));
                WriteToChiselNes(jtag, target, 0x00000000, writeData);
                Assert.IsTrue(jtag.Close());
            };
            Action<ChiselNesAccessTarget, uint[]> readTest = (target, writeData) => {
                Assert.IsTrue(jtag.Open(device));
                var readData = ReadFromChiselNes(jtag, target, 0x00000000, (uint)writeData.Length);
                Assert.IsTrue(jtag.Close());

                Assert.IsTrue(Enumerable.SequenceEqual(writeData, readData));
            };

            // 4[byte/entry] で転送データ数を求める
            var commonRegWordSize = 32 / 4;
            var prgRomWordSize = 0x1_0000 / 4;
            var saveRamWordSize = 0x1000 / 4;
            var chrRomWordSize = 0x1_0000 / 4;

            var commonRegWriteData = Enumerable.Range(0, commonRegWordSize).Select(x => (uint)x).ToArray();
            commonRegWriteData[0] = 0x1a53454e; //iNES Header "0x4e, 0x45,0x53, 0x1a"
            var prgRomWriteData = Enumerable.Range(0, prgRomWordSize).Select(x => (uint)x * 2 + 0x12345670).ToArray();
            var saveRamWriteData = Enumerable.Range(0, saveRamWordSize).Select(x => (uint)x * 4 + 0x89abcde0).ToArray();
            var chrRomWriteData = Enumerable.Range(0, chrRomWordSize).Select(x => (uint)x * 8 + 0xf02468a0).ToArray();

            writeTest(ChiselNesAccessTarget.CartCommon, commonRegWriteData);
            writeTest(ChiselNesAccessTarget.CartPrg, prgRomWriteData);
            writeTest(ChiselNesAccessTarget.CartSave, saveRamWriteData);
            writeTest(ChiselNesAccessTarget.CartChr, chrRomWriteData);
            //readTest(ChiselNesAccessTarget.CartCommon, commonRegWriteData); // 最適化で未使用要素が消えるので、Verifyしない
            readTest(ChiselNesAccessTarget.CartPrg, prgRomWriteData);
            readTest(ChiselNesAccessTarget.CartSave, saveRamWriteData);
            readTest(ChiselNesAccessTarget.CartChr, chrRomWriteData);
        }
        #endregion
    }
}