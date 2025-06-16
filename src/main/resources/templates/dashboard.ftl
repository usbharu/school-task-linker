<!-- src/main/resources/templates/dashboard.ftl -->
<#import "layout.ftl" as layout>
<@layout.default title="ダッシュボード" user=user>
    <div class="space-y-6">
        <div class="flex flex-col sm:flex-row justify-between items-center gap-4">
            <h1 class="text-2xl font-bold text-slate-800">登録済み課題一覧</h1>
            <form action="/check-mail" method="post">
                <button type="submit" class="w-full sm:w-auto px-4 py-2 bg-indigo-600 text-white font-semibold rounded-lg shadow-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2">
                    手動でメールをチェック
                </button>
            </form>
        </div>

        <#-- ステータス・エラーメッセージ -->
        <#if status??>
            <div class="bg-blue-100 border-l-4 border-blue-500 text-blue-700 p-4" role="alert">
                <p>
                    <#if status == "manual_check_started">手動でのメールチェックを開始しました。処理には数分かかる場合があります。
                    <#elseif status == "task_deleted">課題を削除しました。
                    </#if>
                </p>
            </div>
        </#if>
        <#if error??>
            <div class="bg-red-100 border-l-4 border-red-500 text-red-700 p-4" role="alert">
                <p class="font-bold">エラー</p>
                <p>処理に失敗しました。</p>
            </div>
        </#if>

        <div class="bg-white p-4 sm:p-6 rounded-lg shadow">
            <!-- 絞り込み・ソートフォーム -->
            <form id="task-controls" method="get" action="/dashboard" class="mb-4 grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                    <label for="course" class="block text-sm font-medium text-slate-700">講義名</label>
                    <select name="course" id="course" class="mt-1 block w-full rounded-md border-slate-300 shadow-sm focus:border-indigo-300 focus:ring focus:ring-indigo-200 focus:ring-opacity-50">
                        <option value="all">すべての講義</option>
                        <#if courseNames?has_content>
                            <#list courseNames as course>
                                <option value="${course?html}" <#if currentCourse == course>selected</#if>>${course?html}</option>
                            </#list>
                        </#if>
                    </select>
                </div>
                <div>
                    <label for="filter" class="block text-sm font-medium text-slate-700">ステータス</label>
                    <select name="filter" id="filter" class="mt-1 block w-full rounded-md border-slate-300 shadow-sm focus:border-indigo-300 focus:ring focus:ring-indigo-200 focus:ring-opacity-50">
                        <option value="all" <#if currentFilter == "all">selected</#if>>すべて</option>
                        <option value="incomplete" <#if currentFilter == "incomplete">selected</#if>>未完了</option>
                        <option value="overdue" <#if currentFilter == "overdue">selected</#if>>期限切れ</option>
                    </select>
                </div>
                <div>
                    <label for="sort" class="block text-sm font-medium text-slate-700">ソート</label>
                    <select name="sort" id="sort" class="mt-1 block w-full rounded-md border-slate-300 shadow-sm focus:border-indigo-300 focus:ring focus:ring-indigo-200 focus:ring-opacity-50">
                        <option value="deadline_asc" <#if currentSort == "deadline_asc">selected</#if>>期限が近い順</option>
                        <option value="deadline_desc" <#if currentSort == "deadline_desc">selected</#if>>期限が遠い順</option>
                    </select>
                </div>
            </form>

            <!-- 課題一覧テーブル -->
            <div class="overflow-x-auto">
                <#if tasks?has_content>
                    <table class="min-w-full divide-y divide-slate-200">
                        <thead class="bg-slate-50">
                        <tr>
                            <th class="px-4 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">講義名</th>
                            <th class="px-4 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">課題名</th>
                            <th class="px-4 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">期限</th>
                            <th class="px-4 py-3 text-right text-xs font-medium text-slate-500 uppercase tracking-wider">操作</th>
                        </tr>
                        </thead>
                        <tbody class="bg-white divide-y divide-slate-200">
                        <#list tasks as task>
                            <tr>
                                <td class="px-4 py-4 whitespace-nowrap text-sm text-slate-500">
                                    <a href="/dashboard?course=${task.courseName?url}&filter=${currentFilter}&sort=${currentSort}"
                                       class="text-indigo-600 hover:text-indigo-800 hover:underline"
                                       title="${task.courseName?html}で絞り込み">
                                        ${task.courseName?html}
                                    </a>
                                </td>
                                <td class="px-4 py-4 whitespace-nowrap text-sm font-medium text-slate-900">
                                    <button type="button" class="view-task-body text-left text-blue-600 hover:text-blue-800 hover:underline" data-task-id="${task.id}">
                                        ${task.taskName?html}
                                    </button>
                                </td>
                                <td class="px-4 py-4 whitespace-nowrap text-sm <#if task.overdue>text-red-600 font-bold<#else>text-slate-500</#if>">
                                    ${task.deadlineJstFormatted?html}
                                    <#if task.overdue><span class="text-xs ml-1">(期限切れ)</span></#if>
                                </td>
                                <td class="px-4 py-4 whitespace-nowrap text-right text-sm font-medium">
                                    <form action="/task/delete" method="post" onsubmit="return confirm('課題「${task.taskName?js_string}」を削除します。よろしいですか？');">
                                        <input type="hidden" name="id" value="${task.id}">
                                        <button type="submit" class="text-red-600 hover:text-red-800">削除</button>
                                    </form>
                                </td>
                            </tr>
                        </#list>
                        </tbody>
                    </table>
                <#else>
                    <p class="text-slate-500 text-center py-8">表示する課題はありません。メールをチェックするか、設定を確認してください。</p>
                </#if>
            </div>
        </div>
    </div>

    <!-- 課題本文表示用モーダル -->
    <div id="task-body-modal" class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full hidden z-50">
        <div class="relative top-20 mx-auto p-5 border w-full max-w-3xl shadow-lg rounded-md bg-white">
            <div class="mt-3">
                <div class="flex justify-between items-start mb-4">
                    <h3 id="modal-task-title" class="text-lg leading-6 font-medium text-gray-900"></h3>
                    <button id="close-task-modal-button" class="text-gray-400 hover:text-gray-600">
                        <span class="sr-only">Close</span>
                        <svg class="h-6 w-6" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>
                <div class="mt-2 px-4 py-3 bg-slate-50 rounded-md max-h-[60vh] overflow-y-auto">
                    <pre id="modal-task-body" class="whitespace-pre-wrap text-sm text-slate-800 font-sans"></pre>
                </div>
            </div>
        </div>
    </div>

    <script>
        document.addEventListener('DOMContentLoaded', () => {
            const filterSelect = document.getElementById('filter');
            const sortSelect = document.getElementById('sort');
            const courseSelect = document.getElementById('course');
            const form = document.getElementById('task-controls');

            function submitForm() {
                form.submit();
            }

            filterSelect.addEventListener('change', submitForm);
            sortSelect.addEventListener('change', submitForm);
            courseSelect.addEventListener('change', submitForm);

            // --- 課題詳細モーダルのためのスクリプト ---
            const modal = document.getElementById('task-body-modal');
            const modalTitle = document.getElementById('modal-task-title');
            const modalBody = document.getElementById('modal-task-body');
            const closeModalButton = document.getElementById('close-task-modal-button');

            document.querySelectorAll('.view-task-body').forEach(button => {
                button.addEventListener('click', async (event) => {
                    const taskId = event.currentTarget.dataset.taskId;
                    modalBody.textContent = '読み込み中...';
                    modal.classList.remove('hidden');

                    try {
                        const response = await fetch('/task/' + taskId);
                        if (!response.ok) {
                            throw new Error('Task not found');
                        }
                        const task = await response.json();

                        // innerHTMLを使わず、DOMを安全に構築して設定
                        modalTitle.innerHTML = ''; // 一旦クリア
                        const courseNode = document.createTextNode(task.courseName);
                        const brNode = document.createElement('br');
                        const spanNode = document.createElement('span');
                        spanNode.className = 'font-bold text-xl';
                        spanNode.textContent = task.taskName;
                        modalTitle.appendChild(courseNode);
                        modalTitle.appendChild(brNode);
                        modalTitle.appendChild(spanNode);

                        modalBody.textContent = task.body;

                    } catch (error) {
                        console.error('Failed to fetch task body:', error);
                        modalBody.textContent = 'エラー: 課題の詳細を取得できませんでした。';
                    }
                });
            });

            const closeModal = () => modal.classList.add('hidden');
            closeModalButton.addEventListener('click', closeModal);
            modal.addEventListener('click', (event) => {
                // 背景の黒い部分をクリックしても閉じるように
                if (event.target === modal) {
                    closeModal();
                }
            });
        });
    </script>
</@layout.default>
