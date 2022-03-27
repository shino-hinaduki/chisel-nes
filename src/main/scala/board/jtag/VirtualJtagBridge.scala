package board.jtag

import chisel3._
import chisel3.internal.firrtl.Width
import chisel3.util.log2Up
import chisel3.util.Cat

import board.jtag.types.VirtualJtagIO
import board.jtag.types.VirtualInstruction

import board.ram.types.AsyncFifoDequeueIO
import board.ram.types.AsyncFifoEnqueueIO
import board.access.types.InternalAccessCommand
import board.access.types.InternalAccessCommandMasterIO

/**
  * Virtual JTAG Intel(R) FPGA IP Coreと接続し、DebugAccessPortとの接続を行う
  * 本ModuleはtckのClock Domainとして扱う
  */
class VirtualJtagBridge extends RawModule {
  // 命令フォーマットが正しくないときに返す値。どれだけ読み出してもこの値を返し続ける。AccessTarget=invalidをわざと指定して疎通確認にも使える
  val invalidData: UInt = 0x665599aa.U

  // InternalAccessCommand.Request.dataWidth分のデータをシフト状態を取り扱うのに必要なビット幅
  val shiftCountWidth = log2Up(InternalAccessCommand.Request.dataWidth)
  // IRで指定できる baseOffset は下位16bit分だけなので、カウントと足し合わせる前に拡張するための幅
  val offsetPaddingWidth = InternalAccessCommand.Request.offsetWidth - VirtualInstruction.baseOffsetWidth

  val io = IO(new Bundle {
    // positive reset
    val reset = Input(Reset())
    // Virtual JTAG IP Coreと接続
    val vjtag = new VirtualJtagIO(VirtualInstruction.totalWidth.W)
    // DebugAccess要求先
    val debugAccessQueues = Vec(VirtualInstruction.AccessTarget.all.length, new InternalAccessCommandMasterIO)
  })

  // JTAG TCK Domain
  withClockAndReset(io.vjtag.tck, io.reset) {

    /******************************************************************************/
    /* JTAG制御関連                                                               */
    // 現在キャプチャされている命令
    val irInReg = Reg(new VirtualInstruction)
    io.vjtag.ir_out := irInReg.raw // そのまま向けておく
    // Shift DRの制御カウント, 1byte内でのシフト数を指す
    val shiftCountReg = RegInit(UInt(shiftCountWidth.W), 0.U)
    // Shift DRでアクセスが発生したデータオフセット。irInReg.baseAddr + この値をアクセス先にする
    val burstAccessCountReg = RegInit(UInt(InternalAccessCommand.Request.offsetWidth.W), 0.U)
    // 入力するデータ
    val dataInReg     = RegInit(UInt(InternalAccessCommand.Request.dataWidth.W), 0.U) // 現在TDIからshift inしているデータ
    val postDataInReg = RegInit(UInt(InternalAccessCommand.Request.dataWidth.W), 0.U) // (Write命令時のみのデバッグ用。dataInRegで完成した1byteを次1byteが完成するまで保持)
    // 出力するデータ
    val dataOutReg    = RegInit(UInt(InternalAccessCommand.Request.dataWidth.W), 0.U) // 現在TDOにshift outしているデータ
    val preDataOutReg = RegInit(UInt(InternalAccessCommand.Request.dataWidth.W), 0.U) // (Read時にDAPからの結果を格納する。cnt=0で要求を出し、cnt=7までに回収できる想定)

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
      dataInReg := Cat(io.vjtag.tdi, dataInReg(InternalAccessCommand.Request.dataWidth - 1, 1))
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
      dataOutReg := Cat(false.B, dataOutReg(InternalAccessCommand.Request.dataWidth - 1, 1))
    }
    // 1bitはbypassしてtdoRegに直接セットしつつ、次1byteのデータを設定する
    def setDataOutReg(data: UInt) = {
      tdoReg     := data(0)
      dataOutReg := Cat(false.B, data(InternalAccessCommand.Request.dataWidth - 1, 1)) // 1bitはBypass済
    }
    // preDataOutに値を設定する
    def setPreDataOutReg(data: UInt) = {
      preDataOutReg := data
    }

