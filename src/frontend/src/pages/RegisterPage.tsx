import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '../api/errors'
import { useAuth } from '../auth'
import { getEmailHistory } from '../auth/emailHistory'

function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    if (password !== confirmPassword) {
      setError('Mật khẩu xác nhận chưa khớp.')
      return
    }
    setSaving(true)
    try {
      await register({ displayName, email, password })
      navigate('/dashboard', { replace: true })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể tạo tài khoản. Vui lòng thử lại.'))
    } finally {
      setSaving(false)
    }
  }

  return <section className="auth-layout auth-layout--register">
    <div className="auth-intro-block">
      <p className="eyebrow">PERSONAL KNOWLEDGE INTELLIGENCE</p>
      <h1>Make room for what you are learning.</h1>
      <p className="intro">Collect your sources, find evidence later, and keep the next useful thing close.</p>
    </div>
    <div className="auth-card-wrap">
      <div className="auth-page">
        <p className="eyebrow">CREATE ACCOUNT</p>
        <h2>Start with KnowledgeOS</h2>
        <p className="auth-copy">Create your personal space for the things you are learning.</p>
        <form className="page-panel form-stack" onSubmit={submit}>
          {error && <div className="status-card status-card--error" role="alert">{error}</div>}
          <label htmlFor="register-name">Tên hiển thị<input id="register-name" name="name" autoComplete="name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} minLength={2} maxLength={100} required /></label>
          <label htmlFor="register-email">Email<input id="register-email" type="email" name="email" list="saved-register-emails" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
          <datalist id="saved-register-emails">{getEmailHistory().map((savedEmail) => <option key={savedEmail} value={savedEmail} />)}</datalist>
          <label htmlFor="register-password">Mật khẩu<span className="password-field"><input id="register-password" type={showPassword ? 'text' : 'password'} name="password" autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} minLength={8} maxLength={72} required /><button type="button" className="password-toggle" onClick={() => setShowPassword((visible) => !visible)}>{showPassword ? 'Ẩn' : 'Hiện'}</button></span></label>
          <label htmlFor="register-confirm-password">Xác nhận mật khẩu<input id="register-confirm-password" type={showPassword ? 'text' : 'password'} name="confirmPassword" autoComplete="new-password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} minLength={8} maxLength={72} required /></label>
          <p className="auth-help">At least 8 characters. This password is used only for your KnowledgeOS account.</p>
          <button className="button button--primary" disabled={saving}>{saving ? 'Đang tạo tài khoản…' : 'Tạo tài khoản'}</button>
        </form>
        <p className="auth-switch">Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>
      </div>
    </div>
  </section>
}

export default RegisterPage
