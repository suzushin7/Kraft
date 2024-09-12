package com.suzushinlab.kraft

import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Kraftクラス
 * @property domain ドメイン
 * @property siteName サイト名
 * @property twitterId Twitter ID
 */
class Kraft(
    domain: String,
    siteName: String,
    twitterId: String) {
    private val fileProcessor = FileProcessor()
    private val htmlGenerator = HTMLGenerator(domain, siteName, twitterId)
    private val sitemapGenerator = SitemapGenerator(domain)
    private val rssGenerator = RSSGenerator(domain, siteName)
    private val pingManager = PingManager()

    fun cleanBuild(isPublicBuild: Boolean) {
        val dirName = if(isPublicBuild) "_output" else "output"
        val dir = File(dirName).toPath()
        if (Files.exists(dir)) {
            // 全てのファイルを削除
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach { it.delete() }
        }
        build(isPublicBuild)
    }

    fun build(isPublicBuild: Boolean) {
        var contentDir = "content"
        var outputDir = "output"
        var templateDir = "templates"

        if(isPublicBuild) {
            contentDir = "_content"
            outputDir = "_output"
            templateDir = "_templates"
        }

        val cfg = Configuration(Configuration.VERSION_2_3_31).apply {
            defaultEncoding = "UTF-8"
            templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        }
        cfg.setDirectoryForTemplateLoading(File(templateDir))

        val articles = htmlGenerator.createArticles(contentDir)
        htmlGenerator.generateHtml(articles, cfg, outputDir)
        htmlGenerator.generateTagPage(articles, cfg, outputDir)
        fileProcessor.compressAndCopyStaticFiles(contentDir, outputDir)
        sitemapGenerator.generate(articles, outputDir)
        rssGenerator.generate(articles, outputDir)

        println("FINISH: サイトの生成が完了しました。")
    }

    fun ping(file: String) {
        pingManager.ping(file)
    }
}