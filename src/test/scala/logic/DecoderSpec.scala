package logic

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class DecoderSpec extends AnyFreeSpec with ChiselScalatestTester {
  case class TestPattern(nEn: Bool, b: Bool, a: Bool, expectY: UInt)
  val patterns = Array(
    // disable
    TestPattern(true.B, false.B, false.B, "b1111".U),
    TestPattern(true.B, false.B, true.B, "b1111".U),
    TestPattern(true.B, true.B, false.B, "b1111".U),
    TestPattern(true.B, true.B, true.B, "b1111".U),
    // enable
    TestPattern(false.B, false.B, false.B, "b1110".U),
    TestPattern(false.B, false.B, true.B, "b1101".U),
    TestPattern(false.B, true.B, false.B, "b1011".U),
    TestPattern(false.B, true.B, true.B, "b0111".U)
  )
  patterns foreach { p =>
    {
      s"Does it match the truth table [$p]" in {
        test(new Decoder) { dut =>
          {
            dut.io.nEn.poke(p.nEn)
            dut.io.b.poke(p.b)
            dut.io.a.poke(p.a)
            dut.clock.step(1)
            dut.io.y.expect(p.expectY)
          }
        }
      }
    }
  }
}
