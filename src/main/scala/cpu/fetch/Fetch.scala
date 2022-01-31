package cpu.fetch

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.MuxLookup

import cpu.types.Addressing
import cpu.types.Instruction
import cpu.bus.BusSlavePort
import chisel3.util.switch
import chisel3.util.is

/** IFがPrefetchし(てDecodeし)た命令を提供する, 使う側はFlippedして使う
  */
class FetchControlSlave extends Bundle {
  // 立ち上がり変化で要求する, discardbyXXXがtrueならそちらを優先する
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
  val idle, read = Value

}

/** DataBusからの命令取得と、その内容をデコードしてRegisterに保持する役割を持つ
  */
class Fetch extends Module {
  val io = IO(new Bundle {
    // 現在のステータス確認用
    val status = Output(FetchStatus())
    // Addr/DataBusのArbiterと接続
    val busMaster = Flipped(new BusSlavePort())
    // EX,INTからFetch制御する用に公開するI/F
    val control = new FetchControlSlave()
  })

  // internal
  val statusReg = RegInit(FetchStatus(), FetchStatus.idle)
  // Read関連
  val readReqAddrReg = RegInit(UInt(16.W), 0.U)
  // EX,INTからの開始トリガはposedgeを起点にする
  val prevReqReg = RegInit(Bool(), false.B)
  // EX,INTに見せる関連
  val validReg        = RegInit(Bool(), false.B)
  val readDoneAddrReg = RegInit(UInt(16.W), 0.U)
  val readDataReg     = RegInit(UInt(8.W), 0.U) // Decodeする前の生データ
  val instructionReg  = RegInit(Instruction(), Instruction.invalid)
  val addressingReg   = RegInit(Addressing(), Addressing.invalid)

  // internal
  io.status := statusReg

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

  // Req立ち上がり検出用
  val onRequest = (!prevReqReg) | io.control.req // 今回の立ち上がりで判断させる
  prevReqReg := io.control.req

  switch(statusReg) {
    is(FetchStatus.idle) {
      // 出力レジスタクリア or 現状維持
      when(io.control.discardByEx | io.control.discardByInt) {
        validReg        := false.B
        readDoneAddrReg := 0.U
        readDataReg     := 0.U
        instructionReg  := Instruction.invalid
        addressingReg   := Addressing.invalid
      }.otherwise {
        validReg        := validReg
        readDoneAddrReg := readDoneAddrReg
        readDataReg     := readDataReg
        instructionReg  := instructionReg
        addressingReg   := addressingReg
      }

      // idle->read遷移。discardとは並行して処理可能
      when(onRequest) {
        statusReg      := FetchStatus.read // status=readでRead要求を出す
        readReqAddrReg := io.control.pc
      }.otherwise {
        statusReg      := FetchStatus.idle
        readReqAddrReg := readReqAddrReg
      }
    }
    is(FetchStatus.read) {
      when(io.control.discardByEx | io.control.discardByInt) {
        // Read要求は現状維持
        statusReg      := FetchStatus.read
        readReqAddrReg := readReqAddrReg

        validReg        := false.B
        readDoneAddrReg := 0.U
        readDataReg     := 0.U
        instructionReg  := Instruction.invalid
        addressingReg   := Addressing.invalid
      }.elsewhen(io.busMaster.valid) {
        // read結果をレジスタに格納して、idle遷移
        statusReg      := FetchStatus.idle
        readReqAddrReg := 0.U

        validReg        := true.B
        readDoneAddrReg := readReqAddrReg // 前回要求していたアドレスで、現在のPCではない
        readDataReg     := io.busMaster.dataOut
        instructionReg  := MuxLookup(io.busMaster.dataOut, Instruction.invalid, Decode.lookUpTableForInstruction())
        addressingReg   := MuxLookup(io.busMaster.dataOut, Addressing.invalid, Decode.lookUpTableForAddressing())
      }.otherwise {
        // Read未完了, 現状維持
        statusReg      := FetchStatus.read
        readReqAddrReg := readReqAddrReg

        validReg        := validReg
        readDoneAddrReg := readDoneAddrReg
        readDataReg     := readDataReg
        instructionReg  := instructionReg
        addressingReg   := addressingReg
      }
    }
  }

}
