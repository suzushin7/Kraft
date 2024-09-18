<?php
// counter.php
date_default_timezone_set('Asia/Tokyo');

$exclude_ips = array(
  '60.94.29.54',
);

// クライアントのIPアドレスが除外リストに含まれているかチェック
if (in_array($_SERVER['REMOTE_ADDR'], $exclude_ips)) {
    // データを読み込んでそのまま返す
    $json_file = 'counter.json';
    if (file_exists($json_file)) {
        $data = json_decode(file_get_contents($json_file), true);
    } else {
        $data = array(
            'start_date' => date('Y-m-d'), // カウント開始日を設定
            'total' => 0,
            'daily' => array()
        );
    }

    // JSON形式でデータを返す
    header('Content-Type: application/json');
    echo json_encode($data);
    exit;
}

// 以下、元のコードと同じ

// JSONファイルのパス
$json_file = 'counter.json';

// データを初期化
$data = array();

// ファイルロックを使用して安全にデータを更新
$fp = fopen($json_file, 'c+'); // 読み書き用にオープン。存在しなければ作成。
if (flock($fp, LOCK_EX)) { // 排他ロックを取得
    // 既存のデータを読み込む
    $filesize = filesize($json_file);
    if ($filesize > 0) {
        $json_data = fread($fp, $filesize);
        $data = json_decode($json_data, true);
    } else {
        // ファイルが空の場合はデータを初期化
        $data = array(
            'start_date' => date('Y-m-d'), // カウント開始日を設定
            'total' => 0,
            'daily' => array()
        );
    }

    // ファイルポインタを先頭に戻す
    fseek($fp, 0);

    // 今日の日付を取得（YYYY-MM-DD形式）
    $today = date('Y-m-d');

    // 全期間のアクセス数をインクリメント
    $data['total'] += 1;

    // 今日のアクセス数をインクリメント
    if (isset($data['daily'][$today])) {
        $data['daily'][$today] += 1;
    } else {
        $data['daily'][$today] = 1;
    }

    // 過去30日より古いデータを削除
    $threshold = strtotime('-30 days');
    foreach ($data['daily'] as $date => $count) {
        if (strtotime($date) < $threshold) {
            unset($data['daily'][$date]);
        }
    }

    // ファイルを空にする
    ftruncate($fp, 0);

    // 更新されたデータを書き込む
    fwrite($fp, json_encode($data));

    // ロックを解除
    flock($fp, LOCK_UN);
} else {
    // ロックの取得に失敗した場合のエラー処理
    header('HTTP/1.1 500 Internal Server Error');
    echo json_encode(array('error' => 'ファイルのロックに失敗しました。'));
    fclose($fp);
    exit;
}

fclose($fp);

// JSON形式でデータを返す
header('Content-Type: application/json');
echo json_encode($data);

?>
