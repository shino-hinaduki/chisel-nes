package joy

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class ShiftRegisterSpec extends AnyFreeSpec with ChiselScalatestTester {
  "Serial input test" in {
    test(new ShiftRegister) { dut =>
      {
        val testDatas = Array(0x00, 0xff, 0xaa, 0x55, 0xa5, 0x5a)
        testDatas foreach { testData =>
          {
            // 8cycleかけてデータ入力
            for (i <- 0 until 8) {
              dut.io.pi.poke(0x00.U)
              dut.io.si.poke((((testData >> i) & 0x1) == 0x1).B)
              dut.io.isParallel.poke(false.B)
              dut.clock.step(1)
            }
            // 現時点でtestDatas(0)が見えているはずなので今回1cyc+7cycでデータを確認
            for (i <- 0 until 8) {
              dut.io.q7.expect((((testData >> i) & 0x1) == 0x1).B)
              dut.io.isParallel.poke(false.B)
              dut.clock.step(1)
            }
          }
        }
      }
    }
  }
}
