<div class="search">
  <input type="text" class="tag-search" placeholder="タグを入力してください">
  <button class="search-button">検索</button>
</div>

<script type="text/javascript">
  function redirectToTagPage() {
    const keyword = document.querySelector('.tag-search').value.trim().toLowerCase();
    if (keyword) {
      window.location.href = "/tag/" + encodeURIComponent(keyword);
    }
  }

  document.querySelector('.search-button').addEventListener('click', redirectToTagPage);

  document.querySelector('.tag-search').addEventListener('keypress', function(event) {
    if (event.key === 'Enter') {
      redirectToTagPage();
    }
  });
</script>
