package ram

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class RamSpec extends AnyFreeSpec with ChiselScalatestTester {
  "Read and confirm what you have written" in {
    test(new Ram()) { dut =>
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

  "Chip is not selected" in {
    test(new Ram()) { dut =>
      // write incremental pattern
      for (i <- 0 until 0x0800) {
        dut.io.addr.poke(i.U)
        dut.io.dataIn.poke((i & 0xff).U)
        dut.io.nChipSelect.poke(true.B)
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
        dut.io.dataOut.expect(0x00.U)
      }
    }
  }

  "Write is disabled" in {
    test(new Ram()) { dut =>
      // write incremental pattern
      for (i <- 0 until 0x0800) {
        dut.io.addr.poke(i.U)
        dut.io.dataIn.poke((i & 0xff).U)
        dut.io.nChipSelect.poke(false.B)
        dut.io.nOutputEnable.poke(true.B)
        dut.io.nWriteEnable.poke(true.B)
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
        dut.io.dataOut.expect(0x00.U)
      }
    }
  }

  "Read is disabled" in {
    test(new Ram()) { dut =>
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
        dut.io.nOutputEnable.poke(true.B)
        dut.io.nWriteEnable.poke(true.B)
        dut.clock.step(1)
        dut.io.dataOut.expect(0x00.U)
      }
    }
  }

  "Writing in stripes" in {
    test(new Ram()) { dut =>
      // write stripes pattern
      for (i <- 0 until 0x0800) {
        dut.io.addr.poke(i.U)
        dut.io.dataIn.poke((i & 0xff).U)
        dut.io.nChipSelect.poke((i % 2 == 0).B)
        dut.io.nOutputEnable.poke(true.B)
        dut.io.nWriteEnable.poke(false.B)
        dut.clock.step(1)
      }
      for (i <- 0 until 0x0800) {
        dut.io.addr.poke(i.U)
        dut.io.dataIn.poke((0xff - (i & 0xff)).U)
        dut.io.nChipSelect.poke(false.B)
        dut.io.nOutputEnable.poke(true.B)
        dut.io.nWriteEnable.poke((i % 2 == 1).B)
        dut.clock.step(1)
      }
      // read decremental pattern
      for (j <- 0 until 0x0800) {
        val i = 0x0800 - j - 1
        dut.io.addr.poke(i.U)
        dut.io.dataIn.poke(0.U)
        dut.io.nChipSelect.poke(false.B)
        dut.io.nOutputEnable.poke(false.B)
        dut.io.nWriteEnable.poke(true.B)
        dut.clock.step(1)
        dut.io.dataOut.expect(if (i % 2 == 1) (i & 0xff).U else (0xff - (i & 0xff)).U)
      }
    }
  }
}
