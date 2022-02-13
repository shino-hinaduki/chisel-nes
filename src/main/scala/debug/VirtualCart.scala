package debug

import chisel3._

/**
 * Mapper機能をサポートしない、単純なカードリッジの模倣IP
 * @param initNesFilePath 初期値として読み込む.NES File
 */
class VirtualCart(val initNesFilePath: String) extends Module {
  val io = IO(new Bundle {
    // TODO: 定義
  })

  // val ExpansionAreaRamBaseOffset = 0x4018;
  // val ExpansionAreaRamSize       = 0x2000 - 0x18;
  // val ExpansionAreaRam           = SyncReadMem(ExpansionAreaRamSize, UInt(8.W))

  // val BatteryBackedRamBaseOffset = 0x6000;
  // val BatteryBackedRamSize       = 0x2000;
  // val BatteryBackedRam           = SyncReadMem(BatteryBackedRamSize, UInt(8.W))

  // val PrgRomBaseOffset = 0x8000;
  // val PrgRomSize       = 0x8000;
  // val PrgRom           = SyncReadMem(PrgRomSize, UInt(8.W))

  // .NES Fileを初期値としてロード
  val nesFile = new NesFile(initNesFilePath)

}
