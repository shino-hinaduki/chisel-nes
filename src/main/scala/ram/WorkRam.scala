package ram

import chisel3._

/** 本体内臓のWork RAM
  * 0x0000 ~ 0x07ff の 2KiB
  * 0x0800 ~ 0x1fff にミラーされている
  */
class WorkRam extends Ram {}
