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
            var testReadData = jtag.ReadShiftDr((uint)testWriteData.Length);
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

            // test�I��
            Assert.IsTrue(jtag.Close());

            // ���Ғl�m�F
            Assert.AreEqual(testReadData[0], (byte)0xa5);

        }

        /// <summary>
        /// �����Ȗ��߂𓊂���VirtualJtagBridge.invalidData���ǂݏo���邩�m�F����
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

            // test�I��
            Assert.IsTrue(jtag.Close());

            // ���Ғl�m�F
            Assert.AreEqual(testReadData[0], (byte)0xa5);

        }
    }
}