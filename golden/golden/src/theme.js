const THEME_KEY = 'gt-theme'

function applyTheme(theme) {
  const t = theme === 'dark' ? 'dark' : 'light'
  document.documentElement.setAttribute('data-theme', t)
  try {
    localStorage.setItem(THEME_KEY, t)
  } catch {
    // ignore
  }
  return t
}

export function getInitialTheme() {
  try {
    const saved = localStorage.getItem(THEME_KEY)
    if (saved === 'dark' || saved === 'light') return saved
  } catch {
    // ignore
  }
  const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)')?.matches
  return prefersDark ? 'dark' : 'light'
}

export function initTheme() {
  return applyTheme(getInitialTheme())
}

export function setTheme(theme) {
  return applyTheme(theme)
}

export function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme')
  return applyTheme(current === 'dark' ? 'light' : 'dark')
}

