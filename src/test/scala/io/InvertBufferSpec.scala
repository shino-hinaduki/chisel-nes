package io

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class InvertBufferSpec extends AnyFreeSpec with ChiselScalatestTester {
  case class TestInput(nEn0: Bool, nEn1: Bool, a: UInt)
  case class TestExpect(oe0: Bool, oe1: Bool, y0: UInt, y1: UInt) // z=trueの場合、値自体は関与しない

  val patterns = Array(
    // All 1
    (TestInput(true.B, true.B, 0x3f.U), TestExpect(false.B, false.B, 0xf.U, 0x3.U)),
    (TestInput(false.B, true.B, 0x3f.U), TestExpect(true.B, false.B, 0x0.U, 0x3.U)),
    (TestInput(true.B, false.B, 0x3f.U), TestExpect(false.B, true.B, 0xf.U, 0x0.U)),
    (TestInput(false.B, false.B, 0x3f.U), TestExpect(true.B, true.B, 0x0.U, 0x0.U)),
    // All 0
    (TestInput(true.B, true.B, 0x00.U), TestExpect(false.B, false.B, 0x0.U, 0x0.U)),
    (TestInput(false.B, true.B, 0x00.U), TestExpect(true.B, false.B, 0xf.U, 0x0.U)),
    (TestInput(true.B, false.B, 0x00.U), TestExpect(false.B, true.B, 0x0.U, 0x3.U)),
    (TestInput(false.B, false.B, 0x00.U), TestExpect(true.B, true.B, 0xf.U, 0x3.U)),
    // checker
    (TestInput(false.B, false.B, 0xaa.U), TestExpect(true.B, true.B, 0x5.U, 0x1.U)),
    (TestInput(false.B, false.B, 0x55.U), TestExpect(true.B, true.B, 0xa.U, 0x2.U))
  )
  patterns foreach {
    case (input, expect) => {
      s"Verify that the output can be Hi-Z [$input, $expect]" in {
        test(new InvertBuffer) { dut =>
          {

            dut.io.nEn0.poke(input.nEn0)
            dut.io.nEn1.poke(input.nEn1)
            dut.io.a.poke(input.a)
            dut.clock.step(1)

            // OEを確認
            dut.io.y0.oe.expect(expect.oe0)
            dut.io.y1.oe.expect(expect.oe1)
            // OE=trueなら、データも確認
            if (expect.oe0.litToBoolean) {
              dut.io.y0.data.expect(expect.y0)
            }
            if (expect.oe1.litToBoolean) {
              dut.io.y1.data.expect(expect.y1)
            }
          }
        }
      }
    }
  }
}
