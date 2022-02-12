package cpu

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.switch
import chisel3.util.is
import chisel3.util.Cat

import _root_.bus.types.BusIO
import cpu.types.Addressing
import cpu.types.OperandFetchIO

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
  protected def setReadReq(addr: UInt, totalCount: Option[UInt]) = {
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
  protected def keepReadReq() = {
    reqReadReg          := reqReadReg
    readReqAddrReg      := readReqAddrReg
    currentReadCountReg := currentReadCountReg
    totalReadCountReg   := totalReadCountReg
  }

  /* OF完了関連 */
  // status, 出力レジスタ、Read要求ともにクリアする
  protected def resetAndDone(isIllegal: Boolean) = {
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
  // アドレスだけ報告して完了する
  protected def reportAddrAndDone(addr: UInt) = {
    statusReg := OperandFetchStatus.idle
    setResult(Some(addr), None)
    clearReadReq()
  }
  // 現在読みだしたアドレスとデータを報告して完了する
  protected def reportCurrentReadDataAndDone() = {
    statusReg := OperandFetchStatus.idle
    setResult(Some(readReqAddrReg), Some(io.busMaster.dataOut)) // 現在の結果
    clearReadReq()
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
      switch(io.control.addressing) {
        // 初期値に戻す, validRegも立てない
        is(Addressing.invalid) {
          resetAndDone(isIllegal = true)
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
          statusReg := OperandFetchStatus.readOperand
          clearResult(false.B)
          setReadReq(io.control.opcodeAddr + 1.U, Some(1.U))
        }
        // lower,upper
        is(Addressing.absolute) {
          statusReg := OperandFetchStatus.readOperand
          clearResult(false.B)
          setReadReq(io.control.opcodeAddr + 1.U, Some(2.U))
        }
        // lowerのみ
        is(Addressing.zeroPage) {
          statusReg := OperandFetchStatus.readOperand
          clearResult(false.B)
          setReadReq(io.control.opcodeAddr + 1.U, Some(1.U))
        }
        // lowerのみ
        is(Addressing.xIndexedZeroPage) {
          statusReg := OperandFetchStatus.readOperand
          clearResult(false.B)
          setReadReq(io.control.opcodeAddr + 1.U, Some(1.U))
        }
        // lowerのみ
        is(Addressing.yIndexedZeroPage) {
          statusReg := OperandFetchStatus.readOperand
          clearResult(false.B)
          setReadReq(io.control.opcodeAddr + 1.U, Some(1.U))
        }
        // lower,upper
        is(Addressing.xIndexedAbsolute) {
          statusReg := OperandFetchStatus.readOperand
          clearResult(false.B)
          setReadReq(io.control.opcodeAddr + 1.U, Some(2.U))
        }
        // lower,upper
        is(Addressing.yIndexedAbsolute) {
          statusReg := OperandFetchStatus.readOperand
          clearResult(false.B)
          setReadReq(io.control.opcodeAddr + 1.U, Some(2.U))
        }
        // offsetのみ
        is(Addressing.relative) {
          statusReg := OperandFetchStatus.readOperand
          clearResult(false.B)
          setReadReq(io.control.opcodeAddr + 1.U, Some(1.U))
        }
        // lower,upper
        is(Addressing.indirect) {
          statusReg := OperandFetchStatus.readOperand
          clearResult(false.B)
          setReadReq(io.control.opcodeAddr + 1.U, Some(2.U))
        }
        // lowerのみ
        is(Addressing.xIndexedIndirect) {
          statusReg := OperandFetchStatus.readOperand
          clearResult(false.B)
          setReadReq(io.control.opcodeAddr + 1.U, Some(1.U))
        }
        // lowerのみ
        is(Addressing.indirectYIndexed) {
          statusReg := OperandFetchStatus.readOperand
          clearResult(false.B)
          setReadReq(io.control.opcodeAddr + 1.U, Some(1.U))
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
      val readData1Req = Cat(0.U(8.W), io.busMaster.dataOut)       // Immediate, ZeroPage, Relative (上と同じだが2byte)
      val readData2Req = Cat(io.busMaster.dataOut, readTmpRegs(0)) // Absolute, Indirect
      switch(currentReadCountReg) {
        is(0.U) {} // 要求時にインクリメント済
        is(1.U) { readTmpRegs(0) := io.busMaster.dataOut }
        is(2.U) { readTmpRegs(1) := io.busMaster.dataOut } // for debug
        is(3.U) { assert(false, "illegal state") }
      }

      // Read進捗で分岐
      when(currentReadCountReg < totalReadCountReg) {
        // Burst継続、次のアドレスに要求を出す
        setReadReq(readReqAddrReg + 1.U, None)
      }.otherwise {
        // 処理ごと、Read進捗ごとに分岐
        switch(io.control.addressing) {
          // 想定外の挙動, idleに戻す
          is(Addressing.invalid) {
            resetAndDone(isIllegal = true)
          }
          // 想定外の挙動, idleに戻す
          is(Addressing.implied) {
            resetAndDone(isIllegal = true)
          }
          // 想定外の挙動, idleに戻す
          is(Addressing.accumulator) {
            resetAndDone(isIllegal = true)
          }
          // 読みだした結果をそのまま報告して完了
          is(Addressing.immediate) {
            assert(statusReg === OperandFetchStatus.readOperand)
            reportCurrentReadDataAndDone()
          }
          // Lower,Higherでアドレスは完成
          is(Addressing.absolute) {
            switch(statusReg) {
              is(OperandFetchStatus.idle) {
                resetAndDone(isIllegal = true)
              }
              is(OperandFetchStatus.readOperand) {
                when(io.control.reqDataFetch) {
                  // 読みだしたアドレスにReadをかける
                  statusReg := OperandFetchStatus.readData
                  setReadReq(readData2Req, Some(1.U))
                }.otherwise {
                  // データ読み出しがなければアドレスを報告して終わり
                  reportAddrAndDone(readData2Req)
                }
              }
              is(OperandFetchStatus.readPointer) {
                resetAndDone(isIllegal = true)
              }
              is(OperandFetchStatus.readData) {
                // 現在読み出しているアドレスとデータを報告
                reportCurrentReadDataAndDone()
              }
            }
          }
          is(Addressing.zeroPage) {}
          is(Addressing.xIndexedZeroPage) {}
          is(Addressing.yIndexedZeroPage) {}
          is(Addressing.xIndexedAbsolute) {}
          is(Addressing.yIndexedAbsolute) {}
          is(Addressing.relative) {}
          is(Addressing.indirect) {}
          is(Addressing.xIndexedIndirect) {}
          is(Addressing.indirectYIndexed) {}
        }
      }

    }
  }
}
