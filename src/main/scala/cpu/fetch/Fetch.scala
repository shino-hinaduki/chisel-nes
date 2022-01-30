package cpu.fetch

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.MuxLookup

import cpu.types.Addressing
import cpu.types.Instruction
import cpu.bus.BusSlavePort

/** IFがPrefetchし(てDecodeし)た命令を提供する, 使う側はFlippedして使う
  */
class FetchControlSlave extends Bundle {
  // Fetchが有効であればtrue, validになったあとreqをおろしても最後に取得した値を保持する
  val req = Input(Bool())
  // ProgramCounterの値をそのまま見せる
  val pc = Input(UInt(16.W))
  // Fetchした結果を破棄する場合はtrue, reqよりも優先される。 branch系命令でEX側から制御される想定
  val discardByEx = Input(Bool())
  // Fetchした結果を破棄する場合はtrue, reqよりも優先される。 割り込み時に使用する想定
  val discardByInt = Input(Bool())

  // 有効なデータであればtrue
  val valid = Output(Bool())
  // 命令が配置されていたアドレス
  val addr = Output(UInt(16.W))
  // 命令の生データ
  val data = Output(UInt(8.W))
  // Decodeした命令
  val instruction = Output(Instruction())
  // Decodeした命令のアドレッシング方式
  val addressing = Output(Addressing())

}

// Fetch状況を示します
object FetchStatus extends ChiselEnum {
  val idle, read, reset = Value

}

/** DataBusからの命令取得と、その内容をデコードしてRegisterに保持する役割を持つ
  */
class Fetch extends Module {
  val io = IO(new Bundle {
    // Addr/DataBusのArbiterと接続
    val busMaster = Flipped(new BusSlavePort())
    // EX,INTからFetch制御する用に公開するI/F
    val control = new FetchControlSlave()
  })

  // internal
  val statusReg = RegInit(FetchStatus(), FetchStatus.idle)
  // Read関連
  val readReqAddrReg = RegInit(UInt(16.W), 0.U)
  // EX,INTに見せる関連
  val validReg        = RegInit(Bool(), false.B)
  val readDoneAddrReg = RegInit(UInt(16.W), 0.U)
  val readDataReg     = RegInit(UInt(8.W), 0.U) // Decodeする前の生データ
  val instructionReg  = RegInit(Instruction(), Instruction.invalid)
  val addressingReg   = RegInit(Addressing(), Addressing.invalid)

  // BusMaster -> BusArbiterSlavePort
  io.busMaster.addr        := readReqAddrReg
  io.busMaster.req         := (statusReg == FetchStatus.read).B // status=Readであれば処理し続ける
  io.busMaster.writeEnable := false.B                           // Read Only
  io.busMaster.dataIn      := DontCare                          // Writeすることはない

  // IF -> EX,INTに見せる関連はレジスタそのまま公開する
  io.control.valid       := validReg
  io.control.addr        := readDoneAddrReg
  io.control.data        := readDataReg
  io.control.instruction := instructionReg
  io.control.addressing  := addressingReg

  // control入力で処理を分岐する
  when(io.control.discardByEx | io.control.discardByInt) {
    // reset: レジスタをクリア
    statusReg       := FetchStatus.idle
    readReqAddrReg  := DontCare
    validReg        := false.B
    readDoneAddrReg := 0.U
    readDataReg     := 0.U
    instructionReg  := Instruction.invalid
    addressingReg   := Addressing.invalid
  }.elsewhen(io.control.req) {
    // read
    statusReg := FetchStatus.read
    // すでに要求済で帰ってきていたら結果を保持
    when(io.busMaster.valid) {
      // Read完了
      validReg        := true.B
      readDoneAddrReg := readReqAddrReg // 前回要求していたアドレスで、現在のPCではない
      readDataReg     := io.busMaster.dataOut
      instructionReg  := MuxLookup(io.busMaster.dataOut, Instruction.invalid, Decode.lookUpTableForInstruction())
      addressingReg   := MuxLookup(io.busMaster.dataOut, Addressing.invalid, Decode.lookUpTableForAddressing())
    }.otherwise {
      // Read未完了
      validReg        := false.B // 他の要素はケア不要
      readDoneAddrReg := DontCare
      readDataReg     := DontCare
      instructionReg  := Instruction.invalid
      addressingReg   := Addressing.invalid
    }

    // 次の要求先も出しておく(statusReg=readで要求自体はアサートされる)
    readReqAddrReg := io.control.pc
  }.otherwise {
    // idle: 現状維持
    statusReg       := FetchStatus.idle
    readReqAddrReg  := readReqAddrReg
    validReg        := validReg
    readDoneAddrReg := readDoneAddrReg
    readDataReg     := readDataReg
    instructionReg  := instructionReg
    addressingReg   := addressingReg
  }
}
