<!DOCTYPE html>
<html lang="ja">
<head>

  <#include "head.ftl">

  <link rel="canonical" href="https://${domain}${slug}">
  <title>${title} | ${siteName}</title>
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
            <li>Current Page</li>
            <li>
            <#list tags as tag>
              <a href="/tag/${tag?replace(" ", "-")?lower_case}/">#${tag}</a>
            </#list>
            </li>
          </ol>
        </nav>

        <#include "search.ftl">

        <article>
          <header>
            <h1>${title}</h1>
            <p>Tags:
              <#list tags as tag>
              <a class="tag" href="/tag/${tag?replace(" ", "-")?lower_case}/">#${tag}</a>
              </#list>
            </p>
            <p>Published on: <time datetime="${publishDate}">${publishDate}</time></p>
            <#if updateDate??>
            <p>Updated on: <time datetime="${updateDate}">${updateDate}</time></p>
            </#if>
          </header>

          <div class="ad-description">
            <p>※記事によっては、本文に広告のリンクが含まれることがあります。</p>
          </div>

          <div class="content">
            ${content}

            <#include "related-tags.ftl">

            <#include "share.ftl">

            <div class="prev"></div>
            <div class="next"></div>
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
