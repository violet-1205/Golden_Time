<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useData } from '../store/data'
import { useAuth } from '../store/auth'
import { VueDraggableNext as Draggable } from 'vue-draggable-next'

const { events, fetchEvents, stats, fetchStats, recentEvents, fetchRecentEvents, eventsByRegion, fetchEventsByRegion } = useData()
const { users, fetchUsers, isAdmin } = useAuth()

onMounted(async () => {
  await fetchEvents()
  await fetchStats()
  await fetchRecentEvents()
  await fetchEventsByRegion()
  if (isAdmin.value) {
    await fetchUsers()
  }
})

const totalEvents = computed(() => events.value.length)
const unsentCount = computed(() => events.value.filter((ev) => !ev.sentToFire && !ev.sentToSafety).length)

// 자원 활용도 계산 (기기번호 기준)
const totalDevices = computed(() => {
  if (!isAdmin.value) return 0
  return users.value.reduce((acc, user) => acc + (user.vehicles ? user.vehicles.length : 0), 0)
})

const usedDevices = computed(() => {
  const uniqueSerials = new Set(events.value.map((ev) => ev.serialNumber).filter((s) => s))
  return uniqueSerials.size
})

const resourcePercent = computed(() => {
  if (totalDevices.value === 0) return 0
  return Math.round((usedDevices.value / totalDevices.value) * 100)
})

// 도넛 차트: r=38, 둘레 = 2π×38 ≈ 238.8
const donutArc = computed(() => resourcePercent.value * 2.388)

// 번호판 인식 정확도 (평균 confidence)
const averageAccuracy = computed(() => {
  const val = stats.value?.averageConfidence || 0
  return (val * 100).toFixed(1)
})

// 오늘의 통계 데이터 (+ 미전송)
const todayTotal = computed(() => stats.value?.totalEventsToday || 0)
const todayFire = computed(() => stats.value?.sentToFireToday || 0)
const todaySafety = computed(() => stats.value?.sentToSafetyToday || 0)
const todayUnsent = computed(() => {
  const total = Number(todayTotal.value) || 0
  const fire = Number(todayFire.value) || 0
  const safety = Number(todaySafety.value) || 0
  const v = total - fire - safety
  return v > 0 ? v : 0
})

const topRegions = computed(() => {
  const source = eventsByRegion.value || {}
  return Object.entries(source)
    .map(([name, count]) => ({ name, count: Number(count) || 0 }))
    .filter((item) => item.count > 0)
    .sort((a, b) => b.count - a.count)
    .slice(0, 6)
})

const maxRegionCount = computed(() => (topRegions.value.length ? topRegions.value[0].count : 1))

// ========= 대시보드 배치 변경(드래그) =========
const DASH_LAYOUT_KEY = 'goldentime.dashboard.main.layout.v2'

const defaultLayout = [
  { id: 'todayTotal', colSpan: 1, rowSpan: 1 },
  { id: 'todayFire', colSpan: 1, rowSpan: 1 },
  { id: 'todaySafety', colSpan: 1, rowSpan: 1 },
  { id: 'todayUnsent', colSpan: 1, rowSpan: 1 },
  { id: 'region', colSpan: 1, rowSpan: 2 },
  { id: 'realtime', colSpan: 1, rowSpan: 2 },
  { id: 'resource', colSpan: 1, rowSpan: 2 },
  { id: 'accuracy', colSpan: 1, rowSpan: 2 },
  { id: 'recent', colSpan: 4, rowSpan: 2 },
]

// (요청 기반) 허용 크기: 기본은 “고정”처럼 동작하지만, 원하면 나중에 옵션을 늘리기 쉬움
const allowedSizesById = {
  todayTotal: [{ colSpan: 1, rowSpan: 1 }],
  todayFire: [{ colSpan: 1, rowSpan: 1 }],
  todaySafety: [{ colSpan: 1, rowSpan: 1 }],
  todayUnsent: [{ colSpan: 1, rowSpan: 1 }],

  region: [{ colSpan: 1, rowSpan: 2 }],
  realtime: [{ colSpan: 1, rowSpan: 2 }],
  resource: [{ colSpan: 1, rowSpan: 2 }],
  accuracy: [{ colSpan: 1, rowSpan: 2 }],

  recent: [{ colSpan: 4, rowSpan: 2 }],
}

