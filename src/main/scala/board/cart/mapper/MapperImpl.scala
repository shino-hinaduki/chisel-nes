package board.cart.mapper

import chisel3._
import board.cart.types.CartridgeIO
import board.ram.types.RamIO

/**
  * Mapperの実装。VirtualCartridgeで実体化して、必要最低限の信号線などとともに呼び出される
  * assignCpu/assignPpuの実装を合わせてcartにあるポートの出力は何かしら決定する必要がある
  */
trait MapperImpl {

  /**
    * 最初に一度だけ呼び出される。Mapperのreg定義などはこの中で行う
    * 必要ない場合もあるため、デフォルト実装では何もしない
    * 本関数は、cpuClock, cpuResetのドメインで呼び出される
    */
  def setupCpu(
      cart: CartridgeIO,
      cpuClock: Clock,
      cpuReset: Reset,
      prgRam: RamIO,
      saveRam: RamIO,
      inesHeader: Vec[UInt],
  ): Unit = {}

  /**
    * 最初に一度だけ呼び出される。Mapperのreg定義などはこの中で行う
    * 必要ない場合もあるため、デフォルト実装では何もしない
    * 本関数は、ppuClock, ppuResetのドメインで呼び出される
    */
  def setupPpu(
      cart: CartridgeIO,
      ppuClock: Clock,
      ppuReset: Reset,
      chrRam: RamIO,
      inesHeader: Vec[UInt],
  ): Unit = {}

  /**
    * 引数に与えられた情報をもとに、Cartrigeの挙動を実装する
    * 本関数は、cpuClock, cpuResetのドメインで呼び出される
    */
  def assignCpu(
      cart: CartridgeIO,
      cpuClock: Clock,
      cpuReset: Reset,
      prgRam: RamIO,
      saveRam: RamIO,
      inesHeader: Vec[UInt],
  ): Unit

  /**
    * 引数に与えられた情報をもとに、Cartrigeの挙動を実装する
    * 本関数は、ppuClock, ppuResetのドメインで呼び出される。inesHeaderにはppuClockで同期を取ったものを指定する
    */
  def assignPpu(
      cart: CartridgeIO,
      ppuClock: Clock,
      ppuReset: Reset,
      chrRam: RamIO,
      inesHeader: Vec[UInt],
  ): Unit
}
