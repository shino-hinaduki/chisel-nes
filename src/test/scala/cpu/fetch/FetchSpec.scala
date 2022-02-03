package cpu.fetch

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import cpu.types.Instruction
import cpu.types.Addressing

class FetchSpec extends AnyFreeSpec with ChiselScalatestTester {
  // バスアクセス確認用の単純なパターン
  val testMem = Seq(
    0xea.U, // 0x0000 : NOP Implied
    0xe8.U, // 0x0001 : INX Implied
    0xc8.U, // 0x0002 : INY Implied
    0xa9.U, // 0x0003 : LDA Immediate
    0x00.U, // 0x0004 : (op0)
    0xca.U, // 0x0005 : DEX Implied
    0x88.U, // 0x0006 : DEY Implied
    0x4c.U, // 0x0007 : JMP Absolute
    0x00.U, // 0x0008 : (op0)
    0x00.U, // 0x0009 : (op1)
    0xea.U, // 0x000a : NOP Implied
    0xea.U, // 0x000b : NOP Implied
    0xea.U, // 0x000c : NOP Implied
    0xea.U, // 0x000d : NOP Implied
    0xea.U, // 0x000e : NOP Implied
    0x00.U  // 0x000f : BRK Implied
  );

  /* EXからの設定 */
  // PCの現在値を指定した上で、Fetchを有効化します
  def setRequest(f: Fetch, pc: UInt) = {
    f.io.control.reqStrobe.poke(true.B)
    f.io.control.pc.poke(pc)
    f.io.control.discard.poke(false.B)
  }

  // setRequestを呼び出したあと、clock cycleを跨いだあとに呼び出します。
  def setAfterRequest(f: Fetch) = {
    f.io.control.reqStrobe.poke(false.B) // posedgeで検出なので落としておく
  }
  // 要求がない状態に初期化します
  def setDisableReq(f: Fetch) = {
    f.io.control.reqStrobe.poke(false.B)
    f.io.control.pc.poke(0xffff.U) // Don't care
    f.io.control.discard.poke(false.B)
  }

  // Fetchデータ破棄を実行します
  def setDiscard(f: Fetch) = {
    f.io.control.discard.poke(true.B)
  }

  // Fetchデータ破棄を解除します
  def clearDiscard(f: Fetch) = {
    f.io.control.discard.poke(false.B)
  }

  /* EXへ出力したデータの期待値確認 */
  // 正しい値がFetchされていることを確認
  def expectValidFetchData(f: Fetch, addr: UInt, data: UInt, instruction: Instruction.Type, addressing: Addressing.Type) = {
    f.io.control.valid.expect(true.B) // 有効なはず
    f.io.control.addr.expect(addr)
    f.io.control.data.expect(data)
    f.io.control.instruction.expect(instruction)
    f.io.control.addressing.expect(addressing)
  }

  // Read中であることを確認します
  def expectBusy(f: Fetch) = {
    f.io.control.busy.expect(true.B)
  }

  // Read中ではないことを確認します
  def expectIdle(f: Fetch) = {
    f.io.control.busy.expect(false.B)
  }

  // Fetchされたデータがないことを確認
  def expectInvalidFetchData(f: Fetch) = {
    f.io.control.valid.expect(false.B) // 無効。ほかは不問
  }

  /* EXへ出力したデータの期待値確認 */
  // 現在の状態をもとにextMemをReadする。戻り値はReadした値が入る(dataOutには設定済)
  // busy=trueの場合、今回のReadは保留される
  def simExtMem(f: Fetch, extMem: Seq[UInt], busy: Boolean): Option[UInt] = {
    val req = f.io.busMaster.req.peek()
    if (busy) {
      // Busyもしくは要求が来ていない場合は、有効な値は返さない
      f.io.busMaster.valid.poke(false.B)
      f.io.busMaster.dataOut.poke(0x00.U) // Don't care
      None
    } else {
      val addr        = f.io.busMaster.addr.peek()
      val writeEnable = f.io.busMaster.writeEnable.peek()
      if (writeEnable.litToBoolean) {
        // 通常Writeはしないはず...
        assert(false)
        f.io.busMaster.valid.poke(false.B)
        f.io.busMaster.dataOut.poke(0x00.U) // Don't care
        None
      } else {
        val data = extMem(addr.litValue.toInt)
        // Arbiterの応答にセット
        f.io.busMaster.valid.poke(true.B)
        f.io.busMaster.dataOut.poke(data)
        Some(data)
      }
    }
  }

