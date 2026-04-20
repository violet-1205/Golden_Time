// 개발: 빈 문자열 (Vite 프록시가 /api → localhost:1111 전달)
// 프로덕션: VITE_API_BASE_URL=https://your-backend.railway.app
const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

function getCookie(name) {
  try {
    const match = document.cookie.match(new RegExp('(^|;\\s*)' + name + '=([^;]*)'))
    return match ? decodeURIComponent(match[2]) : ''
  } catch {
    return ''
  }
}

export function getXsrfToken() {
  return getCookie('XSRF-TOKEN')
}

export async function csrfFetch(input, init = {}) {
  const method = (init.method || 'GET').toUpperCase()
  const headers = new Headers(init.headers || {})
  const isSafeMethod = method === 'GET' || method === 'HEAD' || method === 'OPTIONS'
  const fullUrl = API_BASE + input

  if (!isSafeMethod) {
    let token = getCookie('XSRF-TOKEN')
    if (!token) {
      await fetch(API_BASE + '/api/users/me', { method: 'GET', credentials: 'include' }).catch(() => {})
      token = getCookie('XSRF-TOKEN')
    }
    if (!token) {
      await fetch(API_BASE + '/api/notices', { method: 'GET', credentials: 'include' }).catch(() => {})
      token = getCookie('XSRF-TOKEN')
    }
    if (!token) {
      await fetch(API_BASE + '/', { method: 'GET', credentials: 'include' }).catch(() => {})
      token = getCookie('XSRF-TOKEN')
    }
    if (token) headers.set('X-XSRF-TOKEN', token)
  }

  return fetch(fullUrl, {
    credentials: 'include',
    ...init,
    headers,
  })
}

