package com.suzushinlab.kraft

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import freemarker.template.Configuration
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
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
            val fullTitle = "${article.metadata.title} | $siteName"
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

            // {{ latest_posts n }}を置換
            // 最新記事n件を取得して表示
            val latestPostsRegex = """\{\{\s*latest_posts\s+(\d+)\s*}}""".toRegex()
            val latestPostsMatch = latestPostsRegex.find(htmlContentWithToc)
            if (latestPostsMatch != null) {
                val n = latestPostsMatch.groupValues[1].toInt()
                val latestArticles = allArticles.take(n)
                val latestPostsDiv = buildString {
                    append("<div class=\"latest-posts\">\n")
                    append("<ul>\n")
                    latestArticles.forEach { article ->
                        append("<li><a href=\"${article.metadata.slug}\">${article.metadata.title}</a></li>\n")
                    }
                    append("</ul>\n")
                    append("</div>\n")
                }
                htmlContentWithToc = htmlContentWithToc.replace(latestPostsMatch.value, latestPostsDiv)
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

            // 前後の記事へのリンクを作成
            val prevArticle = allArticles.getOrNull(allArticles.indexOf(article) + 1)
            val nextArticle = allArticles.getOrNull(allArticles.indexOf(article) - 1)
            val prevLink = prevArticle?.metadata?.slug
            val nextLink = nextArticle?.metadata?.slug

            // 前後の記事へのリンクを追加
            val document = Jsoup.parse(htmlContentWithToc)
            if(prevLink != null) {
                val prevLinkElement = Element("a")
                    .attr("href", prevLink)
                    .text(prevArticle.metadata.title)
                val prevLinkParagraph = Element("p")
                prevLinkParagraph.text("Previous: ")
                prevLinkParagraph.appendChild(prevLinkElement)
                document.select("div.prev").first()?.appendChild(prevLinkParagraph)
            }
            if(nextLink != null) {
                val nextLinkElement = Element("a")
                    .attr("href", nextLink)
                    .text(nextArticle.metadata.title)
                val nextLinkParagraph = Element("p")
                nextLinkParagraph.text("Next: ")
                nextLinkParagraph.appendChild(nextLinkElement)
                document.select("div.next").first()?.appendChild(nextLinkParagraph)
            }
            htmlContentWithToc = document.html()

            // slugが/で終わる場合はindex.htmlを追加
            val path = if (article.metadata.slug.endsWith("/")) {
                article.metadata.slug + "index.html"
            } else {
                article.metadata.slug
            }

            // 出力先のファイル
            val outputFile = File("${outputDir}${path}")

            // ファイルが存在する場合は、ハッシュ値を比較して更新が必要か判定
            if (outputFile.exists()) {
                val outputFileContent = outputFile.readText()
                // 文字列の長さを比較。同じ場合はハッシュ値を比較
                if (outputFileContent.length == htmlContentWithToc.length) {
                    // ハッシュ値を比較
                    val prevHash = outputFileContent.hashCode()
                    val currentHash = htmlContentWithToc.hashCode()
                    if (currentHash == prevHash) {
                        println("HTML: ${outputFile.path}は変更がないため、上書きしません。")
                        continue
                    }
                }
            }

            // 出力先のファイルが存在しないか、更新が必要な場合はファイルを書き込む
            outputFile.apply {
                if(!parentFile.exists()) {
                    parentFile.mkdirs()
                }
                writeText(htmlContentWithToc)
                println("HTML: ${outputFile.path}を作成しました。")
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
            val fullTitle = "Tag: $tag | $siteName"
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
            val outputFile = File("${outputDir}/tag/${slug}/index.html")

            // ファイルが存在する場合は、ハッシュ値を比較して更新が必要か判定
            if (outputFile.exists()) {
                val outputFileContent = outputFile.readText()
                // 文字列の長さを比較。同じ場合はハッシュ値を比較
                if (outputFileContent.length == htmlContentWithToc.length) {
                    // ハッシュ値を比較
                    val prevHash = Util.getHash(outputFileContent)
                    val currentHash = Util.getHash(htmlContentWithToc)
                    if (currentHash == prevHash) {
                        println("HTML: ${outputFile.path}は変更がないため、上書きしません。")
                        return@forEach
                    }
                }
            }
            // 出力先のファイルが存在しないか、更新が必要な場合はファイルを書き込む
            outputFile.apply {
                if(!parentFile.exists()) {
                    parentFile.mkdirs()
                }
                writeText(htmlContentWithToc)
                println("HTML: ${outputFile.path}を作成しました。")
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