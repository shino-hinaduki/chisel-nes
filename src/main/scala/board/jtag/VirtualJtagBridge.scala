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
  * 本Module自身はSystem Clock、Virutal JTAG Coreとの連携部分はtckのClock Domainとして扱う
  */
class VirtualJtagBridge extends Module {
  // VIRを24bit想定で解釈するため、固定値とする
  val irWidth = 24.W
  // 命令フォーマットが正しくないときに返す値。どれだけ読み出してもこの値を返し続ける
  val invalidData: UInt = 0xa5.U

  val io = IO(new Bundle {
    // Virtual JTAG IP Coreと接続
    val vjtag = new VirtualJtagIO(irWidth)
    // DebugAccessPortとのI/F  TODO: 非同期クロックの載せ替え...。
    val dap = Flipped(new DebugAccessIO)
  })

  // TODO: クロック載せ替えしてDAP制御できるようにする
  io.dap.writeData := 0.U
  io.dap.addr      := 0.U
  io.dap.reqStrobe := false.B
  io.dap.isWrite   := false.B
  io.dap.dataKind  := DebugAccessDataKind.invalid

  // JTAG Clock Domain
  withClock(io.vjtag.tck) {
    // 現在キャプチャされている命令
    val irInReg = Reg(new VirtualInstruction)
    io.vjtag.ir_out := irInReg.raw // そのまま向けておく
    // Shift DRの制御カウント, 1byte内でのシフト数を指す
    val shiftCountReg = RegInit(UInt(3.W), 0.U)
    // Shift DRでアクセスが発生したデータオフセット。irInReg.baseAddr + この値をアクセス先にする
    val addrOffsetReg = RegInit(UInt(32.W), 0.U)
    // 入力するデータ
    val dataInReg     = RegInit(UInt(8.W), 0.U) // 現在TDIからshift inしているデータ
    val postDataInReg = RegInit(UInt(8.W), 0.U) // (Write命令時のみのデバッグ用。dataInRegで完成した1byteを次1byteが完成するまで保持)
    // 出力するデータ
    val dataOutReg    = RegInit(UInt(8.W), 0.U) // 現在TDOにshift outしているデータ
    val preDataOutReg = RegInit(UInt(8.W), 0.U) // (Read時にDAPからの結果を格納する。cnt=0で要求を出し、cnt=7までに回収できる想定)

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
    def resetAddrOffset() = {
      addrOffsetReg := 0.U
    }
    // アクセス先オフセットをすすめる
    def incAddrOffset() = {
      addrOffsetReg := addrOffsetReg + 1.U
    }
    // dataInRegをクリアする
    def resetDataInReg() = {
      dataInReg     := 0.U
      postDataInReg := 0.U
    }
    // TDIの値をdataInRegのMSBに取り込みつつ、LSBを破棄
    def shiftDataInReg() = {
      dataInReg := Cat(io.vjtag.tdi, dataInReg(7, 1))
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
      dataOutReg := Cat(false.B, dataOutReg(7, 1))
    }

    // ReadをDAPに投げる
    def reqReadToDap(kind: DebugAccessDataKind.Type, addr: UInt) = {
      // TODO: AsyncFIFO経由でDAPに要求を出す
    }
    // TODO: Read応答が来たものを preDataOutRegに格納する
    // WriteをDAPに投げる
    def reqWriteToDap(kind: DebugAccessDataKind.Type, addr: UInt, data: UInt) = {
      // TODO: AsyncFIFO経由でDAPに要求を出す
    }

    when(io.vjtag.virtual_state_cdr) {
      // Capture_DR
    }.elsewhen(io.vjtag.virtual_state_sdr) {
      // Shift_DR
      when(shiftCountReg === 0.U) {
        // 0回目
        when(irInReg.isValid) {
          when(irInReg.isWrite) {
            // Write: 1byte終わるまで特にイベントなし
            incShiftCount()
            shiftDataInReg()
            shiftDataOutReg()
          }.otherwise {
            // Read: PreDataOut -> DataOutへの転送 & 初回のTDOはBypassしてPreDataOutを出す, 次回Readの発行
            incShiftCount()
            resetAddrOffset() // 次回Read要求を出したので進めておく
            shiftDataInReg()
            // PreDataOut -> dataOutReg, tdoへ転送
            tdoReg     := preDataOutReg(0)
            dataOutReg := Cat(false.B, preDataOutReg(7, 1)) // 1bitはBypass済
            // 次回データ用にRead
            reqReadToDap(irInReg.dataKind, irInReg.baseAddr + addrOffsetReg)
            incAddrOffset() // 次のアドレスにすすめる
          }
        }.otherwise {
          // Invalid: Read相当で取り扱うが、ReadせずInvalidDataを返す
          // DataIn/DataOutは実施
          incShiftCount()
          shiftDataInReg()
          // DataOut,TDO に invalidDataをセット
          tdoReg        := invalidData(0)
          dataOutReg    := Cat(false.B, invalidData(7, 1)) // 1bitはBypass済
          preDataOutReg := invalidData                     // (なくても良いが念の為)
        }
      }.elsewhen(shiftCountReg < 6.U) {
        // 1~6回目: 種別問わずシフトを進める
        incShiftCount()
        shiftDataInReg()
        shiftDataOutReg()
      }.otherwise {
        // 7回目
        when(irInReg.isValid) {
          when(irInReg.isWrite) {
            // Write: shiftはしておくものの、最後のTDIのデータをBypassしてデータを完成させ、Write要求を出す
            val writeData = Cat(io.vjtag.tdi, dataInReg(7, 1))
            reqWriteToDap(irInReg.dataKind, irInReg.baseAddr + addrOffsetReg, writeData)
            incAddrOffset() // 次のアドレスにすすめる
            // 他のレジスタ処理
            postDataInReg := writeData // postDataInにも記録しておく(実質デバッグ用)
            shiftDataInReg()  // 使わないと思うが、進めておく(本cycでDataInRegが完成)
            resetShiftCount() // 0bit目からCaptureし直し
          }.otherwise {
            // Read: 全bit送り出す状態。0bit目に戻るだけ
            shiftDataInReg()
            shiftDataOutReg()
            resetShiftCount() // 0bit目の処理に戻り、次のPreDataOutに手を付ける
          }
        }.otherwise {
          // Invalid: Readに同じく
          shiftDataInReg()
          shiftDataOutReg()
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
      val baseAddr            = VirtualInstruction.getBaseAddr(io.vjtag.ir_in)
      val isWrite             = VirtualInstruction.getIsWrite(io.vjtag.ir_in)

      // 初期データの準備を行う
      when(isValid) {
        when(isWrite) {
          // Write: 1byte書くごとに発行するので何もしない
          resetAddrOffset()
          resetDataInReg()
          resetDataOutReg()
          resetShiftCount()
        }.otherwise {
          // Read: 最初に読み出すデータを準備しておく
          resetAddrOffset()
          resetDataInReg()
          resetDataOutReg()
          reqReadToDap(dataKind, baseAddr)
          shiftCountReg := 1.U // 初回のReadを出すので、1byte進めておく
        }
      }.otherwise {
        // Invalid: dataOutRegには無効データを埋めておく
        resetAddrOffset()
        resetDataInReg()
        dataOutReg    := invalidData
        preDataOutReg := invalidData
        resetShiftCount()
      }
    }.otherwise {
      // NOP
    }
  }
}
