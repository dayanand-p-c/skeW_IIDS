/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: ['class', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        bg: 'var(--color-bg)',
        surface: 'var(--color-surface)',
        'surface-2': 'var(--color-surface-2)',
        border: 'var(--color-border)',
        'border-strong': 'var(--color-border-strong)',
        text: 'var(--color-text)',
        'text-secondary': 'var(--color-text-secondary)',
        'text-muted': 'var(--color-text-muted)',
        brand: {
          DEFAULT: 'var(--color-brand)',
          hover: 'var(--color-brand-hover)',
          tint: 'var(--color-brand-tint)',
        },
        sidebar: {
          bg: 'var(--color-sidebar-bg)',
          text: 'var(--color-sidebar-text)',
          active: 'var(--color-sidebar-active)',
          border: 'var(--color-sidebar-border)',
          hover: 'var(--color-sidebar-hover)',
        },
        status: {
          safe: 'var(--color-status-safe)',
          'safe-tint': 'var(--color-status-safe-tint)',
          watch: 'var(--color-status-watch)',
          'watch-tint': 'var(--color-status-watch-tint)',
          critical: 'var(--color-status-critical)',
          'critical-tint': 'var(--color-status-critical-tint)',
        },
        heatmap: {
          1: 'var(--color-heatmap-1)',
          2: 'var(--color-heatmap-2)',
          3: 'var(--color-heatmap-3)',
          4: 'var(--color-heatmap-4)',
          5: 'var(--color-heatmap-5)',
        },
      },
      fontFamily: {
        sans: ['"IBM Plex Sans"', '-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'Roboto', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'ui-monospace', 'monospace'],
      },
      borderRadius: {
        card: '8px',
        btn: '6px',
      },
      boxShadow: {
        card: 'var(--shadow-card)',
        sm: '0 1px 2px rgba(16, 24, 40, 0.06)',
      },
      fontSize: {
        'page-title': ['24px', { lineHeight: '32px', fontWeight: '600' }],
        'section-title': ['18px', { lineHeight: '26px', fontWeight: '600' }],
        'card-title': ['14px', { lineHeight: '20px', fontWeight: '600' }],
        'body-text': ['14px', { lineHeight: '22px', fontWeight: '400' }],
        'table-text': ['13px', { lineHeight: '20px', fontWeight: '400' }],
        'field-label': ['12px', { lineHeight: '16px', fontWeight: '500', letterSpacing: '0.02em' }],
        'kpi-value': ['28px', { lineHeight: '32px', fontWeight: '600' }],
        'kpi-caption': ['12px', { lineHeight: '16px', fontWeight: '400' }],
      },
    },
  },
  plugins: [],
}
