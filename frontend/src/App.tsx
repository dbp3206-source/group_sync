import { BrowserRouter, Link, Route, Routes } from 'react-router-dom'
import './App.css'
import HealthPage from './pages/HealthPage'

function App() {
  return (
    <BrowserRouter>
      <div className="app-shell">
        <header className="app-header">
          <Link className="brand" to="/">
            GroupSync
          </Link>
          <span className="phase-label">Foundation checkpoint</span>
        </header>
        <main className="app-main">
          <Routes>
            <Route path="/" element={<HealthPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}

export default App