  "Single fetch verification" in {
    test(new Fetch) { dut =>
      {
        // 初期化
        setDisableReq(dut)
        dut.clock.step(1)
        expectInvalidFetchData(dut)
        expectIdle(dut)

        // 最低限のパターン
        setRequest(dut, 0x0000.U)
        dut.clock.step(1) // 1cycで現在の内容をRegに控えてArbiterに要求
        simExtMem(dut, testMem, false)
        expectInvalidFetchData(dut)
        expectBusy(dut)

        setAfterRequest(dut)
        dut.clock.step(1) // 2cyc目で応答が来る
        simExtMem(dut, testMem, false)
        expectValidFetchData(dut, 0x0000.U, 0xea.U, Instruction.nop, Addressing.implied)
        expectIdle(dut)

        dut.clock.step(10) // その後はIdleのまま
        simExtMem(dut, testMem, false)
        expectValidFetchData(dut, 0x0000.U, 0xea.U, Instruction.nop, Addressing.implied)
        expectIdle(dut)
      }
    }
  }

  "Sequential fetch verification" in {
    test(new Fetch) { dut =>
      {
        // 初期化
        setDisableReq(dut)
        dut.clock.step(1)
        expectInvalidFetchData(dut)
        expectIdle(dut)

        // 1要求目のRead中に2要求目を出してあって、順繰り処理するパターン

        // T0: PC=0x0000でFetch有効化
        setRequest(dut, 0x0000.U)
        dut.clock.step(1)
        simExtMem(dut, testMem, false)
        expectInvalidFetchData(dut)
        expectBusy(dut)

        // T1: T0のFetchが終わっていないので待機
        setAfterRequest(dut)
        dut.clock.step(1)
        expectValidFetchData(dut, 0x0000.U, 0xea.U, Instruction.nop, Addressing.implied)
        expectIdle(dut)

        // T2: 0x0000がEXで処理を始めたので、次をPrefetch要求
        setRequest(dut, 0x0001.U)
        dut.clock.step(1)
        simExtMem(dut, testMem, false)
        // クリアしていないので、前回の結果はそのまま残っている
        expectValidFetchData(dut, 0x0000.U, 0xea.U, Instruction.nop, Addressing.implied)
        expectBusy(dut)

        // T3: T2のFetchが終わっていないので待機
        setAfterRequest(dut)
        dut.clock.step(1)
        expectValidFetchData(dut, 0x0001.U, 0xe8.U, Instruction.inx, Addressing.implied)
        expectIdle(dut)
      }
    }
  }

  "Verify data retention and destruction" in {
    test(new Fetch) { dut =>
      {
        // 初期化
        setDisableReq(dut)
        dut.clock.step(1)
        expectInvalidFetchData(dut)
        expectIdle(dut)

        // 最低限のパターン
        setRequest(dut, 0x0007.U)
        dut.clock.step(1) // 1cycで現在の内容をRegに控えてArbiterに要求
        simExtMem(dut, testMem, false)
        expectInvalidFetchData(dut)
        expectBusy(dut)

        setAfterRequest(dut)
        dut.clock.step(1) // 2cyc目で応答が来る
        simExtMem(dut, testMem, false)
        expectValidFetchData(dut, 0x0007.U, 0x4c.U, Instruction.jmp, Addressing.absolute)
        expectIdle(dut)

        dut.clock.step(10) // その後時間経過してもIdleのままで結果だけが保持される
        simExtMem(dut, testMem, false)
        expectValidFetchData(dut, 0x0007.U, 0x4c.U, Instruction.jmp, Addressing.absolute)
        expectIdle(dut)

        setDiscard(dut)
        dut.clock.step(1) // その後時間経過してもIdleのままで結果だけが保持される
        simExtMem(dut, testMem, false)
        expectInvalidFetchData(dut)
        expectIdle(dut)

        clearDiscard(dut)
        dut.clock.step(10) // その後時間経過してもIdleのままで結果もクリアされたまま
        simExtMem(dut, testMem, false)
        expectInvalidFetchData(dut)
        expectIdle(dut)
      }
    }
  }

  "Request a fetch and destroy at the same time" in {
    test(new Fetch) { dut =>
      {
        // 初期化
        setDisableReq(dut)
        dut.clock.step(1)
        expectInvalidFetchData(dut)
        expectIdle(dut)

        // 適当な命令を読ませる
        setRequest(dut, 0x0007.U)
        dut.clock.step(1) // 1cycで現在の内容をRegに控えてArbiterに要求
        simExtMem(dut, testMem, false)
        expectInvalidFetchData(dut)
        expectBusy(dut)

        setAfterRequest(dut)
        dut.clock.step(1) // 2cyc目で応答が来る
        simExtMem(dut, testMem, false)
        expectValidFetchData(dut, 0x0007.U, 0x4c.U, Instruction.jmp, Addressing.absolute)
        expectIdle(dut)

        // データが出力された事前状態の完成
        dut.clock.step(3)
        simExtMem(dut, testMem, false)
        expectValidFetchData(dut, 0x0007.U, 0x4c.U, Instruction.jmp, Addressing.absolute)
        expectIdle(dut)

        // 適当な命令を読ませつつ、データ破棄要求する
        setRequest(dut, 0x0000.U)
        setDiscard(dut)
        dut.clock.step(1) // 1cycで現在の内容をRegに控えてArbiterに要求
        simExtMem(dut, testMem, false)
        expectInvalidFetchData(dut) // データ破棄を確認
        expectBusy(dut)             // Readは継続

        setAfterRequest(dut)
        clearDiscard(dut)
        dut.clock.step(1) // 2cyc目で応答が来る
        simExtMem(dut, testMem, false)
        expectValidFetchData(dut, 0x0000.U, 0xea.U, Instruction.nop, Addressing.implied)
        expectIdle(dut)

      }
    }
  }

