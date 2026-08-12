import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { checkInWithToken } from '../api/badminton'
import { getApiErrorMessage } from '../api/errors'

function CheckinPage() {
  const [params] = useSearchParams(); const [token, setToken] = useState(params.get('token') ?? ''); const [message, setMessage] = useState(''); const [error, setError] = useState('')
  async function submit(event: React.FormEvent) { event.preventDefault(); setError(''); setMessage(''); try { const result = await checkInWithToken(token.trim()); setMessage(`${result.sessionTitle}: ${result.alreadyCheckedIn ? 'already checked in' : 'checked in successfully'} (${result.status}).`) } catch (e) { setError(getApiErrorMessage(e, 'Could not check in.')) } }
  return <section className="narrow-page"><div className="page-heading"><div><p className="eyebrow">Badminton check-in</p><h1>Check in to session</h1><p className="intro">Open the organizer’s QR/token link while logged in. The server validates the session and your registration.</p></div></div>{error && <div className="alert alert-danger">{error}</div>}{message && <div className="alert alert-success">{message}</div>}<form className="page-panel form-stack" onSubmit={submit}><label>QR/token<input value={token} onChange={(e) => setToken(e.target.value)} required autoFocus /></label><button className="btn btn-primary">Check in</button></form></section>
}
export default CheckinPage
