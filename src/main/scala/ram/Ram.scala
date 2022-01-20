package ram

import chisel3._

/** 6116 5V 2K x 8 Asynchronous Static RAM
  * WRAM, VRAMがこれを使用している
  */
class Ram extends Module {
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
      io.dataOut := DontCare
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
