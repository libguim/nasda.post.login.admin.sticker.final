/**
 * 나의 영감 저장소 - Main JavaScript
 * 브런치/핀터레스트 감성 디자인
 *
 * ✅ 핵심:
 * - URL에 keyword가 있으면 "검색 모드"
 * - 검색 모드에서는:
 *   1) 홈 상태 복원(sessionStorage) 금지
 *   2) 카테고리 클릭 시 API 로딩 금지
 *   3) 무한 스크롤 금지
 */

/** ✅ 페이지 구분 */
function isHomePage() {
    return location.pathname === "/" || location.pathname === "";
}

/** ✅ 공통 유틸 */
function escapeHtml(str) {
    return String(str ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function showLoadingSpinner() {
    const indicator = document.getElementById("loadingIndicator");
    if (indicator) indicator.classList.remove("hidden");
}

function hideLoadingSpinner() {
    const indicator = document.getElementById("loadingIndicator");
    if (indicator) indicator.classList.add("hidden");
}

/** ✅ 헤더 호출 브릿지 */
function openSearchModal() {
    openSearchModalLegacy();
}
function closeSearchModal() {
    closeSearchModalLegacy();
}

/** ========================================
 * 1. 전역 변수
 * ======================================== */
let currentCategory = "전체";
let isLoading = false;

/** ========================================
 * 2. 모바일 검색 모달
 * ======================================== */
function openSearchModalLegacy() {
    const modal = document.getElementById("searchModal");
    if (!modal) return;

    if (modal.classList && modal.classList.contains("hidden")) {
        modal.classList.remove("hidden");
        modal.classList.add("flex");
    } else {
        modal.style.display = "flex";
    }

    document.body.style.overflow = "hidden";

    const input = modal.querySelector(".search-input-modal");
    if (input) setTimeout(() => input.focus(), 100);
}

function closeSearchModalLegacy() {
    const modal = document.getElementById("searchModal");
    if (!modal) return;

    if (modal.classList && !modal.classList.contains("hidden")) {
        modal.classList.add("hidden");
        modal.classList.remove("flex");
    } else {
        modal.style.display = "none";
    }
    document.body.style.overflow = "";
}

document.addEventListener("DOMContentLoaded", function () {
    const modal = document.getElementById("searchModal");
    if (modal) {
        modal.addEventListener("click", function (e) {
            if (e.target === modal) closeSearchModalLegacy();
        });
    }
    document.addEventListener("keydown", function (e) {
        if (e.key === "Escape") closeSearchModalLegacy();
    });
});

/** ========================================
 * 3. Lazy Loading
 * ======================================== */
function initLazyLoading() {
    const images = document.querySelectorAll("img[data-src]");
    if (images.length === 0) return;

    if ("IntersectionObserver" in window) {
        const imageObserver = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    const img = entry.target;
                    img.src = img.dataset.src;
                    img.removeAttribute("data-src");
                    imageObserver.unobserve(img);
                }
            });
        });

        images.forEach((img) => imageObserver.observe(img));
    } else {
        images.forEach((img) => {
            img.src = img.dataset.src;
            img.removeAttribute("data-src");
        });
    }
}

/** ========================================
 * 4. (레거시) URL 이동 방식 카테고리
 * ======================================== */
function filterByCategory(category) {
    currentCategory = category;
    window.location.href = `/?category=${encodeURIComponent(category)}`;
}

/** ========================================
 * ✅ HOME 전용 로직
 * ======================================== */
