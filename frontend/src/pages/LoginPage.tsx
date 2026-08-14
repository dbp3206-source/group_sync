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

  function useDemoAccount() {
    setEmail(DEMO_EMAIL)
    setPassword('DemoOnly-GroupSync-2026!')
    setError('')
  }

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
      <p className="eyebrow">LỊCH CÁ NHÂN · HOẠT ĐỘNG NHÓM</p>
      <h1>Mọi người gặp nhau đúng lúc.</h1>
      <p className="intro">Theo dõi lịch riêng, tìm khung giờ chung và tổ chức buổi học hoặc đánh cầu lông trong một nơi rõ ràng.</p>
      <div className="auth-value-list" aria-label="GroupSync giúp bạn">
        <span><b>01</b> Lịch riêng không bị lộ nội dung</span>
        <span><b>02</b> Tìm thời gian rảnh của cả nhóm</span>
        <span><b>03</b> Theo dõi buổi chơi và kết quả</span>
      </div>
    </div>
    <div className="auth-card-wrap">
      <div className="auth-page">
        <p className="eyebrow">Chào mừng trở lại</p>
        <h2>Đăng nhập</h2>
        <p className="auth-copy">Dùng email và mật khẩu GroupSync của bạn.</p>
        <form className="page-panel form-stack" onSubmit={submit}>
          {registered && <div className="status-card status-card--success" role="status">Tài khoản đã được tạo. Bạn có thể đăng nhập ngay.</div>}
          {error && <div className="status-card status-card--error" role="alert">{error}</div>}
          <label htmlFor="login-email">Email<input id="login-email" type="email" name="email" list="saved-emails" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
          <datalist id="saved-emails">{getEmailHistory().map((savedEmail) => <option key={savedEmail} value={savedEmail} />)}<option value={DEMO_EMAIL} /></datalist>
          <label htmlFor="login-password">Mật khẩu<span className="password-field"><input id="login-password" type={showPassword ? 'text' : 'password'} name="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required /><button type="button" className="password-toggle" onClick={() => setShowPassword((visible) => !visible)}>{showPassword ? 'Ẩn' : 'Hiện'}</button></span></label>
          <p className="auth-help">GroupSync sử dụng mật khẩu riêng và không truy cập mật khẩu Gmail của bạn.</p>
          {import.meta.env.DEV && <button type="button" className="button button--secondary" onClick={useDemoAccount}>Dùng tài khoản demo</button>}
          <button className="button button--primary" disabled={saving}>{saving ? 'Đang đăng nhập…' : 'Đăng nhập'}</button>
        </form>
        <p className="auth-switch">Chưa có tài khoản? <Link to="/register">Tạo tài khoản</Link></p>
      </div>
    </div>
  </section>
}

export default LoginPage
