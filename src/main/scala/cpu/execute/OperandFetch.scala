package cpu.execute

import chisel3._
import chisel3.experimental.ChiselEnum

import bus.BusSlavePort
import cpu.types.Addressing

/** OperandFetch状況を示します
 */
object OperandFetchStatus extends ChiselEnum {
  val idle, read = Value
}

/** OperandFetchする機能を提供する, 使う側はFlippedして使う
  */
class OperandFetchControl extends Bundle {
  // 立ち上がり変化で要求する, busyが解除されるまで入力データ保持が必要
  val reqStrobe = Input(Bool())
  // 命令が配置されていたアドレス
  val opcodeAddr = Input(UInt(16.W))
  // Fetchした結果を破棄する場合はtrue
  val discard = Input(Bool())
  // Decodeした命令のアドレッシング方式
  val addressing = Input(Addressing())
  // アドレスを求めるだけであればfalse(メモリ転送系の命令など)、それ以外はtrue
  val reqReadData = Input(Bool())
  // X reg。そのまま見せれば良い
  val regX = Input(UInt(8.W))
  // Y reg。そのまま見せれば良い
  val regY = Input(UInt(8.W))

  // 処理中であればtrue, この状態ではreqStrobeを受け付けない
  val busy = Output(Bool())
  // 処理完了後、有効なデータになっていればtrue
  val valid = Output(Bool())
  // 対象のアドレス
  val dstAddr = Output(UInt(16.W))
  // 読みだした結果。reqReadDataがfalseの場合はDon't care
  val readData = Output(UInt(16.W))
  // A regを参照してほしい場合はtrue(そもそもOperandFetchに投げる必要ない)
  val referA = Output(Bool())
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
  val statusReg = RegInit(OperandFetchStatus(), OperandFetchStatus.idle)
  val readCount = RegInit(UInt(3.W), 0.U) // 最大でIndirectIndexedでデータを読み出すケースで4回
  // 制御入力
  val prevReqStrobeReg = RegInit(Bool(), false.B)
  // BusMaster関連
  val readReqAddrReg = RegInit(UInt(16.W), 0.U)
  // 結果出力
  val validReg    = RegInit(Bool(), false.B)
  val dstAddrReg  = RegInit(UInt(16.W), 0.U)
  val readDataReg = RegInit(UInt(16.W), 0.U)
  val referAReg   = RegInit(Bool(), false.B)

  // internal
  io.status := statusReg
  // BusMaster -> BusArbiterSlavePort
  io.busMaster.addr        := readReqAddrReg
  io.busMaster.req         := statusReg === OperandFetchStatus.read // status=Readであれば処理し続ける
  io.busMaster.writeEnable := false.B                               // Read Only
  io.busMaster.dataIn      := DontCare                              // Writeすることはない
  // EXにはそのまま結果を見せる
  io.control.busy     := statusReg === OperandFetchStatus.read
  io.control.valid    := validReg
  io.control.dstAddr  := dstAddrReg
  io.control.readData := readDataReg
  io.control.referA   := referAReg

  // Req立ち上がり検出用
  val onRequest = (!prevReqStrobeReg) & io.control.reqStrobe // 今回の立ち上がりで判断させる
  prevReqStrobeReg := io.control.reqStrobe
}
