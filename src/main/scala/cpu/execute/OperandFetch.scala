package cpu.execute

import chisel3._
import chisel3.experimental.ChiselEnum

import bus.BusSlavePort
import cpu.types.Addressing
import chisel3.util.switch
import chisel3.util.is

/** OperandFetch状況を示します
 */
object OperandFetchStatus extends ChiselEnum {
  val idle, readRom, readRam = Value
}

/** OperandFetchする機能を提供する, 使う側はFlippedして使う
  */
class OperandFetchControl extends Bundle {
  // 立ち上がり変化で要求する, busyが解除されるまで入力データ保持が必要
  val reqStrobe = Input(Bool())
  // 命令が配置されていたアドレス
  val opcodeAddr = Input(UInt(16.W))
  // Decodeした命令のアドレッシング方式
  val addressing = Input(Addressing())
  // アドレスを求めるだけであればfalse(メモリ転送系の命令など)、それ以外はtrue
  val reqDataFetch = Input(Bool())
  // A reg。そのまま見せれば良い
  val a = Input(UInt(8.W))
  // X reg。そのまま見せれば良い
  val x = Input(UInt(8.W))
  // Y reg。そのまま見せれば良い
  val y = Input(UInt(8.W))

  // 処理中であればtrue, この状態ではreqStrobeを受け付けない
  val busy = Output(Bool())
  // 処理完了後、有効なデータになっていればtrue。reqStrobeから最短1cycで出力
  val valid = Output(Bool())
  // 対象のアドレス
  val dstAddr = Output(UInt(16.W))
  // 読みだした結果。reqReadDataがfalseの場合はDon't care
  val readData = Output(UInt(16.W))
  // dstAddrに有効なデータが入っていればtrue. A regを参照してほしい場合はfalse
  val dstAddrValid = Output(Bool())
  // readDataに有効なデータが入っていればtrue. Implied, Accumulate, MemWrite系の命令だとfalse
  val readDataValid = Output(Bool())
}

/** 指定されたAddressing modeに従ってデータを読み出します
  */
class OperandFetch extends Module {
  val io = IO(new Bundle {
    // 現在のステータス
    val status = Output(OperandFetchStatus())
    // Addr/Data BusMaster
    val busMaster = Flipped(new BusSlavePort())
    // OperandFetch制御用
    val control = new OperandFetchControl
  })

  // 内部
  val statusReg        = RegInit(OperandFetchStatus(), OperandFetchStatus.idle)
  val currentReadCount = RegInit(UInt(3.W), 0.U) // 最大でIndirectIndexedでデータを読み出すケースで4回
  val totalReadCount   = RegInit(UInt(3.W), 0.U) // 最大でIndirectIndexedでデータを読み出すケースで4回
  // 制御入力
  val prevReqStrobeReg = RegInit(Bool(), false.B)
  // BusMaster関連
  val reqReadReg     = RegInit(Bool(), false.B)
  val readReqAddrReg = RegInit(UInt(16.W), 0.U)
  // 結果出力
  val validReg         = RegInit(Bool(), false.B)
  val dstAddrReg       = RegInit(UInt(16.W), 0.U)
  val readDataReg      = RegInit(UInt(16.W), 0.U)
  val dstAddrValidReg  = RegInit(Bool(), false.B)
  val readDataValidReg = RegInit(Bool(), false.B)

  // internal
  io.status := statusReg
  // BusMaster -> BusArbiterSlavePort
  io.busMaster.addr        := readReqAddrReg
  io.busMaster.req         := reqReadReg
  io.busMaster.writeEnable := false.B  // Read Only
  io.busMaster.dataIn      := DontCare // Writeすることはない
  // EXにはそのまま結果を見せる
  io.control.busy          := statusReg =/= OperandFetchStatus.idle
  io.control.valid         := validReg
  io.control.dstAddr       := dstAddrReg
  io.control.readData      := readDataReg
  io.control.dstAddrValid  := dstAddrValidReg
  io.control.readDataValid := readDataValidReg

  // Req立ち上がり検出用
  val onRequest = (!prevReqStrobeReg) & io.control.reqStrobe // 今回の立ち上がりで判断させる
  prevReqStrobeReg := io.control.reqStrobe

  switch(statusReg) {
    is(OperandFetchStatus.idle) {
      // idle->read開始
      when(onRequest) {
        switch(io.control.addressing) {
          // 初期値に戻す, validRegも立てない
          is(Addressing.invalid) {
            statusReg := OperandFetchStatus.idle // 完了

            validReg         := false.B
            dstAddrReg       := 0.U
            readDataReg      := 0.U
            dstAddrValidReg  := false.B
            readDataValidReg := false.B

            reqReadReg       := false.B
            readReqAddrReg   := 0.U
            currentReadCount := 0.U
            totalReadCount   := 0.U
          }
          // 処理不要
          is(Addressing.implied) {
            statusReg := OperandFetchStatus.idle // 完了

            validReg         := true.B
            dstAddrReg       := 0.U
            readDataReg      := 0.U
            dstAddrValidReg  := false.B
            readDataValidReg := false.B

            reqReadReg       := false.B
            readReqAddrReg   := 0.U
            currentReadCount := 0.U
            totalReadCount   := 0.U
          }
          // データだけ載せて完了
          is(Addressing.accumulator) {
            statusReg := OperandFetchStatus.idle // 完了

            validReg         := true.B
            dstAddrReg       := DontCare
            readDataReg      := io.control.a
            dstAddrValidReg  := false.B
            readDataValidReg := true.B

            reqReadReg       := false.B
            readReqAddrReg   := 0.U
            currentReadCount := 0.U
            totalReadCount   := 0.U
          }
          // 1dataだけ読んで終わり
          is(Addressing.immediate) {
            statusReg := OperandFetchStatus.readRom

            validReg         := false.B
            dstAddrReg       := 0.U
            readDataReg      := 0.U
            dstAddrValidReg  := false.B
            readDataValidReg := false.B

            reqReadReg       := true.B
            readReqAddrReg   := (io.control.opcodeAddr + 1.U)
            currentReadCount := 1.U
            totalReadCount   := 1.U
          }
          // lower,upper
          is(Addressing.absolute) {
            statusReg := OperandFetchStatus.readRom

            validReg         := false.B
            dstAddrReg       := 0.U
            readDataReg      := 0.U
            dstAddrValidReg  := false.B
            readDataValidReg := false.B

            reqReadReg       := true.B
            readReqAddrReg   := (io.control.opcodeAddr + 1.U)
            currentReadCount := 1.U
            totalReadCount   := 2.U
          }
          // TODO: 他のアドレッシングモード
        }
      }.otherwise {
        // 現状維持
        statusReg := OperandFetchStatus.idle

        validReg         := validReg
        dstAddrReg       := dstAddrReg
        readDataReg      := readDataReg
        dstAddrValidReg  := dstAddrValidReg
        readDataValidReg := readDataValidReg

        reqReadReg       := false.B
        readReqAddrReg   := 0.U
        currentReadCount := 0.U
        totalReadCount   := 0.U
      }
    }
    is(OperandFetchStatus.readRom) {
      //TODO: totalReadCountまでよむ + reqDataFetch=trueなら継続してReadを仕掛ける
    }
    is(OperandFetchStatus.readRam) {
      //TODO: totalReadCountまで回収して完了する
    }
  }
}