function normalizeLayout(raw) {
  const byId = new Map(defaultLayout.map((x) => [x.id, x]))
  const rawArr = Array.isArray(raw) ? raw : []
  const normalizedRaw = rawArr
    .map((x) => {
      if (typeof x === 'string') return { id: x }
      if (!x || typeof x !== 'object') return null
      if (!('id' in x)) return null
      return x
    })
    .filter(Boolean)

  const ids = normalizedRaw.map((x) => x.id).filter((id) => byId.has(id))
  const unique = [...new Set(ids)]
  const missing = defaultLayout.map((x) => x.id).filter((id) => !unique.includes(id))

  const overridesById = new Map(normalizedRaw.map((x) => [x.id, x]))
  const merged = [...unique, ...missing].map((id) => {
    const base = byId.get(id)
    const o = overridesById.get(id) || {}
    const candColSpan = Number.isInteger(o.colSpan) && o.colSpan >= 1 ? o.colSpan : base.colSpan
    const candRowSpan = Number.isInteger(o.rowSpan) && o.rowSpan >= 1 ? o.rowSpan : base.rowSpan

    const allowed = allowedSizesById[id]
    if (!allowed) return { ...base, colSpan: candColSpan, rowSpan: candRowSpan }
    const isAllowed = allowed.some((x) => x.colSpan === candColSpan && x.rowSpan === candRowSpan)
    if (!isAllowed) return base
    return { ...base, colSpan: candColSpan, rowSpan: candRowSpan }
  })

  // 최신 신고는 항상 마지막에 둬서(레이아웃 깨짐 방지) 기본 흐름 유지
  const recentIdx = merged.findIndex((x) => x.id === 'recent')
  if (recentIdx >= 0 && recentIdx !== merged.length - 1) {
    const [r] = merged.splice(recentIdx, 1)
    merged.push(r)
  }

  return merged
}

function loadLayout() {
  try {
    const raw = JSON.parse(localStorage.getItem(DASH_LAYOUT_KEY) || 'null')
    return normalizeLayout(raw)
  } catch {
    return normalizeLayout(null)
  }
}

const dashboardLayout = ref(loadLayout())
const layoutEditMode = ref(false)

watch(
  dashboardLayout,
  (val) => {
    try {
      localStorage.setItem(
        DASH_LAYOUT_KEY,
        JSON.stringify(val.map((x) => ({ id: x.id, colSpan: x.colSpan, rowSpan: x.rowSpan }))),
      )
    } catch {
      // ignore
    }
  },
  { deep: true },
)

function resetLayout() {
  dashboardLayout.value = normalizeLayout(null)
  try {
    localStorage.removeItem(DASH_LAYOUT_KEY)
  } catch {
    // ignore
  }
}

function gridStyle(item) {
  return {
    gridColumn: `span ${item.colSpan}`,
    gridRow: `span ${item.rowSpan}`,
  }
}

function updateCardSize(id, colSpan, rowSpan) {
  dashboardLayout.value = dashboardLayout.value.map((x) => {
    if (x.id !== id) return x
    return { ...x, colSpan, rowSpan }
  })
}

function sizePresetsFor(id) {
  const allowed = allowedSizesById[id] || []
  return allowed.length
    ? allowed.map((x) => ({ label: `${x.colSpan}x${x.rowSpan}`, colSpan: x.colSpan, rowSpan: x.rowSpan }))
    : [{ label: '1x1', colSpan: 1, rowSpan: 1 }]
}
</script>

