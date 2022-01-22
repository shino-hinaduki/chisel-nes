package ram

import chisel3._

/** 6116 5V 2K x 8 Asynchronous Static RAM
  * WRAM, VRAMがこれを使用している
  * 
  * Truth Table
  * | Mode    | /CS | /OE | /WE | I/O    |
  * | Standby | H   | X   | X   | High-Z |
  * | Read    | L   | L   | H   | Dout   |
  * | Read    | L   | H   | H   | High-Z |
  * | Write   | L   | X   | L   | Din    |
  * 
  * /WE=LであればWrite Modeになっており、Write優先のロジックになっている
  * /WE=H, /CS=LであればReadだが、/OE次第でDout, High-Zが決定する
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

  val ramSize = 0x0800; // 2KiB
  val ram     = SyncReadMem(ramSize, UInt(8.W))

  when(!io.nChipSelect) {
    when(!io.nWriteEnable) {
      // | Write | /CS=L, /OE=X, /WE=L | Din |
      ram.write(io.addr, io.dataIn)
      io.dataOut := io.dataIn
    }.otherwise {
      when(!io.nOutputEnable) {
        // | Read | /CS=L, /OE=L, /WE=H | Dout |
        io.dataOut := ram.read(io.addr)
      }.otherwise {
        // | Read | /CS=L, /OE=H, /WE=H | High-Z |
        io.dataOut := DontCare
      }
    }
  }.otherwise {
    // | Standby | /CS=H, /OE=X, /WE=X | High-Z |
    io.dataOut := DontCare
  }
}
