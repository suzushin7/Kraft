# Kraft

このリポジトリは、Kraftのソースコードを管理するためのリポジトリです。

## Kraftとは

Kraftは、Kotlin製の静的サイトジェネレーターです。
Kraftは、MarkdownファイルをHTMLファイルに変換することができます。

主な機能は以下の通りです。

- MarkdownファイルをHTMLファイルに変換
- 静的ファイル群（CSS/JS/画像）を圧縮してコピー
- sitemap.xmlの生成
- rss.xmlの生成

## 使い方

`src/main/kotlin/com/suzushinlab/kraft/Main.kt`（以下、Main.kt）の設定を変更してください。
また、適宜`_templates`フォルダや`_content`フォルダの中身を編集してください。
その後、`Main.kt`を実行してください。
ビルド後、`_output`フォルダの中身を全てWebサーバーにアップロードしてください。

編集するファイルは以下の通りです。

- `_templates`フォルダ内のファイル
- `_content`フォルダ内のファイル
- `Main.kt`ファイル

## ライセンス

このリポジトリは、MITライセンスの元で公開されています。

## 作者

- 名前：鈴木俊吾（すずしん）
- ブログ：[スズシンラボ](https://suzushinlab.com/)
- Twitter：[@suzushin7](https://twitter.com/suzushin7)