<template>
  <div class="main-view">
    <div class="panel">
      <div class="page-header">
        <div>
          <h2 class="page-title">대시보드</h2>
          <p class="page-subtitle">시스템의 실시간 활성 사건 및 자원 활용도를 파악하세요.</p>
        </div>
        <div class="header-actions">
          <button class="btn-secondary" @click="layoutEditMode = !layoutEditMode">
            {{ layoutEditMode ? '배치 변경 종료' : '배치 변경' }}
          </button>
          <button class="btn-secondary" :disabled="!layoutEditMode" @click="resetLayout">초기화</button>
        </div>
      </div>

      <div class="content-body">
        <Draggable
          :list="dashboardLayout"
          item-key="id"
          :disabled="false"
          handle=".drag-handle"
          class="dashboard-main-grid"
          ghost-class="drag-ghost"
          chosen-class="drag-chosen"
          drag-class="drag-dragging"
        >
          <div
            v-for="element in dashboardLayout"
            :key="element.id"
            class="dash-item"
            :class="{ 'is-editing': layoutEditMode }"
            :style="gridStyle(element)"
          >
            <button v-if="layoutEditMode" class="drag-handle" type="button" title="드래그로 위치 변경">⋮⋮</button>
            <div v-if="layoutEditMode" class="size-controls">
              <button
                v-for="opt in sizePresetsFor(element.id)"
                :key="opt.label"
                type="button"
                class="size-btn"
                :class="{ active: element.colSpan === opt.colSpan && element.rowSpan === opt.rowSpan }"
                @click="updateCardSize(element.id, opt.colSpan, opt.rowSpan)"
              >
                {{ opt.label }}
              </button>
            </div>

            <template v-if="element.id === 'todayTotal'">
              <div class="summary-card total">
                <div class="summary-icon">🚨</div>
                <div class="summary-info">
                  <span class="summary-label">오늘의 탐지</span>
                  <span class="summary-value">{{ todayTotal }}건</span>
                </div>
              </div>
            </template>

            <template v-else-if="element.id === 'todayFire'">
              <div class="summary-card fire">
                <div class="summary-icon">🚒</div>
                <div class="summary-info">
                  <span class="summary-label">소방청 전송</span>
                  <span class="summary-value">{{ todayFire }}건</span>
                </div>
              </div>
            </template>

            <template v-else-if="element.id === 'todaySafety'">
              <div class="summary-card safety">
                <div class="summary-icon">🛡️</div>
                <div class="summary-info">
                  <span class="summary-label">안전신문고 전송</span>
                  <span class="summary-value">{{ todaySafety }}건</span>
                </div>
              </div>
            </template>

            <template v-else-if="element.id === 'todayUnsent'">
              <div class="summary-card pending-today">
                <div class="summary-icon">⏳</div>
                <div class="summary-info">
                  <span class="summary-label">오늘 미전송</span>
                  <span class="summary-value">{{ todayUnsent }}건</span>
                </div>
              </div>
            </template>

            <template v-else-if="element.id === 'region'">
              <section class="stat-card region-card" aria-label="지역별 사건 발생 현황">
                <div class="region-card-head">
                  <p class="card-label">지역별 사건 발생 현황</p>
                </div>
                <div class="region-card-body">
                  <div v-if="topRegions.length === 0" class="region-empty">
                    집계된 지역 데이터가 없습니다.
                  </div>
                  <ul v-else class="region-list">
                    <li v-for="(region, idx) in topRegions" :key="region.name" class="region-item">
                      <div class="region-meta">
                        <span class="region-rank">{{ idx + 1 }}</span>
                        <span class="region-name">{{ region.name }}</span>
                        <span class="region-count">{{ region.count }}건</span>
                      </div>
                      <div class="region-bar-bg">
                        <div
                          class="region-bar-fill"
                          :style="{ width: `${(region.count / maxRegionCount) * 100}%` }"
                        ></div>
                      </div>
                    </li>
                  </ul>
                </div>
                <p class="card-desc">상위 지역 기준 실시간 발생 분포</p>
              </section>
            </template>

            <template v-else-if="element.id === 'realtime'">
              <div class="stat-card realtime-card">
                <p class="card-label">실시간 활성 사건</p>
                <div class="big-value">
                  {{ totalEvents }}건
                  <span class="change up">
                    <span class="dot red"></span> 미전송 {{ unsentCount }}건
                  </span>
                </div>
                <p class="card-desc">현재 접수된 전체 신고 사건 현황</p>
              </div>
            </template>

            <template v-else-if="element.id === 'resource'">
              <div class="stat-card resource-card">
                <p class="card-label">자원 활용도 (기기 가동률)</p>
                <div class="donut-wrap">
                  <svg viewBox="0 0 100 100" class="donut-svg">
                    <circle class="donut-track" cx="50" cy="50" r="38" />
                    <circle
                      class="donut-arc"
                      cx="50" cy="50" r="38"
                      transform="rotate(-90 50 50)"
                      :stroke-dasharray="`${donutArc} 238.8`"
                    />
                  </svg>
                  <div class="donut-center">
                    <span class="donut-val">{{ resourcePercent }}%</span>
                    <span class="donut-sub">{{ usedDevices }}/{{ totalDevices }} 유닛</span>
                  </div>
                </div>
                <p class="card-desc">전체 등록 기기 중 이벤트 발생 기기 비율</p>
              </div>
            </template>

            <template v-else-if="element.id === 'accuracy'">
              <div class="stat-card accuracy-card">
                <p class="card-label">번호판 인식 모델 정확도</p>
                <div class="accuracy-content">
                  <div class="accuracy-value">
                    <span class="val-num">{{ averageAccuracy }}</span>
                    <span class="val-unit">%</span>
                  </div>
                  <div class="accuracy-bar-bg">
                    <div class="accuracy-bar-fill" :style="{ width: averageAccuracy + '%' }"></div>
                  </div>
                </div>
                <p class="card-desc">OCR 분석 결과의 평균 신뢰도 지수</p>
              </div>
            </template>

            <template v-else-if="element.id === 'recent'">
              <div class="recent-events-card">
                <p class="card-label">최신 신고 목록 (5건)</p>
                <table class="events-table">
                  <thead>
                    <tr>
                      <th>사용자 차량 번호</th>
                      <th>인식된 번호판</th>
                      <th>발생 시각</th>
                      <th>신고 상태</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="event in recentEvents" :key="event.gtId">
                      <td data-label="사용자 차량 번호">{{ event.carNumber }}</td>
                      <td data-label="인식된 번호판">{{ event.detectedPlate || '-' }}</td>
                      <td data-label="발생 시각">{{ new Date(event.createdAt).toLocaleString() }}</td>
                      <td data-label="신고 상태">
                        <span v-if="event.sentToFire" class="status-tag fire">소방청</span>
                        <span v-else-if="event.sentToSafety" class="status-tag safety">안전신문고</span>
                        <span v-else class="status-tag pending">미전송</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </template>
          </div>
        </Draggable>
      </div>
    </div>
  </div>
