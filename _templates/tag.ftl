<!DOCTYPE html>
<html lang="ja">
<head>

    <#include "head.ftl">

    <meta name="robots" content="noindex">
    <link rel="canonical" href="https://${domain}/tag/${tag?replace(" ", "-")?lower_case}/">
    <title>Tag: ${tag} | ${siteName}</title>
</head>
<body>
  <div id="top" class="container">
    <div class="bookmark-logo">
      <a href="/">${siteName}</a>
    </div>

    <#include "global-menu.ftl">

    <main>
      <nav class="breadcrumb">
        <ol>
          <li><a href="/">Home</a></li>
          <li><a href="/tag/${tag?replace(" ", "-")?lower_case}/">#${tag}</a></li>
          <li>Current Page</li>
        </ol>
      </nav>

      <#include "search.ftl">

      <article>
        <header>
          <h1>${title}</h1>
          <p>Tag: <a class="tag" href="/tag/${tag?replace(" ", "-")?lower_case}/">#${tag}</a></p>
          <p>Published on: <time datetime="${publishDate}">${publishDate}</time></p>
          <#if updateDate??>
          <p>Updated on: <time datetime="${updateDate}">${updateDate}</time></p>
          </#if>
        </header>

        <div class="content">
          <h2>Articles in #${tag}</h2>
          <p>タグ「${tag}」が設定されているブログ記事一覧です。</p>
          <ul>
            <#list articles as article>
            <li><a href="${article.metadata.slug}">${article.metadata.title}</a></li>
            </#list>
          </ul>

          <#include "share.ftl">

        </div>

        <#include "profile.ftl">

        <#include "cta.ftl">

        <a href="#top" class="back-to-top">TOPに戻る</a>
      </article>

      <#include "counter.ftl">

    </main>

    <#include "footer.ftl">

  </div>
</body>
</html>
