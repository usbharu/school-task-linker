<!-- src/main/resources/templates/partials/regex_modal.ftl -->
<#macro default>
    <div id="regex-modal" class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full hidden z-50">
        <div class="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-md bg-white">
            <div class="mt-3">
                <h3 id="modal-title" class="text-lg leading-6 font-medium text-gray-900 text-center">新しいルールを追加</h3>
                <div class="mt-2 px-7 py-3">
                    <form id="regex-form" action="/settings/rules/add" method="post" class="space-y-4 text-left">
                        <input type="hidden" id="rule-id" name="id" value="">
                        <div>
                            <label for="rule-name" class="block text-sm font-medium text-gray-700">ルール名</label>
                            <input type="text" name="name" id="rule-name" required class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm" placeholder="例: 新規課題の通知">
                        </div>
                        <div>
                            <label for="rule-pattern" class="block text-sm font-medium text-gray-700">正規表現パターン (件名)</label>
                            <textarea name="pattern" id="rule-pattern" rows="3" required class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm" placeholder="例: 【課題】(.*)"></textarea>
                            <p class="mt-2 text-xs text-gray-500">メールの件名から情報を抽出するための正規表現です。キャプチャグループ `(.*)` を使ってタスク名などを取得できます。</p>
                        </div>
                        <div>
                            <label for="rule-category" class="block text-sm font-medium text-gray-700">カテゴリ</label>
                            <select name="category" id="rule-category" required class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm">
                                <option value="NEW_ASSIGNMENT">新規課題</option>
                                <option value="DEADLINE_NOTICE">提出期限の通知</option>
                                <option value="EVENT">学校行事</option>
                                <option value="OTHER">その他</option>
                            </select>
                        </div>
                    </form>
                </div>
                <div class="items-center px-4 py-3">
                    <button id="submit-rule-button" type="submit" form="regex-form" class="px-4 py-2 bg-indigo-600 text-white text-base font-medium rounded-md w-full shadow-sm hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500">
                        保存する
                    </button>
                    <button id="close-modal-button" class="mt-2 px-4 py-2 bg-gray-200 text-gray-800 text-base font-medium rounded-md w-full shadow-sm hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-400">
                        キャンセル
                    </button>
                </div>
            </div>
        </div>
    </div>
</#macro>
