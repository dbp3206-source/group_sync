import { BrowserRouter, Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import './App.css'
import { useAuth } from './auth/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import GroupDetailPage from './pages/GroupDetailPage'
import GroupsPage from './pages/GroupsPage'
import CalendarPage from './pages/CalendarPage'
import StudyPage from './pages/StudyPage'
import BadmintonPage from './pages/BadmintonPage'
import HealthPage from './pages/HealthPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import NotificationsPage from './pages/NotificationsPage'
import DashboardPage from './pages/DashboardPage'

function App() {
  return (
    <BrowserRouter><AppShell /></BrowserRouter>
  )
}

function AppShell() {
  const { user, loading, logout } = useAuth()
  const navigate = useNavigate()

  async function signOut() {
    await logout()
    navigate('/login')
  }

  return <div className="app-shell">
    <header className="app-header">
      <Link className="brand" to={user ? '/groups' : '/login'}>GroupSync</Link>
      <nav className="app-nav" aria-label="Main navigation">
        {user && <><Link to="/dashboard">Dashboard</Link><Link to="/groups">Groups</Link><Link to="/calendar">Calendar</Link><Link to="/study">Study</Link><Link to="/badminton">Badminton</Link><Link to="/notifications">Inbox</Link></>}
        {!loading && !user && <Link to="/login">Sign in</Link>}
        {!loading && !user && <Link className="nav-cta" to="/register">Register</Link>}
        {user && <><span className="user-chip">{user.displayName}</span><button className="link-button" onClick={signOut}>Sign out</button></>}
      </nav>
    </header>
    <main className="app-main">
      <Routes>
        <Route path="/health" element={<HealthPage />} />
        <Route element={<ProtectedRoute />}><Route path="/dashboard" element={<DashboardPage />} /><Route path="/notifications" element={<NotificationsPage />} /><Route path="/calendar" element={<CalendarPage />} /><Route path="/study" element={<StudyPage />} /><Route path="/badminton" element={<BadmintonPage />} /></Route>
        <Route path="/login" element={user ? <Navigate to="/groups" replace /> : <LoginPage />} />
        <Route path="/register" element={user ? <Navigate to="/groups" replace /> : <RegisterPage />} />
        <Route element={<ProtectedRoute />}><Route path="/groups" element={<GroupsPage />} /><Route path="/groups/:groupId" element={<GroupDetailPage />} /></Route>
        <Route path="*" element={<Navigate to={user ? '/groups' : '/login'} replace />} />
      </Routes>
    </main>
  </div>
}

export default App
