import { reactive, computed } from 'vue'

const state = reactive({
  events: [],
  stats: {
    averageConfidence: 0
  },
  recentEvents: [],
  eventsByRegion: {},
  notices: [],

  inquiries: [
    {
      id: '1',
      title: '로그인 오류 문의',
      content: '로그인 시 간헐적으로 오류가 발생합니다.',
      authorId: '1',
      authorName: '관리자',
      status: 'answered',
      answer: '확인 후 수정하였습니다. 이용에 불편을 드려 죄송합니다.',
      image: '',
      views: 5,
      createdAt: '2026-03-14',
      updatedAt: '2026-03-15',
    },
  ],

  detections: [
    {
      id: '1',
      type: 'SQL Injection',
      severity: 'high',
      source: '192.168.1.100',
      detectedAt: '2026-03-17 14:32:01',
      status: '차단됨',
    },
    {
      id: '2',
      type: 'XSS Attack',
      severity: 'medium',
      source: '10.0.0.55',
      detectedAt: '2026-03-17 13:10:45',
      status: '차단됨',
    },
    {
      id: '3',
      type: 'Brute Force',
      severity: 'high',
      source: '203.0.113.12',
      detectedAt: '2026-03-17 11:05:22',
      status: '차단됨',
    },
    {
      id: '4',
      type: 'Port Scan',
      severity: 'low',
      source: '172.16.0.200',
      detectedAt: '2026-03-17 09:44:13',
      status: '모니터링',
    },
    {
      id: '5',
      type: 'CSRF',
      severity: 'medium',
      source: '192.168.2.50',
      detectedAt: '2026-03-16 23:18:30',
      status: '차단됨',
    },
  ],
})

