<!-- src/main/resources/templates/layout.ftl -->
<#-- レイアウト全体を "default" という名前のマクロとして定義します -->
<#macro default title user="">
    <!DOCTYPE html>
    <html lang="ja">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>${title?html} | 課題管理システム</title>
        <script src="https://cdn.tailwindcss.com"></script>
        <style>
            body { font-family: 'Inter', sans-serif; }
        </style>
        <link rel="stylesheet" href="https://rsms.me/inter/inter.css">
    </head>
    <body class="bg-slate-50 text-slate-800">
    <div class="min-h-screen flex flex-col">
        <header class="bg-white shadow-sm">
            <nav class="container mx-auto px-4 sm:px-6 lg:px-8">
                <div class="w-full flex items-center justify-between h-16">
                    <div class="flex items-center">
                        <a href="/dashboard" class="font-bold text-xl text-indigo-600">課題マネージャー</a>
                    </div>
                    <#-- 'user'変数が存在する場合のみヘッダーのユーザー情報を表示 -->
                    <#if user??>
                        <div class="flex items-center space-x-4">
                            <span class="text-sm">ようこそ, ${user.username?html} さん</span>
                            <a href="/settings" class="text-sm font-medium text-slate-600 hover:text-indigo-500">設定</a>
                            <a href="/logout" class="text-sm font-medium text-slate-600 hover:text-indigo-500">ログアウト</a>
                        </div>
                    </#if>
                </div>
            </nav>
        </header>

        <main class="flex-grow container mx-auto p-4 sm:p-6 lg:px-8">
            <#-- このマクロを呼び出したテンプレートの内容がここに挿入される -->
            <#nested>
        </main>

        <footer class="bg-white">
            <div class="container mx-auto py-4 px-4 sm:px-6 lg:px-8 text-center text-sm text-slate-500">
                &copy; 2025 課題管理システム
            </div>
        </footer>
    </div>
    </body>
    </html>
</#macro>