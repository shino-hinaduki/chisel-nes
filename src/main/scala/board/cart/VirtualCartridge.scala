package board.cart

import chisel3._

import board.cart.types.CartridgeIO
import board.cart.types.NesFileFormat
import board.access.types.InternalAccessCommand
import board.ram.types.RamIO
import board.jtag.types.VirtualInstruction
import chisel3.util.MuxLookup
import board.cart.types.NameTableMirror

/**
  * VirtualCartridgeでRAM AccessをDebugAccess側から行うときの定義
  *
  * @param accessTarget アクセス対象の識別子
  * @param ram DPRAM I/F
  * @param addrWidth ram のアドレス幅
  * @param dap DebugAccess I/F
  * @param clock 動作するClock Domain
  * @param reset リセット信号
  */
case class VirtualCartDebugAccessDef(
    accessTarget: VirtualInstruction.AccessTarget.Type,
    ram: RamIO,
    addrWidth: Int,
    dap: InternalAccessCommand.SlaveIO,
    clock: Clock,
    reset: Reset
)

/**
  * Cartridge実機を模倣したモジュール定義
  * clock,resetにはCPU Clock domainのものを指定(Mapper-DAP間のClock Crossing考慮が面倒)
  * 
  * @param prgRomAddrWidth PRG-ROMのアドレス空間
  * @param saveRamAddrWidth SAVE-RAMのアドレス空間
  * @param chrRomAddrWidth PRG-ROMのアドレス空間
  * @param emuDataWidth エミュレータからアクセスする時のデータ幅、変更不要のはず
  * @param debugDataWidth DebugAccess経由でアクセスする時のデータ幅
  */