</template>

<style scoped>
.main-view {
  padding: 24px;
  background: #f8fafc;
  min-height: 100%;
}

.panel {
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.page-header {
  padding: 24px 28px;
  border-bottom: 1px solid #edf2f7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.btn-secondary {
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #e2e8f0;
  font-size: 0.86rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  font-family: inherit;
  white-space: nowrap;
}
.btn-secondary:hover:not(:disabled) {
  background: #f8fafc;
}
.btn-secondary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.page-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1a202c;
  margin-bottom: 4px;
}

.page-subtitle {
  font-size: 0.88rem;
  color: #718096;
}

.content-body {
  padding: 28px;
}

.dashboard-main-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-auto-rows: 110px;
  grid-auto-flow: row dense;
  gap: 20px;
  margin-bottom: 24px;
}

.dash-item {
  position: relative;
  min-width: 0;
  min-height: 0;
}

.dash-item.is-editing {
  outline: 2px dashed rgba(59, 130, 246, 0.65);
  outline-offset: 3px;
}

.drag-handle {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 5;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: rgba(255, 255, 255, 0.95);
  color: #64748b;
  font-weight: 900;
  cursor: grab;
  line-height: 1;
}
.drag-handle:active {
  cursor: grabbing;
}

.size-controls {
  position: absolute;
  top: 8px;
  right: 46px;
  z-index: 6;
  display: flex;
  gap: 6px;
  padding: 6px 6px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.size-btn {
  padding: 4px 6px;
  font-size: 0.72rem;
  font-weight: 800;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #475569;
  cursor: pointer;
  border-radius: 6px;
  white-space: nowrap;
}
.size-btn:hover {
  background: #f8fafc;
}
.size-btn.active {
  background: #1a73e8;
  border-color: #1a73e8;
  color: #fff;
}

.grid-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
}

