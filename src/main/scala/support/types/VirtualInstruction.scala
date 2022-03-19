package support.types

import chisel3._
import support.types.DebugAccessDataKind
import chisel3.util.Cat

/**
  * VIRに指定されたデータを解釈した型
  * @param isValid 正しくParseできていればtrue
  * @param dataKind 操作するデータ対象
  * @param write DebugAccessPortに対しReadする場合はfalse,Writeする場合はtrue
  * @param baseAddr 操作するデータのベースアドレス。この値はShift-DRを1byteすすめるたびにオートインクリメントする
  */
case class VirtualInstruction(isValid: Bool, isWrite: Bool, dataKind: DebugAccessDataKind.Type, baseAddr: UInt)

object VirtualInstruction {

  /**
      * VIRの値を解釈した値を返します
      * VIRは24bit設定想定で、中身のフォーマットは { baseAddr[15:8], baseAddr[7:0], isWrite[1], dataKind[6:0] } とする
      * 
      * @param vir USER1命令で設定されたIRの値
      * @return 解釈した命令
      */
  def parse(vir: UInt): VirtualInstruction = {
    val addr            = vir(23, 8)
    val isWrite         = vir(7)
    val (inst, isValid) = DebugAccessDataKind.safe(vir(6, 0))

    VirtualInstruction(isValid, isWrite, inst, addr)
  }
}
