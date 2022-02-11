package logic

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class ShiftRegisterSpec extends AnyFreeSpec with ChiselScalatestTester {
  val testDatas = Array(0x00, 0xff, 0xaa, 0x55, 0xa5, 0x5a, 0xab, 0xcd, 0xef)

  testDatas foreach { testData =>
    {
      f"Serial input test [$testData%02x]" in {
        test(new ShiftRegister) { dut =>
          {
            // 8cycleかけてデータ入力
            for (i <- 0 until 8) {
              dut.io.pi.poke(0x00.U)
              dut.io.si.poke((((testData >> (7 - i)) & 0x1) == 0x1).B)
              dut.io.nSerial.poke(false.B)
              dut.clock.step(1)
            }
            // 現時点でtestDatas(7)が見えているはずなので今回1cyc+7cycでデータを確認
            for (i <- 0 until 8) {
              dut.io.q7.expect((((testData >> (7 - i)) & 0x1) == 0x1).B)
              dut.io.nSerial.poke(false.B)
              dut.clock.step(1)
            }
          }
        }
      }
      f"Parallel input test [$testData%02x]" in {
        test(new ShiftRegister) { dut =>
          {
            Array(1, 2, 5, 10) foreach { inputCycles =>
              {
                // inputCyclesかけてデータ入力
                dut.io.pi.poke(testData.U)
                dut.io.nSerial.poke(true.B)
                dut.clock.step(inputCycles) // nSerial=trueの限りシフトは発生しないはず

                // 現時点でtestDatas(7)が見えているはずなので今回1cyc+7cycでデータを確認
                for (i <- 0 until 8) {
                  dut.io.q7.expect((((testData >> (7 - i)) & 0x1) == 0x1).B)
                  dut.io.nSerial.poke(false.B)
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