.summary-card {
  background: var(--card-bg);
  padding: 20px;
  border: 1px solid var(--card-border);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.2s ease;
  height: 100%;
  box-sizing: border-box;
}

.summary-icon {
  font-size: 28px;
  width: 52px;
  height: 52px;
  background: var(--surface-2);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-solid);
}

.summary-info {
  display: flex;
  flex-direction: column;
}

.summary-label {
  font-size: 0.875rem;
  color: var(--text-muted);
  font-weight: 500;
}

.summary-value {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-h);
}

/* 최신 신고 목록 */
.recent-events-card {
  background: var(--card-bg);
  padding: 20px;
  border: 1px solid var(--card-border);
  margin-top: 0;
  height: 100%;
  box-sizing: border-box;
}

.events-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 12px;
}

.events-table th, .events-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.9rem;
}

.events-table th {
  color: #64748b;
  font-weight: 600;
  background: #f8fafc;
}

.status-tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}
.status-tag.fire { background: #fee2e2; color: #ef4444; }
.status-tag.safety { background: #d1fae5; color: #10b981; }
.status-tag.pending { background: #f1f5f9; color: #64748b; }

.stat-card {
  background: #fff;
  padding: 20px;
  border: 1px solid #eef2f7;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: all 0.2s ease;
  min-height: 0; /* flex 자식의 overflow 방지 */
  box-sizing: border-box;
  overflow: hidden;
  height: 100%;
}

.region-card {
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.region-card-head {
  flex-shrink: 0;
}

.region-card-body {
  flex: 1;
  min-height: 0;
}

.region-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 0.85rem;
}

.region-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.region-item {
  padding: 10px 12px;
  border: 1px solid #edf2f7;
  background: #f8fafc;
}

.region-meta {
  display: grid;
  grid-template-columns: 20px 1fr auto;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.region-rank {
  font-size: 0.78rem;
  font-weight: 800;
  color: #475569;
}

.region-name {
  font-size: 0.85rem;
  font-weight: 700;
  color: #1e293b;
}

.region-count {
  font-size: 0.8rem;
  color: #475569;
  font-weight: 700;
}

.region-bar-bg {
  width: 100%;
  height: 8px;
  background: #e2e8f0;
  overflow: hidden;
}

.region-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #2563eb, #60a5fa);
}

.card-label {
  font-size: 0.82rem;
  color: #64748b;
  font-weight: 600;
  margin-bottom: 12px;
}

.status-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.status-dot.green { background: #10b981; }

.status-text {
  font-size: 1.1rem;
  font-weight: 700;
  color: #1e293b;
}

.big-value {
  font-size: 1.5rem;
  font-weight: 800;
  color: #1e293b;
  display: flex; /* correct line */
  align-items: baseline;
  gap: 8px;
}

.change {
  font-size: 0.78rem;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
}
.change.up { color: #ef4444; }
.change.down { color: #3b82f6; }

.accuracy-content {
  margin: 10px 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.accuracy-value {
  margin-bottom: 12px;
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.accuracy-value .val-num {
  font-size: 2rem;
  font-weight: 800;
  color: #1e293b;
}

.accuracy-value .val-unit {
  font-size: 1rem;
  font-weight: 600;
  color: #64748b;
}

.accuracy-bar-bg {
  width: 100%;
  height: 8px;
  background: #f1f5f9;
  border-radius: 4px;
  overflow: hidden;
}

.accuracy-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
  border-radius: 4px;
  transition: width 0.6s ease-out;
}

.card-desc {
  font-size: 0.72rem;
  color: #94a3b8;
  margin-top: 8px;
}

/* 자원 활용도 */
.row-2 {
  grid-template-columns: 1fr 1fr 1.5fr;
}

.donut-wrap {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto;
}

.donut-track {
  fill: none;
  stroke: #f1f5f9;
  stroke-width: 8;
}

.donut-arc {
  fill: none;
  stroke: #1976d2;
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dasharray 0.5s ease;
}

.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.summary-icon {
  font-size: 28px;
  width: 52px;
  height: 52px;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #f1f5f9;
}

.summary-info {
  display: flex;
  flex-direction: column;
}

.summary-label {
  font-size: 0.875rem;
  color: #64748b;
  font-weight: 500;
}

.summary-value {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1e293b;
}

.donut-val {
  font-size: 1.4rem;
  font-weight: 800;
  color: #1e293b;
}

.donut-sub {
  font-size: 0.68rem;
  color: #64748b;
}

/* 바 차트 */
.bar-chart-area {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 40px;
  height: 140px;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.bar-col {
  width: 40px;
  border-radius: 4px 4px 0 0;
}
.bar-col.success { background: #10b981; }
.bar-col.fail { background: #ef4444; }

.bar-label {
  font-size: 0.75rem;
  color: #64748b;
  margin-top: 8px;
}

.bar-val {
  font-size: 0.88rem;
  font-weight: 700;
  color: #1e293b;
}

/* 핫스팟 */
.hotspot-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hotspot-item {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  background: #f8fafc;
  border-radius: 4px;
  font-size: 0.84rem;
}

.rank {
  font-weight: 800;
  color: #64748b;
  width: 24px;
}

.area {
  flex: 1;
  font-weight: 600;
  color: #1e293b;
}

.count {
  margin: 0 12px;
  color: #1976d2;
  font-weight: 700;
}

.level {
  font-size: 0.72rem;
  padding: 2px 8px;
  background: #e2e8f0;
  color: #475569;
  border-radius: 10px;
}
.level.high {
  background: #fee2e2;
  color: #b91c1c;
}

/* 전국 지도 */
.map-row {
  margin-top: 24px;
}

.map-card {
  padding: 20px;
}

.korea-map-wrap {
  width: 100%;
  max-width: 400px;
  margin: 0 auto;
  display: flex;
  justify-content: center;
}

.korea-svg {
  width: 100%;
  height: auto;
}

.korea-land {
  fill: #f1f5f9;
  stroke: #cbd5e1;
  stroke-width: 1.5;
}

.map-label {
  font-size: 12px;
  font-weight: 800;
  fill: #1e293b;
}

.map-count {
  font-size: 11px;
  font-weight: 600;
  fill: #dc2626;
}

.map-label-sm {
  font-size: 10px;
  font-weight: 600;
  fill: #64748b;
}

/* ===== 다크모드 전용 보정 (대시보드 전역) ===== */
:global(html[data-theme='dark']) .summary-card,
:global(html[data-theme='dark']) .recent-events-card,
:global(html[data-theme='dark']) .stat-card {
  background: #0f1b2d;
  border-color: rgba(255, 255, 255, 0.14);
}

:global(html[data-theme='dark']) .summary-icon {
  background: #0b1626;
  border-color: rgba(255, 255, 255, 0.12);
}

:global(html[data-theme='dark']) .summary-label,
:global(html[data-theme='dark']) .card-label,
:global(html[data-theme='dark']) .card-desc,
:global(html[data-theme='dark']) .region-name,
:global(html[data-theme='dark']) .region-count,
:global(html[data-theme='dark']) .region-rank,
:global(html[data-theme='dark']) .donut-sub,
:global(html[data-theme='dark']) .bar-label,
:global(html[data-theme='dark']) .rank {
  color: #cbd5e1;
}

:global(html[data-theme='dark']) .summary-value,
:global(html[data-theme='dark']) .big-value,
:global(html[data-theme='dark']) .donut-val,
:global(html[data-theme='dark']) .accuracy-value .val-num,
:global(html[data-theme='dark']) .area,
:global(html[data-theme='dark']) .bar-val {
  color: #f8fafc;
}

:global(html[data-theme='dark']) .events-table th {
  background: #111827;
  color: #cbd5e1;
  border-bottom-color: rgba(255, 255, 255, 0.14);
}

:global(html[data-theme='dark']) .events-table td {
  color: #e5e7eb;
  border-bottom-color: rgba(255, 255, 255, 0.12);
}

:global(html[data-theme='dark']) .status-tag.pending {
  background: #1f2937;
  color: #d1d5db;
}

:global(html[data-theme='dark']) .status-tag.fire {
  background: rgba(239, 68, 68, 0.18);
  color: #fca5a5;
}

:global(html[data-theme='dark']) .status-tag.safety {
  background: rgba(16, 185, 129, 0.18);
  color: #6ee7b7;
}

:global(html[data-theme='dark']) .donut-track {
  stroke: #1f2937;
}

:global(html[data-theme='dark']) .region-bar-bg,
:global(html[data-theme='dark']) .accuracy-bar-bg {
  background: #1f2937;
}

@media (max-width: 768px) {
  .main-view {
    padding: 14px;
  }

  .content-body {
    padding: 16px;
  }

  .page-header {
    padding: 18px 16px;
  }

  .dashboard-main-grid {
    gap: 14px;
    grid-auto-rows: 95px;
    margin-bottom: 16px;
  }

  .summary-card,
  .recent-events-card,
  .stat-card,
  .region-card {
    padding: 14px;
  }

  .card-label {
    font-size: 0.78rem;
    margin-bottom: 8px;
  }

  .events-table {
    margin-top: 8px;
  }

  .events-table th,
  .events-table td {
    padding: 8px 10px;
    font-size: 0.78rem;
    word-break: break-word;
    overflow-wrap: anywhere;
  }

  .events-table th {
    white-space: nowrap;
  }

  /* 모바일: 테이블을 카드형(행 단위 세로 표시)으로 전환 */
  .events-table thead {
    display: none;
  }

  .events-table,
  .events-table tbody,
  .events-table tr,
  .events-table td {
    display: block;
    width: 100%;
  }

  .events-table tr {
    border-bottom: 1px solid #f1f5f9;
    padding: 8px 0;
  }

  .events-table td {
    border-bottom: none;
    padding: 2px 0 !important;
  }

  .events-table td::before {
    content: attr(data-label);
    display: block;
    font-weight: 700;
    color: #64748b;
    font-size: 0.74rem;
    margin-bottom: 4px;
    word-break: break-word;
  }

  .status-tag {
    padding: 3px 6px;
    font-size: 0.7rem;
  }

  .drag-handle {
    top: 6px;
    right: 6px;
    width: 22px;
    height: 22px;
    font-size: 0.75rem;
  }

  .size-controls {
    top: 6px;
    right: 38px;
    padding: 4px 5px;
    gap: 5px;
  }

  .size-btn {
    padding: 3px 5px;
    font-size: 0.68rem;
  }
}

@media (max-width: 480px) {
  .main-view {
    padding: 10px;
  }

  .dashboard-main-grid {
    gap: 12px;
    grid-auto-rows: 88px;
  }

  .events-table th,
  .events-table td {
    padding: 7px 8px;
    font-size: 0.74rem;
  }
}
</style>
