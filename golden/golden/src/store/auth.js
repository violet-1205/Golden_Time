import { reactive, computed } from 'vue'
import { csrfFetch } from '../utils/http'

const state = reactive({
  currentUser: null,
  users: [],
  isInitialized: false,
})

export function useAuth() {
  const currentUser = computed(() => state.currentUser)
  const isLoggedIn = computed(() => !!state.currentUser)
  const isAdmin = computed(() => state.currentUser?.role?.toLowerCase() === 'admin')
  const isInitialized = computed(() => state.isInitialized)

  async function fetchMe() {
    try {
      const response = await csrfFetch('/api/users/me')
      if (response.ok) {
        const user = await response.json()
        state.currentUser = user
        state.isInitialized = true
        return true
      }
    } catch (error) {
      console.error('Failed to fetch user info:', error)
    }
    state.currentUser = null
    state.isInitialized = true
    return false
  }

  async function login(username, password) {
    try {
      const params = new URLSearchParams()
      params.append('username', username)
      params.append('password', password)

      // 로그인은 CSRF 예외로 두더라도, credentials는 포함되어야 세션이 잡힘
      const response = await csrfFetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: params,
      })

      if (response.ok) {
        await fetchMe()
        return { success: true, message: '' }
      } else {
        return { success: false, message: '아이디 또는 비밀번호가 올바르지 않습니다.' }
      }
    } catch (error) {
      return { success: false, message: '로그인 중 오류가 발생했습니다.' }
    }
  }

  async function logout() {
    state.currentUser = null
    window.location.href = '/login'
    // 로그아웃 API는 백그라운드로 처리 (응답 기다리지 않음)
    csrfFetch('/api/auth/logout', { method: 'POST' }).catch(() => {})
  }

  async function register(username, name, password, phone, email, address, vehicleNumber, deviceSerial) {
    try {
      const response = await csrfFetch('/api/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          loginId: username,
          password,
          userName: name,
          phone,
          email,
          address,
          carNumber: vehicleNumber,
          serialNumber: deviceSerial,
        }),
      })

      if (response.ok) {
        return { success: true, message: '' }
      } else {
        const msg = await response.text()
        return { success: false, message: msg || '회원가입에 실패했습니다.' }
      }
    } catch (error) {
      return { success: false, message: '회원가입 중 오류가 발생했습니다.' }
    }
  }

  async function updateProfile(updates) {
    try {
      const response = await csrfFetch('/api/users/me', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updates),
      })
      if (response.ok) {
        await fetchMe()
        return { success: true }
      }
      const text = await response.text().catch(() => '')
      return { success: false, status: response.status, message: text || response.statusText }
    } catch (error) {
      console.error('Update profile error:', error)
    }
    return { success: false, message: '네트워크 오류가 발생했습니다.' }
  }

  async function addVehicle(carNumber, serialNumber) {
    try {
      const response = await csrfFetch('/api/users/me/vehicles', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ carNumber, serialNumber }),
      })
      if (response.ok) {
        await fetchMe()
        return { success: true }
      }
    } catch (error) {
      console.error('Add vehicle error:', error)
    }
    return { success: false }
  }

  async function fetchUsers() {
    try {
      const response = await csrfFetch('/api/users')
      if (response.ok) {
        state.users = await response.json()
      }
    } catch (error) {
      console.error('Fetch users error:', error)
    }
  }

  async function deleteUser(userId) {
    try {
      const response = await csrfFetch(`/api/users/${userId}`, {
        method: 'DELETE'
      })
      if (response.ok) {
        await fetchUsers()
        return { success: true }
      } else {
        const msg = await response.text()
        return { success: false, message: msg || '회원 삭제에 실패했습니다.' }
      }
    } catch (error) {
      return { success: false, message: '회원 삭제 중 오류가 발생했습니다.' }
    }
  }

  async function updateRole(userId, role) {
    try {
      const response = await csrfFetch(`/api/users/${userId}/role?role=${role}`, {
        method: 'PUT'
      })
      if (response.ok) {
        await fetchUsers()
        return { success: true }
      } else {
        const msg = await response.text()
        return { success: false, message: msg || '권한 변경에 실패했습니다.' }
      }
    } catch (error) {
      return { success: false, message: '권한 변경 중 오류가 발생했습니다.' }
    }
  }

  return {
    currentUser,
    isLoggedIn,
    isAdmin,
    isInitialized,
    users: computed(() => state.users),
    fetchMe,
    login,
    logout,
    register,
    updateProfile,
    addVehicle,
    fetchUsers,
    deleteUser,
    updateRole,
  }
}
