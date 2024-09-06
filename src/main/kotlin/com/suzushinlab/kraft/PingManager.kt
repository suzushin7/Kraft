package com.suzushinlab.kraft

import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.io.File
import java.net.URI

class PingManager {
    // XML-RPCを使用してPINGリクエストを送信する
    private fun sendPingRequest(url: String) {
        try {
            val config = XmlRpcClientConfigImpl().apply {
                serverURL = URI(url).toURL()
            }

            val client = XmlRpcClient().apply {
                setConfig(config)
            }

            val params = arrayOf(siteName, "https://$domain")
            client.execute("ping", params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // PINGリクエストを送信
    fun ping(file: String) {
        // content/ping.txtファイルが存在しない場合はPINGリクエストを送信しない
        if(!File(file).exists()) {
            println("${file}ファイルが存在しません。PINGリクエストを送信しません。")
            return
        }

        // ファイルからURLリストを読み込む
        val urlList = File(file).readLines()

        // 各URLに対してPINGを送信する
        for (url in urlList) {
            println("Pinging $url via XML-RPC...")
            sendPingRequest(url)
        }
    }
}