package board.cart

import chisel3._
import chisel3.util.MuxLookup
import chisel3.util.switch
import chisel3.util.is

import board.cart.types.CartridgeIO
import board.cart.types.NesFileFormat
import board.access.types.InternalAccessCommand
import board.ram.types.RamIO
import board.jtag.types.VirtualInstruction
import board.cart.types.NameTableMirror
import board.cart.mapper.InvalidMapper
import board.cart.mapper.MapperImpl
import board.cart.mapper.Mapper0

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
) extends RawModule {
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
    // CPU Clock
    val cpuClock = Input(Clock())
    // CPU ClockのReset
    val cpuReset = Input(Reset())
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
  val commonRegsByCpu = withClockAndReset(io.cpuClock, io.cpuReset) { RegInit(VecInit(Seq.fill(commonWords)(0.U(debugDataWidth.W)))) } // 4byteごと
  dontTouch(commonRegsByCpu) // 最適化抑制

  withClockAndReset(io.cpuClock, io.cpuReset) {
    val commonReqDeqReg   = RegInit(Bool(), false.B)
    val commonRespDataReg = RegInit(Bool(), false.B)
    val commonRespEnqReg  = RegInit(Bool(), false.B)
    io.debugAccessCommon.req.rdclk  := io.cpuClock
    io.debugAccessCommon.req.rdreq  := commonReqDeqReg
    io.debugAccessCommon.resp.wrclk := io.cpuClock
    io.debugAccessCommon.resp.wrreq := commonRespEnqReg
    io.debugAccessCommon.resp.data  := commonRespDataReg
    io.isValidHeader                := NesFileFormat.isValidHeader(commonRegsByCpu)

    // Queue Remainがあれば取り出す。OoOにはしない
    withClockAndReset(io.cpuClock, io.cpuReset) {
      when(!io.debugAccessCommon.req.rdempty && !commonReqDeqReg) {
        val offset             = InternalAccessCommand.Request.getOffset(io.debugAccessCommon.req.q)
        val offsetValid        = offset < commonWords.U
        val writeData          = InternalAccessCommand.Request.getData(io.debugAccessCommon.req.q)
        val (reqType, isValid) = InternalAccessCommand.Request.getRequestType(io.debugAccessCommon.req.q)

        when(isValid && offsetValid) {
          switch(reqType) {
            is(InternalAccessCommand.Type.read) {
              // Read & Dequeue
              val readData = commonRegsByCpu(offset)

              commonReqDeqReg   := true.B
              commonRespEnqReg  := true.B
              commonRespDataReg := readData
            }
            is(InternalAccessCommand.Type.write) {
              // Write & Dequeue
              commonRegsByCpu(offset) := writeData

              commonReqDeqReg   := true.B
              commonRespEnqReg  := false.B
              commonRespDataReg := DontCare
            }
          }
        }.otherwise {
          // Invalid Cmd、Dequeueだけ実施
          commonReqDeqReg   := true.B
          commonRespEnqReg  := false.B
          commonRespDataReg := DontCare
        }
      }.otherwise {
        // なにもしない, Enq/Deq中のものは解除
        commonReqDeqReg   := false.B
        commonRespEnqReg  := false.B
        commonRespDataReg := DontCare
      }
    }
  }

  // CPU Clock -> PPU Clock載せ替え
  val commonRegsByPpu = withClockAndReset(io.ppuClock, io.ppuReset) { RegInit(VecInit(Seq.fill(commonWords)(0.U(debugDataWidth.W)))) } // 4byteごと
  dontTouch(commonRegsByPpu) // 最適化抑制

  withClockAndReset(io.ppuClock, io.ppuReset) {
    commonRegsByPpu := RegNext(commonRegsByCpu)
  }

  /*********************************************************************/
  /* DebugAccessReq: Others                                            */
  // DebugAccessPort-DPRAM-Clock Domainの組を定義して、まとめて実装する
  val debugAccessTargets: Seq[VirtualCartDebugAccessDef] = Seq(
    VirtualCartDebugAccessDef(VirtualInstruction.AccessTarget.cartPrg, io.prgRamDebug, prgRomAddrWidth, io.debugAccessPrg, io.cpuClock, io.cpuReset),
    VirtualCartDebugAccessDef(VirtualInstruction.AccessTarget.cartSave, io.saveRamDebug, saveRamAddrWidth, io.debugAccessSave, io.cpuClock, io.cpuReset),
    VirtualCartDebugAccessDef(VirtualInstruction.AccessTarget.cartChr, io.chrRamDebug, chrRomAddrWidth, io.debugAccessChr, io.ppuClock, io.ppuReset),
  )

  // 処理本体
  debugAccessTargets.zipWithIndex.foreach {
    case (x, index) => {
      withClockAndReset(x.clock, x.reset) {
        // temp regs
        val ramAddrReg  = RegInit(UInt(), 0.B)
        val ramDataReg  = RegInit(UInt(), 0.B)
        val ramRdReg    = RegInit(Bool(), false.B)
        val ramWrReg    = RegInit(Bool(), false.B)
        val reqDeqReg   = RegInit(Bool(), false.B)
        val respDataReg = RegInit(UInt(), 0.B)
        val respEnqReg  = RegInit(Bool(), false.B)

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
          respEnqReg  := true.B
        }.otherwise {
          respDataReg := DontCare
          respEnqReg  := false.B
        }
      }
    }
  }

  /*********************************************************************/
  /* Emulatorからのアクセス                                             */
  // ClockはMuxする必要がない
  io.prgRamEmu.clock  := io.cpuClock
  io.saveRamEmu.clock := io.cpuClock
  io.chrRamEmu.clock  := io.ppuClock

  // 利用可能なMapperの列挙
  val mappers: Map[UInt, MapperImpl] = Map(
    0.U -> new Mapper0,
    // TODO: 全部配線し終わった後の動作確認、及び他Mapperの実装
  )

  // Mapperの設定用関数
  def setupCpu(mapper: MapperImpl) = {
    mapper.setupCpu(
      cart = io.cart,
      cpuClock = io.cpuClock,
      cpuReset = io.cpuReset,
      prgRam = io.prgRamEmu,
      saveRam = io.saveRamEmu,
      inesHeader = commonRegsByCpu,
    )
  }
  def assignCpu(mapper: MapperImpl) = {
    mapper.assignCpu(
      cart = io.cart,
      cpuClock = io.cpuClock,
      cpuReset = io.cpuReset,
      prgRam = io.prgRamEmu,
      saveRam = io.saveRamEmu,
      inesHeader = commonRegsByCpu,
    )
  }
  def setupPpu(mapper: MapperImpl) = {
    mapper.setupPpu(
      cart = io.cart,
      ppuClock = io.ppuClock,
      ppuReset = io.ppuReset,
      chrRam = io.chrRamEmu,
      inesHeader = commonRegsByPpu,
    )
  }
  def assignPpu(mapper: MapperImpl) = {
    mapper.assignPpu(
      cart = io.cart,
      ppuClock = io.ppuClock,
      ppuReset = io.ppuReset,
      chrRam = io.chrRamEmu,
      inesHeader = commonRegsByPpu,
    )
  }

  // Invalidだけは先に設定しておく
  val invalidMapper = new InvalidMapper()
  setupCpu(mapper = invalidMapper)
  setupPpu(mapper = invalidMapper)
  assignCpu(mapper = invalidMapper) // 後述の列挙でどれも選ばれなかったときのデフォルト値
  assignPpu(mapper = invalidMapper) // 後述の列挙でどれも選ばれなかったときのデフォルト値

  // 全マッパーの処理を展開する。iNES Headerの値と合致するものは特別扱いする
  mappers.foreach {
    case (index, mapper) => {

      /*********************************************************************/
      /* Access From CPU Bus                                               */
      withClockAndReset(io.cpuClock, io.cpuReset) {
        // Mapper自体の初期化
        setupCpu(mapper = mapper)
        // Mapperが有効化されていたときに割当
        val isEnableRegCpu = RegNext(io.isEnable) // 変更タイミング不明なので同期しておく
        val mapperIndex    = NesFileFormat.mapper(commonRegsByCpu)
        when(isEnableRegCpu && (mapperIndex === index)) {
          assignCpu(mapper = mapper)
        }
      }

      /*********************************************************************/
      /* Access From PPU Bus                                               */
      withClockAndReset(io.ppuClock, io.ppuReset) {
        // Mapper自体の初期化
        setupPpu(mapper = mapper)
        // Mapperが有効化されていたときに割当
        val isEnableRegPpu = RegNext(io.isEnable) // 変更タイミング不明なので同期しておく
        val mapperIndex    = NesFileFormat.mapper(commonRegsByPpu)
        when(isEnableRegPpu && (mapperIndex === index)) {
          assignPpu(mapper = mapper)
        }
      }
    }
  }
}
