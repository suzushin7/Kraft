package com.suzushinlab.kraft

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import freemarker.template.Configuration
import org.jsoup.Jsoup
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.StringWriter
import java.net.URLEncoder
import java.time.LocalDate

/**
 * HTMLGeneratorクラス
 * @property domain ドメイン
 * @property siteName サイト名
 * @property twitterId Twitter ID
 */
class HTMLGenerator(private val domain: String, private val siteName: String, private val twitterId: String) {
    // tagに設定されている記事の個数のリストを生成
    private fun generateTagCountMap(articles: List<Article>): Map<String, Int> {
        val tagCountMap = articles.flatMap { it.metadata.tags }
            .groupingBy { it }
            .eachCount()
        return tagCountMap.toList().sortedByDescending { it.second }.toMap()
    }

    // htmlContent中のH2,H3要素を抽出して目次を生成する
    private fun generateToc(htmlContent: String): String {
        val regex = Regex("""<(h[23])>(.*?)</\1>""")
        var mutableHtmlContent = htmlContent
        val headers = regex.findAll(mutableHtmlContent).toList()

        // 目次HTMLを構築
        val tocBuilder = StringBuilder()
        tocBuilder.append("<nav class=\"table-of-contents\"><h2>目次</h2><ul>")

        headers.forEachIndexed { index, matchResult ->
            val tag = matchResult.groups[1]?.value
            val content = matchResult.groups[2]?.value
            val headerId = "section-$index"

            // 各見出しにIDを追加
            tocBuilder.append("<li class=\"$tag\"><a href=\"#$headerId\">$content</a></li>")

            // 元のHTML内の見出しタグにIDを追加
            mutableHtmlContent = mutableHtmlContent.replaceFirst(matchResult.value, "<$tag id=\"$headerId\">$content</$tag>")
        }

        tocBuilder.append("</ul></nav>")

        // 最初の<h2>タグの前に目次を挿入
        val document = Jsoup.parse(mutableHtmlContent)
        val firstH2 = document.select("h2[id]").first()
        firstH2?.before(tocBuilder.toString())

        return document.html()
    }

    // メタデータとHTMLコンテンツを元にHTMLを生成
    fun generateHtml(articles: List<Article>, cfg: Configuration, outputDir: String) {
        val allArticles = articles.sortedByDescending { it.metadata.publishDate }
        val allTags = articles.flatMap { it.metadata.tags }.distinct()
        val tagCountMap = generateTagCountMap(articles)

        for (article in articles) {
            val template = cfg.getTemplate(article.metadata.template + ".ftl")
            val fullTitle = "${article.metadata.title} | ${siteName}"
            val shareMessage = URLEncoder.encode("記事がシェアされました！ぜひ読んでみてね。\n\n${fullTitle}", "UTF-8")
            val dataModel = mapOf(
                "siteName" to siteName,
                "domain" to domain,
                "title" to article.metadata.title,
                "description" to article.metadata.description,
                "thumbnail" to article.metadata.thumbnail,
                "twitterId" to twitterId,
                "slug" to article.metadata.slug,
                "tags" to article.metadata.tags,
                "allArticles" to allArticles,
                "allTags" to allTags,
                "tagCountMap" to tagCountMap,
                "publishDate" to article.metadata.publishDate,
                "updateDate" to article.metadata.updateDate,
                "content" to article.content,
                "year" to LocalDate.now().year,
                "url" to "https://${domain}${article.metadata.slug}",
                "shareMessage" to shareMessage,
                "year" to LocalDate.now().year
            )

            val output = StringWriter()
            template.process(dataModel, output)

            // 目次を追加する
            var htmlContentWithToc = generateToc(output.toString())

            // {{ latest_posts }}を置換
            // 最新記事15件を取得してリンクを生成
            if(htmlContentWithToc.contains("{{ latest_posts }}")) {
                val latestArticles = allArticles.take(15)
                val latestPosts = buildString {
                    append("<div class=\"latest-posts\">\n<ul>\n")
                    latestArticles.forEach { article ->
                        append("<li><a href=\"${article.metadata.slug}\">${article.metadata.title}</a></li>\n")
                    }
                    append("</ul>\n</div>\n")
                }
                htmlContentWithToc = htmlContentWithToc.replace("<p>{{ latest_posts }}</p>", latestPosts)
            }

            // {{ tags }}を置換
            // タグ一覧を生成
            if(htmlContentWithToc.contains("{{ tags }}")) {
                // tagが登録されている記事の数を取得
                val tagsWithCount = allArticles.flatMap { it.metadata.tags }
                    .groupingBy { it }
                    .eachCount()
                    .toList()
                    .sortedByDescending { it.second }

                val tags = buildString {
                    append("<div class=\"tags\">\n")
                    tagsWithCount.forEach { (tag, count) ->
                        val slug = tag.replace(" ", "-").lowercase()
                        append("<a href=\"/tag/${slug}/\" class=\"tag\">#${tag} (${count})</a>")
                    }
                    append("</div>\n")
                }
                htmlContentWithToc = htmlContentWithToc.replace("<p>{{ tags }}</p>", tags)
            }

            // {{ search }}を置換
            // search.ftlの内容を挿入
            if(htmlContentWithToc.contains("{{ search }}")) {
                val searchTemplate = cfg.getTemplate("search.ftl")
                val searchOutput = StringWriter()
                searchTemplate.process(dataModel, searchOutput)
                htmlContentWithToc = htmlContentWithToc.replace("<p>{{ search }}</p>", searchOutput.toString())
            }

            // slugが/で終わる場合はindex.htmlを追加
            val path = if (article.metadata.slug.endsWith("/")) {
                article.metadata.slug + "index.html"
            } else {
                article.metadata.slug
            }

            // ファイルに書き込み
            File("${outputDir}${path}").apply {
                parentFile.mkdirs()
                writeText(htmlContentWithToc)
            }
        }
    }

