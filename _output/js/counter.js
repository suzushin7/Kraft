fetch('/php/counter.php')
  .then(response => response.json())
  .then(data => {
    const totalViews = data.total;
    const startDate = data.start_date;

    // 合計PVと開始日を表示
    document.getElementById('totalViews').textContent = 'Total: ' + totalViews + ' PV';
    document.getElementById('startDate').textContent = 'Start Date: ' + startDate;

    const dailyData = data.daily;
    const dailyViewsList = document.getElementById('dailyViews');

    // 過去30日間のデータをリストに表示
    dailyViewsList.innerHTML = '';

    // 過去30日間のPVを配列に格納
    const pvArray = [];
    for (let i = 0; i <= 29; i++) {
      const date = new Date();
      date.setDate(date.getDate() - i);
      const dateString = date.toLocaleDateString('ja-JP',
        { year: 'numeric', month: '2-digit', day: '2-digit', timeZone: 'Asia/Tokyo' })
        .replace(/\//g, '-');

      const views = dailyData[dateString] || 0;
      pvArray.push(views); // PVを配列に追加
    }

    // 最大PVを取得
    const maxPV = Math.max(...pvArray);

    // グラフを生成
    pvArray.forEach((views, index) => {
      const listItem = document.createElement('li');

      // 日付表示
      const date = new Date();
      date.setDate(date.getDate() - index);
      const dateString = date.toLocaleDateString('ja-JP', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-');

      const dateSpan = document.createElement('span');
      dateSpan.className = 'date';
      dateSpan.textContent = dateString;

      // グラフを表示するためのspan
      const graphSpan = document.createElement('span');
      graphSpan.className = 'graph';

      const scaledPV = Math.round((views / maxPV) * 10);
      graphSpan.textContent = '*'.repeat(scaledPV);

      // PV表示
      const pvSpan = document.createElement('span');
      pvSpan.className = 'pv';
      pvSpan.textContent = views + ' PV';

      // liに各要素を追加
      listItem.appendChild(dateSpan);
      listItem.appendChild(graphSpan);
      listItem.appendChild(pvSpan);

      dailyViewsList.appendChild(listItem);
    });
  })
  .catch(error => console.error('Error:', error));
