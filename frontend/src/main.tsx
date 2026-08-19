import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// Outfit (sans-serif display): self-hosted via fontsource, Vite bundles the woff2 assets.
// This replaces the previous render-blocking Google Fonts @import in index.css.
import '@fontsource/outfit/400.css'
import '@fontsource/outfit/500.css'
import '@fontsource/outfit/600.css'
import '@fontsource/outfit/700.css'
import './index.css'
import App from './App.tsx'
import { AuthProvider } from './auth'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider><App /></AuthProvider>
  </StrictMode>,
)
