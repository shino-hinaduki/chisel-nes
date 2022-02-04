package ppu

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class DFlipFlopSpec extends AnyFreeSpec with ChiselScalatestTester {
  val patterns = Array(0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef)
  patterns foreach { pattern =>
    {
      f"Let the input data be retained [$pattern%02x]" in {
        test(new DFlipFlop) { dut =>
          {
            // データ入力
            dut.io.d.poke(pattern.U)
            dut.io.nEn.poke(false.B)
            dut.clock.step(1)
            dut.io.q.expect(pattern.U)

            // Disable状態でClockを入れる
            dut.io.d.poke(0xff.U)
            dut.io.nEn.poke(true.B)
            dut.clock.step(1)
            dut.io.q.expect(pattern.U) // 前回から変わらず

          }
        }
      }
    }
  }
}
