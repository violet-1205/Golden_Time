/**
 * Spring LocalDateTime JSON(타임존 없음)을 한국 표준시 기준으로 파싱한 뒤 KST로 표시한다.
 * 서버(JVM)가 Asia/Seoul로 저장·직렬화하는 전제와 맞춘다.
 */
function parseServerDateTime(ts) {
  if (ts == null || ts === '') return null
  if (ts instanceof Date) return ts
  if (Array.isArray(ts) && ts.length >= 3) {
    const [y, mo, d, h = 0, mi = 0, s = 0] = ts
    const pad = (n) => String(n).padStart(2, '0')
    const iso = `${y}-${pad(mo)}-${pad(d)}T${pad(h)}:${pad(mi)}:${pad(s)}`
    return new Date(`${iso}+09:00`)
  }
  const s = String(ts).trim()
  if (/^\d{4}-\d{2}-\d{2}T/.test(s) && !/[zZ]|[+-]\d{2}:?\d{2}$/.test(s)) {
    return new Date(`${s}+09:00`)
  }
  return new Date(s)
}

export function formatKstDateTime(ts) {
  const d = parseServerDateTime(ts)
  if (!d || Number.isNaN(d.getTime())) return '-'
  return d
    .toLocaleString('ko-KR', {
      timeZone: 'Asia/Seoul',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    })
    .replace(/\./g, '-')
    .replace(/ /g, ' ')
    .replace(/- /g, ' ')
}
