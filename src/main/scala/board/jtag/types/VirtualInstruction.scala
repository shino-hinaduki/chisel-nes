package board.jtag.types

import chisel3._
import chisel3.util.Cat
import chisel3.experimental.ChiselEnum

/**
  * Virtual JTAGで受付可能な命令の定義
  */
class VirtualInstruction extends Bundle {
  /* IR生の値 */
  val raw = UInt(VirtualInstruction.totalWidth.W)
  /* 正しくParseできていればtrue */
  val isValid = Bool()
  /* 操作するデータ対象 */
  val isWrite = Bool()
  /* DebugAccessPortに対しReadする場合はfalse,Writeする場合はtrue */
  val accessTarget = VirtualInstruction.AccessTarget.Type()
  /* 操作するデータのベースアドレス。この値はShift-DRをすすめるたびにオートインクリメントする */
  val baseOffset = UInt(VirtualInstruction.baseOffsetWidth.W)
}

object VirtualInstruction {
  // IRのbit width
  val totalWidth = 24
  // Offset指定可能なビット幅
  val baseOffsetWidth = 16

  /**
  * DAPのアクセス対象
  */
  object AccessTarget extends ChiselEnum {
    val invalid      = Value(0x00.U)
    val info         = Value(0x01.U)
    val debug        = Value(0x02.U)
    val cpuBusMaster = Value(0x03.U)
    val ppuBusMaster = Value(0x04.U)
    val frameBuffer  = Value(0x10.U)
    val audio        = Value(0x20.U)
    val cartInfo     = Value(0x30.U)
    val cartPrg      = Value(0x31.U)
    val cartBatt     = Value(0x32.U)
    val cartChr      = Value(0x33.U)
    val readTest     = Value(0x7e.U)
    val invalid2     = Value(0x7f.U)
  }

  /**
    * BaseAddrをParseして取得する
    * @param vir Virtual IRの値
    * @return BaseAddr
    */
  def getBaseOffset(vir: UInt): UInt = vir(23, 8)

  /**
    * VIRをParseして取得する
    * @param vir Virtual IRの値
    * @return IsWrite
    */
  def getIsWrite(vir: UInt): Bool = vir(7)

  /**
    * VIRをParseしてaccessTargetとIsValidを取得する。IsValidはaccessTargetがInvalid指定だった場合でもfalseになる
    * @param vir Virtual IRの値
    * @return accessTarget, IsValid
    */
  def getaccessTargetAndIsValid(vir: UInt): (AccessTarget.Type, Bool) = {
    val (accessTarget, isValid) = AccessTarget.safe(vir(AccessTarget.getWidth - 1, 0))
    (accessTarget, isValid && (accessTarget =/= AccessTarget.invalid) && (accessTarget =/= AccessTarget.invalid2))
  }

  /**
    * VIRの値を解釈した値で更新します
    * VIRは24bit設定想定で、中身のフォーマットは { baseAddr[15:8], baseAddr[7:0], isWrite[1], accessTarget[6:0] } とする
    * 
    * @param vir USER1命令で設定されたIRの値
    * @return 解釈した命令
    */
  def parse(dst: VirtualInstruction, vir: UInt) = {
    val (accessTarget, isValid) = getaccessTargetAndIsValid(vir)

    dst.raw          := vir
    dst.baseOffset   := getBaseOffset(vir)
    dst.isWrite      := getIsWrite(vir)
    dst.accessTarget := accessTarget
    dst.isValid      := isValid
  }

  /**
    * 無効値を設定
    * @param dst 更新対象
    */
  def setInvalid(dst: VirtualInstruction) = {
    dst.raw          := 0.U(totalWidth.W)
    dst.baseOffset   := 0.U(baseOffsetWidth.W)
    dst.isWrite      := false.B
    dst.accessTarget := AccessTarget.invalid
    dst.isValid      := false.B
  }
}
