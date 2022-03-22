package support

import chisel3._
import chisel3.util.switch
import chisel3.util.is
import chisel3.util.Cat
import chisel3.experimental.ChiselEnum

import support.types.DebugAccessIO
import support.types.DebugAccessDataKind

/**
  * デバッグ向けRead専用レジスタマップへの読み書きを提供する
  */
class DebugAccessPort extends Module {
  // 初期値(無効値)
  val initialData = 0x88.U(8.W);

  val io = IO(new Bundle {
    val control = new DebugAccessIO
    // TODO: 必要なモジュールを接続
  })

  // 制御用 TODO: 各所で切り替える
  val busyReg = RegInit(Bool(), false.B)
  val doneReg = RegInit(Bool(), false.B)
  io.control.busy := busyReg
  io.control.done := doneReg

  // 読み出しデータ
  val readDataReg      = RegInit(UInt(8.W), initialData)
  val readDataValidReg = RegInit(Bool(), false.B)
  io.control.readData      := readDataReg
  io.control.readDataValid := readDataValidReg

  // 読み出すアドレスマップは上位ビットで決める
  when(io.control.dataKind === DebugAccessDataKind.invalid) {
    readDataReg      := DontCare
    readDataValidReg := false.B
  }.elsewhen(io.control.dataKind === DebugAccessDataKind.readTest) {
    // Read Test
    when(io.control.isWrite) {
      readDataReg      := DontCare
      readDataValidReg := false.B
    }.otherwise {
      readDataReg      := readTest(io.control.addr)
      readDataValidReg := true.B
    }
  }.elsewhen(io.control.dataKind === DebugAccessDataKind.info) {
    // Info
    when(io.control.isWrite) {
      readDataReg      := DontCare
      readDataValidReg := false.B
      // TODO: Write可能にするなら
    }.otherwise {
      readDataReg      := readInfo(io.control.addr)
      readDataValidReg := true.B
    }
  }.otherwise {
    // TODO: 他の実装
    readDataReg      := DontCare
    readDataValidReg := false.B
  }

  protected def readPpuBus(addr: UInt): UInt = {
    // TODO: BusArbiterを使ったRead
    addr
  }

  protected def readCpuBus(addr: UInt): UInt = {
    // TODO: BusArbiterを使ったRead
    addr
  }

  protected def readEmulateCart(addr: UInt): UInt = {
    // TODO: .NES から展開したデータの読み出し
    addr
  }

  protected def readScreen(addr: UInt): UInt = {
    // 2byte/pixelなので、pixel位置に変換
    val pixelIndex = addr >> 1.U
    // X位置は0~256
    val xIndex = pixelIndex(7, 0)
    // Y位置はXより上位で0~256
    val yIndex = pixelIndex(15, 8)

    // TODO: FrameBufferから読み出し
    pixelIndex
  }

  protected def readTest(addr: UInt): UInt = addr

  protected def readInfo(addr: UInt): UInt = if (!addr.isLit) { initialData } // TODO: 多分この書き方だとコンパイル時に消えている気がする
  else {
    addr.litValue.toInt match {
      // Identify
      case 0x0000 => 0xaa.U(8.W)
      case 0x0001 => 0x99.U(8.W)
      case 0x0002 => 0x55.U(8.W)
      case 0x0003 => 0x66.U(8.W)
      // CPU Reg
      case 0x0020 => 0x00.U(8.W) // TODO: PC(L)
      case 0x0021 => 0x00.U(8.W) // TODO: PC(H)
      case 0x0022 => 0x00.U(8.W) // TODO: A
      case 0x0023 => 0x00.U(8.W) // TODO: X
      case 0x0024 => 0x00.U(8.W) // TODO: Y
      case 0x0025 => 0x00.U(8.W) // TODO: SP
      case 0x0026 => 0x00.U(8.W) // TODO: P
      // APU/JoyPad/IO Reg
      case 0x0040 => 0x00.U(8.W) // TODO: 0x4000
      case 0x0041 => 0x00.U(8.W) // TODO: 0x4001
      case 0x0042 => 0x00.U(8.W) // TODO: 0x4002
      case 0x0043 => 0x00.U(8.W) // TODO: 0x4003
      case 0x0044 => 0x00.U(8.W) // TODO: 0x4004
      case 0x0045 => 0x00.U(8.W) // TODO: 0x4005
      case 0x0046 => 0x00.U(8.W) // TODO: 0x4006
      case 0x0047 => 0x00.U(8.W) // TODO: 0x4007
      case 0x0048 => 0x00.U(8.W) // TODO: 0x4008
      case 0x0049 => 0x00.U(8.W) // TODO: 0x4009
      case 0x004a => 0x00.U(8.W) // TODO: 0x400a
      case 0x004b => 0x00.U(8.W) // TODO: 0x400b
      case 0x004c => 0x00.U(8.W) // TODO: 0x400c
      case 0x004d => 0x00.U(8.W) // TODO: 0x400d
      case 0x004e => 0x00.U(8.W) // TODO: 0x400e
      case 0x004f => 0x00.U(8.W) // TODO: 0x400f
      case 0x00a0 => 0x00.U(8.W) // TODO: 0x4010
      case 0x00a1 => 0x00.U(8.W) // TODO: 0x4011
      case 0x00a2 => 0x00.U(8.W) // TODO: 0x4012
      case 0x00a3 => 0x00.U(8.W) // TODO: 0x4013
      case 0x00a4 => 0x00.U(8.W) // TODO: 0x4014
      case 0x00a5 => 0x00.U(8.W) // TODO: 0x4015
      case 0x00a6 => 0x00.U(8.W) // TODO: 0x4016
      case 0x00a7 => 0x00.U(8.W) // TODO: 0x4017
      case 0x00a8 => 0x00.U(8.W) // TODO: 0x4018
      case 0x00a9 => 0x00.U(8.W) // TODO: 0x4019
      case 0x00aa => 0x00.U(8.W) // TODO: 0x401a
      case 0x00ab => 0x00.U(8.W) // TODO: 0x401b
      case 0x00ac => 0x00.U(8.W) // TODO: 0x401c
      case 0x00ad => 0x00.U(8.W) // TODO: 0x401d
      case 0x00ae => 0x00.U(8.W) // TODO: 0x401e
      case 0x00af => 0x00.U(8.W) // TODO: 0x401f
      case _      => initialData
    }
  }
}
