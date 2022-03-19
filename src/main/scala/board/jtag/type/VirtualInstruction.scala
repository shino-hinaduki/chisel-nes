package board.jtag.types

import chisel3._
import support.types.DebugAccessDataKind
import chisel3.util.Cat

/**
  * Virtual JTAGで受付可能な命令の定義
  */
class VirtualInstruction extends Bundle {
  /* IR生の値 */
  val raw = UInt(24.W)
  /* 正しくParseできていればtrue */
  val isValid = Bool()
  /* 操作するデータ対象 */
  val isWrite = Bool()
  /* DebugAccessPortに対しReadする場合はfalse,Writeする場合はtrue */
  val dataKind = DebugAccessDataKind.Type()
  /* 操作するデータのベースアドレス。この値はShift-DRを1byteすすめるたびにオートインクリメントする */
  val baseAddr = UInt(16.W)
}
object VirtualInstruction {

  /**
      * VIRの値を解釈した値で更新します
      * VIRは24bit設定想定で、中身のフォーマットは { baseAddr[15:8], baseAddr[7:0], isWrite[1], dataKind[6:0] } とする
      * 
      * @param vir USER1命令で設定されたIRの値
      * @return 解釈した命令
      */
  def update(dst: VirtualInstruction, vir: UInt) = {
    val (dataKind, isValid) = DebugAccessDataKind.safe(vir(DebugAccessDataKind.getWidth - 1, 0))

    dst.raw      := vir
    dst.baseAddr := vir(23, 8)
    dst.isWrite  := vir(7)
    dst.dataKind := dataKind
    dst.isValid  := isValid
  }

  /**
    * 無効値を設定
    * @param dst 更新対象
    */
  def setInvalid(dst: VirtualInstruction) = {
    dst.raw      := 0.U(24.W)
    dst.baseAddr := 0.U(16.W)
    dst.isWrite  := false.B
    dst.dataKind := DebugAccessDataKind.invalid
    dst.isValid  := false.B
  }
}
