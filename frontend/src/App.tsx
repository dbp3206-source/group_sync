import { BrowserRouter, Link, NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { BrainCircuit, BookOpenText, ChartNoAxesCombined, Compass, Home, LogOut, Menu, Sparkles } from 'lucide-react'
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
import ProfileSetupPage from './pages/ProfileSetupPage'
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
    { to: '/insights', label: 'Insights', icon: ChartNoAxesCombined },
    { to: '/guide', label: 'Guide', icon: Compass },
  ]

  return <div className={`app-shell${user ? ' app-shell--signed-in' : ' app-shell--guest'}`}>
    {user && <aside className="app-sidebar" aria-label="Điều hướng chính">
      <Link className="brand" to="/dashboard"><span className="brand-mark" aria-hidden="true">K</span><span>KnowledgeOS</span></Link>
      <nav className="sidebar-nav">
        {navItems.map((item) => { const Icon = item.icon; return <NavLink key={item.to} to={item.to} className={({ isActive }) => `sidebar-link${isActive ? ' is-active' : ''}`}><Icon aria-hidden="true" size={19} strokeWidth={1.8} /><span>{item.label}</span></NavLink> })}
      </nav>
      <div className="sidebar-foot">
        <Link className="account-link" to="/profile" aria-label="Open your profile"><Avatar displayName={user.displayName} avatarUrl={user.avatarUrl} /><span><strong>{user.displayName}</strong><small>Personal library</small></span></Link>
        <button className="sign-out-button" onClick={signOut}><LogOut size={17} aria-hidden="true" />Sign out</button>
      </div>
    </aside>}
    {user && <nav className="mobile-nav" aria-label="Điều hướng di động">
      {navItems.map((item) => { const Icon = item.icon; return <NavLink key={item.to} to={item.to} className={({ isActive }) => `mobile-nav-link${isActive ? ' is-active' : ''}`}><Icon aria-hidden="true" size={20} strokeWidth={1.8} /><span>{item.label}</span></NavLink> })}
    </nav>}
    {user && <header className="mobile-app-header"><Link className="brand" to="/dashboard"><span className="brand-mark" aria-hidden="true">K</span><span>KnowledgeOS</span></Link><Menu size={20} aria-hidden="true" /></header>}
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
          <Route path="/knowledge/library" element={<KnowledgeLibraryPage />} />
          <Route path="/knowledge/library/:resourceId" element={<ResourceWorkspacePage />} />
          <Route path="/knowledge/ask" element={<KnowledgeAskPage />} />
          <Route path="/knowledge/focus" element={<KnowledgeFocusPage />} />
          <Route path="/knowledge/insights" element={<KnowledgeInsightsPage />} />
          <Route path="/knowledge/guide" element={<KnowledgeGuidePage />} />
          <Route path="/profile/setup" element={<ProfileSetupPage />} />
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
