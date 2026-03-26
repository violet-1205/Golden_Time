<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '../store/auth'
import { useData } from '../store/data'

const { isAdmin } = useAuth()
const { notices, addNotice, updateNotice, deleteNotice, fetchNotices, fetchNoticeDetail } = useData()

const viewMode = ref('list')
const selectedNotice = ref(null)
const isEditMode = ref(false)

const sortedNotices = computed(() =>
  [...notices.value].sort((a, b) => {
    if (a.important === b.important) return 0
    return a.important ? -1 : 1
  })
)

let previewObjectUrl = null

function revokePreview() {
  if (previewObjectUrl) {
    URL.revokeObjectURL(previewObjectUrl)
    previewObjectUrl = null
  }
}

onMounted(() => {
  fetchNotices()
})

async function openDetail(notice) {
  const detail = await fetchNoticeDetail(notice.id)
  selectedNotice.value = detail || notice
  isEditMode.value = false
  viewMode.value = 'detail'
}

const form = ref({ title: '', content: '', image: '', important: false })
const formMsg = ref('')
const imageFileName = ref('')
const pendingImageFile = ref(null)

function openForm(notice = null) {
  const validNotice = notice && typeof notice === 'object' && 'id' in notice
  isEditMode.value = !!validNotice
  revokePreview()
  pendingImageFile.value = null
  if (validNotice) {
    form.value = {
      title: notice.title,
      content: notice.content,
      image: notice.image || '',
      important: notice.important || false,
    }
    imageFileName.value = ''
  } else {
    form.value = { title: '', content: '', image: '', important: false }
    imageFileName.value = ''
  }
  formMsg.value = ''
  viewMode.value = 'form'
}

function handleImageChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  revokePreview()
  pendingImageFile.value = file
  imageFileName.value = file.name
  previewObjectUrl = URL.createObjectURL(file)
  form.value.image = previewObjectUrl
}

async function handleSubmit() {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    formMsg.value = '제목과 내용을 모두 입력하세요.'
    return
  }
  if (isEditMode.value && selectedNotice.value) {
    await updateNotice(selectedNotice.value.id, {
      title: form.value.title,
      content: form.value.content,
      important: form.value.important,
      imageFile: pendingImageFile.value,
    })
  } else {
    await addNotice({
      title: form.value.title,
      content: form.value.content,
      important: form.value.important,
      imageFile: pendingImageFile.value,
    })
  }
  revokePreview()
  pendingImageFile.value = null
  viewMode.value = 'list'
}

async function handleDelete(id) {
  if (confirm('공지사항을 삭제하시겠습니까?')) {
    await deleteNotice(id)
    viewMode.value = 'list'
  }
}
</script>

