<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '../store/auth'

const { users, fetchUsers, deleteUser, updateRole } = useAuth()

onMounted(async () => {
  await fetchUsers()
})

const userList = computed(() =>
  users.value.filter((u) => u.loginId !== 'admin')
)

const expandedId = ref(null)

function toggleExpand(userId) {
  expandedId.value = expandedId.value === userId ? null : userId
}

function vehicleCount(user) {
  return user.vehicles ? user.vehicles.length : 0
}

async function handleDeleteUser(userId, e) {
  e.stopPropagation() // 행 클릭 이벤트 방지
  if (!confirm('정말로 이 회원을 삭제하시겠습니까? 관련 데이터가 모두 삭제됩니다.')) return
  
  const res = await deleteUser(userId)
  if (res.success) {
    alert('회원이 삭제되었습니다.')
  } else {
    alert(res.message)
  }
}

async function handleToggleRole(user, e) {
  e.stopPropagation() // 행 클릭 이벤트 방지
  const newRole = user.role?.toUpperCase() === 'ADMIN' ? 'USER' : 'ADMIN'
  const roleText = newRole === 'ADMIN' ? '관리자' : '일반 사용자'
  
  if (!confirm(`'${user.userName}' 회원의 권한을 ${roleText}(으)로 변경하시겠습니까?`)) return
  
  const res = await updateRole(user.userId, newRole)
  if (res.success) {
    alert('권한이 변경되었습니다.')
  } else {
    alert(res.message)
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.split('T')[0]
}
</script>

<template>
  <div class="user-management">
    <div class="panel">
      <div class="page-header">
        <h2 class="page-title">회원 관리</h2>
        <p class="page-subtitle">시스템에 등록된 전체 회원 목록입니다. 사용자 행을 클릭하여 차량 정보를 확인하세요.</p>
      </div>

      <table class="data-table">
        <thead>
          <tr>
            <th class="col-no">ID</th>
            <th>로그인 ID</th>
            <th>이름</th>
            <th>전화번호</th>
            <th>이메일</th>
            <th>권한</th>
            <th>가입일</th>
            <th>작업</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="userList.length === 0">
            <td colspan="8" class="empty-msg">등록된 회원이 없습니다.</td>
          </tr>

          <template v-for="(user, idx) in userList" :key="user.userId">
            <!-- 사용자 행 -->
            <tr
              :class="['clickable-row', { expanded: expandedId === user.userId }]"
              @click="toggleExpand(user.userId)"
            >
              <td>{{ idx + 1 }}</td>
              <td>{{ user.loginId }}</td>
              <td>{{ user.userName }}</td>
              <td>{{ user.phone || '-' }}</td>
              <td>{{ user.email || '-' }}</td>
              <td>
                <span 
                  :class="['role-badge', user.role?.toUpperCase() === 'ADMIN' ? 'badge-admin' : 'badge-user']"
                  @click="handleToggleRole(user, $event)"
                  title="클릭하여 권한 변경"
                >
                  {{ user.role?.toUpperCase() === 'ADMIN' ? 'ADMIN' : 'USER' }}
                </span>
              </td>
              <td>{{ formatDate(user.createdAt) }}</td>
              <td>
                <button class="btn-delete-user" @click="handleDeleteUser(user.userId, $event)">삭제</button>
              </td>
            </tr>

            <!-- 차량 정보 펼침 행 -->
            <tr v-if="expandedId === user.userId" class="expand-row">
              <td colspan="8">
                <div class="vehicle-expand">
                  <p class="vehicle-header">
                    등록된 차량 ({{ vehicleCount(user) }}대)
                  </p>
                  <table v-if="vehicleCount(user) > 0" class="vehicle-table">
                    <thead>
                      <tr>
                        <th>차량 번호</th>
                        <th>단말기 시리얼</th>
                        <th>등록일</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="v in user.vehicles" :key="v.vehicleId">
                        <td><strong>{{ v.carNumber || '-' }}</strong></td>
                        <td>{{ v.serialNumber || '-' }}</td>
                        <td>{{ v.createdAt }}</td>
                      </tr>
                    </tbody>
                  </table>
                  <p v-else class="no-vehicle">등록된 차량이 없습니다.</p>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.user-management {
  padding: 24px;
  background: var(--page-bg);
  min-height: 100%;
}

.panel {
  background: var(--bg-card);
  border: 1px solid var(--border-solid);
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.page-header {
  padding: 24px 28px;
  border-bottom: 1px solid var(--border-solid);
}

.page-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-h);
  margin-bottom: 4px;
}

.page-subtitle {
  font-size: 0.88rem;
  color: var(--text-muted);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.data-table th {
  text-align: left;
  padding: 14px 20px;
  background: var(--surface-2);
  color: var(--text-muted);
  font-weight: 600;
  border-bottom: 1px solid var(--border-solid);
}

.data-table td {
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-solid);
  color: var(--text);
}

.col-no { width: 60px; }

.clickable-row {
  cursor: pointer;
  transition: background 0.12s;
}

.clickable-row:hover {
  background: var(--surface-2);
}

.clickable-row.expanded {
  background: var(--surface-2);
}

.role-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 2px;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.badge-admin {
  background: #fef3c7;
  color: #b45309;
  border: 1px solid #fde68a;
  cursor: pointer;
}

.badge-user {
  background: #dbeafe;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
  cursor: pointer;
}

.btn-delete-user {
  padding: 4px 10px;
  background: transparent;
  color: #e53e3e;
  border: 1px solid #fed7d7;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  border-radius: 2px;
  transition: all 0.2s;
}

.btn-delete-user:hover {
  background: #e53e3e;
  color: #fff;
  border-color: #e53e3e;
}

/* 펼침 행 */
.expand-row td {
  padding: 0;
  border-bottom: 1px solid var(--border-solid);
  background: var(--surface-2);
}

.vehicle-expand {
  padding: 20px 28px;
}

.vehicle-header {
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--text-h);
  margin-bottom: 12px;
}

.vehicle-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.86rem;
  border: 1px solid var(--border-solid);
  background: var(--bg-card);
}

.vehicle-table th {
  text-align: left;
  padding: 10px 16px;
  background: var(--surface-2);
  color: var(--text-muted);
  font-weight: 600;
  border-bottom: 1px solid var(--border-solid);
}

.vehicle-table td {
  padding: 12px 16px;
  color: var(--text);
  border-right: 1px solid var(--border-solid);
}

.vehicle-table td:last-child {
  border-right: none;
}

.no-vehicle {
  font-size: 0.84rem;
  color: #9eaab8;
  padding: 8px 0;
}

.empty-msg {
  text-align: center;
  padding: 48px;
  color: #9eaab8;
  font-size: 0.88rem;
}
</style>
