package cpu.fetch

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import cpu.types.Instruction
import cpu.types.Addressing

class FetchSpec extends AnyFreeSpec with ChiselScalatestTester {
  // バスアクセス確認用の単純なパターン
  val testMem = Seq(
    0xea.U, // NOP Implied
    0xe8.U, // INX Implied
    0xc8.U, // INY Implied
    0xa9.U, // LDA Immediate
    0x00.U, // (op0)
    0xca.U, // DEX Implied
    0x88.U, // DEY Implied
    0x4c.U, // JMP Absolute
    0x00.U, // (op0)
    0x00.U, // (op1)
    0xea.U, // NOP Implied
    0xea.U, // NOP Implied
    0xea.U, // NOP Implied
    0xea.U  // NOP Implied
  );

  /* EXからの設定 */
  // PCの現在値を指定した上で、Fetchを有効化します
  def setEnableReq(f: Fetch, pc: UInt) = {
    f.io.control.req.poke(true.B)
    f.io.control.pc.poke(pc)
    f.io.control.discardByEx.poke(false.B)
    f.io.control.discardByInt.poke(false.B)
  }

  // PCの現在値付きを指定した上で、Fetchを有効化します
  def setDisableReq(f: Fetch) = {
    f.io.control.req.poke(false.B)
    f.io.control.pc.poke(0xffff.U) // Don't care
    f.io.control.discardByEx.poke(false.B)
    f.io.control.discardByInt.poke(false.B)
  }

  // EX要因でのFetchデータ破棄を実行します
  def setDiscardByEx(f: Fetch) = {
    f.io.control.req.poke(false.B) // Don't care
    f.io.control.pc.poke(0xffff.U) // Don't care
    f.io.control.discardByEx.poke(true.B)
    f.io.control.discardByInt.poke(false.B)
  }

  // EX要因でのFetchデータ破棄を実行します
  def setDiscardByInt(f: Fetch) = {
    f.io.control.req.poke(true.B)  // Don't care だが、EXに無関係に通知されたと想定する
    f.io.control.pc.poke(0xffff.U) // Don't care
    f.io.control.discardByEx.poke(false.B)
    f.io.control.discardByInt.poke(true.B)
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

        // 最低限のパターン
        setEnableReq(dut, 0x0000.U)
        dut.clock.step(1) // 1cycで現在の内容をRegに控えてArbiterに要求
        simExtMem(dut, testMem, false)
        expectInvalidFetchData(dut)

        dut.clock.step(1) // 2cyc目で応答が来る
        simExtMem(dut, testMem, false)
        expectValidFetchData(dut, 0x0000.U, 0xea.U, Instruction.nop, Addressing.implied)
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

        // 1要求目のRead中に2要求目を出してあって、順繰り処理するパターン

        // T0: PC=0x0000でFetch有効化
        setEnableReq(dut, 0x0000.U)
        dut.clock.step(1)
        simExtMem(dut, testMem, false)
        expectInvalidFetchData(dut)

        // T1: T0のFetchが終わっていないので待機
        dut.clock.step(1)
        expectValidFetchData(dut, 0x0000.U, 0xea.U, Instruction.nop, Addressing.implied)

        // T2: 0x0000がEXで処理を始めたので、次をPrefetch要求
        setEnableReq(dut, 0x0001.U)
        dut.clock.step(1)
        simExtMem(dut, testMem, false)
        // クリアしていないので、前回の結果はそのまま残っている
        expectValidFetchData(dut, 0x0000.U, 0xea.U, Instruction.nop, Addressing.implied)

        // T3: T2のFetchが終わっていないので待機
        dut.clock.step(1)
        expectValidFetchData(dut, 0x0001.U, 0xe8.U, Instruction.inx, Addressing.implied)
      }
    }
  }

}
