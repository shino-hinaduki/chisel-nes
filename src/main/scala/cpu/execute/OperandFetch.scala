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
  val statusReg           = RegInit(OperandFetchStatus(), OperandFetchStatus.idle)
  val currentReadCountReg = RegInit(UInt(3.W), 0.U) // 最大でIndirectIndexedでデータを読み出すケースで2+2+1回
  val totalReadCountReg   = RegInit(UInt(3.W), 0.U) // 最大でIndirectIndexedでデータを読み出すケースで2+2+1回
  // 制御入力
  val prevReqStrobeReg = RegInit(Bool(), false.B)
  // BusMaster関連
  val reqReadReg     = RegInit(Bool(), false.B)
  val readReqAddrReg = RegInit(UInt(16.W), 0.U)
  val readTmpReg0    = RegInit(UInt(8.W), 0.U) // Indirectなどで一時読みする場合に使用する, Resultと同じタイミングで初期化
  val readTmpReg1    = RegInit(UInt(8.W), 0.U) // Indirectなどで一時読みする場合に使用する, Resultと同じタイミングで初期化
  // 結果出力
  val validReg         = RegInit(Bool(), false.B)
  val dstAddrReg       = RegInit(UInt(16.W), 0.U)
  val readDataReg      = RegInit(UInt(8.W), 0.U)
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

  // 出力レジスタを初期化
  def clearResult(isValid: Bool) = {
    validReg         := isValid
    dstAddrReg       := 0.U
    readDataReg      := 0.U
    dstAddrValidReg  := false.B
    readDataValidReg := false.B
    readTmpReg0      := 0.U
    readTmpReg1      := 0.U
  }
  // 出力レジスタに結果を設定
  def setResult(dstAddr: Option[UInt], readData: Option[UInt]) = {
    validReg := true.B
    dstAddr match {
      // 有効データ
      case Some(addr) => {
        dstAddrReg      := addr
        dstAddrValidReg := true.B
      }
      // 無効データ
      case None => {
        dstAddrReg      := DontCare
        dstAddrValidReg := false.B
      }
    }
    readData match {
      // 有効データ
      case Some(data) => {
        readDataReg      := data
        readDataValidReg := true.B
      }
      // 無効データ
      case None => {
        readDataReg      := DontCare
        readDataValidReg := false.B
      }
    }
    // 結果を出すときに明示する必要はないので保持
    readTmpReg0 := readTmpReg0
    readTmpReg1 := readTmpReg1
  }
  // 出力レジスタの結果保持
  def keepResult() = {
    validReg         := validReg
    dstAddrReg       := dstAddrReg
    readDataReg      := readDataReg
    dstAddrValidReg  := dstAddrValidReg
    readDataValidReg := readDataValidReg
    readTmpReg0      := readTmpReg0
    readTmpReg1      := readTmpReg1
  }
  // BusMasterのRead要求をクリア
  def clearReadReq() = {
    reqReadReg          := false.B
    readReqAddrReg      := 0.U
    currentReadCountReg := 0.U
    totalReadCountReg   := 0.U
  }
  // BusMasterにRead要求を設定
  def setReadReq(addr: UInt, totalCount: Option[UInt]) = {
    reqReadReg     := true.B
    readReqAddrReg := addr
    totalCount match {
      // 新規Read
      case Some(count) => {
        currentReadCountReg := 1.U
        totalReadCountReg   := count
      }
      // 継続
      case None => {
        currentReadCountReg := currentReadCountReg + 1.U // increment
        totalReadCountReg   := totalReadCountReg         // 据え置き
      }
    }
  }
  // Read要求を保持
  def keepReadReq() = {
    reqReadReg          := reqReadReg
    readReqAddrReg      := readReqAddrReg
    currentReadCountReg := currentReadCountReg
    totalReadCountReg   := totalReadCountReg
  }

  // 処理本体はステータスで処理分岐
  switch(statusReg) {
    is(OperandFetchStatus.idle) {
      when(!onRequest) {
        // 現状維持
        statusReg := OperandFetchStatus.idle
        keepResult()
        clearReadReq() // read要求はいらないので念の為クリア
      }.otherwise {
        // idle->read開始
        switch(io.control.addressing) {
          // 初期値に戻す, validRegも立てない
          is(Addressing.invalid) {
            statusReg := OperandFetchStatus.idle // 完了
            clearResult(false.B)
            clearReadReq()
          }
          // 処理不要
          is(Addressing.implied) {
            statusReg := OperandFetchStatus.idle // 完了
            clearResult(true.B)
            clearReadReq()
          }
          // データだけ載せて完了
          is(Addressing.accumulator) {
            statusReg := OperandFetchStatus.idle // 完了
            setResult(None, Some(io.control.a))
            clearReadReq()
          }
          // OpCodeの次のデータが即値
          is(Addressing.immediate) {
            statusReg := OperandFetchStatus.readRom
            clearResult(false.B)
            setReadReq(io.control.opcodeAddr + 1.U, Some(1.U))
          }
          // lower,upper
          is(Addressing.absolute) {
            statusReg := OperandFetchStatus.readRom
            clearResult(false.B)
            setReadReq(io.control.opcodeAddr + 1.U, Some(2.U))
          }
          // lowerのみ
          is(Addressing.zeroPage) {
            statusReg := OperandFetchStatus.readRom
            clearResult(false.B)
            setReadReq(io.control.opcodeAddr + 1.U, Some(1.U))
          }
          // lowerのみ
          is(Addressing.xIndexedZeroPage) {
            statusReg := OperandFetchStatus.readRom
            clearResult(false.B)
            setReadReq(io.control.opcodeAddr + 1.U, Some(1.U))
          }
          // lowerのみ
          is(Addressing.yIndexedZeroPage) {
            statusReg := OperandFetchStatus.readRom
            clearResult(false.B)
            setReadReq(io.control.opcodeAddr + 1.U, Some(1.U))
          }
          // lower,upper
          is(Addressing.xIndexedAbsolute) {
            statusReg := OperandFetchStatus.readRom
            clearResult(false.B)
            setReadReq(io.control.opcodeAddr + 1.U, Some(2.U))
          }
          // lower,upper
          is(Addressing.yIndexedAbsolute) {
            statusReg := OperandFetchStatus.readRom
            clearResult(false.B)
            setReadReq(io.control.opcodeAddr + 1.U, Some(2.U))
          }
          // offsetのみ
          is(Addressing.relative) {
            statusReg := OperandFetchStatus.readRom
            clearResult(false.B)
            setReadReq(io.control.opcodeAddr + 1.U, Some(1.U))
          }
          // lower,upper
          is(Addressing.indirect) {
            statusReg := OperandFetchStatus.readRom
            clearResult(false.B)
            setReadReq(io.control.opcodeAddr + 1.U, Some(2.U))
          }
          // lowerのみ
          is(Addressing.xIndexedIndirect) {
            statusReg := OperandFetchStatus.readRom
            clearResult(false.B)
            setReadReq(io.control.opcodeAddr + 1.U, Some(1.U))
          }
          // lowerのみ
          is(Addressing.indirectYIndexed) {
            statusReg := OperandFetchStatus.readRom
            clearResult(false.B)
            setReadReq(io.control.opcodeAddr + 1.U, Some(1.U))
          }
        }
      }
    }
    is(OperandFetchStatus.readRom) {
      //TODO: totalReadCountRegまでよむ + reqDataFetch=trueなら継続してReadを仕掛ける
      when(!io.busMaster.valid) {
        // read完了まで現状維持
        keepResult()
        keepReadReq()
      }.otherwise {
        // 処理ごと、Read進捗ごとに分岐
        // TODO:
      }
    }
    is(OperandFetchStatus.readRam) {
      //TODO: totalReadCountRegまで回収して完了する
    }
  }
}
