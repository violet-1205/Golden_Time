<script setup>
import { ref, computed, onBeforeUnmount, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '../store/auth'

const router = useRouter()
const route = useRoute()
const { currentUser, isAdmin, logout } = useAuth()

const isSidebarCollapsed = ref(false)
const isSidebarHidden = ref(false)

let sidebarMq = null
function syncSidebarForViewport() {
  if (!sidebarMq) return
  const isMobile = sidebarMq.matches

  // 모바일에서는 기본적으로 사이드바를 숨겨서 화면이 찌그러지지 않게 함
  if (isMobile) {
    isSidebarCollapsed.value = false
    isSidebarHidden.value = true
  }
}

onMounted(() => {
  sidebarMq = window.matchMedia('(max-width: 768px)')
  syncSidebarForViewport()
  sidebarMq.addEventListener('change', syncSidebarForViewport)
})

onBeforeUnmount(() => {
  if (!sidebarMq) return
  sidebarMq.removeEventListener('change', syncSidebarForViewport)
})

function toggleSidebar() {
  // "중간 상태(접기)로 한번 걸쳐서"가 아니라,
  // 열린 상태 <-> 닫힌 상태만 토글하도록 단순화.
  // 모바일에서는 기본값이 hidden=true 이며, 여기서만 토글한다.
  if (sidebarMq && sidebarMq.matches) {
    isSidebarCollapsed.value = false
    isSidebarHidden.value = !isSidebarHidden.value
    return
  }

  // 데스크탑에서도 접기/펴기 2단계를 없애고, 완전 닫힘/열림만 토글
  isSidebarCollapsed.value = false
  isSidebarHidden.value = !isSidebarHidden.value
}

const navItems = computed(() => {
  if (isAdmin.value) {
    return [
      { label: '대시보드', path: '/main' },
      { label: '신고 목록', path: '/dashboard' },
      { label: '회원 관리', path: '/users' },
      { label: '공지사항', path: '/notice' },
    ]
  } else {
    return [
      { label: '신고 목록', path: '/dashboard' },
      { label: '마이페이지', path: '/mypage' },
      { label: '공지사항', path: '/notice' },
      { label: '문의게시판', path: '/inquiry' },
    ]
  }
})

function handleLogout() {
  logout()
  router.push('/login?logout=1')
}

</script>

<template>
  <div class="app-layout">

    <!-- ===== 상단 헤더 ===== -->
    <header class="top-header">

      <!-- 1행: 로고 + 인사 + 날씨/위치 + 로그아웃 -->
      <div class="header-top">

        <!-- 사이드바가 숨겨졌을 때만 나타나는 복구 버튼 -->
        <button v-if="isSidebarHidden" class="btn-toggle-restore" @click="toggleSidebar" title="메뉴 열기">
          <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M4 6h16M4 12h16M4 18h16" /></svg>
        </button>

        <!-- 로고 -->
        <div class="logo" @click="isAdmin ? router.push('/main') : router.push('/dashboard')">
          <img src="/logos/logo.png" alt="GoldenTime" class="logo-img" />
        </div>

        <!-- 인사말 (flex:1 로 남은 공간 채움) -->
        <div class="header-greeting">
          <span class="role-badge" :class="isAdmin ? 'badge-admin' : 'badge-user'">
            {{ isAdmin ? '관리자' : '사용자' }}
          </span>
          <span class="greeting-text">
            <strong>{{ currentUser?.userName || currentUser?.loginId }}</strong>님, 안녕하세요!
          </span>
        </div>

        <!-- 우측 여백 -->
        <div class="header-spacer"></div>

        <!-- 로그아웃 -->
        <button class="btn-logout" @click="handleLogout">로그아웃</button>

      </div>

    </header>

    <!-- ===== 바디: 사이드바 + 콘텐츠 ===== -->
    <div class="body-area">

      <!-- 좌측 사이드바 -->
      <aside :class="['sidebar', { collapsed: isSidebarCollapsed, hidden: isSidebarHidden }]">
        <div class="sidebar-header" v-if="!isSidebarHidden">
          <p class="nav-header" v-if="!isSidebarCollapsed">메뉴</p>
          <button class="btn-sidebar-toggle" @click="toggleSidebar" title="메뉴 닫기">
            <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
              <path d="M11 19l-7-7 7-7M19 19l-7-7 7-7" />
            </svg>
          </button>
        </div>
        <nav class="sidebar-nav" v-if="!isSidebarHidden">
          <router-link
            v-for="item in navItems"
            :key="item.label"
            :to="item.path"
            :class="['sidebar-item', { active: route.path === item.path }]"
            :title="isSidebarCollapsed ? item.label : ''"
          >
            <span class="bullet" v-if="!isSidebarCollapsed">•</span>
            <span class="item-label" v-if="!isSidebarCollapsed">{{ item.label }}</span>
            <span class="item-icon-only" v-else>{{ item.label[0] }}</span>
          </router-link>
        </nav>
      </aside>

      <!-- 메인 콘텐츠 -->
      <main class="main-content">
        <slot />
      </main>

    </div>

    <!-- ===== 하단 푸터 ===== -->
    <div class="footer-trigger"></div>
    <footer class="app-footer">

      <!-- 본문 행 -->
      <div class="footer-body">

        <!-- 좌측: 로고 + 주소 -->
        <div class="footer-left">
          <div class="footer-logo">
            <img src="/logos/logo.png" alt="GoldenTime" class="footer-logo-img" />
          </div>
          <div class="footer-info">
            <p>인천 부평구 광장로 16 부평역사쇼핑몰 6층 &nbsp;/&nbsp; 인천 부평구 부평동 738-21</p>
            <p>우편번호: 21404 &nbsp;/&nbsp; 전화: 0507-1355-6224</p>
            <p class="copyright">Copyright© 2026 GoldenTime All Rights Reserved.</p>
          </div>
        </div>

        <!-- 우측: 파트너 로고 -->
        <div class="footer-partners">
          <img src="/logos/logo-moel.png" alt="고용노동부" class="partner-logo" />
          <img src="/logos/logo-straffic.png" alt="STraffic" class="partner-logo" />
          <img src="/logos/logo-its.png" alt="ITS KOREA" class="partner-logo" />
          <img src="/logos/logo-mbc.png" alt="MBC아카데미 컴퓨터교육센터" class="partner-logo" />
        </div>

      </div>
    </footer>

  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

/* ===== 헤더 ===== */
.top-header {
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-top {
  display: flex;
  align-items: center;
  padding: 0 28px;
  height: 84px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 0 #e5e7eb, 0 2px 10px rgba(0, 0, 0, 0.05);
}

/* 로고 */
.logo {
  display: flex;
  align-items: center;
  cursor: pointer;
  flex-shrink: 0;
  margin-right: 24px;
  padding-right: 24px;
  border-right: 1px solid #e5e7eb;
  height: 40px;
}

/* 헤더 복구 버튼 */
.btn-toggle-restore {
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 8px;
  margin-right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.btn-toggle-restore:hover {
  background: #f3f4f6;
  color: #111827;
}

.logo-img {
  height: 38px;
  width: auto;
  object-fit: contain;
}

/* 인사말 */
.header-greeting {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.greeting-text {
  font-size: 0.875rem;
  color: #4b5563;
  letter-spacing: 0;
}

.greeting-text strong {
  color: #111827;
  font-weight: 700;
}

.header-spacer { flex: 1; }

/* 역할 배지 */
.role-badge {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 2px;
  flex-shrink: 0;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.badge-admin {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fde68a;
}

.badge-user {
  background: #dbeafe;
  color: #1e40af;
  border: 1px solid #bfdbfe;
}

/* 로그아웃 버튼 */
.btn-logout {
  padding: 6px 16px;
  background: transparent;
  color: #374151;
  border: 1px solid #d1d5db;
  border-radius: 0;
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  font-family: inherit;
  flex-shrink: 0;
}

.btn-logout:hover {
  background: #0d2137;
  color: #fff;
  border-color: #0d2137;
}


/* ===== 바디 ===== */
.body-area {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* ── 다크 네이비 사이드바 ── */
.sidebar {
  width: 196px;
  flex-shrink: 0;
  background: #0d2137;
  border-right: none;
  box-shadow: 4px 0 16px rgba(0, 0, 0, 0.20);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 20px 0 16px;
  position: relative;
  z-index: 10;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar.hidden {
  width: 0;
  padding: 0;
  overflow: hidden;
  box-shadow: none;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  margin-bottom: 6px;
}

.sidebar.collapsed .sidebar-header {
  justify-content: center;
  padding: 0 0 10px;
}

.btn-sidebar-toggle {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.btn-sidebar-toggle:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.nav-header {
  font-size: 0.66rem;
  color: rgba(255, 255, 255, 0.28);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin: 0;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 8px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.58);
  text-decoration: none;
  transition: background 0.15s, color 0.15s, border-left-color 0.15s;
  border-left: 3px solid transparent;
}

.sidebar.collapsed .sidebar-item {
  padding: 10px 0;
  justify-content: center;
  border-left: none;
}

.sidebar-item:hover {
  background: rgba(255, 255, 255, 0.07);
  color: rgba(255, 255, 255, 0.92);
  border-left-color: rgba(96, 165, 250, 0.4);
}

.sidebar-item.active {
  color: #ffffff;
  font-weight: 700;
  background: rgba(30, 107, 175, 0.30);
  border-left-color: #60a5fa;
}

.sidebar.collapsed .sidebar-item.active {
  background: rgba(30, 107, 175, 0.45);
  border-left-color: transparent;
}

.bullet {
  font-size: 0.55rem;
  color: rgba(255, 255, 255, 0.22);
  flex-shrink: 0;
  transition: color 0.15s;
}

.item-icon-only {
  font-weight: 700;
  font-size: 1rem;
}

.sidebar-item:hover .bullet {
  color: rgba(96, 165, 250, 0.6);
}

.sidebar-item.active .bullet {
  color: #60a5fa;
}

/* ===== 메인 콘텐츠 ===== */
.main-content {
  flex: 1;
  overflow-y: auto;
  position: relative;
  background-color: #f8fafc;
}

/* ===== 푸터 ===== */
.app-footer {
  background: #f8f9fa;
  border-top: 1px solid #e0e0e0;
  flex-shrink: 0;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  transform: translateY(100%);
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.1);
}

/* 푸터 트리거 영역 (마우스를 가까이 대면 나타나게 하기 위함) */
.footer-trigger {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 20px;
  z-index: 999;
}

.app-layout:has(.app-footer:hover) .app-footer,
.footer-trigger:hover + .app-footer,
.app-footer:hover {
  transform: translateY(0);
}

/* 본문 행 */
.footer-body {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px 28px;
  gap: 24px;
}

/* 좌측 로고+주소 */
.footer-left {
  display: flex;
  align-items: flex-start;
  gap: 18px;
}

.footer-logo { flex-shrink: 0; }

.footer-logo-img {
  height: 44px;
  width: auto;
  object-fit: contain;
}

.footer-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.footer-info p {
  font-size: 0.77rem;
  color: #666;
  margin: 0;
  line-height: 1.55;
}

.copyright {
  color: #aaa !important;
  font-size: 0.73rem !important;
  margin-top: 2px;
}

/* 파트너 */
.footer-partners {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-shrink: 0;
}

.partner-logo {
  height: 34px;
  width: auto;
  object-fit: contain;
  opacity: 0.7;
  mix-blend-mode: multiply;
  transition: opacity 0.2s;
}

.partner-logo:hover {
  opacity: 1;
}

@media (max-width: 768px) {
  .header-top {
    padding: 0 16px;
    height: 62px;
  }

  .greeting-text {
    display: none;
  }

  .logo {
    margin-right: 12px;
    padding-right: 12px;
  }

  .sidebar {
    position: fixed;
    top: 62px;
    left: 0;
    /* 모바일에서는 하단 푸터(고정) 높이만큼만 여유 공간을 남김 */
    height: calc(100vh - 62px - 160px);
    z-index: 2000;
  }

  .sidebar.hidden {
    left: 0;
    width: 0;
  }

  .body-area {
    /* header+footer(고정) 영향으로 가로 스크롤/잘림 방지 */
    overflow: hidden;
  }

  .footer-trigger {
    display: none;
  }

  .app-footer {
    /* 모바일에서는 hover로 footer를 꺼내기 어렵기 때문에 기본 표시 */
    transform: translateY(0);
    position: fixed;
  }

  .main-content {
    /* 고정 푸터가 본문을 덮지 않도록 하단 여백 확보 */
    padding-bottom: 160px;
  }

  /* 모바일 푸터: 내용 줄이기 */
  .footer-body {
    flex-direction: column;
    gap: 12px;
    padding: 16px !important;
  }

  .footer-partners {
    display: none;
  }

  .footer-info p {
    font-size: 0.72rem;
    line-height: 1.45;
  }

  /* 주소 1줄 + 저작권만 남기기(2번째 p: 우편/전화 줄 숨김) */
  .footer-info p:nth-child(2) {
    display: none;
  }

  .copyright {
    font-size: 0.66rem !important;
  }
}
</style>