(function initHomeModule() {
    if (!isHomePage()) return;

    const grid = document.getElementById("masonryGrid");
    if (!grid) return;

    const params = new URLSearchParams(location.search);
    const hasKeyword = params.has("keyword") && (params.get("keyword") || "").trim() !== "";

    const IS_SEARCH_MODE =
        (typeof window.__IS_SEARCH_PAGE__ === "boolean" && window.__IS_SEARCH_PAGE__ === true) ||
        hasKeyword;

    if (IS_SEARCH_MODE) {
        try {
            sessionStorage.removeItem("homeScrollState:v1");
        } catch (e) {}
        return;
    }

    let homeCurrentPage = 0;
    let homeIsLoading = false;
    let homeHasMore =
        typeof window.__HOME_HAS_NEXT__ === "boolean" ? window.__HOME_HAS_NEXT__ : true;

    let activeCategory = params.get("category") || "전체";
    const pageSize =
        typeof window.__HOME_PAGE_SIZE__ === "number" ? window.__HOME_PAGE_SIZE__ : 12;

    const HOME_STATE_KEY = "homeScrollState:v1";

    function saveHomeState(extra = {}) {
        try {
            const state = {
                scrollY: window.scrollY || 0,
                currentPage: homeCurrentPage,
                hasMore: homeHasMore,
                activeCategory,
                pageSize,
                ts: Date.now(),
                ...extra,
            };
            sessionStorage.setItem(HOME_STATE_KEY, JSON.stringify(state));
        } catch (e) {}
    }

    function loadHomeState() {
        try {
            const raw = sessionStorage.getItem(HOME_STATE_KEY);
            if (!raw) return null;
            return JSON.parse(raw);
        } catch (e) {
            return null;
        }
    }

    function clearHomeState() {
        try {
            sessionStorage.removeItem(HOME_STATE_KEY);
        } catch (e) {}
    }

    window.navigateToCreate = function navigateToCreate() {
        window.location.href = "/posts/create";
    };

    window.setActiveCategory = async function setActiveCategory(category) {
        activeCategory = category;

        document.querySelectorAll(".category-btn").forEach((btn) => {
            btn.classList.remove("bg-[#D4C4B0]", "text-white");
            btn.classList.add("bg-[#F9F6F1]", "text-[#8B7355]", "hover:bg-[#F0EBE3]");
        });

        const selectedBtn = document.querySelector(`.category-btn[data-category="${category}"]`);
        if (selectedBtn) {
            selectedBtn.classList.remove("bg-[#F9F6F1]", "text-[#8B7355]", "hover:bg-[#F0EBE3]");
            selectedBtn.classList.add("bg-[#D4C4B0]", "text-white");
        }

        homeCurrentPage = 0;
        homeHasMore = true;

        saveHomeState();
        await loadPostsByCategory(category);
    };

    async function apiGetPosts(paramsObj) {
        if (window.axios && typeof window.axios.get === "function") {
            const res = await window.axios.get("/api/posts", { params: paramsObj });
            return res.data;
        }

        const query = new URLSearchParams();
        Object.entries(paramsObj).forEach(([k, v]) => {
            if (v !== null && v !== undefined && v !== "") query.append(k, v);
        });

        const res = await fetch(`/api/posts?${query.toString()}`);
        if (!res.ok) throw new Error("Failed to load posts");
        return await res.json();
    }

    function renderEmptyStateWithCta() {
        grid.innerHTML = `
      <div class="col-span-full text-center py-24">
        <p class="text-2xl font-semibold text-[#5A4D41]" style="font-family:'Playfair Display', serif;">
          아직 작성된 게시글이 없어요
        </p>
        <p class="mt-3 text-[#8B7355]" style="font-family:'Noto Sans KR', sans-serif;">
          오른쪽 아래 <b>+</b> 버튼을 눌러 첫 기록을 남겨보세요!
        </p>
        <div class="mt-8">
          <a href="/posts/create"
             class="inline-flex items-center gap-2 px-6 py-3 rounded-2xl bg-[#D4C4B0] text-white hover:bg-[#C9B5A0] transition"
             style="font-family:'Noto Sans KR', sans-serif;">
            <span class="text-xl">＋</span>
            <span class="font-medium">게시글 작성</span>
          </a>
        </div>
      </div>
    `;
    }

    function renderPosts(posts) {
        if (!posts || posts.length === 0) {
            renderEmptyStateWithCta();
            return;
        }

        grid.innerHTML = posts
            .filter((p) => p && p.id != null)
            .map((p) => {
                const title = escapeHtml(p.title || "");
                const imageUrl =
                    p.imageUrl && String(p.imageUrl).trim() ? p.imageUrl : "https://picsum.photos/400/600";

                return `
          <a href="/posts/${p.id}"
             class="home-card group relative overflow-hidden rounded-2xl cursor-pointer transition-all duration-500 hover:shadow-xl bg-[#F9F6F1] block">
            <img src="${imageUrl}"
                 alt="${title}"
                 class="home-card-img w-full bg-[#F9F6F1] transition-transform duration-700 group-hover:scale-105"
                 style="display:block;"
                 onerror="this.onerror=null;this.src='https://picsum.photos/400/600';" />
            <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-black/0 to-black/0 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
              <div class="absolute bottom-0 left-0 right-0 p-4">
                <h3 class="text-white" style="font-family:'Noto Sans KR', sans-serif; font-weight:600;">
                  ${title}
                </h3>
              </div>
            </div>
          </a>
        `;
            })
            .join("");
    }

    function appendPosts(posts) {
        const html = (posts || [])
            .filter((p) => p && p.id != null)
            .map((p) => {
                const title = escapeHtml(p.title || "");
                const imageUrl =
                    p.imageUrl && String(p.imageUrl).trim() ? p.imageUrl : "https://picsum.photos/400/600";

                return `
          <a href="/posts/${p.id}"
             class="home-card group relative overflow-hidden rounded-2xl cursor-pointer transition-all duration-500 hover:shadow-xl bg-[#F9F6F1] block">
            <img src="${imageUrl}"
                 alt="${title}"
                 class="home-card-img w-full bg-[#F9F6F1] transition-transform duration-700 group-hover:scale-105"
                 style="display:block;"
                 onerror="this.onerror=null;this.src='https://picsum.photos/400/600';" />
            <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-black/0 to-black/0 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
              <div class="absolute bottom-0 left-0 right-0 p-4">
                <h3 class="text-white" style="font-family:'Noto Sans KR', sans-serif; font-weight:600;">
                  ${title}
                </h3>
              </div>
            </div>
          </a>
        `;
            })
            .join("");

        grid.insertAdjacentHTML("beforeend", html);
    }

    async function loadPostsByCategory(category) {
        showLoadingSpinner();
        try {
            const page = await apiGetPosts({
                page: 0,
                size: pageSize,
                category: category !== "전체" ? category : null,
            });

            const posts = page.content || [];
            renderPosts(posts);

            homeHasMore = !page.last;
            homeCurrentPage = page.number || 0;

            saveHomeState();
        } catch (e) {
            console.error("게시물 로드 오류:", e);
        } finally {
            hideLoadingSpinner();
        }
    }

    async function loadMorePosts() {
        if (homeIsLoading || !homeHasMore) return;

        homeIsLoading = true;
        showLoadingSpinner();

        try {
            const page = await apiGetPosts({
                page: homeCurrentPage + 1,
                size: pageSize,
                category: activeCategory !== "전체" ? activeCategory : null,
            });

            const posts = page.content || [];
            if (!posts || posts.length === 0) {
                homeHasMore = false;
                saveHomeState();
                return;
            }

            homeCurrentPage = page.number;
            homeHasMore = !page.last;
            appendPosts(posts);

            saveHomeState();
        } catch (e) {
            console.error("추가 게시물 로드 오류:", e);
            homeHasMore = false;
            saveHomeState();
        } finally {
            homeIsLoading = false;
            hideLoadingSpinner();
        }
    }

    async function restoreHomeIfNeeded() {
        const state = loadHomeState();
        if (!state) return;

        if (state.ts && Date.now() - state.ts > 2 * 60 * 60 * 1000) {
            clearHomeState();
            return;
        }

        if (state.activeCategory) activeCategory = state.activeCategory;

        document.querySelectorAll(".category-btn").forEach((btn) => {
            btn.classList.remove("bg-[#D4C4B0]", "text-white");
            btn.classList.add("bg-[#F9F6F1]", "text-[#8B7355]", "hover:bg-[#F0EBE3]");
        });
        const selectedBtn = document.querySelector(`.category-btn[data-category="${activeCategory}"]`);
        if (selectedBtn) {
            selectedBtn.classList.remove("bg-[#F9F6F1]", "text-[#8B7355]", "hover:bg-[#F0EBE3]");
            selectedBtn.classList.add("bg-[#D4C4B0]", "text-white");
        }

        const targetPage = Number(state.currentPage);
        if (!Number.isFinite(targetPage) || targetPage < 0) {
            requestAnimationFrame(() => window.scrollTo(0, state.scrollY || 0));
            return;
        }

        try {
            homeIsLoading = true;
            for (let p = 0; p <= targetPage; p++) {
                const page = await apiGetPosts({
                    page: p,
                    size: pageSize,
                    category: activeCategory !== "전체" ? activeCategory : null,
                });

                const posts = page.content || [];
                if (p === 0) renderPosts(posts);
                else appendPosts(posts);

                homeCurrentPage = page.number || p;
                homeHasMore = !page.last;
            }
        } catch (e) {
            console.warn("restoreHomeIfNeeded load failed:", e);
        } finally {
            homeIsLoading = false;
        }

        requestAnimationFrame(() => window.scrollTo(0, state.scrollY || 0));
    }

    document.addEventListener("click", (e) => {
        const a = e.target.closest("a");
        if (!a) return;
        const href = a.getAttribute("href") || "";
        if (href.startsWith("/posts/")) saveHomeState();
    });

    let __saveT = null;
    window.addEventListener("scroll", function () {
        const scrollPosition = window.innerHeight + window.scrollY;
        const threshold = document.body.offsetHeight - 500;

        if (scrollPosition >= threshold) loadMorePosts();

        if (__saveT) return;
        __saveT = setTimeout(() => {
            saveHomeState();
            __saveT = null;
        }, 250);
    });

    window.addEventListener("pageshow", () => restoreHomeIfNeeded());
    document.addEventListener("DOMContentLoaded", () => restoreHomeIfNeeded());
})();

/** ========================================
 * 초기화
 * ======================================== */
document.addEventListener("DOMContentLoaded", function () {
    console.log("나의 영감 저장소 - 페이지 로드 완료");
    initLazyLoading();
});

/** 전역 에러 로깅 */
window.addEventListener("error", function (e) {
    console.error("JavaScript Error:", e.error);
});
window.addEventListener("unhandledrejection", function (e) {
    console.error("Unhandled Promise Rejection:", e.reason);
});
