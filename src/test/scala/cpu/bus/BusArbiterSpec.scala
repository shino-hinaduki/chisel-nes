package cpu.bus

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class BusArbiterSpec extends AnyFreeSpec with ChiselScalatestTester {
  /* Bus Mater: 設定 */
  // Write要求を投げる
  def reqWrite(p: BusSlavePort, addr: UInt, data: UInt) = {
    p.addr.poke(addr)
    p.req.poke(true.B)
    p.writeEnable.poke(true.B)
    p.dataIn.poke(data)
  }
  // Read要求を投げる
  def reqRead(p: BusSlavePort, addr: UInt) = {
    p.addr.poke(addr)
    p.req.poke(true.B)
    p.writeEnable.poke(false.B)
    p.dataIn.poke(0.U) // Don't care
  }
  // 要求を行わない
  def release(p: BusSlavePort) = {
    p.addr.poke(0.U) // Don't care
    p.req.poke(false.B)
    p.writeEnable.poke(true.B) // Don't care
    p.dataIn.poke(0.U)         // Don't care
  }

  /* Bus Mater: 期待値確認 */
  // Read応答の期待値を確認
  def expectReadResp(p: BusSlavePort, data: UInt) = {
    p.valid.expect(true.B)
    p.dataOut.expect(data)
  }
  // Busyで待たされていることを期待
  def expectNoResp(p: BusSlavePort) = {
    p.valid.expect(false.B)
    // dataOutはDon't care
  }

  /* 外部Bus: 設定 */
  // 外部Busの値を更新
  def setExtDataIn(arbiter: BusArbiter, data: UInt) = {
    arbiter.io.extDataIn.poke(data)
  }

  /* 外部Bus: 期待値確認 */
  // 外部BusへのReadアクセス期待値を確認する
  def expectExtRead(arbiter: BusArbiter, addr: UInt) = {
    arbiter.io.extAddr.expect(addr)
    arbiter.io.extDataOut.oe.expect(false.B)
  }
  // 外部BusへのWriteアクセス期待値を確認する
  def expectExtWrite(arbiter: BusArbiter, addr: UInt, data: UInt) = {
    arbiter.io.extAddr.expect(addr)
    arbiter.io.extDataOut.oe.expect(true.B)
    arbiter.io.extDataOut.data.expect(data)
  }
  // 外部Busへのアクセスがないことを確認する
  def expectExtRelease(arbiter: BusArbiter) = {
    arbiter.io.extDataOut.oe.expect(false.B)
  }

  /* test setup関連 */
  // 何も要求がない状態にセットアップする, 1cycle使用する
  def setupIdleState(arbiter: BusArbiter) = {
    for (i <- 0 until arbiter.n) {
      release(arbiter.io.slavePorts(i))
    }
    arbiter.clock.step(1)
    for (i <- 0 until arbiter.n) {
      expectExtRelease(arbiter)
      expectNoResp(arbiter.io.slavePorts(i))
    }
  }

  /* test用外付けメモリの挙動 */
  // arbiterの現在の状態をもとにextMemをRead/Writeする。戻り値はReadした場合にその値が入る(extDataInには設定済)
  def simExtMem(arbiter: BusArbiter, extMem: Array[UInt]): Option[UInt] = {
    val addr        = arbiter.io.extAddr.peek()
    val writeEnable = arbiter.io.writeEnable.peek()

    if (writeEnable.litToBoolean) {
      // Write
      arbiter.io.extDataOut.oe.expect(true.B) // Dataは有効でTriStateにはなっていないはず
      val data = arbiter.io.extDataOut.data.peek()
      extMem(addr.litValue.toInt) = data

      // 読み出しデータはなし
      None
    } else {
      // Read
      val data = extMem(addr.litValue.toInt)
      setExtDataIn(arbiter, data)

      // 読みだしたデータを返しておく
      Some(data)
    }
  }
  // 特に差し支えなければ dut.n を見たほうが良い
  val defaultBusMasterNum = 4
  val addressRange        = 0x10000; // 16bit

  "Verify that they can be accessed and read individually" in {
    test(new BusArbiter(defaultBusMasterNum)) { dut =>
      {
        // 全release
        setupIdleState(dut)

        // 各portごとにRead確認
        for (i <- 0 until dut.n) {
          val p = dut.io.slavePorts(i)
          // Read(addr:0xabcd) -> 0xef
          reqRead(p, 0xabcd.U)
          dut.clock.step(1)
          expectExtRead(dut, 0xabcd.U)
          setExtDataIn(dut, 0xef.U)

          expectReadResp(p, 0xef.U)

          // Read(addr:0x1234) -> 0x56
          reqRead(p, 0x1234.U)
          dut.clock.step(1)
          expectExtRead(dut, 0x1234.U)
          setExtDataIn(dut, 0x56.U)

          expectReadResp(p, 0x56.U)
          // Nop -> No resp
          release(p)
          dut.clock.step(1)
          expectExtRelease(dut)
          expectNoResp(p)
        }

      }
    }
  }

  "Verify that they can be accessed and write individually" in {
    test(new BusArbiter(defaultBusMasterNum)) { dut =>
      {
        // 全release
        setupIdleState(dut)

        // 各portごとにWrite確認
        for (i <- 0 until dut.n) {
          val p = dut.io.slavePorts(i)
          // Write(addr:0xabcd) -> 0xef
          reqWrite(p, 0xabcd.U, 0xef.U)
          dut.clock.step(1)
          expectExtWrite(dut, 0xabcd.U, 0xef.U)

          // Write(addr:0x1234) -> 0x56
          reqWrite(p, 0x1234.U, 0x56.U)
          dut.clock.step(1)
          expectExtWrite(dut, 0x1234.U, 0x56.U)

          // Nop -> No resp
          release(p)
          dut.clock.step(1)
          expectExtRelease(dut)
          expectNoResp(p)
        }

      }
    }
  }

  "Check for writing from port 0 and reading from port 1" in {
    test(new BusArbiter(2)) { dut =>
      {
        // 2port使う
        val writePort = dut.io.slavePorts(0)
        val readPort  = dut.io.slavePorts(1)

        // 全release
        setupIdleState(dut)

        // def外部の外付けメモリ
        val testMem = Array.fill(addressRange) { 0x00.U }

        val addr = 0xabcd.U;
        val data = 0xef.U;

        // Write
        reqWrite(writePort, addr, data)
        dut.clock.step(1)
        simExtMem(dut, testMem)
        release(writePort)

        // Read
        reqRead(readPort, addr)
        dut.clock.step(1)
        simExtMem(dut, testMem)
        expectReadResp(readPort, data)
        release(readPort)

      }
    }
  }

  "Check for multiple writing from port 0 and multiple reading from port 1" in {
    test(new BusArbiter(2)) { dut =>
      {
        // 2port使う
        val writePort = dut.io.slavePorts(0)
        val readPort  = dut.io.slavePorts(1)

        // 全release
        setupIdleState(dut)

        // def外部の外付けメモリ
        val testMem = Array.fill(addressRange) { 0x00.U }

        // Write
        for (i <- 0 until addressRange) {
          val addr = i.U
          val data = (i & 0xff).U
          reqWrite(writePort, addr, data)
          dut.clock.step(1)
          simExtMem(dut, testMem)
          release(writePort)
        }

        // Read
        for (i <- 0 until addressRange) {
          val addr = (addressRange - i - 1).U // 逆順
          val data = ((addressRange - i - 1) & 0xff).U
          reqRead(readPort, addr)
          dut.clock.step(1)
          simExtMem(dut, testMem)
          expectReadResp(readPort, data)
          release(readPort)
        }
      }
    }
  }

  "Check if writing from port 0 and reading from port 1 are alternately performed" in {
    test(new BusArbiter(2)) { dut =>
      {
        // 2port使う
        val writePort = dut.io.slavePorts(0)
        val readPort  = dut.io.slavePorts(1)

        // 全release
        setupIdleState(dut)

        // def外部の外付けメモリ
        val testMem = Array.fill(addressRange) { 0x00.U }

        // Write
        for (i <- 0 until addressRange) {
          val addr = i.U
          val data = (i & 0xff).U

          // 同時に要求を出して、優先度の低いport1があとから読み出すことを確認する
          // |T| port0      | port1     |
          // |0| release    | release   |
          // |1| Write Req  | Read Req  |
          // |2| Write Done |    ||     |
          // |3| release    | Read Done | (T=1 に戻る)

          // T1
          reqWrite(writePort, addr, data)
          reqRead(readPort, addr)
          dut.clock.step(1)
          simExtMem(dut, testMem)
          expectNoResp(readPort) // port0=Write優先で処理されていない

          // T2
          release(writePort)
          dut.clock.step(1)
          simExtMem(dut, testMem)
          expectNoResp(writePort)
          expectReadResp(readPort, data) // port1=Read応答が帰ってくる
        }
      }
    }
  }
  // TODO: 調停されるテストケースの追加
}
