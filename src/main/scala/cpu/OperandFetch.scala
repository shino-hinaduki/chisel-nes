package cpu

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.switch
import chisel3.util.is
import chisel3.util.Cat

import _root_.bus.types.BusIO
import cpu.types.Addressing
import cpu.types.OperandFetchIO
import cpu.register.IndexRegister
import cpu.addressing._
import cpu.addressing.Process._

/** OperandFetch状況を示します
 */
object OperandFetchStatus extends ChiselEnum {
  // idle        : 処理なし
  // readOperand : OpCode後のデータを読み出し中
  // readPointer : (Indirect系のみ) アドレス取得のためのRAM Read
  // readData    : (redDataFetch=true時のみ、かつImmediate/Accumulate以外) データ取得のためのRAM Read
  val idle, readOperand, readPointer, readData = Value
}

/**
 * 指定されたAddressing modeに従ってデータを読み出します
 * @param resetOnPanic 想定していない挙動に陥ったときに放置せずにOFをリセットする
 */
class OperandFetch(resetOnPanic: Boolean) extends Module {
  val io = IO(new Bundle {
    // 現在のステータス
    val status = Output(OperandFetchStatus())
    // Addr/Data BusMaster
    val busMaster = Flipped(new BusIO())
    // OperandFetch制御用
    val control = new OperandFetchIO
  })

  // 内部
  val statusReg  = RegInit(OperandFetchStatus(), OperandFetchStatus.idle)
  val illegalReg = RegInit(Bool(), false.B)
  // 制御入力
  val prevReqStrobeReg = RegInit(Bool(), false.B)
  // BusMaster関連
  val reqReadReg     = RegInit(Bool(), false.B)
  val readReqAddrReg = RegInit(UInt(16.W), 0.U)
  // Read管理
  val burstReadLimit      = 2                                                    // Read要求の連続要求数
  val currentReadCountReg = RegInit(UInt(burstReadLimit.W), 0.U)                 // High/Lowで2回
  val totalReadCountReg   = RegInit(UInt(burstReadLimit.W), 0.U)                 // High/Lowで2回
  val readTmpRegs         = Seq.fill(burstReadLimit) { RegInit(UInt(8.W), 0.U) } // Indirectなどで一時読みする場合に使用する, Resultと同じタイミングで初期化
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

  /* 出力レジスタ関連 */
  // 出力レジスタを初期化
  protected def clearResult(isValid: Bool) = {
    validReg         := isValid
    dstAddrReg       := 0.U
    readDataReg      := 0.U
    dstAddrValidReg  := false.B
    readDataValidReg := false.B
    readTmpRegs foreach { _ := 0.U }
  }
  // 出力レジスタに結果を設定
  protected def setResult(dstAddr: Option[UInt], readData: Option[UInt]) = {
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
    readTmpRegs foreach { r => r := r }
  }
  // 出力レジスタの結果保持
  protected def keepResult() = {
    validReg         := validReg
    dstAddrReg       := dstAddrReg
    readDataReg      := readDataReg
    dstAddrValidReg  := dstAddrValidReg
    readDataValidReg := readDataValidReg
    readTmpRegs foreach { r => r := r }
  }

  /* Read関連 */
  // BusMasterのRead要求をクリア
  protected def clearReadReq() = {
    reqReadReg          := false.B
    readReqAddrReg      := 0.U
    currentReadCountReg := 0.U
    totalReadCountReg   := 0.U
  }
  // BusMasterにRead要求を設定
  protected def setReadReq(addr: UInt, totalCount: UInt) = {
    reqReadReg          := true.B
    readReqAddrReg      := addr
    currentReadCountReg := 1.U
    totalReadCountReg   := totalCount
  }
  // カウントはクリアせずに次のReadを要求
  protected def continueReadReq(addr: UInt) = {
    assert(currentReadCountReg < totalReadCountReg) // 超えることはないはず

    reqReadReg          := true.B
    readReqAddrReg      := addr
    currentReadCountReg := currentReadCountReg + 1.U // increment
    totalReadCountReg   := totalReadCountReg         // 据え置き
  }
  // Read要求を保持
  protected def keepReadReq() = {
    reqReadReg          := reqReadReg
    readReqAddrReg      := readReqAddrReg
    currentReadCountReg := currentReadCountReg
    totalReadCountReg   := totalReadCountReg
  }
  // status, 出力レジスタ、Read要求ともにクリアする
  protected def doReset(isIllegal: Boolean) = {
    // sim時は止める。それ以外はデバッグレジスタに残す
    assert(!isIllegal, "illegal state")
    if (isIllegal) {
      illegalReg := true.B
    }

    // 状態のリセット
    if (!isIllegal || resetOnPanic) {
      statusReg := OperandFetchStatus.idle
      clearResult(false.B)
      clearReadReq()
    }
  }

