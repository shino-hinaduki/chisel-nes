package board.discrete

import chisel3._
import chisel3.util.Cat

/**
  * ボタン/スイッチ入力のチャタリング除去を行う。dataIn/dataOut間の論理は変化しない
  *
  * @param inputWidth 入力データの幅
  * @param averageNum 平均化数
  * @param isActiveLow ボタンが押されたときにLowになる場合はtrue
  */
class Debounce(val inputWidth: Int, val averageNum: Int, val isActiveLow: Boolean) extends Module {
  // outputReg初期化時のデータ。負論理の場合はAll 1として取り扱う
  val initOutputData =
    if (isActiveLow) { ~(0.U(averageNum.W)) }
    else { 0.U(averageNum.W) }

  val io = IO(new Bundle {
    // データ入力
    val dataIn = Input(UInt(inputWidth.W))
    // データ出力
    val dataOut = Output(UInt(inputWidth.W))
  })

  // 生データ格納先.  bit方向に履歴, index方向にdata, 格納するデータは正論理
  //   io.dataIn(x) ==>> historyRegs(x)(履歴...) == reduce AND ==>> io.dataOut(x)
  //   (==>> で示した部分は isActiveLow = true なら反転して取り扱う)
  val historyRegs = RegInit(VecInit(Seq.fill(inputWidth)(0.U(averageNum.W))))
  val outputReg   = RegInit(UInt(inputWidth.W), initOutputData)

  // 全データ更新する
  for (i <- 0 until inputWidth) {
    // 論理を補正したデータ
    val inData =
      if (isActiveLow) { ~(io.dataIn(i)) }
      else { io.dataIn(i) }

    // historyRegsのLowerから結合する
    val historyReg = historyRegs(i)
    historyReg := Cat(historyReg((averageNum - 1), 1), inData)

    // outputRegにはReductionした結果を入れるが、負論理なら反転させておく
    val outData =
      if (isActiveLow) { ~(historyReg.andR) }
      else { historyReg.andR }
    outputReg(i) := outData
  }
}
