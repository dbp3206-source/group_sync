import { BrowserRouter, Link, NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { BrainCircuit, BookOpenText, Compass, Home, LogOut, Menu, Sparkles, UserRound } from 'lucide-react'
import '../tokens.css'
import './App.css'
import './styles/app-shell.css'
import './styles/redesign.css'
import './styles/knowledgeos-responsive.css'
import { useAuth } from './auth'
import ProtectedRoute from './components/ProtectedRoute'
import HealthPage from './pages/HealthPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ProfilePage from './pages/ProfilePage'
import Avatar from './components/Avatar'
import KnowledgeHomePage from './pages/KnowledgeHomePage'
import KnowledgeLibraryPage from './pages/KnowledgeLibraryPage'
import KnowledgeAskPage from './pages/KnowledgeAskPage'
import KnowledgeFocusPage from './pages/KnowledgeFocusPage'
import KnowledgeInsightsPage from './pages/KnowledgeInsightsPage'
import KnowledgeGuidePage from './pages/KnowledgeGuidePage'
import ErrorBoundary from './components/ErrorBoundary'
import ResourceWorkspacePage from './pages/ResourceWorkspacePage'

function App() {
  return (
    <BrowserRouter>
      <ErrorBoundary>
        <AppShell />
      </ErrorBoundary>
    </BrowserRouter>
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
    { to: '/dashboard', label: 'Home', icon: Home },
    { to: '/library', label: 'Library', icon: BookOpenText },
    { to: '/ask', label: 'Ask', icon: BrainCircuit },
    { to: '/focus', label: 'Focus', icon: Sparkles },
  ]

  return <div className={`app-shell${user ? ' app-shell--signed-in' : ' app-shell--guest'}`}>
    {user && <aside className="app-sidebar" aria-label="Primary navigation">
      <Link className="brand" to="/dashboard"><span className="brand-mark" aria-hidden="true">K</span><span>KnowledgeOS</span></Link>
      <nav className="sidebar-nav">
        {navItems.map((item) => { const Icon = item.icon; return <NavLink key={item.to} to={item.to} className={({ isActive }) => `sidebar-link${isActive ? ' is-active' : ''}`}><Icon aria-hidden="true" size={19} strokeWidth={1.8} /><span>{item.label}</span></NavLink> })}
      </nav>
      <div className="sidebar-foot">
        <NavLink to="/guide" className={({ isActive }) => `sidebar-secondary-link${isActive ? ' is-active' : ''}`}><Compass aria-hidden="true" size={18} /><span>Guide</span></NavLink>
        <Link className="account-link" to="/profile" aria-label="Open your profile"><Avatar displayName={user.displayName} avatarUrl={user.avatarUrl} /><span><strong>{user.displayName}</strong><small>Personal library</small></span></Link>
        <button className="sign-out-button" onClick={signOut}><LogOut size={17} aria-hidden="true" />Sign out</button>
      </div>
    </aside>}
    {user && <nav className="mobile-nav" aria-label="Primary navigation">
      {navItems.map((item) => { const Icon = item.icon; return <NavLink key={item.to} to={item.to} className={({ isActive }) => `mobile-nav-link${isActive ? ' is-active' : ''}`}><Icon aria-hidden="true" size={20} strokeWidth={1.8} /><span>{item.label}</span></NavLink> })}
    </nav>}
    {user && <header className="mobile-app-header"><Link className="brand" to="/dashboard"><span className="brand-mark" aria-hidden="true">K</span><span>KnowledgeOS</span></Link><details className="mobile-account-menu"><summary aria-label="Open account menu"><Menu size={20} aria-hidden="true" /></summary><nav aria-label="Account and help"><NavLink to="/guide"><Compass size={17} aria-hidden="true" />Guide</NavLink><NavLink to="/profile"><UserRound size={17} aria-hidden="true" />Profile</NavLink><button type="button" onClick={signOut}><LogOut size={17} aria-hidden="true" />Sign out</button></nav></details></header>}
    {!user && <header className="guest-header"><Link className="brand" to="/login"><span className="brand-mark" aria-hidden="true">K</span><span>KnowledgeOS</span></Link><nav className="guest-nav"><Link to="/login">Sign in</Link><Link className="button button--quiet" to="/register">Create account</Link></nav></header>}
    <main className="app-main">
      <Routes>
        <Route path="/health" element={<HealthPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<KnowledgeHomePage />} />
          <Route path="/library" element={<KnowledgeLibraryPage />} />
          <Route path="/library/:resourceId" element={<ResourceWorkspacePage />} />
          <Route path="/ask" element={<KnowledgeAskPage />} />
          <Route path="/focus" element={<KnowledgeFocusPage />} />
          <Route path="/insights" element={<KnowledgeInsightsPage />} />
          <Route path="/guide" element={<KnowledgeGuidePage />} />
          <Route path="/knowledge/library" element={<Navigate to="/library" replace />} />
          <Route path="/knowledge/library/:resourceId" element={<ResourceWorkspacePage />} />
          <Route path="/knowledge/ask" element={<Navigate to="/ask" replace />} />
          <Route path="/knowledge/focus" element={<Navigate to="/focus" replace />} />
          <Route path="/knowledge/insights" element={<Navigate to="/insights" replace />} />
          <Route path="/knowledge/guide" element={<Navigate to="/guide" replace />} />
          <Route path="/profile/setup" element={<Navigate to="/profile" replace />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
        <Route path="/login" element={user ? <Navigate to="/dashboard" replace /> : <LoginPage />} />
        <Route path="/register" element={user ? <Navigate to="/dashboard" replace /> : <RegisterPage />} />
        <Route path="*" element={<Navigate to={user ? '/dashboard' : '/login'} replace />} />
      </Routes>
    </main>
  </div>
}

export default App
