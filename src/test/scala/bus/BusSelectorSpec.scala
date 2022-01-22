package bus

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class BusSelectorSpec extends AnyFreeSpec with ChiselScalatestTester {
  "Make sure it is selected for change of address" in {
    test(new BusSelector) { dut =>
      {
        case class TestInput(addr: UInt, phi2: Bool)
        case class TestExpect(nRamSel: Bool, nDataBusEnable: Bool, nRomSel: Bool)

        val patterns = Array(
          // disable
          (TestInput(0x0000.U, false.B), TestExpect(true.B, true.B, true.B)),
          (TestInput(0x1fff.U, false.B), TestExpect(true.B, true.B, true.B)),
          (TestInput(0x2000.U, false.B), TestExpect(true.B, true.B, true.B)),
          (TestInput(0x3fff.U, false.B), TestExpect(true.B, true.B, true.B)),
          (TestInput(0x4000.U, false.B), TestExpect(true.B, true.B, true.B)),
          (TestInput(0x5fff.U, false.B), TestExpect(true.B, true.B, true.B)),
          (TestInput(0x6000.U, false.B), TestExpect(true.B, true.B, true.B)),
          (TestInput(0x7fff.U, false.B), TestExpect(true.B, true.B, true.B)),
          (TestInput(0x8000.U, false.B), TestExpect(true.B, true.B, true.B)),
          (TestInput(0xffff.U, false.B), TestExpect(true.B, true.B, true.B)),
          // enable
          (TestInput(0x0000.U, true.B), TestExpect(false.B, true.B, true.B)),
          (TestInput(0x1fff.U, true.B), TestExpect(false.B, true.B, true.B)),
          (TestInput(0x2000.U, true.B), TestExpect(true.B, false.B, true.B)),
          (TestInput(0x3fff.U, true.B), TestExpect(true.B, false.B, true.B)),
          (TestInput(0x4000.U, true.B), TestExpect(true.B, true.B, true.B)),
          (TestInput(0x5fff.U, true.B), TestExpect(true.B, true.B, true.B)),
          (TestInput(0x6000.U, true.B), TestExpect(true.B, true.B, true.B)),
          (TestInput(0x7fff.U, true.B), TestExpect(true.B, true.B, true.B)),
          (TestInput(0x8000.U, true.B), TestExpect(true.B, true.B, false.B)),
          (TestInput(0xffff.U, true.B), TestExpect(true.B, true.B, false.B))
        )
        patterns foreach {
          case (input, expect) => {
            dut.io.phi2.poke(input.phi2)
            dut.io.a13.poke(input.addr(13))
            dut.io.a14.poke(input.addr(14))
            dut.io.a15.poke(input.addr(15))
            dut.clock.step(1)

            dut.io.nRamSel.expect(expect.nRamSel)               // 0x0000 ~
            dut.io.nDataBusEnable.expect(expect.nDataBusEnable) // 0x2000 ~
            dut.io.nRomSel.expect(expect.nRomSel)               // 0x8000 ~
          }
        }
      }
    }
  }
}
