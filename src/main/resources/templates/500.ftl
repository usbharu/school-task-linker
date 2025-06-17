<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>500 Internal Server Error | 課題管理システム</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style> body { font-family: 'Inter', sans-serif; } </style>
    <link rel="stylesheet" href="https://rsms.me/inter/inter.css">
</head>
<body class="bg-slate-50">
<div class="min-h-screen flex items-center justify-center text-center px-4">
    <div class="max-w-lg w-full">
        <h1 class="text-8xl font-bold text-red-600">500</h1>
        <p class="mt-4 text-2xl font-semibold text-slate-800">サーバーエラー</p>
        <p class="mt-2 text-slate-600">${message}</p>
        <p class="mt-2 text-sm text-slate-500">問題が解決しない場合は、管理者にお問い合わせください。</p>
        <div class="mt-8">
            <a href="/dashboard" class="px-6 py-3 bg-indigo-600 text-white font-semibold rounded-lg shadow-md hover:bg-indigo-700">
                ダッシュボードに戻る
            </a>
        </div>
    </div>
</div>
</body>
</html>