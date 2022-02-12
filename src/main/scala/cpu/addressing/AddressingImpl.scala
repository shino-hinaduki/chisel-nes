package cpu.addressing

import chisel3._
import _root_.cpu.types.Addressing
import cpu.register.CpuRegister

/**
 * AddressingModeごとのOperandFetchするときの挙動を定義する
 */
trait AddressingImpl {

  /**
   * 対象のAddressingModeを返す
   */
  def addressing: Addressing.Type

  /**
    * Fetch要求を受けたときの処理
    * @param opcodeAddr OpCodeが配置されていたアドレスUInt(16.W)
    * @param reqReadData Dataの読み出しを要求されていればtrue
    * @param reg CPUレジスタの値
    * @return 次の処理
    */
  def onRequest(opcodeAddr: UInt, reqReadData: Boolean, reg: CpuRegister): Process

  /**
    * OP直後のデータを読みだしたあとの処理
    *
    * @param opcodeAddr OpCodeが配置されていたアドレスUInt(16.W)
    * @param reqReadData Dataの読み出しを要求されていればtrue
    * @param readAddr 最後に読みだしたアドレス
    * @param readData Readしたデータ。複数byte Readした場合は結合済
    * @param reg CPUレジスタの値
    * @return 次の処理
    */
  def doneReadOperand(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process

  /**
    * (Indirect限定) 解決先アドレス算出に必要なReadが完了したときの処理
    *
    * @param opcodeAddr OpCodeが配置されていたアドレスUInt(16.W)
    * @param reqReadData Dataの読み出しを要求されていればtrue
    * @param readAddr 最後に読みだしたアドレス
    * @param readData Readしたデータ。複数byte Readした場合は結合済
    * @param reg CPUレジスタの値
    * @return 次の処理
    */
  def doneReadPointer(opcodeAddr: UInt, reqReadData: Boolean, readAddr: UInt, readData: UInt, reg: CpuRegister): Process

  /**
    * データを読みだしたあとの処理
    *
    * @param opcodeAddr OpCodeが配置されていたアドレスUInt(16.W)
    * @param readAddr 最後に読みだしたアドレス
    * @param readData Readしたデータ。複数byte Readした場合は結合済
    * @param reg CPUレジスタの値
    * @return 次の処理
    */
  def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, reg: CpuRegister): Process
}
