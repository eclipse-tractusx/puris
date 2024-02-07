import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, './src'),
      "@assets": path.resolve(__dirname, './src/assets'),
      "@components": path.resolve(__dirname, './src/components'),
      "@contexts": path.resolve(__dirname, './src/contexts'),
      "@features": path.resolve(__dirname, './src/features'),
      "@hooks": path.resolve(__dirname, './src/hooks'),
      "@models": path.resolve(__dirname, './src/models'),
      "@services": path.resolve(__dirname, './src/services'),
      "@util": path.resolve(__dirname, './src/util'),
      "@views": path.resolve(__dirname, './src/views')
    },
  },
})
