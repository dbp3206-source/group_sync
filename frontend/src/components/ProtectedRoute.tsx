import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

function ProtectedRoute() {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) return <div className="page-panel">Đang mở không gian của bạn…</div>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (!user.profileCompleted && location.pathname !== '/profile/setup') return <Navigate to="/profile/setup" replace />
  return <Outlet />
}

export default ProtectedRoute
