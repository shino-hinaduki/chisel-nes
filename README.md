Chisel NES
[![Continuous Integration](https://github.com/shino-hinaduki/chisel-nes/actions/workflows/test.yml/badge.svg?branch=main)](https://github.com/shino-hinaduki/chisel-nes/actions/workflows/test.yml)
=======================

## Demo

TODO:

## HDL Design

TODO:

## EVB for DE0-CV

ファミコンエミュレータを実際のカートリッジで動作させる目的で設計した。

`eda\kicad_6.0.2\nes_peripheral` に、KiCADで設計した基板ファイルがある。


### 機能概要

TODO: 画像等を追加

* DE0-CVのGPIO0,1に直結可能
* カートリッジとの接続
  * 3.3V - 5V変換付き
  * 周辺にプローブ用ピンも引き出し済
* 純正/互換コントローラ接続
  * 3.3V - 5V変換付き
* 純正回路と同じシフトレジスタICを用いたコントローラ入力
  * 3.3Vでプルアップされた入力回路とGNDを公開
* MAX98537を使ったI2S-音声出力回路
* 5Vの外部供給

### BOM

Digikeyで選定した部品を記載。チップコンデンサを始め、サイズ・機能互換であればこれに限らない

| Ref | Value | 数量 | メーカ製品番号  | url |
| --- | ----  | ---- | --- | ------ | ---- |
| C1-C37 |0.1u	| 7 | CL10B104KB8NNWC | https://www.digikey.jp/ja/products/detail/samsung-electro-mechanics/CL10B104KB8NNWC/3887593 |
| C40 | 10p	| 1 | CL10C100CB8NNNC | https://www.digikey.jp/ja/products/detail/samsung-electro-mechanics/CL10C100CB8NNNC/3886727 |
| D1-D3 | LED	| 3 |5988070107F| https://www.digikey.jp/ja/products/detail/dialight/5988070107F/1291269 |
| F1 | Polyfuse_Small	| 1 |0603L300/9SLYR|https://www.digikey.jp/ja/products/detail/littelfuse-inc/0603L300-9SLYR/5427243|
| R1-R22 | 10k	| 2 |RC1608F103CS|https://www.digikey.jp/ja/products/detail/samsung-electro-mechanics/RC1608F103CS/3903493|
| R23	| 0	| 1 |RC0603FR-070RL|https://www.digikey.jp/ja/products/detail/yageo/RC0603FR-070RL/1304008|
| R25, R26 | 100k	| 2 |RC0603JR-07100KL|https://www.digikey.jp/ja/products/detail/yageo/RC0603JR-07100KL/726698|
| U1-U16 | TXB0104PW	| 6 |TXB0104PWR|https://www.digikey.jp/ja/products/detail/texas-instruments/TXB0104PWR/1629102|
| U17, U18 | 4021	| 2 |CD4021BMT|https://www.digikey.jp/ja/products/detail/texas-instruments/CD4021BMT/1690863|
| U19	| MAX98357 | 1 |MAX98357AETE+T|https://www.digikey.jp/ja/products/detail/analog-devices-inc-maxim-integrated/MAX98357AETE-T/4936122|