  /* 実装するAddressing Modeの定義 */
  // 使用可能なAddressing Mode
  val addressingImpls: Seq[AddressingImpl] = Seq(
    new InvalidImpl(),
    new ImpliedImpl(),
    new AccumulatorImpl(),
    new ImmediateImpl(),
    new AbsoluteImpl(),
    new ZeroPageImpl(),
    new IndexedZeroPageImpl(IndexRegister.X()),
    new IndexedZeroPageImpl(IndexRegister.Y()),
    new IndexedAbsoluteImpl(IndexRegister.X()),
    new IndexedAbsoluteImpl(IndexRegister.Y()),
    new RelativeImpl(),
    new IndirectImpl(),
    new XIndexedIndirectImpl(),
    new IndirectYIndexedImpl(),
  )
  // AddressingImplの戻り値をもとに処理を行う
  protected def doAddressingProcess(p: addressing.Process) = p match {
    case Clear(isIllegal) => {
      doReset(isIllegal = isIllegal)
    }
    case ReadOperand(addr, length) => {
      statusReg := OperandFetchStatus.readOperand
      clearResult(isValid = false.B) // Read初回なので結果クリアを兼ねる
      setReadReq(addr, length)
    }
    case ReadPointer(addr, length) => {
      statusReg := OperandFetchStatus.readPointer
      setReadReq(addr, length)
    }
    case ReadData(addr, length) => {
      statusReg := OperandFetchStatus.readData
      setReadReq(addr, length)
    }
    case ReportAddr(addr) => {
      statusReg := OperandFetchStatus.idle
      setResult(Some(addr), None)
      clearReadReq()
    }
    case ReportData(data) => {
      statusReg := OperandFetchStatus.idle
      setResult(None, Some(data))
      clearReadReq()
    }
    case ReportFull(addr, data) => {
      statusReg := OperandFetchStatus.idle
      setResult(Some(addr), Some(data))
      clearReadReq()
    }
    case ReportNone() => {
      statusReg := OperandFetchStatus.idle
      setResult(None, None)
      clearReadReq()
    }
  }

  /* ロジック本体 */
  // 処理本体はステータスで処理分岐
  when(statusReg === OperandFetchStatus.idle) {
    // Idle中なので何もしない。Req検知したら内容に応じて即答するかRead要求を出す
    when(!onRequest) {
      // 現状維持
      statusReg := OperandFetchStatus.idle
      keepResult()
      clearReadReq() // read要求はいらないので念の為クリア
    }.otherwise {
      // idle->read開始
      addressingImpls foreach { impl =>
        when(io.control.addressing === impl.addressing) {
          // 対象のAddressing Modeのコマンドを取り出して処理を行う
          val p = impl.onRequest(reqReadData = io.control.reqDataFetch.litToBoolean, opcodeAddr = io.control.opcodeAddr, reg = io.control.cpuReg)
          doAddressingProcess(p)
        }
      }
    }
  }.otherwise {
    // Read中なので終わるまで待つ、その後はAddressingModeで分岐
    when(!io.busMaster.valid) {
      // read完了まで現状維持
      keepResult()
      keepReadReq()
    }.otherwise {
      // 今回のRead結果を保持, 本cycleで参照するならbusMaster.dataOutを直に見ること
      switch(currentReadCountReg) {
        is(0.U) { assert(false, "illegal state") } // 要求時にインクリメント済
        is(1.U) { readTmpRegs(0) := io.busMaster.dataOut }
        is(2.U) { readTmpRegs(1) := io.busMaster.dataOut } // for debug
        is(3.U) { assert(false, "illegal state") }
      }
      // 今回の参照用
      val readData: UInt = currentReadCountReg.litValue.toInt match {
        case 1 => Cat(0.U(8.W), io.busMaster.dataOut)       // {   $00, lower}: Immediate, ZeroPage, Relative
        case 2 => Cat(io.busMaster.dataOut, readTmpRegs(0)) // { upper, lower}: Absolute, Indirect
        case _ => {
          assert(false, "illegal state")
          0.U
        }
      }

      // Read進捗で分岐
      when(currentReadCountReg < totalReadCountReg) {
        // Burst継続、次のアドレスに要求を出す
        continueReadReq(readReqAddrReg + 1.U)
      }.otherwise {
        // 対象のAddressingImplに処理させる
        addressingImpls foreach { impl =>
          when(io.control.addressing === impl.addressing) {
            // Readしている目的で分岐し、後の処理はAddressing Modeの実装に委ねる
            switch(statusReg) {
              is(OperandFetchStatus.idle) {
                // Read中にIdleにはならないはず
                doReset(isIllegal = true)
              }
              is(OperandFetchStatus.readOperand) {
                val p = impl.doneReadOperand(reqReadData = io.control.reqDataFetch.litToBoolean, opcodeAddr = io.control.opcodeAddr, reg = io.control.cpuReg, readData = readData)
                doAddressingProcess(p)
              }
              is(OperandFetchStatus.readPointer) {
                val p = impl.doneReadPointer(reqReadData = io.control.reqDataFetch.litToBoolean, opcodeAddr = io.control.opcodeAddr, reg = io.control.cpuReg, readData = readData)
                doAddressingProcess(p)
              }
              is(OperandFetchStatus.readData) {
                val p = impl.doneReadData(opcodeAddr = io.control.opcodeAddr, reg = io.control.cpuReg, readAddr = readReqAddrReg, readData = readData)
                doAddressingProcess(p)
              }
            }
          }
        }
      }

    }
  }

}
