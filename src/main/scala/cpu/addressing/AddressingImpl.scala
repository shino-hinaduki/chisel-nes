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
    * @param reqReadData Dataの読み出しを要求されていればtrue
    * @param opcodeAddr OpCodeが配置されていたアドレスUInt(16.W)
    * @param reg CPUレジスタの値
    * @return 次の処理
    */
  def onRequest(reqReadData: Boolean, opcodeAddr: UInt, reg: CpuRegister): Process

  /**
    * OP直後のデータを読みだしたあとの処理
    *
    * @param reqReadData Dataの読み出しを要求されていればtrue
    * @param opcodeAddr OpCodeが配置されていたアドレスUInt(16.W)
    * @param reg CPUレジスタの値
    * @param readData Readしたデータ。複数byte Readした場合は結合済
    * @return 次の処理
    */
  def doneReadOperand(reqReadData: Boolean, opcodeAddr: UInt, reg: CpuRegister, readData: UInt): Process

  /**
    * (Indirect限定) 解決先アドレス算出に必要なReadが完了したときの処理
    *
    * @param reqReadData Dataの読み出しを要求されていればtrue
    * @param opcodeAddr OpCodeが配置されていたアドレスUInt(16.W)
    * @param reg CPUレジスタの値
    * @param readData Readしたデータ。複数byte Readした場合は結合済
    * @return 次の処理
    */
  def doneReadPointer(reqReadData: Boolean, opcodeAddr: UInt, reg: CpuRegister, readData: UInt): Process

  /**
    * データを読みだしたあとの処理
    *
    * @param readAddr 最後に読みだしたアドレス
    * @param opcodeAddr OpCodeが配置されていたアドレスUInt(16.W)
    * @param reg CPUレジスタの値
    * @param readData Readしたデータ。複数byte Readした場合は結合済
    * @return 次の処理
    */
  def doneReadData(opcodeAddr: UInt, readAddr: UInt, readData: UInt, reg: CpuRegister): Process
}