export function useData() {
  async function fetchEvents() {
    try {
      const response = await fetch('/api/dashboard/events', { credentials: 'include' })
      if (response.ok) {
        state.events = await response.json()
      }
    } catch (error) {
      console.error('Fetch events error:', error)
    }
  }

  async function fetchStats() {
    try {
      const response = await fetch('/api/dashboard/stats', { credentials: 'include' })
      if (response.ok) {
        state.stats = await response.json()
      }
    } catch (error) {
      console.error('Fetch stats error:', error)
    }
  }

  async function fetchRecentEvents() {
    try {
      const response = await fetch('/api/dashboard/recent-events', { credentials: 'include' })
      if (response.ok) {
        state.recentEvents = await response.json()
      }
    } catch (error) {
      console.error('Fetch recent events error:', error)
    }
  }

  async function fetchEventsByRegion() {
    try {
      const response = await fetch('/api/dashboard/events-by-region', { credentials: 'include' })
      if (response.ok) {
        state.eventsByRegion = await response.json()
      }
    } catch (error) {
      console.error('Fetch events by region error:', error)
    }
  }

  async function addEvent(eventData) {
    try {
      const formData = new FormData()
      if (eventData.videoFile) {
        formData.append('videoFile', eventData.videoFile)
      }
      if (eventData.vehicleId) {
        formData.append('vehicleId', eventData.vehicleId)
      }
      // 사용자가 영상만 올리기로 했으므로 다른 필드는 백엔드에서 처리하거나 기본값 사용
      // carNumber, serialNumber 등은 이제 필수값이 아니게 될 것임

      const response = await fetch('/api/dashboard/upload', {
        method: 'POST',
        body: formData,
      })

      if (response.ok) {
        await fetchEvents()
        return { success: true }
      }
    } catch (error) {
      console.error('Add event error:', error)
    }
    return { success: false }
  }

  async function deleteEvent(gtId) {
    try {
      const response = await fetch(`/api/dashboard/events/${gtId}`, {
        method: 'DELETE',
      })
      if (response.ok) {
        await fetchEvents()
        return { success: true }
      }
    } catch (error) {
      console.error('Delete event error:', error)
    }
    return { success: false }
  }

  async function sendEvent(gtId, target) {
    try {
      const response = await fetch(`/api/dashboard/events/${gtId}/send?target=${target}`, {
        method: 'POST',
      })
      if (response.ok) {
        await fetchEvents()
        return { success: true }
      }
    } catch (error) {
      console.error('Send event error:', error)
    }
    return { success: false }
  }

  function formatNoticeDate(iso) {
    if (!iso) return ''
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return String(iso)
    return d.toLocaleDateString('ko-KR')
  }

  function mapNoticeFromApi(n) {
    return {
      id: String(n.id),
      title: n.title,
      content: n.content,
      important: !!n.important,
      author: n.author || '관리자',
      image: n.image || '',
      views: n.views ?? 0,
      createdAt: formatNoticeDate(n.createdAt),
    }
  }

  async function fetchNotices() {
    try {
      const response = await fetch('/api/notices', { credentials: 'include' })
      if (response.ok) {
        const list = await response.json()
        state.notices = list.map(mapNoticeFromApi)
      }
    } catch (error) {
      console.error('Fetch notices error:', error)
    }
  }

  async function addNotice(payload) {
    const fd = new FormData()
    fd.append('title', payload.title)
    fd.append('content', payload.content)
    fd.append('important', String(!!payload.important))
    if (payload.imageFile) fd.append('imageFile', payload.imageFile)
    const response = await fetch('/api/notices', {
      method: 'POST',
      body: fd,
      credentials: 'include',
    })
    if (response.ok) await fetchNotices()
  }

  async function updateNotice(id, payload) {
    const fd = new FormData()
    fd.append('title', payload.title)
    fd.append('content', payload.content)
    fd.append('important', String(!!payload.important))
    if (payload.imageFile) fd.append('imageFile', payload.imageFile)
    const response = await fetch(`/api/notices/${id}`, {
      method: 'PUT',
      body: fd,
      credentials: 'include',
    })
    if (response.ok) await fetchNotices()
  }

  async function deleteNotice(id) {
    const response = await fetch(`/api/notices/${id}`, {
      method: 'DELETE',
      credentials: 'include',
    })
    if (response.ok) await fetchNotices()
  }

  async function fetchNoticeDetail(id) {
    const response = await fetch(`/api/notices/${id}`, { credentials: 'include' })
    if (response.ok) {
      const n = await response.json()
      const mapped = mapNoticeFromApi(n)
      const idx = state.notices.findIndex((x) => String(x.id) === String(id))
      if (idx >= 0) state.notices[idx] = { ...state.notices[idx], ...mapped }
      return mapped
    }
    return null
  }

  function addInquiry(inquiry) {
    state.inquiries.unshift({
      ...inquiry,
      id: String(Date.now()),
      status: 'pending',
      answer: '',
      image: inquiry.image || '',
      views: 0,
      createdAt: new Date().toLocaleDateString('ko-KR'),
      updatedAt: new Date().toLocaleDateString('ko-KR'),
    })
  }

  function incrementInquiryViews(id) {
    const inq = state.inquiries.find((i) => i.id === id)
    if (inq) inq.views++
  }

  function updateEvent(id, updates) {
    const ev = state.events.find((e) => e.id === id)
    if (ev) Object.assign(ev, updates)
  }

  function updateInquiry(id, updates) {
    const inquiry = state.inquiries.find((i) => i.id === id)
    if (inquiry) {
      Object.assign(inquiry, { ...updates, updatedAt: new Date().toLocaleDateString('ko-KR') })
    }
  }

  function deleteInquiry(id) {
    const idx = state.inquiries.findIndex((i) => i.id === id)
    if (idx !== -1) state.inquiries.splice(idx, 1)
  }

  return {
    events: computed(() => state.events),
    stats: computed(() => state.stats),
    recentEvents: computed(() => state.recentEvents),
    eventsByRegion: computed(() => state.eventsByRegion),
    notices: computed(() => state.notices),
    inquiries: computed(() => state.inquiries),
    detections: computed(() => state.detections),
    fetchEvents,
    fetchStats,
    fetchRecentEvents,
    fetchEventsByRegion,
    addEvent,
    deleteEvent,
    sendEvent,
    updateEvent,
    fetchNotices,
    addNotice,
    updateNotice,
    deleteNotice,
    fetchNoticeDetail,
    addInquiry,
    incrementInquiryViews,
    updateInquiry,
    deleteInquiry,
  }
}
