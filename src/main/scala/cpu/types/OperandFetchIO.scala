package cpu.types

import chisel3._
import chisel3.experimental.ChiselEnum

import _root_.bus.types.BusIO
import cpu.types.Addressing
import cpu.register.CpuRegister

/** OperandFetchする機能を提供する, 使う側はFlippedして使う
 */
class OperandFetchIO extends Bundle {
  // 立ち上がり変化で要求する, busyが解除されるまで入力データ保持が必要
  val reqStrobe = Input(Bool())
  // 命令が配置されていたアドレス
  val opcodeAddr = Input(UInt(16.W))
  // Decodeした命令のアドレッシング方式
  val addressing = Input(Addressing())
  // アドレスを求めるだけであればfalse(メモリ転送系の命令など)、それ以外はtrue
  val reqDataFetch = Input(Bool())
  // CPU reg。そのまま見せれば良い
  val cpuReg = Input(new CpuRegister())

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
