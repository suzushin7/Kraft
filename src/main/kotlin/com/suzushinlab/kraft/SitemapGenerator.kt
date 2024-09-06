package com.suzushinlab.kraft

import java.io.File

class SitemapGenerator(private val domain: String) {
    // サイトマップを生成
    fun generate(articles: List<Article>, outputDir: String) {
        val sitemapContent = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n")

            for (article in articles) {
                val lastmodDate = article.metadata.updateDate ?: article.metadata.publishDate

                append("<url>\n")
                append("<loc>https://${domain}${article.metadata.slug}</loc>\n")
                append("<lastmod>${lastmodDate}</lastmod>\n")
                append("<changefreq>daily</changefreq>\n")
                append("</url>\n")
            }

            append("</urlset>")
        }

        File("${outputDir}/sitemap.xml").writeText(sitemapContent)
        println("サイトマップの生成が完了しました。")
    }
}