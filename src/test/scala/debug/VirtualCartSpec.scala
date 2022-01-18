// See README.md for license details.

package debug

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class NesFileSpec extends AnyFreeSpec {
  "Can correctly identify NES headers" in {
    NesFile.isValidHeader(Array[Byte](0x4e, 0x45, 0x53, 0x1a))
  }
  "Can detect erroneous NES headers" in {
    NesFile.isValidHeader(Array[Byte](0x4f, 0x45, 0x53, 0x1a))
  }
  "Can correctly parse iNES File headers" in {
    val srcData = Array[Byte](
      // header
      0x4e, 0x45, 0x53, 0x1a, //   - 0~3   : identify "0x4e, 0x45, 0x53, 0x1a"
      0x2,                    //   - 4     : PRG ROM Bank数
      0x1,                    //   - 5     : CHR ROM Bank数
      0x07, 0x00,             //   - 6,7   : config (mirror=Vertical, Battery-Back RAMあり, trainerあり, Mapper0)
      0x0,                    //   - 8     : PRG-RAM Bank数
      0x0,                    //   - 9     : TV System
      0x0,                    //   - 10    : TV System PRG-RAM
      0x0, 0x0, 0x0, 0x0, 0x0 //   - 11~15 : reserved

    )
    val nesFile = new NesFile(srcData)

    assert(nesFile.prgRomSize == (0x2 * 0x4000))
    assert(nesFile.chrRomSize == (0x1 * 0x2000))
    assert(nesFile.nameTableMirror == NameTableMirror.Vertical)
    assert(nesFile.isExistsBatteryBackedRam)
    assert(nesFile.trainerSize == 512)
    assert(nesFile.mapper == 0)
    assert(nesFile.trainerOffset == 16)
    assert(nesFile.prgRomOffset == 16 + 512)
    assert(nesFile.chrRomOffset == 16 + 512 + (0x2 * 0x4000))
  }
}
class VirtualCartSpec extends AnyFreeSpec with ChiselScalatestTester {
//   "TODO" in {
//     test(new VirtualCart("")) { dut => }
//   }
}
