package joy

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class ShiftRegisterSpec extends AnyFreeSpec with ChiselScalatestTester {
  val testDatas = Array(0x00, 0xff, 0xaa, 0x55, 0xa5, 0x5a, 0xab, 0xcd, 0xef)

  "Serial input test" in {
    test(new ShiftRegister) { dut =>
      {
        testDatas foreach { testData =>
          {
            // 8cycleかけてデータ入力
            for (i <- 0 until 8) {
              dut.io.pi.poke(0x00.U)
              dut.io.si.poke((((testData >> (7 - i)) & 0x1) == 0x1).B)
              dut.io.isParallel.poke(false.B)
              dut.clock.step(1)
            }
            // 現時点でtestDatas(7)が見えているはずなので今回1cyc+7cycでデータを確認
            for (i <- 0 until 8) {
              dut.io.q7.expect((((testData >> (7 - i)) & 0x1) == 0x1).B)
              dut.io.isParallel.poke(false.B)
              dut.clock.step(1)
            }
          }
        }
      }
    }
  }

  "Parallel input test" in {
    test(new ShiftRegister) { dut =>
      {
        testDatas foreach { testData =>
          {
            Array(1, 2, 5, 10) foreach { inputCycles =>
              {
                // inputCyclesかけてデータ入力
                dut.io.pi.poke(testData.U)
                dut.io.isParallel.poke(true.B)
                dut.clock.step(inputCycles) // isParallel=trueの限りシフトは発生しないはず

                // 現時点でtestDatas(7)が見えているはずなので今回1cyc+7cycでデータを確認
                for (i <- 0 until 8) {
                  dut.io.q7.expect((((testData >> (7 - i)) & 0x1) == 0x1).B)
                  dut.io.isParallel.poke(false.B)
                  dut.clock.step(1)
                }
              }
            }
          }
        }
      }
    }
  }

}
