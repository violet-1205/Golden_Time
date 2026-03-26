<script setup>
import { ref, onMounted, watch } from 'vue'
import { useAuth } from '../store/auth'
import { useData } from '../store/data'

const { currentUser, isAdmin } = useAuth()
const { events, fetchEvents, addEvent, deleteEvent, sendEvent, updateEvent } = useData()

onMounted(async () => {
  await fetchEvents()
})

const showUploadModal = ref(false)
const showSendModal = ref(false)
const eventToSend = ref(null)

const uploadForm = ref({
  videoFile: null,
  videoName: '',
  vehicleId: '',
})

// 모달 열릴 때 기본 차량 설정
watch(showUploadModal, (newVal) => {
  if (newVal && currentUser.value?.vehicles?.length > 0) {
    uploadForm.value.vehicleId = currentUser.value.vehicles[0].vehicleId
  }
})

function handleVideoChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploadForm.value.videoName = file.name
  uploadForm.value.videoFile = file
}

async function handleUpload() {
  if (!uploadForm.value.videoFile || !uploadForm.value.vehicleId) return
  
  const res = await addEvent({
    videoFile: uploadForm.value.videoFile,
    vehicleId: uploadForm.value.vehicleId
  })
  
  if (res.success) {
    uploadForm.value = { videoFile: null, videoName: '', vehicleId: '' }
    showUploadModal.value = false
  }
}

// 상세보기 (기존 수정 기능 활용)
const showDetailModal = ref(false)
const selectedEvent = ref(null)

function openEdit(ev) {
  selectedEvent.value = ev
  showDetailModal.value = true
}

async function handleDelete(gtId) {
  if (!confirm('정말로 이 신고 기록을 삭제하시겠습니까?')) return
  
  const res = await deleteEvent(gtId)
  if (res.success) {
    alert('삭제되었습니다.')
  } else {
    alert('삭제에 실패했습니다.')
  }
}

function openSendModal(ev) {
  eventToSend.value = ev
  showSendModal.value = true
}

async function handleSend(target) {
  if (!eventToSend.value) return
  
  const targetName = target === 'fire' ? '소방청' : '안전신문고'
  if (!confirm(`${targetName}으로 이 신고 기록을 전송하시겠습니까?`)) return
  
  const res = await sendEvent(eventToSend.value.gtId, target)
  if (res.success) {
    alert(`${targetName}으로 전송되었습니다.`)
    showSendModal.value = false
    eventToSend.value = null
  } else {
    alert('전송에 실패했습니다.')
  }
}

function formatTimestamp(ts) {
  if (!ts) return '-'
  return new Date(ts).toLocaleString('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
    hour12: false
  }).replace(/\./g, '-').replace(/ /g, ' ').replace(/- /g, ' ');
}

</script>

