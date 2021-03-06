package bus

import chisel3._
import chisel3.util.Cat

import common.TriState
import types.BusIO

/** Address Bus/Data Busの調停を行う
 *  execute > interrupt > fetchの優先度で調停する想定
 *  自アクセスが有効であるかは、reqをアサート後、次サイクル以後のvalidで判断する
 *  @param n 生成するポート数。1以上に設定する必要がある
 */
class BusArbiter(val n: Int) extends Module {
  assert(n > 0) // 最低1portは必要
  val io = IO(new Bundle {
    // CPU内部のBus Masterと接続する
    val slavePorts = Vec(n, new BusIO())
    // アドレス出力、Master固定
    val extAddr = Output(UInt(16.W))
    // 外部からのデータ入力
    val extDataIn = Input(UInt(8.W))
    // 外部へのデータ出力
    val extDataOut = Output(TriState(UInt(8.W)))
    // Writeが有効ならtrue、Readが有効ならfalse
    val writeEnable = Output(Bool())
    // (debug用)アクセス要求中BusMasterのステータス
    val debugReq = Output(UInt(n.W))
    // (debug用)アクセス要求を通しているBusMaterのステータス
    val debugValid = Output(UInt(n.W))
  })

  val addrReg        = RegInit(UInt(16.W), 0.U)
  val writeEnableReg = RegInit(Bool(), false.B)
  val dataOutReg     = RegInit(UInt(8.W), 0.U)
  val validReg       = RegInit(UInt(n.W), 0.U)

  // 共通
  io.extAddr     := addrReg
  io.writeEnable := writeEnableReg
  io.debugReq    := Cat(io.slavePorts.map(_.req)) // MSB: slavePorts[n] ~ LSB: slavePorts[0]
  io.debugValid  := validReg                      // for debug
  // BusMasterデータ出力→内部でReg受けして外部素子にデータ入力
  io.extDataOut.data := dataOutReg
  io.extDataOut.oe   := writeEnableReg
  // 外部データ出力→内部BusMasterデータ入力は常に見せておいて、Validで判断
  io.slavePorts.zipWithIndex.foreach {
    case (p: BusIO, i: Int) => {
      p.dataOut := io.extDataIn
      p.valid   := validReg(i.U)
    }
  }

  // slavePortsのindexが小さいほど優先度が高く要求を通す
  when(Cat(io.slavePorts.map(_.req)).orR) {
    for (index <- (0 until n).reverse) { // chiselの生成物を見る限り、最後に代入したものが採用される模様
      val p = io.slavePorts(index)
      when(p.req) {
        // 要求を外部に通す
        addrReg        := p.addr
        writeEnableReg := p.writeEnable
        dataOutReg     := p.dataIn            // Readの場合はWriteEnable=falseなので特に参照されない
        validReg       := (1 << index).U(n.W) // BusMaster側が読み取る
      }
    }
  }.otherwise {
    // addr, dataOutは不問、WriteEnableは立てず開放しておく
    addrReg        := DontCare
    writeEnableReg := false.B
    dataOutReg     := DontCare
    validReg       := 0.U
  }
}
