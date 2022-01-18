package ram

import chisel3._

/** CPUからアクセス可能なWRAM
 * 0x0000 - 0x07ff WRAM
  */
class WorkRam extends Module {
  val io = IO(new Bundle {
    val addr          = Input(UInt(11.W))
    val dataIn        = Input(UInt(8.W))
    val dataOut       = Output(UInt(8.W))
    val nChipSelect   = Input(Bool())
    val nOutputEnable = Input(Bool())
    val nWriteEnable  = Input(Bool())
  })

  val ramSize = 0x0800;
  val ram     = SyncReadMem(ramSize, UInt(8.W))

  when(!io.nChipSelect) {
    // chip選択済み
    when(!io.nWriteEnable) {
      // RAMへ書き込み
      ram.write(io.addr, io.dataIn)
    }.otherwise {
      // 書き込み無効
      when(!io.nOutputEnable) {
        // RAMから読み出し
        io.dataOut := ram.read(io.addr)
      }.otherwise {
        // R/Wいずれも無効
        io.dataOut := DontCare
      }
    }
  }.otherwise {
    // Chipが選択されていない
    io.dataOut := DontCare
  }
}