  "Request Fetch and Discard at the same time, leaving the Discard request untouched" in {
    test(new Fetch) { dut =>
      {
        // 初期化
        setDisableReq(dut)
        dut.clock.step(1)
        expectInvalidFetchData(dut)
        expectIdle(dut)

        // 適当な命令を読ませる
        setRequest(dut, 0x0007.U)
        dut.clock.step(1) // 1cycで現在の内容をRegに控えてArbiterに要求
        simExtMem(dut, testMem, false)
        expectInvalidFetchData(dut)
        expectBusy(dut)

        setAfterRequest(dut)
        dut.clock.step(1) // 2cyc目で応答が来る
        simExtMem(dut, testMem, false)
        expectValidFetchData(dut, 0x0007.U, 0x4c.U, Instruction.jmp, Addressing.absolute)
        expectIdle(dut)

        // データが出力された事前状態の完成
        dut.clock.step(3)
        simExtMem(dut, testMem, false)
        expectValidFetchData(dut, 0x0007.U, 0x4c.U, Instruction.jmp, Addressing.absolute)
        expectIdle(dut)

        // 適当な命令を読ませつつ、データ破棄要求する
        setRequest(dut, 0x0000.U)
        setDiscard(dut)
        dut.clock.step(1) // 1cycで現在の内容をRegに控えてArbiterに要求
        simExtMem(dut, testMem, false)
        expectInvalidFetchData(dut) // データ破棄を確認
        expectBusy(dut)             // Readは継続

        // データ破棄要求を保持する
        dut.clock.step(10)
        simExtMem(dut, testMem, false)
        expectInvalidFetchData(dut) // データ破棄を確認
        expectBusy(dut)             // Readは継続

        // データ破棄を解除すると、次cycで結果が入る
        setAfterRequest(dut)
        clearDiscard(dut)
        dut.clock.step(1) // 2cyc目で応答が来る
        simExtMem(dut, testMem, false)
        expectValidFetchData(dut, 0x0000.U, 0xea.U, Instruction.nop, Addressing.implied)
        expectIdle(dut)

      }
    }
  }

  "Bus is busy and read response is delayed" in {
    test(new Fetch) { dut =>
      {
        // 初期化
        setDisableReq(dut)
        dut.clock.step(1)
        expectInvalidFetchData(dut)
        expectIdle(dut)

        // 適当な命令を読ませる
        setRequest(dut, 0x0007.U)
        dut.clock.step(1) // 1cycで現在の内容をRegに控えてArbiterに要求
        simExtMem(dut, testMem, busy = true)
        expectInvalidFetchData(dut)
        expectBusy(dut)

        // Busyなので応答が来ない
        setAfterRequest(dut)
        dut.clock.step(1)
        simExtMem(dut, testMem, busy = true)
        expectInvalidFetchData(dut)
        expectBusy(dut)

        dut.clock.step(10)
        simExtMem(dut, testMem, busy = true)
        expectInvalidFetchData(dut)
        expectBusy(dut)

        // Busyではなくなると応答が来る
        dut.clock.step(1)
        simExtMem(dut, testMem, busy = false) // read結果がdataoutに乗る
        expectInvalidFetchData(dut)
        expectBusy(dut)

        dut.clock.step(1) // dataoutの結果を取得、及びデコード
        simExtMem(dut, testMem, busy = false)
        expectValidFetchData(dut, 0x0007.U, 0x4c.U, Instruction.jmp, Addressing.absolute)
        expectIdle(dut)
      }
    }
  }

