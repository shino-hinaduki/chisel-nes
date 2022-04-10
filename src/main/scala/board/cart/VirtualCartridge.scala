package board.cart

import chisel3._

import board.cart.types.CartridgeIO
import board.access.types.InternalAccessCommand
import board.ram.types.RamIO
import board.jtag.types.VirtualInstruction

/**
  * VirtualCartridgeでRAM AccessをDebugAccess側から行うときの定義
  *
  * @param accessTarget
  * @param ram
  * @param dap
  * @param clock
  * @param reset
  */
case class VirtualCartDebugAccessDef(accessTarget: VirtualInstruction.AccessTarget.Type, ram: RamIO, dap: InternalAccessCommand.SlaveIO, clock: Clock, reset: Reset)

/**
  * Cartridge実機を模倣したモジュール定義
  * clock,resetにはCPU Clock domainのものを指定(Mapper-DAP間のClock Crossing考慮が面倒)
  * 
  * @param prgRomAddrWidth PRG-ROMのアドレス空間
  * @param chrRomAddrWidth PRG-ROMのアドレス空間
  * @param emuDataWidth エミュレータからアクセスする時のデータ幅、変更不要のはず
  * @param debugDataWidth DebugAccess経由でアクセスする時のデータ幅
  */
class VirtualCartridge(
    val prgRomAddrWidth: Int = 17,
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
    val saveRamEmu = Flipped(new RamIO(addrWidth = prgRomAddrWidth, dataWidth = emuDataWidth))
    val chrRamEmu  = Flipped(new RamIO(addrWidth = chrRomAddrWidth, dataWidth = emuDataWidth))

    val prgRamDebug  = Flipped(new RamIO(addrWidth = prgRomAddrWidth, dataWidth = debugDataWidth))
    val saveRamDebug = Flipped(new RamIO(addrWidth = prgRomAddrWidth, dataWidth = debugDataWidth))
    val chrRamDebug  = Flipped(new RamIO(addrWidth = chrRomAddrWidth, dataWidth = debugDataWidth))

    // VJTAGからデータの読み書きを実現する
    val debugAccessCommon = new InternalAccessCommand.SlaveIO
    val debugAccessPrg    = new InternalAccessCommand.SlaveIO
    val debugAccessSave   = new InternalAccessCommand.SlaveIO
    val debugAccessChr    = new InternalAccessCommand.SlaveIO
  })

  /*********************************************************************/
  /* DebugAccessReq: Common(先頭16byte iNES Header, 後半reserved)       */
  withClockAndReset(clock, reset) {
    val commonRegs      = RegInit(VecInit(Seq.fill(commonWords)(0.U(debugDataWidth.W)))) // 4byteごと
    val commonRdDataReg = RegInit(Bool(), false.B)
    val commonRdDeqReg  = RegInit(Bool(), false.B)
    val commonRdEnqReg  = RegInit(Bool(), false.B)
    io.debugAccessCommon.req.rdclk  := clock
    io.debugAccessCommon.req.rdreq  := commonRdDeqReg
    io.debugAccessCommon.resp.wrclk := clock
    io.debugAccessCommon.resp.wrreq := commonRdEnqReg
    io.debugAccessCommon.resp.data  := commonRdDataReg
    // 要求を引き抜き
    def commonReqDequeue() = {
      commonRdDeqReg := true.B
    }
    // 要求を引き抜かない
    def commonReqNop() = {
      commonRdDeqReg := false.B
    }
    // 応答を積む
    def commonRespEnqueue(data: UInt) = {
      commonRdDataReg := data
      commonRdEnqReg  := true.B
    }
    // 応答を積まない
    def commonRespNop() = {
      commonRdDataReg := DontCare
      commonRdEnqReg  := false.B
    }

    // Queue Remainがあれば取り出す。OoOにはしない
    when(io.debugAccessCommon.req.rdempty && !commonRdDeqReg) {
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
        val readData = commonRegs(offset)
        commonReqDequeue()
        commonRespEnqueue(readData)
      }.elsewhen(reqType === InternalAccessCommand.Type.write) {
        // Write & Dequeue
        commonRegs(offset) := writeData
        commonReqDequeue()
        commonRespNop()
      }
    }.otherwise {
      // なにもしない, Enq/Deq中のものは解除
      commonReqNop()
      commonRespNop()
    }
  }

  /*********************************************************************/
  /* DebugAccessReq: Others                                            */
  // DebugAccessPort-DPRAM-Clock Domainの組を定義して、まとめて実装する
  val debugAccessTargets: Seq[VirtualCartDebugAccessDef] = Seq(
    VirtualCartDebugAccessDef(VirtualInstruction.AccessTarget.cartPrg, io.prgRamDebug, io.debugAccessPrg, clock, reset),
    VirtualCartDebugAccessDef(VirtualInstruction.AccessTarget.cartSave, io.saveRamDebug, io.debugAccessSave, clock, reset),
    VirtualCartDebugAccessDef(VirtualInstruction.AccessTarget.cartChr, io.chrRamDebug, io.debugAccessChr, io.ppuClock, io.ppuReset),
  )

  /*********************************************************************/
  /* Access From CPU Bus                                               */
  // TODO: CPU/PPU DomainからRAMを見せるときの定義
  // TODO: commonRegsの先頭16byteを見てMapperの実装を切り替えられるようにする

  /*********************************************************************/
  /* Access From PPU Bus                                               */
  // TODO: CPU/PPU DomainからRAMを見せるときの定義

  /*********************************************************************/
  // TODO: 実装したら削除
  io.cart.cpu.dataIn      := 0x39.U
  io.cart.cpu.isNotIrq    := true.B
  io.cart.ppu.dataIn      := 0x39.U
  io.cart.ppu.isNotVramCs := true.B

  io.prgRamEmu.address := 0.U
  io.prgRamEmu.clock   := clock
  io.prgRamEmu.data    := 0.U
  io.prgRamEmu.rden    := false.B
  io.prgRamEmu.wren    := false.B

  io.chrRamEmu.address := 0.U
  io.chrRamEmu.clock   := clock
  io.chrRamEmu.data    := 0.U
  io.chrRamEmu.rden    := false.B
  io.chrRamEmu.wren    := false.B

  io.saveRamEmu.address := 0.U
  io.saveRamEmu.clock   := clock
  io.saveRamEmu.data    := 0.U
  io.saveRamEmu.rden    := false.B
  io.saveRamEmu.wren    := false.B

  io.prgRamDebug.address := 0.U
  io.prgRamDebug.clock   := clock
  io.prgRamDebug.data    := 0.U
  io.prgRamDebug.rden    := false.B
  io.prgRamDebug.wren    := false.B

  io.chrRamDebug.address := 0.U
  io.chrRamDebug.clock   := clock
  io.chrRamDebug.data    := 0.U
  io.chrRamDebug.rden    := false.B
  io.chrRamDebug.wren    := false.B

  io.saveRamDebug.address := 0.U
  io.saveRamDebug.clock   := clock
  io.saveRamDebug.data    := 0.U
  io.saveRamDebug.rden    := false.B
  io.saveRamDebug.wren    := false.B

  io.debugAccessPrg.req.rdclk  := clock
  io.debugAccessPrg.req.rdreq  := false.B
  io.debugAccessPrg.resp.data  := 0.U
  io.debugAccessPrg.resp.wrclk := clock
  io.debugAccessPrg.resp.wrreq := false.B

  io.debugAccessSave.req.rdclk  := clock
  io.debugAccessSave.req.rdreq  := false.B
  io.debugAccessSave.resp.data  := 0.U
  io.debugAccessSave.resp.wrclk := clock
  io.debugAccessSave.resp.wrreq := false.B

  io.debugAccessChr.req.rdclk  := clock
  io.debugAccessChr.req.rdreq  := false.B
  io.debugAccessChr.resp.data  := 0.U
  io.debugAccessChr.resp.wrclk := clock
  io.debugAccessChr.resp.wrreq := false.B

}
