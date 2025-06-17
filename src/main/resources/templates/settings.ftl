<!-- src/main/resources/templates/settings.ftl -->
<#import "layout.ftl" as layout>
<#import "/partials/regex_modal.ftl" as regex_modal>

<@layout.default title="設定" user=user>
    <h1 class="text-2xl font-bold text-slate-800 mb-6">設定</h1>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- 左側のメニュー -->
        <div class="lg:col-span-1">
            <div class="bg-white p-4 rounded-lg shadow">
                <h2 class="font-semibold text-lg mb-4">設定項目</h2>
                <nav class="space-y-1">
                    <a href="#mail-settings" class="block px-3 py-2 rounded-md text-base font-medium text-slate-700 hover:bg-slate-100">メールサーバー設定</a>
                    <a href="#regex-rules" class="block px-3 py-2 rounded-md text-base font-medium text-slate-700 hover:bg-slate-100">正規表現ルール</a>
                    <a href="#todo-integration" class="block px-3 py-2 rounded-md text-base font-medium text-slate-700 hover:bg-slate-100">ToDoサービス連携</a>
                </nav>
            </div>
        </div>

        <!-- 右側のコンテンツ -->
        <div class="lg:col-span-2 space-y-8">

            <#-- ステータス・エラーメッセージ -->
            <#if status??>
                <div class="bg-green-100 border-l-4 border-green-500 text-green-700 p-4" role="alert">
                    <p class="font-bold">成功</p>
                    <p>
                        <#if status == "google_connected">Googleアカウントとの連携が完了しました。
                        <#elseif status == "google_disconnected">Googleアカウントとの連携を解除しました。
                        <#elseif status == "google_list_saved">ToDoリストの設定を保存しました。
                        <#elseif status == "mail_saved">メールサーバー設定を保存しました。
                        <#elseif status == "rule_added">正規表現ルールを追加しました。
                        <#elseif status == "rule_updated">正規表現ルールを更新しました。
                        <#elseif status == "rule_deleted">正規表現ルールを削除しました。
                        </#if>
                    </p>
                </div>
            </#if>
            <#if error??>
                <div class="bg-red-100 border-l-4 border-red-500 text-red-700 p-4" role="alert">
                    <p class="font-bold">エラー</p>
                    <p>処理に失敗しました。入力内容を確認してください。</p>
                </div>
            </#if>

            <!-- メールサーバー設定 -->
            <section id="mail-settings" class="bg-white p-6 rounded-lg shadow scroll-mt-20">
                <h3 class="text-xl font-semibold mb-4">メールサーバー設定</h3>
                <form action="/settings/mail" method="post" class="space-y-4">
                    <div>
                        <label for="host" class="block text-sm font-medium text-slate-700">POP3サーバーホスト (TLS)</label>
                        <input type="text" name="host" id="host" value="${(mailSettings.host)!''}" required class="mt-1 block w-full rounded-md border-slate-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm" placeholder="pop.example.com">
                    </div>
                    <div>
                        <label for="port" class="block text-sm font-medium text-slate-700">ポート</label>
                        <input type="number" name="port" id="port" value="${(mailSettings.port)!'995'}" required class="mt-1 block w-full rounded-md border-slate-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm" placeholder="995">
                    </div>
                    <div>
                        <label for="email" class="block text-sm font-medium text-slate-700">メールアドレス</label>
                        <input type="email" name="email" id="email" value="${(mailSettings.email)!''}" required class="mt-1 block w-full rounded-md border-slate-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm" placeholder="user@example.com">
                    </div>
                    <div>
                        <label for="password" class="block text-sm font-medium text-slate-700">パスワード</label>
                        <input type="password" name="password" id="password" required class="mt-1 block w-full rounded-md border-slate-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm" placeholder="••••••••">
                        <p class="mt-2 text-xs text-gray-500">パスワードはデータベースに保存されます。このシステムの管理者が閲覧可能な点にご注意ください。</p>
                    </div>
                    <button type="submit" class="px-4 py-2 bg-indigo-600 text-white font-semibold rounded-lg shadow-md hover:bg-indigo-700">保存</button>
                </form>
            </section>

            <!-- 正規表現ルール -->
            <section id="regex-rules" class="bg-white p-6 rounded-lg shadow scroll-mt-20">
                <div class="flex justify-between items-center mb-4">
                    <h3 class="text-xl font-semibold">正規表現ルール</h3>
                    <button id="add-rule-button" class="px-4 py-2 bg-green-600 text-white font-semibold rounded-lg shadow-md hover:bg-green-700">
                        新しいルールを追加
                    </button>
                </div>
                <div class="overflow-x-auto">
                    <#if regexRules?has_content>
                        <table class="min-w-full divide-y divide-gray-200">
                            <thead class="bg-gray-50">
                            <tr>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ルール名</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">カテゴリ</th>
                                <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
                            </tr>
                            </thead>
                            <tbody class="bg-white divide-y divide-gray-200">
                            <#list regexRules as rule>
                                <tr>
                                    <td class="px-6 py-4 whitespace-nowrap">
                                        <div class="text-sm font-medium text-gray-900">${rule.name?html}</div>
                                        <div class="text-xs text-gray-500 truncate max-w-xs" title="${rule.pattern?html}">${rule.pattern?html}</div>
                                    </td>
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${rule.category?html}</td>
                                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium space-x-2">
                                        <button class="edit-rule-button text-indigo-600 hover:text-indigo-900"
                                                data-id="${rule.id}"
                                                data-name="${rule.name?html}"
                                                data-pattern="${rule.pattern?html}"
                                                data-category="${rule.category?html}">
                                            編集
                                        </button>
                                        <form action="/settings/rules/delete" method="post" class="inline-block" onsubmit="return confirm('このルールを本当に削除しますか？');">
                                            <input type="hidden" name="id" value="${rule.id}">
                                            <button type="submit" class="text-red-600 hover:text-red-900">削除</button>
                                        </form>
                                    </td>
                                </tr>
                            </#list>
                            </tbody>
                        </table>
                    <#else>
                        <p class="text-slate-500 text-center py-4">現在設定されているルールはありません。</p>
                    </#if>
                </div>
            </section>

            <!-- ToDoサービス連携 -->
            <section id="todo-integration" class="bg-white p-6 rounded-lg shadow scroll-mt-20">
                <h3 class="text-xl font-semibold mb-4">ToDoサービス連携</h3>
                <div class="space-y-6">
                    <div>
                        <h4 class="font-medium text-lg">Google Tasks</h4>
                        <#if isGoogleConnected>
                            <div class="mt-4 space-y-4">
                                <form action="/settings/google/savelist" method="post">
                                    <div>
                                        <label for="taskListId" class="block text-sm font-medium text-gray-700">タスクの追加先リスト</label>
                                        <select id="taskListId" name="taskListId" class="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md">
                                            <#list googleTaskLists as list>
                                                <option value="${list.id}" <#if currentTaskListId?? && currentTaskListId == list.id>selected</#if>>${list.title?html}</option>
                                            </#list>
                                        </select>
                                    </div>
                                    <button type="submit" class="px-4 py-2 bg-indigo-600 text-white font-semibold rounded-lg shadow-md hover:bg-indigo-700">
                                        保存する
                                    </button>
                                </form>
                                <div class="border-t pt-4">
                                    <form action="/settings/google/disconnect" method="post" onsubmit="return confirm('Googleアカウントとの連携を解除します。よろしいですか？');">
                                        <button type="submit" class="text-sm text-red-600 hover:text-red-800 hover:underline">
                                            連携を解除する
                                        </button>
                                    </form>
                                </div>
                            </div>
                        <#else>
                            <p class="text-sm text-slate-600 my-2">GoogleのToDoリストと連携します。</p>
                            <a href="/oauth/google/start" class="inline-block px-4 py-2 bg-blue-600 text-white font-semibold rounded-lg shadow-md hover:bg-blue-700">
                                Googleアカウントで連携
                            </a>
                        </#if>
                    </div>
                </div>
            </section>
        </div>
    </div>

