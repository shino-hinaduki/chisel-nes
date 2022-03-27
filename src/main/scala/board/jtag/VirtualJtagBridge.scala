package board.jtag

import chisel3._
import chisel3.internal.firrtl.Width

import board.jtag.types.VirtualJtagIO
import board.jtag.types.VirtualInstruction

import support.types.DebugAccessDataKind
import support.types.DebugAccessIO
import chisel3.util.log2Up
import chisel3.util.Cat

/**
  * Virtual JTAG Intel(R) FPGA IP Coreと接続し、DebugAccessPortとの接続を行う
  * 本ModuleはtckのClock Domainとして扱う
  */
class VirtualJtagBridge extends RawModule {
  // VIRを24bit想定で解釈するため、固定値とする
  val irWidth = 24
  // 指定できるアドレスは24bit空間(IR内の内訳の都合で、バーストしないと17bit目以後は見えない)
  val offsetWidth = 24
  // 読み書きは32bit単位で行う。隣接byteとの同時書き換えに不都合があれば上位Xbyteをreservedにする
  val dataWidth = 32
  // dataWidth分のデータをシフト状態を取り扱うのに必要なビット幅
  val shiftCountWidth = log2Up(dataWidth)
  // 命令フォーマットが正しくないときに返す値。どれだけ読み出してもこの値を返し続ける
  val invalidData: UInt = 0x665599aa.U

  val io = IO(new Bundle {
    // positive reset
    val reset = Input(Reset())
    // Virtual JTAG IP Coreと接続
    val vjtag = new VirtualJtagIO(irWidth.W)
  })

