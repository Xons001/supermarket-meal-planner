;(() => {
  const saved = localStorage.getItem('smp-visitor-theme') || 'SYSTEM'
  const dark =
    saved === 'DARK' || (saved === 'SYSTEM' && matchMedia('(prefers-color-scheme: dark)').matches)
  document.documentElement.dataset.theme = dark ? 'dark' : 'light'
})()
