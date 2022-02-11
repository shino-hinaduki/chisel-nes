package cpu

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.MuxLookup
import chisel3.util.switch
import chisel3.util.is

import bus.BusIO
import cpu.types.Addressing
import cpu.types.Instruction
import cpu.types.InstructionFetchControl
import cpu.types.InstructionFetchStatus

/** DataBusからの命令取得と、その内容をデコードしてRegisterに保持する役割を持つ
  */
class InstructionFetch extends Module {
  val io = IO(new Bundle {
    // 現在のステータス確認用
    val status = Output(InstructionFetchStatus())
    // Addr/DataBusのArbiterと接続
    val busMaster = Flipped(new BusIO())
    // EX,INTからFetch制御する用に公開するI/F
    val control = new InstructionFetchControl()
  })

  // internal
  val statusReg = RegInit(InstructionFetchStatus(), InstructionFetchStatus.idle)
  // Read関連
  val readReqAddrReg = RegInit(UInt(16.W), 0.U)
  // EX,INTからの開始トリガはposedgeを起点にする
  val prevReqStrobeReg = RegInit(Bool(), false.B)
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
  io.busMaster.req         := statusReg === InstructionFetchStatus.read // status=Readであれば処理し続ける
  io.busMaster.writeEnable := false.B                                   // Read Only
  io.busMaster.dataIn      := DontCare                                  // Writeすることはない

  // IF -> EX,INTに見せる関連はレジスタそのまま公開する
  io.control.busy        := statusReg === InstructionFetchStatus.read
  io.control.valid       := validReg
  io.control.addr        := readDoneAddrReg
  io.control.data        := readDataReg
  io.control.instruction := instructionReg
  io.control.addressing  := addressingReg

  // Req立ち上がり検出用
  val onRequest = (!prevReqStrobeReg) & io.control.reqStrobe // 今回の立ち上がりで判断させる
  prevReqStrobeReg := io.control.reqStrobe

  switch(statusReg) {
    is(InstructionFetchStatus.idle) {
      // 出力レジスタクリア or 現状維持
      when(io.control.discard) {
        validReg        := false.B
        readDoneAddrReg := DontCare
        readDataReg     := DontCare
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
        statusReg      := InstructionFetchStatus.read // status=readでRead要求を出す
        readReqAddrReg := io.control.pc
      }.otherwise {
        statusReg      := InstructionFetchStatus.idle
        readReqAddrReg := readReqAddrReg
      }
    }
    is(InstructionFetchStatus.read) {
      when(io.control.discard) {
        // Read要求は現状維持, Read結果は回収しない
        statusReg      := InstructionFetchStatus.read
        readReqAddrReg := readReqAddrReg

        validReg        := false.B
        readDoneAddrReg := DontCare
        readDataReg     := DontCare
        instructionReg  := Instruction.invalid
        addressingReg   := Addressing.invalid
      }.elsewhen(io.busMaster.valid) {
        // read結果をレジスタに格納して、idle遷移
        statusReg      := InstructionFetchStatus.idle
        readReqAddrReg := 0.U

        validReg        := true.B
        readDoneAddrReg := readReqAddrReg // 前回要求していたアドレスで、現在のPCではない
        readDataReg     := io.busMaster.dataOut
        instructionReg  := MuxLookup(io.busMaster.dataOut, Instruction.invalid, InstructionDecode.lookUpTableForInstruction())
        addressingReg   := MuxLookup(io.busMaster.dataOut, Addressing.invalid, InstructionDecode.lookUpTableForAddressing())
      }.otherwise {
        // Read未完了, 現状維持
        statusReg      := InstructionFetchStatus.read
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
