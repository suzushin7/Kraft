package com.suzushinlab.kraft

/**
 * Articleクラス
 * @property metadata 記事メタデータ
 * @property content 記事内容
 */
data class Article(val metadata: ArticleMetadata, val content: String) {
    data class ArticleMetadata(
        val title: String,
        val tags: List<String>,
        val publishDate: String,
        val updateDate: String?,
        val slug: String,
        val thumbnail: String,
        val description: String,
        val template: String
    )
}