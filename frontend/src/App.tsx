import { BrowserRouter, Link, NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import './App.css'
import './styles/app-shell.css'
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
import BadmintonSessionDetailPage from './pages/BadmintonSessionDetailPage'
import BadmintonProfilePage from './pages/BadmintonProfilePage'
import CheckinPage from './pages/CheckinPage'
import TournamentPage from './pages/TournamentPage'
import ProfileSetupPage from './pages/ProfileSetupPage'
import ProfilePage from './pages/ProfilePage'
import AvailabilityPage from './pages/AvailabilityPage'
import Avatar from './components/Avatar'

function App() {
  return (
    <BrowserRouter><AppShell /></BrowserRouter>
  )
}

function AppShell() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function signOut() {
    await logout()
    navigate('/login')
  }

  const navItems = [
    { to: '/dashboard', label: 'Trang chủ', icon: '⌂' },
    { to: '/calendar', label: 'Lịch của tôi', icon: '◫' },
    { to: '/groups', label: 'Nhóm', icon: '◌' },
    { to: '/notifications', label: 'Thông báo', icon: '◉' },
    { to: '/profile', label: 'Hồ sơ', icon: '◍' },
  ]

  return <div className={`app-shell${user ? ' app-shell--signed-in' : ' app-shell--guest'}`}>
    {user && <aside className="app-sidebar" aria-label="Điều hướng chính">
      <Link className="brand" to="/dashboard"><span className="brand-mark" aria-hidden="true">G</span><span>GroupSync</span></Link>
      <nav className="sidebar-nav">
        {navItems.map((item) => <NavLink key={item.to} to={item.to} className={({ isActive }) => `sidebar-link${isActive ? ' is-active' : ''}`}><span aria-hidden="true">{item.icon}</span><span>{item.label}</span></NavLink>)}
      </nav>
      <div className="sidebar-foot">
        <Link className="account-link" to="/profile" aria-label="Mở hồ sơ của bạn"><Avatar displayName={user.displayName} avatarUrl={user.avatarUrl} /><span><strong>{user.displayName}</strong><small>Tài khoản của bạn</small></span></Link>
        <button className="sign-out-button" onClick={signOut}>Đăng xuất</button>
      </div>
    </aside>}
    {user && <nav className="mobile-nav" aria-label="Điều hướng di động">
      {navItems.map((item) => <NavLink key={item.to} to={item.to} className={({ isActive }) => `mobile-nav-link${isActive ? ' is-active' : ''}`}><span aria-hidden="true">{item.icon}</span><span>{item.label}</span></NavLink>)}
    </nav>}
    {!user && <header className="guest-header"><Link className="brand" to="/login"><span className="brand-mark" aria-hidden="true">G</span><span>GroupSync</span></Link><nav className="guest-nav"><Link to="/login">Đăng nhập</Link><Link className="button button--quiet" to="/register">Tạo tài khoản</Link></nav></header>}
    <main className="app-main">
      <Routes>
        <Route path="/health" element={<HealthPage />} />
        <Route element={<ProtectedRoute />}><Route path="/dashboard" element={<DashboardPage />} /><Route path="/notifications" element={<NotificationsPage />} /><Route path="/calendar" element={<CalendarPage />} /><Route path="/study" element={<StudyPage />} /><Route path="/badminton" element={<BadmintonPage />} /><Route path="/groups/:groupId/availability" element={<AvailabilityPage />} /><Route path="/badminton/sessions/:sessionId" element={<BadmintonSessionDetailPage />} /><Route path="/badminton/profile" element={<BadmintonProfilePage />} /><Route path="/profile/setup" element={<ProfileSetupPage />} /><Route path="/profile" element={<ProfilePage />} /></Route>
        <Route path="/check-in" element={user ? <CheckinPage /> : <Navigate to="/login" replace />} />
        <Route element={<ProtectedRoute />}><Route path="/tournaments" element={<TournamentPage />} /></Route>
        <Route path="/login" element={user ? <Navigate to="/dashboard" replace /> : <LoginPage />} />
        <Route path="/register" element={user ? <Navigate to="/dashboard" replace /> : <RegisterPage />} />
        <Route element={<ProtectedRoute />}><Route path="/groups" element={<GroupsPage />} /><Route path="/groups/:groupId" element={<GroupDetailPage />} /></Route>
        <Route path="*" element={<Navigate to={user ? '/dashboard' : '/login'} replace />} />
      </Routes>
    </main>
  </div>
}

export default App
