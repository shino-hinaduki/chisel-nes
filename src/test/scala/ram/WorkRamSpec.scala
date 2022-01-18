package ram

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class WorkRamSpec extends AnyFreeSpec with ChiselScalatestTester {

  "Read and confirm what you have written" in {
    test(new WorkRam()) { dut =>
      // write incremental pattern
      for (i <- 0 until 0x0800) {
        dut.io.addr.poke(i.U)
        dut.io.dataIn.poke((i & 0xff).U)
        dut.io.nChipSelect.poke(false.B)
        dut.io.nOutputEnable.poke(true.B)
        dut.io.nWriteEnable.poke(false.B)
        dut.clock.step(1)
      }
      // read incremental pattern
      for (i <- 0 until 0x0800) {
        dut.io.addr.poke(i.U)
        dut.io.dataIn.poke(0.U)
        dut.io.nChipSelect.poke(false.B)
        dut.io.nOutputEnable.poke(false.B)
        dut.io.nWriteEnable.poke(true.B)
        dut.clock.step(1)
        dut.io.dataOut.expect((i & 0xff).U)
      }
    }
  }
}