    /******************************************************************************/
    /* Access対象制御関連                                                          */
    // 要求時のOffset/Data/Type
    val debugAccessReqDataReg = RegInit(UInt(InternalAccessCommand.Request.cmdWidth.W), 0.U)
    // Enqueue/Dequeue Req(Port分だけ生成)
    val debugAccessReqWrEnRegs  = RegInit(UInt(io.debugAccessQueues.size.W), 0.U)
    val debugAccessRespRdEnRegs = RegInit(UInt(io.debugAccessQueues.size.W), 0.U)
    // 初回のRead要求の場合、直接DataOutRegに設定してもう一度Readを発行する
    val debugAccessFirstReadReg = RegInit(Bool(), false.B)
    // Read結果を捨てた場合のステ先。非ゼロだと不具合だとわかるので一応残しておく
    val debugAccessInvalidRespReg = RegInit(UInt(InternalAccessCommand.Response.dataWidth.W), 0.U)

    // port全体に処理追加
    io.debugAccessQueues.zipWithIndex.foreach {
      case (q, index) => {
        // Enqueue側初期化
        q.req.data  := debugAccessReqDataReg
        q.req.wrclk := io.vjtag.tck
        q.req.wrreq := debugAccessReqWrEnRegs(index)

        // Dequeue側初期化
        q.resp.rdclk := io.vjtag.tck
        q.resp.rdreq := debugAccessRespRdEnRegs(index)

        // Read応答の処理
        val isRespExist   = !q.resp.rdempty                         // Read応答がある
        val isInReadCmd   = irInReg.isValid && !irInReg.isWrite     // VIRにReadが積まれている
        val isValidTarget = irInReg.accessTarget.asUInt === index.U // AccessTargetが一致している
        when(isRespExist) {
          // QueueにRead応答がある
          val readData = InternalAccessCommand.Response.getData(q.resp.q)
          when(!isInReadCmd || !isValidTarget) {
            // 不正な状態: データは捨てるが、デバッグ用に残しておく
            // (TCK * 32cycの間にデータ応答を準備できなかった場合、もしくは途中でTLRに戻され別命令を流された場合など)
            debugAccessInvalidRespReg := readData
          }.elsewhen(debugAccessFirstReadReg) {
            // 初回のRead応答: dataOutに直接セット
            setDataOutReg(readData)
            debugAccessFirstReadReg := false.B // 初回Read完了
          }.otherwise {
            // 2回目以後: preDataOutにセットして要求を解除. 次の要求は32cycごとのタイミングで行われるのでBurstCountなどはいじらない
            setPreDataOutReg(readData)
          }
          // Dequeueするのでこのcycはててておく
          debugAccessRespRdEnRegs.bitSet(index.U, true.B)
        }.otherwise {
          // このQueueにはRead要求がないので落としておく
          debugAccessRespRdEnRegs.bitSet(index.U, false.B)
        }
      }
    }

    // Enqueue要求をクリア
    def clearReqToInternal() = {
      debugAccessReqDataReg  := DontCare
      debugAccessReqWrEnRegs := 0.U
    }
    // ReqQueueにEnqueueする
    def setReqToInternal(index: UInt, reqData: UInt) = {
      debugAccessReqDataReg := reqData
      debugAccessReqWrEnRegs.bitSet(index, true.B)
    }

    // Read Cmdを対象のQueueに発行する
    def reqReadToInternal(accessTarget: VirtualInstruction.AccessTarget.Type, offset: UInt) = {
      // AccessTarget定義は、Queue定義と一致
      val index = accessTarget.asUInt
      // Queueに乗せるデータ
      val reqData = InternalAccessCommand.Request.encode(
        request = InternalAccessCommand.Type.read,
        offset = offset,
        data = 0.U, // Don't care
      )

      // レジスタに反映
      setReqToInternal(index, reqData)
    }