<#-- モーダルをレンダリング -->
    <@regex_modal.default />

    <script>
        document.addEventListener('DOMContentLoaded', () => {
            const modal = document.getElementById('regex-modal');
            const addRuleButton = document.getElementById('add-rule-button');
            const closeModalButton = document.getElementById('close-modal-button');
            const editRuleButtons = document.querySelectorAll('.edit-rule-button');

            const modalTitle = document.getElementById('modal-title');
            const form = document.getElementById('regex-form');
            const ruleIdInput = document.getElementById('rule-id');
            const ruleNameInput = document.getElementById('rule-name');
            const rulePatternInput = document.getElementById('rule-pattern');
            const ruleCategoryInput = document.getElementById('rule-category');

            const openModal = () => modal.classList.remove('hidden');
            const closeModal = () => modal.classList.add('hidden');

            addRuleButton.addEventListener('click', () => {
                modalTitle.textContent = '新しいルールを追加';
                form.action = '/settings/rules/add';
                ruleIdInput.value = '';
                form.reset();
                openModal();
            });

            editRuleButtons.forEach(button => {
                button.addEventListener('click', () => {
                    modalTitle.textContent = 'ルールを編集';
                    form.action = '/settings/rules/update';
                    ruleIdInput.value = button.dataset.id;
                    ruleNameInput.value = button.dataset.name;
                    rulePatternInput.value = button.dataset.pattern;
                    ruleCategoryInput.value = button.dataset.category;
                    openModal();
                });
            });

            closeModalButton.addEventListener('click', closeModal);
            modal.addEventListener('click', (event) => {
                if (event.target === modal) {
                    closeModal();
                }
            });
        });
    </script>
</@layout.default>
