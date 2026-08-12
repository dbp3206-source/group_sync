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
      const from = (location.state as { from?: string } | null)?.from ?? '/groups'
      navigate(from, { replace: true })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Could not sign in.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="auth-page">
      <p className="eyebrow">GroupSync account</p>
      <h1>Welcome back.</h1>
      <p className="intro">Sign in with the email and password used for your GroupSync account.</p>
      <form className="page-panel form-stack" onSubmit={submit}>
        {registered && <div className="alert alert-success" role="status">Account created. You can sign in now.</div>}
        {error && <div className="alert alert-danger" role="alert">{error}</div>}
        <label>Email<input type="email" name="email" list="saved-emails" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
        <datalist id="saved-emails">{getEmailHistory().map((savedEmail) => <option key={savedEmail} value={savedEmail} />)}<option value={DEMO_EMAIL} /></datalist>
        <label>Password<input type="password" name="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required /></label>
        <p className="auth-help">Your browser may offer saved email accounts automatically. GroupSync uses its own local password; it does not read or use your Gmail password.</p>
        <button type="button" className="btn btn-outline-secondary" onClick={useDemoAccount}>Use demo account</button>
        <button className="btn btn-primary" disabled={saving}>{saving ? 'Signing in…' : 'Sign in'}</button>
      </form>
      <p className="auth-switch">New here? <Link to="/register">Create an account</Link></p>
    </section>
  )
}

export default LoginPage
