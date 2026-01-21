// /static/js/comment.js
(() => {
    "use strict";

    const DEFAULT_PAGE_SIZE = 5;

    function qs(sel, root = document) {
        return root.querySelector(sel);
    }
    function qsa(sel, root = document) {
        return Array.from(root.querySelectorAll(sel));
    }

    function getPostId() {
        const form = qs("#comment-form");
        const fromForm = form?.dataset?.postId;
        if (fromForm) return fromForm;

        const pag = qs(".comment-pagination");
        const fromPag = pag?.dataset?.postId;
        if (fromPag) return fromPag;

        const postDiv = qs("[data-post-id]");
        const fromTop = postDiv?.dataset?.postId;
        if (fromTop) return fromTop;

        return null;
    }

    function getCurrentPageAndSizeFromDOM() {
        const pag = qs(".comment-pagination");
        if (!pag) return { page: 0, size: DEFAULT_PAGE_SIZE };

        const active = qs(".comment-pagination .page-number.active", pag);
        const page = active?.dataset?.page ? parseInt(active.dataset.page, 10) : 0;

        const anyBtn = qs(".comment-pagination [data-size]", pag);
        const size = anyBtn?.dataset?.size ? parseInt(anyBtn.dataset.size, 10) : DEFAULT_PAGE_SIZE;

        return {
            page: Number.isFinite(page) ? page : 0,
            size: Number.isFinite(size) ? size : DEFAULT_PAGE_SIZE,
        };
    }

    function getTotalPagesFromDOM() {
        // pagination이 없으면 totalPages=1로 봄
        const pag = qs(".comment-pagination");
        if (!pag) return 1;

        const pageBtns = qsa(".comment-pagination .page-number", pag);
        if (pageBtns.length === 0) return 1;

        // page-number 버튼들은 0..totalPages-1을 모두 렌더링하므로 길이가 totalPages
        return pageBtns.length;
    }

    function getCommentCountOnPageFromDOM() {
        const list = qs("#comments-list");
        if (!list) return 0;

        // "아직 댓글이 없어요" 같은 안내 div가 있으면 실제 댓글 0
        const items = qsa(".comment-item", list);
        return items.length;
    }

    function scrollToComments() {
        const anchor = qs("#comments");
        if (anchor) anchor.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    async function fetchHtml(url, options = {}) {
        const res = await fetch(url, {
            method: options.method || "GET",
            body: options.body,
            headers: options.headers || {},
            credentials: "same-origin",
            redirect: "follow",
        });

        const text = await res.text();
        if (!res.ok) {
            console.error("Request failed:", res.status, text);
            throw new Error(`Request failed: ${res.status}`);
        }
        return text;
    }

    function extractAndReplaceComments(htmlText) {
        const doc = new DOMParser().parseFromString(htmlText, "text/html");

        const newList = qs("#comments-list", doc);
        const newPagination = qs(".comment-pagination", doc);
        const newCount = qs("#comment-count-text", doc);

        const curList = qs("#comments-list");
        const curPagination = qs(".comment-pagination");
        const curCount = qs("#comment-count-text");

        if (!newList || !curList) {
            throw new Error("댓글 리스트 영역(#comments-list)을 찾지 못했습니다.");
        }

        curList.innerHTML = newList.innerHTML;

        // pagination 교체/제거/추가
        if (curPagination) {
            if (newPagination) {
                curPagination.outerHTML = newPagination.outerHTML;
            } else {
                curPagination.remove();
            }
        } else {
            const wrapper = qs(".comments-sticky-wrapper");
            if (wrapper && newPagination) {
                wrapper.insertAdjacentHTML("beforeend", newPagination.outerHTML);
            }
        }

        if (curCount && newCount) {
            curCount.textContent = newCount.textContent;
        }
    }

    // ✅ 비어있는 페이지면 이전 페이지로 자동 이동
    async function ensureNonEmptyPage(page, size) {
        // 현재 페이지가 0이거나, 댓글이 있으면 그대로
        const count = getCommentCountOnPageFromDOM();
        if (count > 0) return { page, size };

        // 댓글 0이고 page > 0이면 이전으로
        if (page > 0) {
            const prevPage = page - 1;
            await loadCommentsPage(prevPage, size, { keepScroll: true, autoFixEmpty: false });
            return { page: prevPage, size };
        }

        // page=0인데 비어있으면 그냥 유지(진짜 댓글이 없는 케이스)
        return { page, size };
    }

    async function loadCommentsPage(page, size, { keepScroll = true, autoFixEmpty = true } = {}) {
        const postId = getPostId();
        if (!postId) throw new Error("postId를 찾지 못했습니다.");

        const url = `/posts/${postId}?page=${page}&size=${size}#comments`;
        const html = await fetchHtml(url);

        extractAndReplaceComments(html);

        if (autoFixEmpty) {
            // 로드 후 비어있으면(삭제 등으로 인해) 자동 이전 페이지로
            await ensureNonEmptyPage(page, size);
        }

        if (keepScroll) scrollToComments();
    }

    function openEditMode(commentItemEl) {
        const view = qs(".comment-content-view", commentItemEl);
        const box = qs(".comment-edit-box", commentItemEl);
        if (!view || !box) return;

        view.classList.add("hidden");
        box.classList.remove("hidden");

        // textarea value를 현재 텍스트로 동기화
        const originText = qs(".comment-text", commentItemEl)?.textContent ?? "";
        const ta = qs(".comment-edit-textarea", commentItemEl);
        if (ta) ta.value = originText;
    }

    function closeEditMode(commentItemEl) {
        const view = qs(".comment-content-view", commentItemEl);
        const box = qs(".comment-edit-box", commentItemEl);
        if (!view || !box) return;

        box.classList.add("hidden");
        view.classList.remove("hidden");
    }

    function updateCharCount() {
        const ta = qs("#comment-content");
        const counter = qs("#comment-char-count");
        if (!ta || !counter) return;
        counter.textContent = `${ta.value.length}/500`;
    }

    // ✅ create/edit/delete 후 어디를 로드할지 정책
    async function afterMutationReload({ type }) {
        const { page, size } = getCurrentPageAndSizeFromDOM();

        if (type === "create") {
            // 새 댓글은 0페이지에서 바로 보여주고 싶으면 0으로
            await loadCommentsPage(0, size, { keepScroll: true, autoFixEmpty: false });
            return;
        }

        // edit/delete는 현재 페이지 유지하되, delete는 empty page 자동 보정
        await loadCommentsPage(page, size, { keepScroll: true, autoFixEmpty: type === "delete" });
    }

    async function postFormViaFetch(formEl, { type }) {
        const formData = new FormData(formEl);

        const html = await fetchHtml(formEl.action, {
            method: (formEl.method || "POST").toUpperCase(),
            body: formData,
        });

        extractAndReplaceComments(html);

        // create/edit/delete 후 페이지 로딩 정책 적용
        await afterMutationReload({ type });

        if (type === "create") {
            const ta = qs("#comment-content");
            if (ta) ta.value = "";
            updateCharCount();
        }
    }

    // =========================
    // 이벤트 위임
    // =========================
    document.addEventListener("click", async (e) => {
        // 댓글 페이징 버튼
        const pageBtn = e.target.closest(".comment-pagination .btn-page");
        if (pageBtn) {
            e.preventDefault();
            const page = parseInt(pageBtn.dataset.page ?? "0", 10);
            const size = parseInt(pageBtn.dataset.size ?? `${DEFAULT_PAGE_SIZE}`, 10);

            try {
                await loadCommentsPage(page, size, { keepScroll: true, autoFixEmpty: false });
            } catch (err) {
                console.error(err);
                alert("댓글을 불러오지 못했습니다.");
            }
            return;
        }

        // 수정 버튼
        const editBtn = e.target.closest(".btn-comment-edit");
        if (editBtn) {
            e.preventDefault();
            const item = editBtn.closest(".comment-item");
            if (item) openEditMode(item);
            return;
        }

        // 수정 취소
        const cancelBtn = e.target.closest(".btn-edit-cancel");
        if (cancelBtn) {
            e.preventDefault();
            const item = cancelBtn.closest(".comment-item");
            if (item) closeEditMode(item);
            return;
        }
    });

    // submit 캡처링으로 가로채기
    document.addEventListener(
        "submit",
        async (e) => {
            const form = e.target;

            // 댓글 작성
            if (form && form.id === "comment-form") {
                e.preventDefault();
                try {
                    await postFormViaFetch(form, { type: "create" });
                } catch (err) {
                    console.error(err);
                    alert("댓글 등록에 실패했습니다.");
                }
                return;
            }

            // 댓글 삭제
            if (form && form.matches("form.inline-form")) {
                const ok = confirm("댓글을 삭제할까요?");
                if (!ok) {
                    e.preventDefault();
                    return;
                }

                e.preventDefault();
                try {
                    await postFormViaFetch(form, { type: "delete" });
                } catch (err) {
                    console.error(err);
                    alert("댓글 삭제에 실패했습니다.");
                }
                return;
            }

            // 댓글 수정 저장
            if (form && form.closest(".comment-edit-box")) {
                e.preventDefault();
                try {
                    await postFormViaFetch(form, { type: "edit" });
                } catch (err) {
                    console.error(err);
                    alert("댓글 수정에 실패했습니다.");
                }
                return;
            }
        },
        true
    );

    document.addEventListener("input", (e) => {
        if (e.target && e.target.id === "comment-content") updateCharCount();
    });

    document.addEventListener("DOMContentLoaded", () => {
        updateCharCount();
    });
})();
