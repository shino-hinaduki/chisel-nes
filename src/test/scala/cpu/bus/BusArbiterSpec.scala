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

  // 特に差し支えなければ dut.n を見たほうが良い
  val defaultBusMasterNum = 4

  "Verify that they can be accessed and read individually" in {
    test(new BusArbiter(defaultBusMasterNum)) { dut =>
      {
        // 全release
        for (i <- 0 until dut.n) {
          release(dut.io.slavePorts(i))
        }
        dut.clock.step(1)
        for (i <- 0 until dut.n) {
          expectExtRelease(dut)
          expectNoResp(dut.io.slavePorts(i))
        }

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
        for (i <- 0 until dut.n) {
          release(dut.io.slavePorts(i))
        }
        dut.clock.step(1)
        for (i <- 0 until dut.n) {
          expectExtRelease(dut)
          expectNoResp(dut.io.slavePorts(i))
        }

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
}