class VirtualCartridge(
    val prgRomAddrWidth: Int = 17,
    val saveRamAddrWidth: Int = 13,
    val chrRomAddrWidth: Int = 17,
    val emuDataWidth: Int = 8,
    val debugDataWidth: Int = 32,
) extends Module {
  // .iNES Fileのヘッダ
  val inesFileHeaderBytes: Int = 16
  // Commonとして確保するレジスタ数。
  val commonReservedBytes: Int = 16
  // debugAccessCommonでアクセスできる領域
  val commonBytes: Int = inesFileHeaderBytes + commonReservedBytes
  // debugAccessCommonでアクセスできる領域(4byte単位)
  val commonWords: Int = commonBytes / 4

  val io = IO(new Bundle {
    // 本物のCartridgeに入れる信号と同じもの
    val cart = new CartridgeIO
    // VirtualCartridgeを利用する場合はtrue
    val isEnable = Input(Bool())
    // PPU Clock
    val ppuClock = Input(Clock())
    // PPU ClockのReset
    val ppuReset = Input(Reset())

    // DPRAMをPRG-ROM, SAVE-RAM, CHR-ROMの3種類置く。VJTAGから書き込む側を別ポートにする
    val prgRamEmu  = Flipped(new RamIO(addrWidth = prgRomAddrWidth, dataWidth = emuDataWidth))
    val saveRamEmu = Flipped(new RamIO(addrWidth = saveRamAddrWidth, dataWidth = emuDataWidth))
    val chrRamEmu  = Flipped(new RamIO(addrWidth = chrRomAddrWidth, dataWidth = emuDataWidth))
    // 4byteごとのアクセスなので、アクセスレンジが2bit分減る
    val prgRamDebug  = Flipped(new RamIO(addrWidth = prgRomAddrWidth - 2, dataWidth = debugDataWidth))
    val saveRamDebug = Flipped(new RamIO(addrWidth = saveRamAddrWidth - 2, dataWidth = debugDataWidth))
    val chrRamDebug  = Flipped(new RamIO(addrWidth = chrRomAddrWidth - 2, dataWidth = debugDataWidth))

    // VJTAGからデータの読み書きを実現する
    val debugAccessCommon = new InternalAccessCommand.SlaveIO
    val debugAccessPrg    = new InternalAccessCommand.SlaveIO
    val debugAccessSave   = new InternalAccessCommand.SlaveIO
    val debugAccessChr    = new InternalAccessCommand.SlaveIO

    // for debug
    // debugAccessCommonでWriteされたiNES Headerが正しい値ならtrue
    val isValidHeader = Output(Bool())
  })

  /*********************************************************************/
  /* DebugAccessReq: Common(先頭16byte iNES Header, 後半reserved)       */
  val commonRegsByCpu = withClockAndReset(clock, reset) { RegInit(VecInit(Seq.fill(commonWords)(0.U(debugDataWidth.W)))) } // 4byteごと
  withClockAndReset(clock, reset) {
    val commonReqDeqReg   = RegInit(Bool(), false.B)
    val commonRespDataReg = RegInit(Bool(), false.B)
    val commonRespEnqReg  = RegInit(Bool(), false.B)
    io.debugAccessCommon.req.rdclk  := clock
    io.debugAccessCommon.req.rdreq  := commonReqDeqReg
    io.debugAccessCommon.resp.wrclk := clock
    io.debugAccessCommon.resp.wrreq := commonRespEnqReg
    io.debugAccessCommon.resp.data  := commonRespDataReg
    io.isValidHeader                := NesFileFormat.isValidHeader(commonRegsByCpu)
    // 要求を引き抜き
    def commonReqDequeue() = {
      commonReqDeqReg := true.B
    }
    // 要求を引き抜かない
    def commonReqNop() = {
      commonReqDeqReg := false.B
    }
    // 応答を積む
    def commonRespEnqueue(data: UInt) = {
      commonRespDataReg := data
      commonRespEnqReg  := true.B
    }
    // 応答を積まない
    def commonRespNop() = {
      commonRespDataReg := DontCare
      commonRespEnqReg  := false.B
    }

    // Queue Remainがあれば取り出す。OoOにはしない
    when(!io.debugAccessCommon.req.rdempty && !commonReqDeqReg) {
      val offset             = InternalAccessCommand.Request.getOffset(io.debugAccessCommon.req.q)
      val offsetValid        = offset < commonWords.U
      val writeData          = InternalAccessCommand.Request.getData(io.debugAccessCommon.req.q)
      val (reqType, isValid) = InternalAccessCommand.Request.getRequestType(io.debugAccessCommon.req.q)

      when(!offsetValid || !isValid) {
        // 未対応、Dequeueだけ実施
        commonReqDequeue()
        commonRespNop()
      }.elsewhen(reqType === InternalAccessCommand.Type.read) {
        // Read & Dequeue
        val readData = commonRegsByCpu(offset)
        commonReqDequeue()
        commonRespEnqueue(readData)
      }.elsewhen(reqType === InternalAccessCommand.Type.write) {
        // Write & Dequeue
        commonRegsByCpu(offset) := writeData
        commonReqDequeue()
        commonRespNop()
      }
    }.otherwise {
      // なにもしない, Enq/Deq中のものは解除
      commonReqNop()
      commonRespNop()
    }
  }

  // CPU Clock -> PPU Clock載せ替え
  val commonRegsByPpu = withClockAndReset(io.ppuClock, io.ppuReset) { RegInit(VecInit(Seq.fill(commonWords)(0.U(debugDataWidth.W)))) } // 4byteごと
  withClockAndReset(io.ppuClock, io.ppuReset) {
    commonRegsByPpu := RegNext(commonRegsByCpu)
  }

  /*********************************************************************/
  /* DebugAccessReq: Others                                            */
  // DebugAccessPort-DPRAM-Clock Domainの組を定義して、まとめて実装する
  val debugAccessTargets: Seq[VirtualCartDebugAccessDef] = Seq(
    VirtualCartDebugAccessDef(VirtualInstruction.AccessTarget.cartPrg, io.prgRamDebug, prgRomAddrWidth, io.debugAccessPrg, clock, reset),
    VirtualCartDebugAccessDef(VirtualInstruction.AccessTarget.cartSave, io.saveRamDebug, saveRamAddrWidth, io.debugAccessSave, clock, reset),
    VirtualCartDebugAccessDef(VirtualInstruction.AccessTarget.cartChr, io.chrRamDebug, chrRomAddrWidth, io.debugAccessChr, io.ppuClock, io.ppuReset),
  )

  // 使用するレジスタは先に宣言しておく
  val ramAddrRegs  = RegInit(VecInit(Seq.fill(debugAccessTargets.size)(0.U)))
  val ramDataRegs  = RegInit(VecInit(Seq.fill(debugAccessTargets.size)(0.U)))
  val ramRdRegs    = RegInit(VecInit(Seq.fill(debugAccessTargets.size)(false.B)))
  val ramWrRegs    = RegInit(VecInit(Seq.fill(debugAccessTargets.size)(false.B)))
  val reqDeqRegs   = RegInit(VecInit(Seq.fill(debugAccessTargets.size)(false.B)))
  val respDataRegs = RegInit(VecInit(Seq.fill(debugAccessTargets.size)(0.U)))
  val respEnqRegs  = RegInit(VecInit(Seq.fill(debugAccessTargets.size)(false.B)))

  // 処理本体
  debugAccessTargets.zipWithIndex.foreach {
    case (x, index) => {
      // temp regs
      val ramAddrReg  = ramAddrRegs(index)
      val ramDataReg  = ramDataRegs(index)
      val ramRdReg    = ramRdRegs(index)
      val ramWrReg    = ramWrRegs(index)
      val reqDeqReg   = reqDeqRegs(index)
      val respDataReg = respDataRegs(index)
      val respEnqReg  = respEnqRegs(index)

      // DebugAccessPort
      x.dap.req.rdclk  := x.clock
      x.dap.req.rdreq  := reqDeqReg
      x.dap.resp.wrclk := x.clock
      x.dap.resp.wrreq := respEnqReg
      x.dap.resp.data  := respDataReg

      // DPRAM
      x.ram.address := ramAddrReg
      x.ram.clock   := x.clock
      x.ram.data    := ramDataReg
      x.ram.rden    := ramRdReg
      x.ram.wren    := ramWrReg

      // Request Dequeue
      when(!x.dap.req.rdempty && !reqDeqReg) {
        val offset             = InternalAccessCommand.Request.getOffset(x.dap.req.q)
        val writeData          = InternalAccessCommand.Request.getData(x.dap.req.q)
        val (reqType, isValid) = InternalAccessCommand.Request.getRequestType(x.dap.req.q)

        when(!isValid) {
          // dequeueだけ実施
          ramAddrReg := DontCare
          ramDataReg := DontCare
          ramRdReg   := false.B
          ramWrReg   := false.B
          reqDeqReg  := true.B
        }.elsewhen(reqType === InternalAccessCommand.Type.read) {
          // DPRAMにReadを投げる
          ramAddrReg := offset
          ramDataReg := DontCare
          ramRdReg   := true.B
          ramWrReg   := false.B
          reqDeqReg  := true.B
        }.elsewhen(reqType === InternalAccessCommand.Type.write) {
          // DPRAMにWriteを投げる
          ramAddrReg := offset
          ramDataReg := writeData
          ramRdReg   := false.B
          ramWrReg   := true.B
          reqDeqReg  := true.B
        }
      }.otherwise {
        // 何もしない。Reqはすべて落とす
        ramAddrReg := DontCare
        ramDataReg := DontCare
        ramRdReg   := false.B
        ramWrReg   := false.B
        reqDeqReg  := false.B
      }

      // Read応答を積む
      when(ramRdReg) {
        respDataReg := x.ram.q
        respEnqReg  := false.B
      }.otherwise {
        respDataReg := DontCare
        respEnqReg  := false.B
      }
    }
  }

  /*********************************************************************/
  /* Access From CPU Bus                                               */
  // TODO: Mapper切り替え

  // IRQなし
  io.cart.cpu.nIrq := true.B

  // PRG ROM固定 /ROMSEL時にデータが出力されるようにする
  val nOePrg = io.cart.cpu.nRomSel
  io.cart.cpu.dataIn   := Mux(!nOePrg, io.prgRamEmu.q, DontCare)
  io.prgRamEmu.address := io.cart.cpu.address
  io.prgRamEmu.clock   := clock
  io.prgRamEmu.data    := io.cart.cpu.dataOut.getData()
  io.prgRamEmu.rden    := !nOePrg
  io.prgRamEmu.wren    := false.B // Writeしない
  // Save RAMはない
  io.saveRamEmu.address := DontCare
  io.saveRamEmu.clock   := clock
  io.saveRamEmu.data    := DontCare
  io.saveRamEmu.rden    := false.B
  io.saveRamEmu.wren    := false.B

  /*********************************************************************/
  /* Access From PPU Bus                                               */
  // TODO: Mapper切り替え

  // /VRAMCS は /PA13直結
  io.cart.ppu.nVramCs := !io.cart.ppu.address(13)
  // Nametable MirrorはHeaderから決定
  io.cart.ppu.vrama10 := Mux(
    NesFileFormat.nameTableMirror(commonRegsByPpu) === NameTableMirror.Horizontal,
    io.cart.ppu.address(10),
    io.cart.ppu.address(11)
  )
  // CHR-ROM PA13で/CS, /RDで/OE。 PA10/11でVRAMA10切り替え
  val nCsChr = io.cart.ppu.address(13)
  val nOeChr = io.cart.ppu.nRd

  io.cart.ppu.dataIn   := Mux(!nCsChr && !nOeChr, io.chrRamEmu.q, DontCare)
  io.chrRamEmu.address := io.cart.ppu.address
  io.chrRamEmu.clock   := io.ppuClock
  io.chrRamEmu.data    := io.cart.ppu.dataOut.getData()
  io.chrRamEmu.rden    := !nCsChr && !nOeChr
  io.chrRamEmu.wren    := false.B

}
