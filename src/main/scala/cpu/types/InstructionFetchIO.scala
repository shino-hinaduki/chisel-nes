package cpu.types

import chisel3._
import chisel3.experimental.ChiselEnum

/** IFがPrefetchし(てDecodeし)た命令を提供する, 使う側はFlippedして使う
  */
class InstructionFetchIO extends Bundle {
  // 立ち上がり変化で要求する, busyが解除されるまで入力データ保持が必要
  val reqStrobe = Input(Bool())
  // ProgramCounterの値をそのまま見せる
  val pc = Input(UInt(16.W))
  // Fetchした結果を破棄する場合はtrue
  val discard = Input(Bool())

  // read処理中であればtrue, この状態ではreqStrobeを受け付けない
  val busy = Output(Bool())
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

/** Fetch状況を示します
 */
object InstructionFetchStatus extends ChiselEnum {
  val idle, read = Value
}