<template>
  <div class="dashboard">
    <div class="panel">
      <div class="page-header">
        <div>
          <h2 class="page-title">신고 목록</h2>
          <p class="page-subtitle">차량 주행 중 발생한 긴급 상황 및 신고 이벤트 목록입니다.</p>
        </div>
        <button v-if="!isAdmin" class="btn-dark" @click="showUploadModal = true">영상 업로드</button>
      </div>

      <table class="data-table">
        <thead>
          <tr>
            <th>사건번호</th>
            <th>기기번호</th>
            <th>사용자 차량 번호</th>
            <th>인식 번호판</th>
            <th>작업</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="events.length === 0">
            <td colspan="5" class="empty-msg">조회된 이벤트 데이터가 없습니다.</td>
          </tr>
          <tr v-for="ev in events" :key="ev.gtId">
            <td data-label="사건번호">{{ ev.gtId }}</td>
            <td data-label="기기번호">{{ ev.serialNumber || '-' }}</td>
            <td data-label="사용자 차량 번호">{{ ev.carNumber }}</td>
            <td data-label="인식 번호판">{{ ev.detectedPlate || '-' }}</td>
            <td class="td-actions" data-label="작업">
              <button class="btn-detail" @click="openEdit(ev)">상세보기</button>
              <button 
                :class="['btn-send', { 'sent-completed': ev.sentToFire || ev.sentToSafety }]" 
                @click="openSendModal(ev)"
                :disabled="ev.sentToFire || ev.sentToSafety"
              >
                {{ (ev.sentToFire || ev.sentToSafety) ? '전송완료' : '전송' }}
              </button>
              <button class="btn-delete" @click="handleDelete(ev.gtId)">삭제</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 상세보기 모달 -->
    <div v-if="showDetailModal" class="modal-overlay" @click.self="showDetailModal = false">
      <div class="modal-box detail-modal">
        <h3 class="modal-title">신고 상세 정보</h3>
        
        <div class="detail-container">
          <!-- 좌측: 영상 -->
          <div class="detail-left">
            <div class="video-container" v-if="selectedEvent?.videoPath">
              <video :src="selectedEvent?.videoPath" class="video-player" controls></video>
            </div>
            <div v-else class="no-video">
              <p>재생할 영상이 없습니다.</p>
            </div>
          </div>

          <!-- 우측: 정보 -->
          <div class="detail-right">
            <div class="detail-info-list">
              <div class="detail-item">
                <span class="label">사건번호</span>
                <span class="value">{{ selectedEvent?.gtId }}</span>
              </div>
              <div class="detail-item">
                <span class="label">기기번호</span>
                <span class="value">{{ selectedEvent?.serialNumber || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="label">사용자 차량 번호</span>
                <span class="value">{{ selectedEvent?.carNumber }}</span>
              </div>
              <div class="detail-item">
                <span class="label">인식 번호판</span>
                <span class="value">{{ selectedEvent?.detectedPlate || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="label">위치</span>
                <span class="value">{{ selectedEvent?.vtIdPath || '알 수 없음' }}</span>
              </div>
              <div class="detail-item">
                <span class="label">발생일시</span>
                <span class="value">{{ formatTimestamp(selectedEvent?.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn-dark" @click="showDetailModal = false">닫기</button>
        </div>
      </div>
    </div>

    <!-- 영상 업로드 모달 -->
    <div v-if="showUploadModal" class="modal-overlay" @click.self="showUploadModal = false">
      <div class="modal-box">
        <h3 class="modal-title">영상 업로드</h3>

        <div class="form-group">
          <p class="upload-guide">업로드할 영상 파일을 선택하고 관련 차량을 지정해주세요.</p>
          
          <label>차량 선택</label>
          <select v-model="uploadForm.vehicleId" class="form-select">
            <option v-for="v in currentUser?.vehicles" :key="v.vehicleId" :value="v.vehicleId">
              {{ v.carNumber }} ({{ v.serialNumber }})
            </option>
            <option v-if="!currentUser?.vehicles?.length" disabled value="">
              등록된 차량이 없습니다. 마이페이지에서 등록해주세요.
            </option>
          </select>
        </div>

        <div class="form-group">
          <label>영상 파일</label>
          <div class="file-row">
            <label class="btn-file">
              파일 선택
              <input type="file" accept="video/*" hidden @change="handleVideoChange" />
            </label>
            <span class="file-name">{{ uploadForm.videoName || '선택된 파일 없음' }}</span>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn-cancel" @click="showUploadModal = false">취소</button>
          <button class="btn-dark" @click="handleUpload" :disabled="!uploadForm.videoFile || !uploadForm.vehicleId">업로드</button>
        </div>
      </div>
    </div>

    <!-- 전송 대상 선택 모달 -->
    <div v-if="showSendModal" class="modal-overlay" @click.self="showSendModal = false">
      <div class="modal-box send-modal">
        <h3 class="modal-title">신고 전송</h3>
        <p class="send-guide">해당 신고 기록을 전송할 기관을 선택해주세요.</p>
        
        <div class="send-options">
          <button class="btn-send-option" @click="handleSend('fire')">
            <div class="option-icon">🚒</div>
            <div class="option-text">
              <span class="option-title">소방청</span>
              <span class="option-desc">긴급 화재 및 구조 신고</span>
            </div>
          </button>
          <button class="btn-send-option" @click="handleSend('safety')">
            <div class="option-icon">⚖️</div>
            <div class="option-text">
              <span class="option-title">안전신문고</span>
              <span class="option-desc">교통 법규 위반 및 안전 신고</span>
            </div>
          </button>
        </div>

        <div class="modal-actions">
          <button class="btn-cancel" @click="showSendModal = false">취소</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
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
  justify-content: space-between;
  align-items: center;
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

.btn-dark {
  padding: 10px 20px;
  background: #1a202c;
  color: #fff;
  border: none;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-dark:hover { background: #2d3748; }

/* 테이블 */
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.data-table th {
  text-align: left;
  padding: 14px 20px;
  background: #f8fafc;
  color: #4a5568;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
}

.data-table td {
  padding: 14px 20px;
  border-bottom: 1px solid #edf2f7;
  color: #2d3748;
}

.btn-detail {
  padding: 6px 14px;
  background: #edf2f7;
  color: #4a5568;
  border: 1px solid #e2e8f0;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-detail:hover {
  background: #e2e8f0;
  color: #1a202c;
}

.btn-send {
  padding: 6px 14px;
  background: #ebf8ff;
  color: #2b6cb0;
  border: 1px solid #bee3f8;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-send:hover {
  background: #bee3f8;
}

.btn-send.sent-completed {
  background: #f0fff4;
  color: #38a169;
  border: 1px solid #c6f6d5;
  cursor: default;
}

.btn-send.sent-completed:hover {
  background: #f0fff4;
}

.td-actions {
  display: flex;
  gap: 8px;
}

.btn-delete {
  padding: 6px 14px;
  background: #fff;
  color: #e53e3e;
  border: 1px solid #fed7d7;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-delete:hover {
  background: #fff5f5;
  border-color: #feb2b2;
}

.empty-msg {
  text-align: center;
  padding: 60px;
  color: #a0aec0;
}

/* 모달 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-box {
  background: #fff;
  padding: 32px;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1a202c;
  margin-bottom: 24px;
}

/* 상세보기 전용 모달 스타일 */
.detail-modal {
  max-width: 900px !important;
  width: 90% !important;
}

.detail-container {
  display: flex;
  gap: 32px;
  margin-bottom: 24px;
}

.detail-left {
  flex: 1.5; /* 영상 영역을 더 크게 */
}

.video-container {
  position: relative;
  width: 100%;
  padding-bottom: 75%; /* 4:3 Aspect Ratio */
  background: #000;
  overflow: hidden;
  border-radius: 4px;
}

.video-player {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.no-video {
  width: 100%;
  padding-bottom: 75%;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.detail-right {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.detail-info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 8px;
}

.detail-item .label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.detail-item .value {
  font-size: 1rem;
  font-weight: 600;
  color: #1e293b;
}

/* 전송 모달 스타일 */
.send-modal {
  max-width: 450px !important;
}

.send-guide {
  font-size: 0.9rem;
  color: #4a5568;
  margin-bottom: 20px;
}

.send-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.btn-send-option {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-send-option:hover {
  border-color: #3182ce;
  background: #f7fafc;
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}

.option-icon {
  font-size: 1.5rem;
}

.option-text {
  display: flex;
  flex-direction: column;
}

.option-title {
  font-size: 1rem;
  font-weight: 700;
  color: #2d3748;
}

.option-desc {
  font-size: 0.8rem;
  color: #718096;
}

.upload-guide {
  font-size: 0.88rem;
  color: #64748b;
  margin-bottom: 16px;
  line-height: 1.5;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.detail-row {
  display: flex;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 8px;
}

.detail-row .label {
  width: 100px;
  font-weight: 600;
  color: #64748b;
  font-size: 0.88rem;
}

.detail-row .value {
  flex: 1;
  color: #1e293b;
  font-size: 0.88rem;
}

.video-preview {
  width: 100%;
  margin-top: 10px;
  border: 1px solid #e2e8f0;
}

.form-group {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #4a5568;
}

.form-group input,
.form-group select {
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  outline: none;
  font-family: inherit;
  font-size: 0.88rem;
  background: #fff;
}

.form-select {
  cursor: pointer;
}

.file-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-file {
  padding: 8px 16px;
  background: #edf2f7;
  border: 1px solid #e2e8f0;
  font-size: 0.8rem;
  cursor: pointer;
}

.file-name {
  font-size: 0.8rem;
  color: #718096;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.btn-cancel {
  padding: 10px 20px;
  background: #fff;
  border: 1px solid #e2e8f0;
  font-weight: 600;
  cursor: pointer;
}

@media (max-width: 768px) {
  .page-header {
    padding: 16px;
  }

  .page-subtitle {
    display: none;
  }

  .data-table {
    table-layout: fixed;
    font-size: 0.78rem;
  }

  .data-table th,
  .data-table td {
    padding: 10px 10px;
    word-break: break-word;
    overflow-wrap: anywhere;
  }

  /* 모바일: 테이블을 카드형(행 단위 세로 표시)으로 전환 */
  .data-table thead {
    display: none;
  }

  .data-table,
  .data-table tbody,
  .data-table tr,
  .data-table td {
    display: block;
    width: 100%;
  }

  .data-table tr {
    border-bottom: 1px solid #f1f5f9;
    padding: 8px 0;
  }

  .data-table td {
    border-bottom: none;
    padding: 2px 0 !important;
  }

  .data-table td::before {
    content: attr(data-label);
    display: block;
    font-weight: 700;
    color: #64748b;
    font-size: 0.74rem;
    margin-bottom: 2px;
    word-break: break-word;
  }

  /* 모바일에서 번호/기기값은 monospace + 줄바꿈 억제로 더 또렷하게 */
  .data-table td[data-label="사건번호"],
  .data-table td[data-label="기기번호"] {
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono',
      'Courier New', monospace;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    word-break: normal;
    overflow-wrap: normal;
    line-height: 1.2;
  }

  .td-actions {
    flex-wrap: wrap;
    gap: 6px;
  }

  .btn-detail,
  .btn-send,
  .btn-delete {
    padding: 6px 10px;
    font-size: 0.76rem;
  }

  .detail-container {
    flex-direction: column;
    gap: 16px;
  }

  .detail-modal {
    width: 95% !important;
  }

  .modal-box {
    padding: 20px;
  }
}

@media (max-width: 480px) {
  .data-table {
    font-size: 0.72rem;
  }

  .data-table th,
  .data-table td {
    padding: 9px 8px;
  }

  .btn-detail,
  .btn-send,
  .btn-delete {
    padding: 6px 8px;
    font-size: 0.7rem;
  }
}
</style>
