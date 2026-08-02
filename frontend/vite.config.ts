import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [react()],
    build: { sourcemap: false },
    server: {
      host: true,
      port: 5173,
      proxy: {
        '/api': {
          target: env.API_PROXY_TARGET || 'http://localhost:8081',
          changeOrigin: true,
        },
        '/actuator': {
          target: env.API_PROXY_TARGET || 'http://localhost:8081',
          changeOrigin: true,
        },
      },
    },
    test: {
      environment: 'jsdom',
      setupFiles: './src/test/setup.ts',
      exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
      css: true,
      coverage: {
        reporter: ['text', 'html'],
      },
    },
  }
})
