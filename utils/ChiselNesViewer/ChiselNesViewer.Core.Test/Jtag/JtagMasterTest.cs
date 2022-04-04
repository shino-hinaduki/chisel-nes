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

        #region JTAG�R�}���h�J��

        /// <summary>
        /// �f�o�C�X�̌����Ɛڑ�
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
        /// IDCODE��DE0-CV�ɏ���Ă���Cyclone V E A4�Ɉ�v���Ă��邩�m�F
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
        /// PULSE_NCONFIG�R�}���h�𔭍s�B�f�o�C�X�����R���t�B�������[�V��������Ă���ΐ���
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
        /// USERCODE�R�}���h�𔭍s�B���Ғl��Quartus Prime�t���c�[���œǂݎ�����l
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
        /// �����̃R�}���h���󂯕t���邩�m�F
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

        #region Virtual JTAG�J��

        /// <summary>
        /// VIR�ւ̏������݂ƁA�K���ȃf�[�^�̗������݃e�X�g
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
            // ����������, e6b954b3cc �̃f�U�C�����Ă��Ă����LED�ɏo��͂�
            const byte VJTAG_USER1 = 0x0e;
            var testVirtualInst = new byte[] { 0x12, 0x34, 0x56 }.Reverse().ToArray();

            jtag.WriteShiftIr(VJTAG_USER1);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(testVirtualInst);
            jtag.MoveShiftDrToShiftIr();

            // USER0 Write testData
            // �������f�[�^�B�������VDR�ɗ����Ă���̂ŉ����N���Ȃ�
            const byte VJTAG_USER0 = 0x0c;
            var testWriteData = new byte[] { 0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef }.Reverse().ToArray();

            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(testWriteData);
            jtag.MoveShiftDrToShiftIr();

            // USER0 Read testData
            // �������f�[�^�̓ǂݏo���B e6b954b3cc �̃f�U�C������bypass����Ă���͂�
            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            var testReadData = jtag.ReadShiftDr();
            jtag.MoveShiftDrToShiftIr();

            // test�I��
            Assert.IsTrue(jtag.Close());

        }

        /// <summary>
        /// �����Ȗ��߂𓊂���VirtualJtagBridge.invalidData���ǂݏo���邩�m�F����
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

            // test�I��
            Assert.IsTrue(jtag.Close());

            // ���Ғl�m�F
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
        /// DebugAccessTester�̃J�E���^�[�̓���m�F���s��
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
            jtag.WriteShiftDr(new byte[] { 0xab, 0xcd, 0x80 }.Reverse()); // Address������Don't care�Ȃ̂Ńf�o�b�O�Ō��₷���l�ɂ���
            jtag.MoveShiftDrToShiftIr();

            // USER0 Write 4byte, �J�E���g�l�� 0xffffff00 �ɂ���
            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(new byte[] { 0xff, 0xff, 0xff, 0x00, }.Reverse()); // �J�E���g�l�̗�������
            jtag.MoveShiftDrToShiftIr();

            // USER1 0x000000(isWrite=false, dataKind = accessTest) 
            jtag.WriteShiftIr(VJTAG_USER1);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(new byte[] { 0xfe, 0xdc, 0x00 }.Reverse()); // Address������Don't care�Ȃ̂Ńf�o�b�O�Ō��₷���l�ɂ���
            jtag.MoveShiftDrToShiftIr();

            // USER0 Read 16byte
            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            var testReadData = jtag.ReadShiftDr();
            jtag.MoveShiftDrToShiftIr();

            // test�I��
            Assert.IsTrue(jtag.Close());

            // ���Ғl�m�F(�ŏ�8byte�͎̂Ă�)
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
        /// DebugAccessTester�ŃA�h���X�o�͂���������m�F���s���BReadUnitSize�ȏ��x�ɓǂ݂������ꍇ
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
            jtag.WriteShiftDr(new byte[] { 0xab, 0xcd, 0x80 }.Reverse()); // Address������Don't care�Ȃ̂Ńf�o�b�O�Ō��₷���l�ɂ���
            jtag.MoveShiftDrToShiftIr();

            // USER0 Write 4byte, �J�E���g�l�� 0x00000000 �ɂ���
            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(new byte[] { 0x00, 0x00, 0x00, 0x00, }.Reverse()); // �J�E���g�l�̗�������
            jtag.MoveShiftDrToShiftIr();


            // ����ɕ����ēǂݏo��
            uint totalRead = 4096;
            uint readUnitBytes = 32; // 1�񂠂���ǂ�Read��
            uint dummyBytes = 8; // �O��Prefetch�����f�[�^������̂ŁA�ŏ�8byte���̂Ă�

            Func<uint, uint, IEnumerable<byte>> doRead = (offset, size) => {
                // USER1 0x000000(isWrite=false, dataKind = accessTest) 
                jtag.WriteShiftIr(VJTAG_USER1);
                jtag.MoveShiftIrToShiftDr();
                jtag.WriteShiftDr(new byte[] { (byte)((offset >> 8) & 0xff), (byte)((offset >> 0) & 0xff), 0x00 }.Reverse()); // Address������Don't care�Ȃ̂Ńf�o�b�O�Ō��₷���l�ɂ���
                jtag.MoveShiftDrToShiftIr();

                // USER0 Read
                jtag.WriteShiftIr(VJTAG_USER0);
                jtag.MoveShiftIrToShiftDr();
                var readData = jtag.ReadShiftDr(size + dummyBytes);
                jtag.MoveShiftDrToShiftIr();
                return readData.Skip((int)dummyBytes);
            };

            /* �A�h���X�s���32byte���ǂݏo�� */
            var dst = new List<byte>(4096);
            while (dst.Count < totalRead) {
                var data = doRead((uint)(dst.Count / 4), readUnitBytes);
                dst.AddRange(data);
            }
            // test�I��
            Assert.IsTrue(jtag.Close());
        }

        #endregion

        // TODO: ChiselNes�Ǝ��R�}���h�Ȃ̂ŁA������������������ Jtag.Command.XXXX�ɐV�݂����N���X�Ɉڊǂ���
        #region ChiselNes�f�o�b�O�R�}���h

        const byte VJTAG_USER1 = 0x0e;
        const byte VJTAG_USER0 = 0x0c;

        public enum ChiselNesAccessTarget: byte {
            AccessTest = 0x00,
            Cpu,
            Ppu,
            Apu,
            Cart,
            FrameBuffer,
            CpuBusMaster,
            PpuBusMaster,
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
            var writeData = src.SelectMany(x => BitConverter.GetBytes(x)).ToArray(); // 4byte�̓��e��1byte���Ƀo����

            jtag.WriteShiftIr(VJTAG_USER0);
            jtag.MoveShiftIrToShiftDr();
            jtag.WriteShiftDr(writeData);
            jtag.MoveShiftDrToShiftIr();
        }
        public uint[] ReadFromChiselNes(JtagMaster jtag, ChiselNesAccessTarget target, uint offset, uint size) {
            // �O����
            jtag.MoveIdle();
            jtag.MoveIdleToShiftIr();

            // 32byte���ǂݏo��
            uint readUnitBytes = 32; // 1�񂠂���ǂ�Read��
            uint dummyBytes = 8; // �O��Prefetch�����f�[�^������̂ŁA�ŏ�8byte���̂Ă�

            Func<uint, uint, IEnumerable<byte>> doRead = (offset, size) => {
                // USER1
                var vir = CreateVir(offset, false, target).ToArray(); // 3byte, { offset[15:0], isWrite[1], target[6:0] }

                jtag.WriteShiftIr(VJTAG_USER1);
                jtag.MoveShiftIrToShiftDr();
                jtag.WriteShiftDr(vir);
                jtag.MoveShiftDrToShiftIr();

                // USER0 Read
                jtag.WriteShiftIr(VJTAG_USER0);
                jtag.MoveShiftIrToShiftDr();
                var readData = jtag.ReadShiftDr(size + dummyBytes);
                jtag.MoveShiftDrToShiftIr();
                return readData.Skip((int)dummyBytes);
            };

            var dst = new List<uint>((int)size);
            while (dst.Count < size) {
                var rawData = doRead((uint)(offset + dst.Count * 4), readUnitBytes);
                var dstData = rawData.Chunk(4).Select(raw => BitConverter.ToUInt32(raw)).ToArray();
                dst.AddRange(dstData);
            }

            return dst.ToArray();
        }

        /// <summary>
        /// FrameBuffer�ւ̃f�[�^�������݃e�X�g
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
                    (uint)((((x * 1) & 0xff) << 16) | (((y * 1) & 0xff) << 8) | (((0) & 0xff) << 0))
                )
            ).ToArray();

            WriteToChiselNes(jtag, ChiselNesAccessTarget.FrameBuffer, 0x00000000, testData);

            Assert.IsTrue(jtag.Close());
        }

        /// <summary>
        /// FrameBuffer�̓ǂ݃e�X�g
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
                    (uint)((((x * 1) & 0xff) << 16) | (((y * 1) & 0xff) << 8) | (((0) & 0xff) << 0))
                )
            ).ToArray();

            //WriteToChiselNes(jtag, ChiselNesAccessTarget.FrameBuffer, 0x00000000, writeData);
            var readData = ReadFromChiselNes(jtag, ChiselNesAccessTarget.FrameBuffer, 0x00000000, (uint)writeData.Length);

            Assert.IsTrue(jtag.Close());
            Assert.IsTrue(Enumerable.SequenceEqual(writeData, readData));
        }
        #endregion
    }
}