  // JTAG TCK Domain
  withClockAndReset(io.vjtag.tck, io.reset) {
    // 現在キャプチャされている命令
    val irInReg = Reg(new VirtualInstruction)
    io.vjtag.ir_out := irInReg.raw // そのまま向けておく
    // Shift DRの制御カウント, 1byte内でのシフト数を指す
    val shiftCountReg = RegInit(UInt(shiftCountWidth.W), 0.U)
    // Shift DRでアクセスが発生したデータオフセット。irInReg.baseAddr + この値をアクセス先にする
    val burstAccessCountReg = RegInit(UInt(offsetWidth.W), 0.U)
    // 入力するデータ
    val dataInReg     = RegInit(UInt(dataWidth.W), 0.U) // 現在TDIからshift inしているデータ
    val postDataInReg = RegInit(UInt(dataWidth.W), 0.U) // (Write命令時のみのデバッグ用。dataInRegで完成した1byteを次1byteが完成するまで保持)
    // 出力するデータ
    val dataOutReg    = RegInit(UInt(dataWidth.W), 0.U) // 現在TDOにshift outしているデータ
    val preDataOutReg = RegInit(UInt(dataWidth.W), 0.U) // (Read時にDAPからの結果を格納する。cnt=0で要求を出し、cnt=7までに回収できる想定)

    // 実際のTDOに出すデータ
    val tdoReg = RegInit(Bool(), false.B)
    io.vjtag.tdo := tdoReg

    // シフト数を初期化する
    def resetShiftCount() = {
      shiftCountReg := 0.U
    }
    // シフト数をすすめる
    def incShiftCount() = {
      shiftCountReg := shiftCountReg + 1.U
    }
    // アクセス先オフセットを初期化する
    def resetBurstCount() = {
      burstAccessCountReg := 0.U
    }
    // アドレスオフセットを指定値に設定
    def setBurstCount(count: UInt) = {
      burstAccessCountReg := count
    }
    // アクセス先オフセットをすすめる
    def incBurstCount() = {
      burstAccessCountReg := burstAccessCountReg + 1.U
    }
    // dataInRegをクリアする
    def resetDataInReg() = {
      dataInReg     := 0.U
      postDataInReg := 0.U
    }
    // TDIの値をdataInRegのMSBに取り込みつつ、LSBを破棄
    def shiftDataInReg() = {
      dataInReg := Cat(io.vjtag.tdi, dataInReg(dataWidth - 1, 1))
    }
    // postDataInに値を設定する
    def setPostDataInReg(data: UInt) = {
      postDataInReg := data
    }
    // dataOutRegをクリアする
    def resetDataOutReg() = {
      tdoReg        := false.B
      dataOutReg    := 0.U
      preDataOutReg := 0.U
    }
    // dataOutRegのLSBをTDOに移しつつ、右シフト
    def shiftDataOutReg() = {
      tdoReg     := dataOutReg(0)
      dataOutReg := Cat(false.B, dataOutReg(dataWidth - 1, 1))
    }
    // 1bitはbypassしてtdoRegに直接セットしつつ、次1byteのデータを設定する
    def setDataOutReg(data: UInt) = {
      tdoReg     := data(0)
      dataOutReg := Cat(false.B, data(dataWidth - 1, 1)) // 1bitはBypass済
    }
    // preDataOutに値を設定する
    def setPreDataOutReg(data: UInt) = {
      preDataOutReg := data
    }
    // ReadをDAPに投げる
    def reqReadToDap(kind: DebugAccessDataKind.Type, addr: UInt) = {
      // TODO: AsyncFIFO経由でDAPに要求を出す
    }
    // TODO: Read応答が来たものを preDataOutRegに格納する(shiftCountReg===1.UならバラしてTDOにもセットする)
    // WriteをDAPに投げる
    def reqWriteToDap(kind: DebugAccessDataKind.Type, addr: UInt, data: UInt) = {
      // TODO: AsyncFIFO経由でDAPに要求を出す
    }

    when(io.vjtag.virtual_state_cdr) {
      // Capture_DR
    }.elsewhen(io.vjtag.virtual_state_sdr) {
      // Shift_DR
      when(shiftCountReg < (dataWidth - 1).U) {
        // 0~30回目: 種別問わずシフトを進める
        incShiftCount()
        shiftDataInReg()
        shiftDataOutReg()
      }.otherwise {
        // 31回目(tdi/tdoには32bit目のデータがあって、1word分終わる状態)

        // tdi + dataInRegでWriteDataは完成
        val writeData = Cat(io.vjtag.tdi, dataInReg(dataWidth - 1, 1))

        // R/W/Invalidで処理が分岐
        when(irInReg.isValid) {
          // IRで指定できる baseOffset は下位16bit分だけなので、カウントと足し合わせる前に拡張する
          val baseOffset = Cat(0.U((offsetWidth - VirtualInstruction.baseOffsetWidth).W), irInReg.baseOffset)
          val burstCount = baseOffset + burstAccessCountReg
          when(irInReg.isWrite) {
            // Write: shiftはしておくものの、最後のTDIのデータをBypassしてデータを完成させ、Write要求を出す
            reqWriteToDap(irInReg.dataKind, burstCount, writeData)
            // Read Dataのregもとりあえず処理はしておく
            shiftDataOutReg()
          }.otherwise {
            // Read: 次のデータをtdo/dataOutにセットしつつ、更に1byte先のRead要求を出す
            setDataOutReg(preDataOutReg)
            // 次回データ用にRead
            reqReadToDap(irInReg.dataKind, burstCount)
          }
          // 共通の後処理
          setPostDataInReg(writeData) // postDataInにも記録しておく(実質デバッグ用)
          shiftDataInReg()            // 利用済で使わないと思うが、進めておく(本cycでDataInRegが完成)
          resetShiftCount()           // 0bit目に戻る
          incBurstCount()             // R/W先は次のアドレスにすすめる
        }.otherwise {
          // Invalid: Readに同じくだが、Invalidな値の入ったpreDataOutRegを使い続ける
          setPostDataInReg(writeData) // postDataInにも記録しておく(実質デバッグ用)
          shiftDataInReg()
          setDataOutReg(preDataOutReg)
          resetShiftCount() // 0bit目の処理に戻り、次のPreDataOutに手を付ける
        }
      }
    }.elsewhen(io.vjtag.virtual_state_e1dr) {
      // Exit1_DR
    }.elsewhen(io.vjtag.virtual_state_pdr) {
      // Pause_DR
    }.elsewhen(io.vjtag.virtual_state_e2dr) {
      // Exit2_DR
    }.elsewhen(io.vjtag.virtual_state_udr) {
      // Update_DR
    }.elsewhen(io.vjtag.virtual_state_cir) {
      // Capture_IR
      // (irInReg.rawを見せているものがキャプチャされるだけなのでケア不要)
    }.elsewhen(io.vjtag.virtual_state_uir) {
      // Update_IR
      // VirtualJTAGなので (ShiftIR USER1, ShiftDR XXXX) で流した XXXX が io.vjtag.ir_in に入っている
      VirtualInstruction.parse(irInReg, io.vjtag.ir_in)

      // まだirInRegには書き込まれていないので、今回の判断用に自前Parseしておく
      val (dataKind, isValid) = VirtualInstruction.getDataKindAndIsValid(io.vjtag.ir_in)
      val baseOffset          = VirtualInstruction.getBaseOffset(io.vjtag.ir_in)
      val isWrite             = VirtualInstruction.getIsWrite(io.vjtag.ir_in)

      // 初期データの準備を行う
      when(isValid) {
        when(isWrite) {
          // Write: 1byte書くごとに発行するので何もしない, ReadできるデータにはInvalidを埋めておく
          resetBurstCount()
          resetDataInReg()
          setDataOutReg(invalidData)    // Read相当で取り扱うが、ReadせずInvalidDataを返す
          setPreDataOutReg(invalidData) // 以後もこのデータを読み出す
          resetShiftCount()
        }.otherwise {
          // Read: 最初に読み出すデータを準備しておく
          setBurstCount(1.U) // 初回のReadを出すので、1word分進めておく
          resetDataInReg()
          resetDataOutReg()
          reqReadToDap(dataKind, baseOffset) // Shift-DRまでに回収して、初回のTDOをセットする想定
          resetShiftCount()
        }
      }.otherwise {
        // Invalid: dataOutRegには無効データを埋めておく
        resetBurstCount()
        resetDataInReg()
        setDataOutReg(invalidData)    // Read相当で取り扱うが、ReadせずInvalidDataを返す
        setPreDataOutReg(invalidData) // 以後もこのデータを読み出す
        resetShiftCount()
      }
    }.otherwise {
      // NOP
    }
  }
}
