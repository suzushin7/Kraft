package com.suzushinlab.kraft

// 変更してください
const val domain = "suzushinlab.com"
const val siteName = "スズシンラボ"
const val twitterId = "@suzushin7"
const val pingFile = "content/ping.txt"
const val sendPing = false
const val isCleanBuild = false

// このプロジェクトの公開調整用のフラグ
// 基本的に、あなたが使う時はtrueに設定してください
const val isPublicBuild = false

// Kraftクラスのインスタンスを生成し、ビルドまたはPINGリクエストを送信する
fun main() {
    val kraft = Kraft(domain, siteName, twitterId)
    if(sendPing) {
        kraft.ping(pingFile)
    } else {
        if(isCleanBuild) {
            kraft.cleanBuild(isPublicBuild)
        } else {
            kraft.build(isPublicBuild)
        }
    }
}