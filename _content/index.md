---
title: トップページ
tags: 
  - ページ
  - ホーム
publish_date: "2024-09-06T00:18:00+09:00"
update_date: "2024-09-06T00:18:00+09:00"
slug: /
thumbnail: /images/home-min.jpg
description: トップページの説明文
template: default
---

## トップページ

トップページの本文です。
この部分を編集してトップページの内容を変更できます。

## 注意点

JS/CSS/画像ファイルは、ビルド時に自動的に圧縮してコピーされます。
その際、各ファイル名は「*-min.(ext)」になります。
ファイルの参照先には注意してください。

例）  
`/images/home.jpg` -> `/images/home-min.jpg`  
`/css/style.css` -> `/css/style-min.css`  
`/js/script.js` -> `/js/script-min.js`

## ビルド方法

`src/main/kotlin/com/suzushinlab/kraft/Main.kt`を実行してください。
ビルド後、`_output`フォルダに必要なファイル群が全て生成されます。

## サーバーへのアップロード

`_output`フォルダにあるファイル群を全てサーバーにアップロードしてください。
