/**
 * sticker.js - 포스트 이미지 꾸미기 및 스티커 관리 로직
 */
(function() {
    const BASE_URL = 'https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/';
    const pathMap = {
        'Smileys & Emotion': 'Smilies', 'People & Body': 'People', 'Animals & Nature': 'Animals',
        'Food & Drink': 'Food', 'Travel & Places': 'Travel and places', 'Activities': 'Activities',
        'Objects': 'Objects', 'Symbols': 'Symbols', 'Flags': 'Flags'
    };
    const stickerData = {
        'Smileys & Emotion': ['Love Letter', 'Smiling Face with Hearts', 'Face Blowing a Kiss', 'Heart with Arrow', 'Heart with Ribbon', 'Sparkling Heart', 'Growing Heart', 'Beating Heart', 'Revolving Hearts', 'Two Hearts', 'Heart Decoration', 'Broken Heart', 'Heart on Fire', 'Mending Heart', 'Red Heart'],
        'People & Body': ['Baby', 'Boy', 'Girl', 'Man', 'Woman', 'Ninja', 'Prince', 'Princess', 'Superhero', 'Clapping Hands', 'Thumbs Up', 'Victory Hand'],
        'Animals & Nature': ['Ant', 'Bat', 'Bear', 'Beetle', 'Bird', 'Butterfly', 'Cat Face', 'Dog Face', 'Elephant', 'Frog', 'Panda', 'Unicorn'],
        'Food & Drink': ['Birthday Cake', 'Pizza', 'Beer Mug', 'Chocolate Bar', 'Candy', 'Hamburger', 'Taco', 'Red Apple', 'Doughnut', 'Cupcake']
    };

    let stickers = []; // 전체 스티커 목록 (서버에서 가져온 것 + 새로 추가한 것)
    let isDecorating = false;

// --- [섹션 1] 통신 담당 함수 (API 리포지토리 역할) ---

    // 장식 저장 (일괄)
    async function addDecorations(decorationObj) {
        const response = await axios.post(`/api/decorations`, decorationObj);
        return response.data;
    }

    // 게시글 전체 장식 조회
    async function getDecorationsByPost(postId) {
        const response = await axios.get(`/api/decorations/post/${postId}`);
        return response.data;
    }

    // 장식 개별 삭제 (필요 시)
    async function removeDecoration(decorationId, currentUserId) {
        const response = await axios.delete(`/api/decorations/${decorationId}?currentUserId=${currentUserId}`);
        return response.data;
    }

// --- [섹션 2] 데이터 로드 및 UI 렌더링 ---

    function loadSavedDecorations() {
        const postId = window.ST_DATA?.postId;
        if (!postId) return;

        // ✅ 통신 함수 호출 및 .then() 처리
        getDecorationsByPost(postId).then(data => {
            stickers = data.map(item => ({
                dbId: item.decorationId,
                imgUrl: item.stickerImageUrl || `${BASE_URL}Activities/Sparkles.png`,
                x: item.posX,
                y: item.posY,
                isSaved: true,
                slideIndex: item.postImageId - 10
            }));
            renderStickers();
        }).catch(e => {
            console.error("기존 장식 로드 실패:", e);
        });
    }

    // --- 서버에서 기존 스티커 데이터 불러오기 ---
    // async function loadSavedDecorations() {
    //     const postId = window.ST_DATA?.postId;
    //     if (!postId) return;
    //
    //     try {
    //         // postId를 기준으로 해당 게시글의 모든 스티커를 가져온다고 가정
    //         const response = await axios.get(`/api/decorations/post/${postId}`);
    //         stickers = response.data.map(item => ({
    //             dbId: item.decorationId,
    //             imgUrl: item.stickerImageUrl || `${BASE_URL}Activities/Sparkles.png`, // URL 필드가 있다면 사용
    //             x: item.posX,
    //             y: item.posY,
    //             isSaved: true,
    //             slideIndex: item.postImageId - 10 // 서버 저장 시 (10 + index)로 저장했으므로 다시 인덱스로 변환
    //         }));
    //         renderStickers();
    //     } catch (err) {
    //         console.error("기존 장식 로드 실패:", err);
    //     }
    // }

    // --- 팔레트 및 렌더링 로직 ---
    function initPalette() {
        const tabContainer = document.getElementById('sticker-category-tabs');
        if (!tabContainer) return;
        tabContainer.innerHTML = '';
        Object.keys(stickerData).forEach((catName, idx) => {
            const tab = document.createElement('button');
            tab.className = `category-btn ${idx === 0 ? 'active' : ''}`;
            tab.textContent = catName;
            tab.onclick = () => {
                tabContainer.querySelectorAll('.category-btn').forEach(b => b.classList.remove('active'));
                tab.classList.add('active');
                tab.scrollIntoView({behavior: 'smooth', inline: 'center'});
                renderPalette(catName);
            };
            tabContainer.appendChild(tab);
        });
        renderPalette(Object.keys(stickerData)[0]);
    }

    function renderPalette(category) {
        const palette = document.getElementById('sticker-palette');
        if (!palette) return;
        palette.innerHTML = '';
        const categoryPath = pathMap[category] || category;
        stickerData[category].forEach((name, index) => {
            const url = `${BASE_URL}${encodeURIComponent(categoryPath)}/${encodeURIComponent(name)}.png`;
            const div = document.createElement('div');
            div.className = 'palette-item cursor-grab p-2 bg-[#F9F6F1] rounded-xl flex items-center justify-center';
            div.innerHTML = `<img src="${url}" class="w-12 h-12 object-contain pointer-events-none">`;
            div.draggable = true;
            div.addEventListener('dragstart', (e) => {
                e.dataTransfer.setData('imgUrl', url);
            });
            palette.appendChild(div);
        });
    }

    // ✅ 핵심: 현재 슬라이드에 맞는 스티커만 필터링해서 렌더링
    function renderStickers() {
        // 1. HTML에 선언된 ID로 레이어 찾기
        const layer = document.getElementById('sticker-layer');
        if (!layer) return;

        // 2. 레이어 비우기
        layer.innerHTML = '';

        // 3. 현재 활성화된 슬라이드 확인 (Swiper 기준)
        const activeSlide = document.querySelector('.swiper-slide-active');
        if (!activeSlide) return;

        // HTML에 data-slide-index가 없으므로, 현재 캔버스가 활성화된 슬라이드 안에 있는지 확인
        const activeCanvas = activeSlide.querySelector('#sticker-canvas');

        // 만약 첫 번째 슬라이드(캔버스 있는 곳)가 아니면 렌더링 스킵 (혹은 전체 노출 선택)
        if (!activeCanvas && !isDecorating) {
            // 꾸미기 중이 아닐 때 다른 슬라이드에서도 스티커를 보이고 싶다면 이 조건을 조정하세요.
        }

        stickers.forEach((s) => {
            const el = document.createElement('div');
            el.className = 'sticker-item absolute transform -translate-x-1/2 -translate-y-1/2 group';
            el.style.left = s.x + '%';
            el.style.top = s.y + '%';
            el.style.position = 'absolute'; // 스타일 명시

            el.innerHTML = `
            <img src="${s.imgUrl}" class="w-12 h-12 object-contain pointer-events-none" style="display:block;">
            ${isDecorating ? `<div class="btn-remove absolute -top-2 -right-2 bg-red-500 text-white rounded-full w-5 h-5 flex items-center justify-center text-xs cursor-pointer" style="z-index:100;">×</div>` : ''}
        `;

            if (isDecorating) {
                el.querySelector('.btn-remove').onclick = (e) => {
                    e.stopPropagation();
                    stickers = stickers.filter(item => item !== s);
                    renderStickers();
                };
            }
            layer.appendChild(el);
        });
    }
    // function renderStickers() {
    //     // 1. 모든 레이어 비우기
    //     document.querySelectorAll('.sticker-layer').forEach(l => l.innerHTML = '');
    //
    //     // 2. 현재 활성화된 슬라이드 찾기
    //     const activeSlide = document.querySelector('.swiper-slide-active');
    //     if (!activeSlide) return;
    //
    //     const layer = activeSlide.querySelector('.sticker-layer');
    //     const currentSlideIdx = Number(activeSlide.querySelector('.sticker-canvas')?.getAttribute('data-slide-index'));
    //
    //     if (!layer) return;
    //
    //     // 3. 현재 슬라이드 인덱스와 일치하는 스티커만 필터링
    //     const currentStickers = stickers.filter(s => Number(s.slideIndex) === currentSlideIdx);
    //
    //     currentStickers.forEach((s) => {
    //         const el = document.createElement('div');
    //         el.className = 'sticker-item absolute transform -translate-x-1/2 -translate-y-1/2 group';
    //         el.style.left = s.x + '%';
    //         el.style.top = s.y + '%';
    //
    //         el.innerHTML = `
    //             <img src="${s.imgUrl}" class="w-12 h-12 object-contain pointer-events-none">
    //             ${isDecorating ? `<div class="btn-remove absolute -top-2 -right-2 bg-red-500 text-white rounded-full w-5 h-5 flex items-center justify-center text-xs cursor-pointer">×</div>` : ''}
    //         `;
    //
    //         if (isDecorating) {
    //             el.draggable = true;
    //             el.querySelector('.btn-remove').onclick = () => {
    //                 // 서버 삭제 로직 필요시 추가 (axios.delete)
    //                 stickers = stickers.filter(item => item !== s);
    //                 renderStickers();
    //             };
    //             el.ondragend = (e) => {
    //                 const rect = layer.getBoundingClientRect();
    //                 s.x = ((e.clientX - rect.left) / rect.width) * 100;
    //                 s.y = ((e.clientY - rect.top) / rect.height) * 100;
    //             };
    //         }
    //         layer.appendChild(el);
    //     });
    // }

    // window.saveDecoration = async function() {
    //     const newItems = stickers.filter(s => !s.isSaved);
    //     if (newItems.length === 0) return alert("새로 추가된 장식이 없습니다.");
    //
    //     // 서버 DTO 형식에 맞춰 데이터 준비
    //     // 각 스티커별로 postImageId를 다르게 설정해야 함
    //     try {
    //         for (const s of newItems) {
    //             const decorationObj = {
    //                 postImageId: 10 + Number(s.slideIndex),
    //                 userId: Number(window.ST_DATA?.currentUserId || 1),
    //                 stickerId: 1,
    //                 stickerImageUrl: s.imgUrl,
    //                 posX: parseFloat(s.x.toFixed(2)),
    //                 posY: parseFloat(s.y.toFixed(2)),
    //                 scale: 1.0,
    //                 rotation: 0.0,
    //                 zIndex: 10
    //             };
    //             await axios.post(`/api/decorations`, decorationObj);
    //         }
    //         alert("저장되었습니다! ✨");
    //         loadSavedDecorations();
    //         window.finishDecoration();
    //     } catch (e) {
    //         console.error(e);
    //         alert("저장 실패");
    //     }
    // };

    // --- [섹션 3] 제어 함수 (저장/취소 등) ---

    window.saveDecoration = function() {
        const newItems = stickers.filter(s => !s.isSaved);
        if (newItems.length === 0) return alert("새로 추가된 장식이 없습니다.");

        const activeCanvas = document.querySelector('.swiper-slide-active .sticker-canvas');
        // const slideIdx = activeCanvas ? Number(activeCanvas.getAttribute('data-slide-index')) : 0;
        const imageID = Number(activeCanvas.getAttribute('data-image-id'));

        // DTO 조립
        const decorationObj = {
            postImageId: imageID,
            userId: Number(window.ST_DATA?.currentUserId || 1),
            decorations: newItems.map(s => ({
                stickerId: 1,
                posX: parseFloat(s.x.toFixed(2)),
                posY: parseFloat(s.y.toFixed(2)),
                scale: 1.0,
                rotation: 0.0,
                zIndex: 10
            }))
        };

        // ✅ 통신 함수 호출 및 .then() 처리
        addDecorations(decorationObj).then(resultList => {
            alert(resultList.length + "개의 스티커가 저장되었습니다! ✨");
            loadSavedDecorations();
            window.finishDecoration();
        }).catch(e => {
            console.error(e);
            alert("저장 실패: " + (e.response?.data?.message || "서버 통신 오류"));
        });
    };

    // --- 외부 노출 함수 ---
    window.startDecoration = function() {
        isDecorating = true;
        const layer = document.getElementById('sticker-layer');
        if (layer) {
            layer.classList.remove('pointer-events-none');
            layer.style.pointerEvents = 'auto'; // JS로 강제 설정
        }
        document.getElementById('deco-start-view')?.classList.add('hidden');
        document.getElementById('deco-active-view')?.classList.remove('hidden');
        initPalette();
        renderStickers();
    };

    window.finishDecoration = function() {
        isDecorating = false;
        document.getElementById('deco-start-view')?.classList.remove('hidden');
        document.getElementById('deco-active-view')?.classList.add('hidden');
        renderStickers();
    };

    window.cancelDecoration = function() {
        if (confirm('취소하시겠습니까?')) {
            loadSavedDecorations(); // 서버 데이터로 원복
            window.finishDecoration();
        }
    };

    // --- 초기 실행 및 이벤트 ---
    document.addEventListener('DOMContentLoaded', () => {
        loadSavedDecorations();

        const swiperEl = document.querySelector('.postImagesSwiper');
        if (swiperEl) {
            // 드래그 허용
            swiperEl.addEventListener('dragover', e => {
                if (!isDecorating) return;
                e.preventDefault();
                e.dataTransfer.dropEffect = 'copy';
            });

            // 드롭 처리
            swiperEl.addEventListener('drop', e => {
                if (!isDecorating) return;
                e.preventDefault();

                // ID로 직접 캔버스 참조
                const activeCanvas = document.getElementById('sticker-canvas');
                if (!activeCanvas) return;

                const imgUrl = e.dataTransfer.getData('imgUrl');
                if (!imgUrl) return;

                const rect = activeCanvas.getBoundingClientRect();
                const x = ((e.clientX - rect.left) / rect.width) * 100;
                const y = ((e.clientY - rect.top) / rect.height) * 100;

                // 좌표가 캔버스 범위를 벗어났는지 체크 (선택 사항)
                if (x < 0 || x > 100 || y < 0 || y > 100) return;

                stickers.push({
                    imgUrl,
                    x,
                    y,
                    isSaved: false,
                    slideIndex: 0 // HTML에 없으므로 첫 번째 슬라이드(0)로 고정
                });
                renderStickers();
            });
        }
    });
    // document.addEventListener('DOMContentLoaded', () => {
    //     loadSavedDecorations(); // 1. 페이지 로드 시 저장된 스티커 가져오기
    //
    //     const swiperEl = document.querySelector('.postImagesSwiper');
    //     if (swiperEl) {
    //         // 2. 드롭 이벤트
    //         swiperEl.addEventListener('dragover', e => e.preventDefault());
    //         swiperEl.addEventListener('drop', e => {
    //             if (!isDecorating) return;
    //             const activeCanvas = document.querySelector('.swiper-slide-active .sticker-canvas');
    //             if (!activeCanvas) return;
    //
    //             const imgUrl = e.dataTransfer.getData('imgUrl');
    //             const rect = activeCanvas.getBoundingClientRect();
    //             const x = ((e.clientX - rect.left) / rect.width) * 100;
    //             const y = ((e.clientY - rect.top) / rect.height) * 100;
    //             const slideIdx = activeCanvas.getAttribute('data-slide-index');
    //
    //             stickers.push({
    //                 imgUrl, x, y, isSaved: false, slideIndex: slideIdx
    //             });
    //             renderStickers();
    //         });
    //
    //         // 3. 슬라이드 변경 시 렌더링 다시 하기
    //         if (swiperEl.swiper) {
    //             swiperEl.swiper.on('slideChange', () => renderStickers());
    //         } else {
    //             // Swiper 객체가 늦게 생성될 경우를 대비
    //             setTimeout(() => {
    //                 if (swiperEl.swiper) swiperEl.swiper.on('slideChange', () => renderStickers());
    //             }, 300);
    //         }
    //     }
    // });

})();