  "Request during request" in {
    test(new Fetch) { dut =>
      {
        // 初期化
        setDisableReq(dut)
        dut.clock.step(1)
        expectInvalidFetchData(dut)
        expectIdle(dut)

        // 適当な命令を読ませる
        setRequest(dut, 0x0007.U)
        dut.clock.step(1) // 1cycで現在の内容をRegに控えてArbiterに要求
        simExtMem(dut, testMem, busy = true)
        expectInvalidFetchData(dut)
        expectBusy(dut)

        // Busyなので応答が来ない
        setAfterRequest(dut)
        dut.clock.step(1)
        simExtMem(dut, testMem, busy = true)
        expectInvalidFetchData(dut)
        expectBusy(dut)

        // この要求は受け付けないことが期待
        setRequest(dut, 0x0000.U)
        dut.clock.step(1)
        simExtMem(dut, testMem, busy = true)
        expectInvalidFetchData(dut)
        expectBusy(dut)

        // Busyなので応答が来ない
        setAfterRequest(dut)
        dut.clock.step(1)
        simExtMem(dut, testMem, busy = true)
        expectInvalidFetchData(dut)
        expectBusy(dut)

        // Busyではなくなると応答が来る
        dut.clock.step(1)
        simExtMem(dut, testMem, busy = false) // read結果がdataoutに乗る
        expectInvalidFetchData(dut)
        expectBusy(dut)

        dut.clock.step(1) // dataoutの結果を取得、及びデコード
        simExtMem(dut, testMem, busy = false)
        expectValidFetchData(dut, 0x0007.U, 0x4c.U, Instruction.jmp, Addressing.absolute)
        expectIdle(dut)
      }
    }
  }

  "Request data destruction during read" in {
    test(new Fetch) { dut =>
      {
        // 初期化
        setDisableReq(dut)
        dut.clock.step(1)
        expectInvalidFetchData(dut)
        expectIdle(dut)

        // 適当な命令を読ませる
        setRequest(dut, 0x0007.U)
        dut.clock.step(1) // 1cycで現在の内容をRegに控えてArbiterに要求
        simExtMem(dut, testMem, false)
        expectInvalidFetchData(dut)
        expectBusy(dut)

        setAfterRequest(dut)
        dut.clock.step(1) // 2cyc目で応答が来る
        simExtMem(dut, testMem, false)
        expectValidFetchData(dut, 0x0007.U, 0x4c.U, Instruction.jmp, Addressing.absolute)
        expectIdle(dut)

        // データが出力された事前状態の完成
        dut.clock.step(3)
        simExtMem(dut, testMem, false)
        expectValidFetchData(dut, 0x0007.U, 0x4c.U, Instruction.jmp, Addressing.absolute)
        expectIdle(dut)

        // 適当な命令を読ませる
        setRequest(dut, 0x0000.U)
        dut.clock.step(1) // 1cycで現在の内容をRegに控えてArbiterに要求
        simExtMem(dut, testMem, busy = true)
        expectValidFetchData(dut, 0x0007.U, 0x4c.U, Instruction.jmp, Addressing.absolute) // まだ前のデータが生きている
        expectBusy(dut)                                                                   // Readは継続

        // Read結果は来ないが前回の結果を保持
        setAfterRequest(dut)
        dut.clock.step(1)
        simExtMem(dut, testMem, busy = true)
        expectValidFetchData(dut, 0x0007.U, 0x4c.U, Instruction.jmp, Addressing.absolute) // まだ前のデータが生きている
        expectBusy(dut)                                                                   // Readは継続

        // Read結果は来ないが前回の結果を破棄
        setDiscard(dut)
        dut.clock.step(1)
        simExtMem(dut, testMem, busy = true)
        expectInvalidFetchData(dut)
        expectBusy(dut) // Readは継続

        // データは破棄したが、応答は来ないまま
        clearDiscard(dut)
        dut.clock.step(1)
        simExtMem(dut, testMem, busy = true)
        expectInvalidFetchData(dut)
        expectBusy(dut) // Readは継続

        // まだ来ない
        dut.clock.step(10)
        simExtMem(dut, testMem, busy = true)
        expectInvalidFetchData(dut)
        expectBusy(dut) // Readは継続

        // この途中で要求しても何も起きない
        setRequest(dut, 0x000f.U)
        dut.clock.step(1)
        simExtMem(dut, testMem, busy = true)
        expectInvalidFetchData(dut)
        expectBusy(dut) // Readは継続

        // 要求解除
        setAfterRequest(dut)
        dut.clock.step(1)
        simExtMem(dut, testMem, busy = true)
        expectInvalidFetchData(dut)
        expectBusy(dut) // Readは継続

        // Readが通る
        dut.clock.step(1)
        simExtMem(dut, testMem, busy = false) // dataoutされる
        expectInvalidFetchData(dut)
        expectBusy(dut) // Readは継続

        // Read結果が来る, 途中に要求した0xfではないことを確認
        dut.clock.step(1) // dataoutした内容をCopy、またDecode
        simExtMem(dut, testMem, busy = false)
        expectValidFetchData(dut, 0x0000.U, 0xea.U, Instruction.nop, Addressing.implied)
        expectIdle(dut)
      }
    }
  }
}