    // MarkdownファイルをパースしてメタデータとHTMLコンテンツを取得
    private fun parseMarkdown(file: File): Article {
        val content = file.readText()
        val parts = content.split("---")
        val frontMatter = parts[1].trim()
        val markdownContent = parts[2]

        val yaml = Yaml()
        val metadataMap: Map<String, Any> = yaml.load(frontMatter)

        // tagsを取得。tagsがない場合は空リストを返す
        val tags = (metadataMap["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        val metadata = Article.ArticleMetadata(
            title = metadataMap["title"] as String,
            tags = tags,
            publishDate = metadataMap["publish_date"] as String,
            updateDate = metadataMap["update_date"] as String,
            slug = metadataMap["slug"] as String,
            thumbnail = metadataMap["thumbnail"] as String,
            description = metadataMap["description"] as String,
            template = metadataMap["template"] as String,
        )

        // MarkdownをHTMLに変換
        val parser = Parser.builder().build()
        val document = parser.parse(markdownContent)
        val renderer = HtmlRenderer.builder().build()
        val htmlContent = renderer.render(document)

        // htmlContent中のpタグの改行を<br>に変換
        val regex = Regex("<p>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)
        val newHtmlContent = regex.replace(htmlContent) {
            "<p>${it.groupValues[1].replace("\n", "<br>")}</p>"
        }

        return Article(metadata, newHtmlContent)
    }

    // tagの一覧ページを生成
    fun generateTagPage(articles: List<Article>, cfg: Configuration, outputDir: String) {
        // タグごとに記事をグループ化
        val articlesInTag = articles.flatMap { article ->
            article.metadata.tags.map { tag -> tag to article }
        }.groupBy({ it.first }, { it.second })

        // 最新の情報を取得
        val latestPublishDate = articles.maxOfOrNull { it.metadata.publishDate }
        val latestUpdateDate = articles.mapNotNull { it.metadata.updateDate }.maxOrNull()
        val latestThumbnail = articles.maxByOrNull { it.metadata.updateDate ?: it.metadata.publishDate }?.metadata?.thumbnail

        // タグごとに記事一覧ページを生成
        articlesInTag.forEach { (tag, articles) ->
            articles.sortedByDescending { article -> article.metadata.publishDate }
            val slug = tag.replace(" ", "-").lowercase()
            val template = cfg.getTemplate("tag.ftl")
            val fullTitle = "Tag: $tag | ${siteName}"
            val shareMessage = URLEncoder.encode("記事がシェアされました！ぜひ読んでみてね。\n\n${fullTitle}\n", "UTF-8")
            val dataModel = mapOf(
                "siteName" to siteName,
                "domain" to domain,
                "title" to "Tag: $tag",
                "description" to "${siteName}に登録されているタグ「${tag}」に関する記事一覧",
                "thumbnail" to latestThumbnail,
                "twitterId" to twitterId,
                "slug" to slug,
                "publishDate" to latestPublishDate,
                "updateDate" to latestUpdateDate,
                "tag" to tag,
                "articles" to articles,
                "url" to "https://${domain}/tag/${slug}/",
                "shareMessage" to shareMessage,
                "year" to LocalDate.now().year
            )

            val output = StringWriter()
            template.process(dataModel, output)

            // 目次を追加する
            val htmlContentWithToc = generateToc(output.toString())

            File("${outputDir}/tag/${slug}/index.html").apply {
                parentFile.mkdirs()
                writeText(htmlContentWithToc)
            }
        }
    }

    // contentディレクトリ内の全ての.mdファイルを処理して記事のリストを生成
    fun createArticles(contentDir: String): List<Article> {
        val contentDirFile = File(contentDir)
        val articles = mutableListOf<Article>()

        // contentディレクトリ内の全ての.mdファイルを処理する
        contentDirFile.walk().forEach { file ->
            if (file.extension == "md") {
                articles.add(parseMarkdown(file))
            }
        }
        return articles
    }
}