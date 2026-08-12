import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '../api/errors'
import { useAuth } from '../auth/AuthContext'
import { getEmailHistory } from '../auth/emailHistory'

function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      await register({ displayName, email, password })
      navigate('/login', { replace: true, state: { registered: true } })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Could not create your account.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="auth-page">
      <p className="eyebrow">Start simply</p>
      <h1>Create your account.</h1>
      <p className="intro">Use an email you already use, including Gmail. This creates a separate GroupSync account.</p>
      <form className="page-panel form-stack" onSubmit={submit}>
        {error && <div className="alert alert-danger" role="alert">{error}</div>}
        <label>Display name<input name="name" autoComplete="name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} minLength={2} maxLength={100} required /></label>
        <label>Email<input type="email" name="email" list="saved-register-emails" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
        <datalist id="saved-register-emails">{getEmailHistory().map((savedEmail) => <option key={savedEmail} value={savedEmail} />)}</datalist>
        <label>Password<input type="password" name="password" autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} minLength={8} maxLength={72} required /></label>
        <p className="auth-help">Choose at least 8 characters. This password belongs to GroupSync; Gmail sign-in/OAuth is not enabled.</p>
        <button className="btn btn-primary" disabled={saving}>{saving ? 'Creating…' : 'Create account'}</button>
      </form>
      <p className="auth-switch">Already registered? <Link to="/login">Sign in</Link></p>
    </section>
  )
}

export default RegisterPage
