package com.suzushinlab.kraft

import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class RSSGenerator(private val domain: String, private val siteName: String) {
    // RSSフィードを生成
    fun generate(articles: List<Article>, outputDir: String) {
        val rssContent = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<rss version=\"2.0\">\n")
            append("<channel>\n")
            append("<title>${siteName}</title>\n")
            append("<link>https://${domain}</link>\n")
            append("<description>Latest articles from ${siteName}</description>\n")
            append("<language>ja-jp</language>\n")
            append("<generator>Kraft</generator>\n")

            // 現在の時間を取得して、RFC 822形式に変換
            val now = ZonedDateTime.now(ZoneId.of("UTC"))
            val outputFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            val buildDate = now.format(outputFormatter)
            append("<lastBuildDate>${buildDate}</lastBuildDate>\n")

            for (article in articles) {
                val date = article.metadata.updateDate ?: article.metadata.publishDate
                val zonedDateTime = ZonedDateTime.parse(date)
                val pubDate = zonedDateTime.format(outputFormatter)

                append("<item>\n")
                append("<title>${article.metadata.title} | ${siteName}</title>\n")
                append("<link>https://${domain}${article.metadata.slug}</link>\n")
                append("<pubDate>${pubDate}</pubDate>\n")
                append("<description>${article.metadata.description}</description>\n")
                append("<category>${article.metadata.tags.first()}</category>\n")
                append("<guid>https://${domain}${article.metadata.slug}</guid>\n")
                append("</item>\n")
            }

            append("</channel>\n")
            append("</rss>")
        }

        val rss = File("${outputDir}/rss.xml")
        rss.writeText(rssContent)
        println("RSS: ${rss.path}の作成が完了しました。")
    }
}