<template>
  <div class="notice">
    <!-- 목록 -->
    <template v-if="viewMode === 'list'">
      <div class="panel">
        <div class="page-header">
          <div>
            <h2 class="page-title">공지사항</h2>
            <p class="page-subtitle">시스템 관련 주요 공지사항을 확인하세요.</p>
          </div>
          <button v-if="isAdmin" class="btn-dark" @click="openForm()">글쓰기</button>
        </div>

        <table class="data-table">
          <thead>
            <tr>
              <th class="col-no">번호</th>
              <th class="col-img">이미지</th>
              <th>제목</th>
              <th class="col-date">작성일</th>
              <th class="col-views">조회수</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="sortedNotices.length === 0">
              <td colspan="5" class="empty-msg">등록된 공지사항이 없습니다.</td>
            </tr>
            <tr
              v-for="(notice, idx) in sortedNotices"
              :key="notice.id"
              :class="['clickable-row', { 'important-row': notice.important }]"
              @click="openDetail(notice)"
            >
              <td>{{ sortedNotices.length - idx }}</td>
              <td>
                <img v-if="notice.image" :src="notice.image" class="thumb" alt="" />
                <span v-else class="no-img">-</span>
              </td>
              <td>
                <span v-if="notice.important" class="important-badge">중요</span>
                {{ notice.title }}
              </td>
              <td>{{ notice.createdAt }}</td>
              <td>{{ notice.views }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <!-- 상세 -->
    <template v-else-if="viewMode === 'detail' && selectedNotice">
      <div class="panel panel-detail">
        <div class="detail-page-header">
          <div class="detail-title-row">
            <span v-if="selectedNotice.important" class="important-badge">중요</span>
            <h2 class="detail-title">{{ selectedNotice.title }}</h2>
          </div>
          <div class="detail-meta">
            <span v-if="selectedNotice.author" class="detail-meta-item">작성자: {{ selectedNotice.author }}</span>
            <span class="detail-meta-item">작성일: {{ selectedNotice.createdAt }}</span>
            <span class="detail-meta-item">조회수: {{ selectedNotice.views }}</span>
          </div>
        </div>

        <div class="detail-page-body">
          <div class="detail-content">{{ selectedNotice.content }}</div>
          <figure v-if="selectedNotice.image" class="detail-figure">
            <img :src="selectedNotice.image" class="detail-img" alt="첨부 이미지" />
          </figure>

          <div class="detail-actions">
            <button type="button" class="btn-list" @click="viewMode = 'list'">목록으로</button>
            <div v-if="isAdmin" class="detail-admin-btns">
              <button type="button" class="btn-edit" @click="openForm(selectedNotice)">수정</button>
              <button type="button" class="btn-delete" @click="handleDelete(selectedNotice.id)">삭제</button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 작성 폼 (관리자) -->
    <template v-else-if="viewMode === 'form'">
      <div class="panel panel-form">
        <div class="form-page-header">
          <h2 class="page-title form-page-title">{{ isEditMode ? '공지사항 수정' : '새 공지사항 작성' }}</h2>
          <p class="page-subtitle form-page-subtitle">
            {{ isEditMode ? '기존 공지 내용을 수정합니다.' : '제목·내용을 입력하고 필요 시 중요 공지로 표시할 수 있습니다.' }}
          </p>
        </div>

        <div class="write-form">
          <div class="form-group">
            <label for="notice-title">제목</label>
            <input
              id="notice-title"
              v-model="form.title"
              type="text"
              class="form-control"
              placeholder="제목을 입력하세요."
            />
          </div>

          <div class="form-group form-group-row">
            <label class="checkbox-label">
              <input v-model="form.important" type="checkbox" class="checkbox-input" />
              <span>중요 공지로 상단 고정</span>
            </label>
          </div>

          <div class="form-group">
            <label>이미지 첨부</label>
            <div class="file-row">
              <label class="btn-file">
                파일 선택
                <input type="file" accept="image/*" hidden @change="handleImageChange" />
              </label>
              <span class="file-name">{{ imageFileName || (form.image ? '기존 이미지 유지' : '선택된 파일 없음') }}</span>
            </div>
          </div>

          <div class="form-group">
            <label for="notice-content">내용</label>
            <textarea
              id="notice-content"
              v-model="form.content"
              class="form-control form-textarea"
              rows="12"
              placeholder="내용을 입력하세요."
            ></textarea>
          </div>

          <div v-if="formMsg" class="msg-error">{{ formMsg }}</div>

          <div class="form-actions">
            <button type="button" class="btn-cancel" @click="viewMode = 'list'">취소</button>
            <button type="button" class="btn-dark" @click="handleSubmit">저장</button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.notice {
  padding: 24px;
  background: #f8fafc;
  min-height: 100%;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.panel {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

/* 헤더 */
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

/* 버튼 */
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

.btn-dark:hover {
  background: #2d3748;
}

.btn-cancel {
  padding: 10px 20px;
  background: #fff;
  border: 1px solid #e2e8f0;
  font-weight: 600;
  cursor: pointer;
}

.btn-cancel:hover {
  background: #f8fafc;
}

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

.data-table tr:last-child td { border-bottom: none; }

.col-no { width: 60px; }
.col-img { width: 70px; }
.col-date { width: 110px; }
.col-views { width: 70px; }

.clickable-row { cursor: pointer; transition: background 0.12s; }
.clickable-row:hover { background: #f9f9f9; }
.important-row { background: #fffbeb; }
.important-row:hover { background: #fef9e7; }

.thumb {
  width: 36px;
  height: 36px;
  object-fit: cover;
  border-radius: 0;
}

.no-img { color: #c8d4e0; }

.important-badge {
  display: inline-block;
  background: #fef3c7;
  color: #b45309;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: 4px;
  margin-right: 6px;
  border: 1px solid #fde68a;
  letter-spacing: 0.02em;
}

.empty-msg {
  text-align: center;
  padding: 48px;
  color: #9eaab8;
  font-size: 0.88rem;
}

/* 상세 */
.panel-detail {
  padding: 0;
  overflow: hidden;
}

.detail-page-header {
  padding: 24px 28px;
  border-bottom: 1px solid var(--border-solid);
  background: var(--surface-2);
}

.detail-title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 10px 12px;
  margin-bottom: 12px;
}

.detail-title {
  flex: 1;
  min-width: 0;
  font-size: 1.2rem;
  font-weight: 700;
  margin: 0;
  line-height: 1.35;
  color: var(--text-h);
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  font-size: 0.82rem;
  color: var(--text-muted);
}

.detail-meta-item {
  white-space: nowrap;
}

.detail-page-body {
  width: 100%;
  padding: 24px 28px 28px;
  box-sizing: border-box;
}

.detail-content {
  min-height: 48px;
  line-height: 1.75;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 0.94rem;
  margin-bottom: 0;
}

.detail-figure {
  margin: 24px 0 0;
  padding: 0;
}

.detail-img {
  max-width: 100%;
  height: auto;
  display: block;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid #e8eef6;
}

.detail-admin-btns {
  display: flex;
  gap: 8px;
}

.btn-list {
  padding: 7px 16px;
  background: #fff;
  color: #4a5568;
  border: 1px solid #d1dbe8;
  border-radius: 0;
  font-size: 0.86rem;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-list:hover { background: #f0f6ff; }

.btn-edit {
  padding: 7px 16px;
  background: #1565c0;
  color: #fff;
  border: none;
  border-radius: 0;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-edit:hover { background: #0d47a1; }

.btn-delete {
  padding: 7px 16px;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 0;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-delete:hover { background: #dc2626; }

/* 작성 폼 */
.panel-form {
  padding: 0;
  overflow: hidden;
}

.form-page-header {
  padding: 24px 28px;
  border-bottom: 1px solid #edf2f7;
}

.form-page-title {
  margin-bottom: 6px;
}

.form-page-subtitle {
  margin: 0;
}

.write-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  padding: 24px 28px 28px;
  box-sizing: border-box;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group-row {
  flex-direction: row;
  align-items: center;
}

.form-group label {
  font-size: 0.82rem;
  color: #475569;
  font-weight: 600;
}

.form-control {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.9rem;
  outline: none;
  font-family: inherit;
  background: #fff;
  color: #1e293b;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.form-control::placeholder {
  color: #94a3b8;
}

.form-control:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.form-textarea {
  min-height: 220px;
  resize: vertical;
  line-height: 1.6;
}

.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-size: 0.88rem;
  color: #334155;
  font-weight: 500;
  user-select: none;
}

.checkbox-input {
  width: 16px;
  height: 16px;
  accent-color: #1a202c;
  cursor: pointer;
}

.file-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-file {
  padding: 6px 14px;
  background: #1565c0;
  color: #fff;
  border-radius: 0;
  font-size: 0.82rem;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
}

.btn-file:hover { background: #0d47a1; }

.file-name {
  font-size: 0.82rem;
  color: #8a9bb5;
}

.msg-error {
  background: #fee2e2;
  color: #dc2626;
  padding: 10px 14px;
  border-radius: 0;
  font-size: 0.86rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 8px;
  border-top: 1px solid #e8eef6;
}

/* ===== 다크모드 전용 보정 (공지 상단/헤더/목록) ===== */
:global(html[data-theme='dark']) .notice .page-header,
:global(html[data-theme='dark']) .notice .detail-page-header,
:global(html[data-theme='dark']) .notice .form-page-header {
  background: #111827;
  border-bottom-color: rgba(255, 255, 255, 0.14);
}

:global(html[data-theme='dark']) .notice .page-title,
:global(html[data-theme='dark']) .notice .detail-title {
  color: #f8fafc;
}

:global(html[data-theme='dark']) .notice .page-subtitle,
:global(html[data-theme='dark']) .notice .detail-meta {
  color: #cbd5e1;
}
</style>
