import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '../api/errors'
import { useAuth } from '../auth/AuthContext'
import { DEMO_EMAIL, getEmailHistory } from '../auth/emailHistory'

function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState(() => getEmailHistory()[0] ?? '')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const registered = (location.state as { registered?: boolean } | null)?.registered

  function useDemoAccount() { setEmail(DEMO_EMAIL); setPassword('DemoOnly-GroupSync-2026!'); setError('') }

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      await login(email, password)
      const from = (location.state as { from?: string } | null)?.from ?? '/dashboard'
      navigate(from, { replace: true })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể đăng nhập. Vui lòng thử lại.'))
    } finally {
      setSaving(false)
    }
  }

  return <section className="auth-layout">
      <div className="auth-intro-block">
      <p className="eyebrow">PERSONAL KNOWLEDGE INTELLIGENCE</p>
      <h1>A place for what you are learning.</h1>
      <p className="intro">Keep your resources connected, retrieve the evidence later, and return to the work that deserves your attention.</p>
      <div className="auth-value-list" aria-label="GroupSync giúp bạn">
        <span><b>01</b> Keep your sources in one personal library</span>
        <span><b>02</b> Ask questions grounded in stored evidence</span>
        <span><b>03</b> Decide what to study next</span>
      </div>
    </div>
    <div className="auth-card-wrap">
      <div className="auth-page">
        <p className="eyebrow">Chào mừng trở lại</p>
        <h2>Đăng nhập</h2>
        <p className="auth-copy">Use your KnowledgeOS email and password.</p>
        <form className="page-panel form-stack" onSubmit={submit}>
          {registered && <div className="status-card status-card--success" role="status">Tài khoản đã được tạo. Bạn có thể đăng nhập ngay.</div>}
          {error && <div className="status-card status-card--error" role="alert">{error}</div>}
          <label htmlFor="login-email">Email<input id="login-email" type="email" name="email" list="saved-emails" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
          <datalist id="saved-emails">{getEmailHistory().map((savedEmail) => <option key={savedEmail} value={savedEmail} />)}<option value={DEMO_EMAIL} /></datalist>
          <label htmlFor="login-password">Mật khẩu<span className="password-field"><input id="login-password" type={showPassword ? 'text' : 'password'} name="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required /><button type="button" className="password-toggle" onClick={() => setShowPassword((visible) => !visible)}>{showPassword ? 'Ẩn' : 'Hiện'}</button></span></label>
          <p className="auth-help">KnowledgeOS uses its own password and never reads your email password.</p>
          {import.meta.env.DEV && <button type="button" className="button button--secondary" onClick={useDemoAccount}>Dùng tài khoản demo</button>}
          <button className="button button--primary" disabled={saving}>{saving ? 'Đang kết nối & đăng nhập…' : 'Đăng nhập'}</button>
          {saving && (
            <p style={{ fontSize: '0.82rem', color: 'var(--kos-muted)', textAlign: 'center', margin: '0.5rem 0 0' }}>
              ⏳ Đang xác thực với máy chủ... (Nếu máy chủ đang khởi động lại từ chế độ ngủ, quá trình có thể mất ~30 giây)
            </p>
          )}
        </form>
        <p className="auth-switch">Chưa có tài khoản? <Link to="/register">Tạo tài khoản</Link></p>
      </div>
    </div>
  </section>
}

export default LoginPage
