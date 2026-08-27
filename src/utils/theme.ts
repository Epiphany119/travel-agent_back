export interface SystemPalette {
  fg: string
  bg: string
  accent: string
  /** optional warm highlight used by the premium brand preset */
  highlight?: string
}

export const DEFAULT_SYSTEM_PALETTE: SystemPalette = {
  fg: '#1D2B27',
  bg: '#F7F3EA',
  accent: '#164E42',
  highlight: '#F27A4F'
}

function validColor(value: unknown, fallback: string): string {
  const color = String(value ?? '').trim()
  return /^#(?:[0-9a-f]{3}|[0-9a-f]{6})$/i.test(color) ? color : fallback
}

/** 将个人主页保存的三色主题扩展为全局界面令牌。 */
export function applySystemPalette(palette: SystemPalette = DEFAULT_SYSTEM_PALETTE) {
  const root = document.documentElement
  const safePalette = {
    fg: validColor(palette.fg, DEFAULT_SYSTEM_PALETTE.fg),
    bg: validColor(palette.bg, DEFAULT_SYSTEM_PALETTE.bg),
    accent: validColor(palette.accent, DEFAULT_SYSTEM_PALETTE.accent),
    highlight: validColor(palette.highlight, DEFAULT_SYSTEM_PALETTE.highlight || '#F27A4F')
  }
  root.style.setProperty('--ink', safePalette.fg)
  root.style.setProperty('--paper', safePalette.bg)
  root.style.setProperty('--forest', safePalette.accent)
  root.style.setProperty('--forest-deep', 'color-mix(in srgb, var(--forest) 78%, #000)')
  root.style.setProperty('--roam', 'color-mix(in srgb, var(--forest) 68%, var(--paper))')
  root.style.setProperty('--roam-soft', 'color-mix(in srgb, var(--forest) 12%, var(--paper))')
  root.style.setProperty('--sunset', safePalette.highlight)
  root.style.setProperty('--sunset-soft', 'color-mix(in srgb, var(--sunset) 10%, var(--paper))')
  root.style.setProperty('--card', 'color-mix(in srgb, var(--paper) 91%, #fff)')
  root.style.setProperty('--wash', 'color-mix(in srgb, var(--paper) 88%, var(--ink) 12%)')
  root.style.setProperty('--ink-2', 'color-mix(in srgb, var(--ink) 64%, var(--paper))')
  root.style.setProperty('--ink-3', 'color-mix(in srgb, var(--ink) 40%, var(--paper))')
  root.style.setProperty('--line', 'color-mix(in srgb, var(--ink) 15%, var(--paper))')
  root.style.setProperty('--shadow-soft', '0 2px 12px color-mix(in srgb, var(--forest) 10%, transparent)')
  root.style.setProperty('--shadow-lift', '0 12px 30px color-mix(in srgb, var(--forest) 18%, transparent)')
  root.style.setProperty('--notes-bg', safePalette.bg)
  root.style.setProperty('--notes-fg', safePalette.fg)
  root.style.setProperty('--notes-accent', safePalette.accent)
  root.style.setProperty('--notes-line', 'color-mix(in srgb, var(--ink) 15%, var(--paper))')
  root.style.setProperty('--notes-wash', 'color-mix(in srgb, var(--notes-bg) 88%, var(--notes-fg) 12%)')

  // Element Plus 组件也使用同一套系统主题，避免页面主体变色但弹窗/输入框仍是默认蓝灰色。
  root.style.setProperty('--el-color-primary', safePalette.accent)
  root.style.setProperty('--el-color-primary-light-3', 'color-mix(in srgb, var(--forest) 70%, #fff)')
  root.style.setProperty('--el-color-primary-light-5', 'color-mix(in srgb, var(--forest) 50%, #fff)')
  root.style.setProperty('--el-color-primary-light-7', 'color-mix(in srgb, var(--forest) 30%, #fff)')
  root.style.setProperty('--el-color-primary-light-9', 'color-mix(in srgb, var(--forest) 10%, var(--paper))')
  root.style.setProperty('--el-color-primary-dark-2', 'var(--forest-deep)')
  root.style.setProperty('--el-bg-color-page', 'var(--paper)')
  root.style.setProperty('--el-bg-color', 'var(--card)')
  root.style.setProperty('--el-bg-color-overlay', 'var(--card)')
  root.style.setProperty('--el-fill-color-light', 'var(--wash)')
  root.style.setProperty('--el-border-color', 'var(--line)')
  root.style.setProperty('--el-border-color-light', 'var(--line)')
  root.style.setProperty('--el-text-color-primary', 'var(--ink)')
  root.style.setProperty('--el-text-color-regular', 'var(--ink-2)')
  root.style.setProperty('--el-text-color-secondary', 'var(--ink-3)')
}

export function parseSystemPalette(value: unknown): SystemPalette {
  try {
    const saved = typeof value === 'string' ? JSON.parse(value) : value
    if (saved && typeof saved === 'object') {
      const p = saved as Partial<SystemPalette>
      return {
        fg: validColor(p.fg, DEFAULT_SYSTEM_PALETTE.fg),
        bg: validColor(p.bg, DEFAULT_SYSTEM_PALETTE.bg),
        accent: validColor(p.accent, DEFAULT_SYSTEM_PALETTE.accent),
        highlight: validColor(p.highlight, DEFAULT_SYSTEM_PALETTE.highlight || '#F27A4F')
      }
    }
  } catch { /* 使用默认主题 */ }
  return { ...DEFAULT_SYSTEM_PALETTE }
}
