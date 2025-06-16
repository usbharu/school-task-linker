<!-- src/main/resources/templates/login.ftl -->
<#-- このテンプレートはlayout.ftlを使いません -->
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ログイン | 課題管理システム</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        body { font-family: 'Inter', sans-serif; }
    </style>
    <link rel="stylesheet" href="https://rsms.me/inter/inter.css">
</head>
<body class="bg-slate-50">
<div class="min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8 p-10 bg-white rounded-xl shadow-lg">
        <div>
            <h2 class="mt-6 text-center text-3xl font-extrabold text-slate-900">
                アカウントにログイン
            </h2>
        </div>
        <#if error??>
            <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative" role="alert">
                <span class="block sm:inline">ログインに失敗しました。IDまたはパスワードを確認してください。</span>
            </div>
        </#if>
        <#if status?? && status == "registered">
            <div class="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded relative" role="alert">
                <span class="block sm:inline">ユーザー登録が完了しました。ログインしてください。</span>
            </div>
        </#if>
        <form class="mt-8 space-y-6" action="/login" method="POST">
            <input type="hidden" name="remember" value="true">
            <div class="rounded-md shadow-sm -space-y-px">
                <div>
                    <label for="username" class="sr-only">ユーザー名</label>
                    <input id="username" name="username" type="text" required class="appearance-none rounded-none relative block w-full px-3 py-2 border border-slate-300 placeholder-slate-500 text-slate-900 rounded-t-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm" placeholder="ユーザー名">
                </div>
                <div>
                    <label for="password" class="sr-only">パスワード</label>
                    <input id="password" name="password" type="password" required class="appearance-none rounded-none relative block w-full px-3 py-2 border border-slate-300 placeholder-slate-500 text-slate-900 rounded-b-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm" placeholder="パスワード">
                </div>
            </div>

            <div>
                <button type="submit" class="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500">
                    ログイン
                </button>
            </div>
            <div class="text-center text-sm">
                <a href="/register" class="font-medium text-indigo-600 hover:text-indigo-500">
                    アカウントをお持ちでないですか？ 新規登録
                </a>
            </div>
        </form>
    </div>
</div>
</body>
</html>
