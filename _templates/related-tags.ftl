<h2>関連タグ</h2>
<p>この記事に付けられているタグの一覧です。<br>タグをクリックすると、そのタグが付けられた記事の一覧を表示します。</p>
<#list tags as tag>
  <a class="tag" href="/tag/${tag?replace(' ', '-')?lower_case}/">#${tag}</a>
</#list>