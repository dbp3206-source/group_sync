import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '../api/errors'
import { useAuth } from '../auth/AuthContext'

function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

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
      <p className="intro">Sign in to manage your groups and invitations.</p>
      <form className="page-panel form-stack" onSubmit={submit}>
        {error && <div className="alert alert-danger" role="alert">{error}</div>}
        <label>Email<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
        <label>Password<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required /></label>
        <button className="btn btn-primary" disabled={saving}>{saving ? 'Signing in…' : 'Sign in'}</button>
      </form>
      <p className="auth-switch">New here? <Link to="/register">Create an account</Link></p>
    </section>
  )
}

export default LoginPage