    // Write Cmdを対象のQueueに発行する
    def reqWriteToInternal(accessTarget: VirtualInstruction.AccessTarget.Type, offset: UInt, data: UInt) = {
      // AccessTarget定義は、Queue定義と一致
      val index = accessTarget.asUInt
      // Queueに乗せるデータ
      val reqData = InternalAccessCommand.Request.encode(
        request = InternalAccessCommand.Type.write,
        offset = offset,
        data = data,
      )

      // レジスタに反映
      setReqToInternal(index, reqData)
    }

    // irInReg, burstAccessCountRegの値から、次のアクセス先アドレスを計算する
    def calcRequestOffset(): UInt = {
      val baseOffset = Cat(0.U(offsetPaddingWidth.W), irInReg.baseOffset)
      baseOffset + burstAccessCountReg
    }

    /******************************************************************************/
    /* VJTAG制御実体                                                              */
    when(io.vjtag.virtual_state_cdr) {
      // Capture_DR
      clearReqToInternal() // Read要求が出しっぱなしにならないように落とす
    }.elsewhen(io.vjtag.virtual_state_sdr) {
      // Shift_DR
      when(shiftCountReg === 0.U) {
        // 1~30回目: Readは次のデータをPrefetchする。それ以外は種別問わずシフトを進める
        when(irInReg.isValid && !irInReg.isWrite) {
          // Read: 次のRead要求を出してBurstCountをインクリメント
          val accessOffset = calcRequestOffset() // incBurstCountする前に計算する
          reqReadToInternal(irInReg.accessTarget, accessOffset)
          incBurstCount() // R/W先は次のアドレスにすすめる
          // その他は共通
          incShiftCount()
          shiftDataInReg()
          shiftDataOutReg()
          // (Read要求は出しているので、Clearは次のcycで行う)
        }.otherwise {
          // 共通
          incShiftCount()
          shiftDataInReg()
          shiftDataOutReg()
          clearReqToInternal() // Read要求が出しっぱなしにならないように落とす
        }
      }.elsewhen(shiftCountReg < (InternalAccessCommand.Request.dataWidth - 1).U) {
        // 1~30回目: 種別問わずシフトを進める
        incShiftCount()
        shiftDataInReg()
        shiftDataOutReg()
        clearReqToInternal() // Read要求が出しっぱなしにならないように落とす
      }.otherwise {
        // 31回目(tdi/tdoには32bit目のデータがあって、1word分終わる状態)

        // tdi + dataInRegでWriteDataは完成
        val writeData = Cat(io.vjtag.tdi, dataInReg(InternalAccessCommand.Request.dataWidth - 1, 1))
        // R/W/Invalidで処理が分岐
        when(irInReg.isValid) {
          // R/Wで分岐
          val accessOffset = calcRequestOffset() // incBurstCountする前に計算する
          when(irInReg.isWrite) {
            // Write: shiftはしておくものの、最後のTDIのデータをBypassしてデータを完成させ、Write要求を出す
            reqWriteToInternal(irInReg.accessTarget, accessOffset, writeData)
            // Read Dataのregもとりあえず処理はしておく
            shiftDataOutReg()
            incBurstCount() // Writeを出したので、次のアドレスにすすめる
          }.otherwise {
            // Read: 次のデータをtdo/dataOutにセット
            setDataOutReg(preDataOutReg)
            // (BurstCountはRead要求時にすすめるのでここでは操作しない)
          }
          // 共通の後処理
          setPostDataInReg(writeData) // postDataInにも記録しておく(実質デバッグ用)
          shiftDataInReg()            // 利用済で使わないと思うが、進めておく(本cycでDataInRegが完成)
          resetShiftCount()           // 0bit目に戻る
          clearReqToInternal()        // Read要求が出しっぱなしにならないように落とす
        }.otherwise {
          // Invalid: Invalidな値の入ったpreDataOutRegを使い続ける
          setPostDataInReg(writeData) // postDataInにも記録しておく(実質デバッグ用)
          shiftDataInReg()
          setDataOutReg(preDataOutReg)
          resetShiftCount()    // 0bit目の処理に戻り、次のPreDataOutに手を付ける
          clearReqToInternal() // Read要求が出しっぱなしにならないように落とす
        }
      }
    }.elsewhen(io.vjtag.virtual_state_e1dr) {
      // Exit1_DR
      clearReqToInternal() // Read要求が出しっぱなしにならないように落とす
    }.elsewhen(io.vjtag.virtual_state_pdr) {
      // Pause_DR
      clearReqToInternal() // Read要求が出しっぱなしにならないように落とす
    }.elsewhen(io.vjtag.virtual_state_e2dr) {
      // Exit2_DR
      clearReqToInternal() // Read要求が出しっぱなしにならないように落とす
    }.elsewhen(io.vjtag.virtual_state_udr) {
      // Update_DR
      clearReqToInternal() // Read要求が出しっぱなしにならないように落とす
    }.elsewhen(io.vjtag.virtual_state_cir) {
      // Capture_IR
      // (irInReg.rawを見せているものがキャプチャされるだけなのでケア不要)
      clearReqToInternal() // Read要求が出しっぱなしにならないように落とす
    }.elsewhen(io.vjtag.virtual_state_uir) {
      // Update_IR
      // VirtualJTAGなので (ShiftIR USER1, ShiftDR XXXX) で流した XXXX が io.vjtag.ir_in に入っている
      VirtualInstruction.parse(irInReg, io.vjtag.ir_in)

      // まだirInRegには書き込まれていないので、今回の判断用に自前Parseしておく
      val (accessTarget, isValid) = VirtualInstruction.getaccessTargetAndIsValid(io.vjtag.ir_in)
      val baseOffset              = Cat(0.U(offsetPaddingWidth.W), VirtualInstruction.getBaseOffset(io.vjtag.ir_in)) // 24bitに拡張しておく
      val isWrite                 = VirtualInstruction.getIsWrite(io.vjtag.ir_in)

      // 初期データの準備を行う
      when(isValid) {
        when(isWrite) {
          // Write: 1byte書くごとに発行するので何もしない, ReadできるデータにはInvalidを埋めておく
          resetBurstCount()
          resetDataInReg()
          setDataOutReg(invalidData)    // Read相当で取り扱うが、ReadせずInvalidDataを返す
          setPreDataOutReg(invalidData) // 以後もこのデータを読み出す
          resetShiftCount()
          clearReqToInternal() // Read要求が出しっぱなしにならないように落とす
        }.otherwise {
          // Read: 最初に読み出すデータを準備しておく
          setBurstCount(1.U) // 初回のReadを出すので、1word分進めておく
          resetDataInReg()
          resetDataOutReg()
          debugAccessFirstReadReg := true.B // Read応答の格納先判別用に、初回かどうか残しておく
          reqReadToInternal(accessTarget, baseOffset) // Shift-DRまでに回収して、初回のTDOをセットする想定
          resetShiftCount()
          // (Read要求は出しているので、Clearは次のcycで行う)
        }
      }.otherwise {
        // Invalid: dataOutRegには無効データを埋めておく
        resetBurstCount()
        resetDataInReg()
        setDataOutReg(invalidData)    // Read相当で取り扱うが、ReadせずInvalidDataを返す
        setPreDataOutReg(invalidData) // 以後もこのデータを読み出す
        resetShiftCount()
        clearReqToInternal() // Read要求が出しっぱなしにならないように落とす
      }
    }.otherwise {
      // NOP
      clearReqToInternal() // Read要求が出しっぱなしにならないように落とす
    }
  }
}
