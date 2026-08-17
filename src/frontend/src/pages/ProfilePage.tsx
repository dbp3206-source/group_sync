import { useState } from 'react'
import { changePassword, updateProfile } from '../api/profile'
import { getApiErrorMessage } from '../api/errors'
import { useAuth } from '../auth/AuthContext'
import Avatar from '../components/Avatar'

function ProfilePage() {
  const { user, updateCurrentUser } = useAuth()
  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [timeZone, setTimeZone] = useState(user?.timeZone ?? 'Asia/Ho_Chi_Minh')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [savingProfile, setSavingProfile] = useState(false)
  const [savingPassword, setSavingPassword] = useState(false)

  async function saveProfile(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    setMessage('')
    setSavingProfile(true)
    try {
      updateCurrentUser(await updateProfile({ displayName, timeZone }))
      setMessage('Thông tin hồ sơ đã được lưu.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể lưu hồ sơ.'))
    } finally {
      setSavingProfile(false)
    }
  }

  async function savePassword(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    setMessage('')
    if (newPassword.length < 8) {
      setError('Mật khẩu mới cần có ít nhất 8 ký tự.')
      return
    }
    setSavingPassword(true)
    try {
      await changePassword(currentPassword, newPassword)
      setCurrentPassword('')
      setNewPassword('')
      setMessage('Mật khẩu đã được đổi.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể đổi mật khẩu.'))
    } finally {
      setSavingPassword(false)
    }
  }

  return <section className="profile-page">
    <header className="page-heading"><div><p className="eyebrow">HỒ SƠ CÁ NHÂN</p><h1>Tài khoản của bạn</h1><p className="intro">Cập nhật thông tin hiển thị và các thiết lập bảo mật cơ bản.</p></div></header>
    {(message || error) && <div className={`status-card ${error ? 'status-card--error' : 'status-card--success'}`} role={error ? 'alert' : 'status'}>{error || message}</div>}
    <div className="profile-grid">
      <form className="profile-panel form-stack" onSubmit={saveProfile}>
        <div className="profile-identity"><Avatar className="avatar-fallback--large" displayName={user?.displayName ?? ''} avatarUrl={user?.avatarUrl} /><div><h2>Thông tin hiển thị</h2><p>{user?.email}</p></div></div>
        <label htmlFor="profile-name">Tên hiển thị<input id="profile-name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} minLength={2} maxLength={100} required /></label>
        <label htmlFor="profile-zone">Múi giờ<select id="profile-zone" value={timeZone} onChange={(event) => setTimeZone(event.target.value)}><option value="Asia/Ho_Chi_Minh">Việt Nam (GMT+7)</option><option value="Asia/Bangkok">Bangkok (GMT+7)</option><option value="Asia/Singapore">Singapore (GMT+8)</option></select></label>
        <button className="button button--primary" disabled={savingProfile}>{savingProfile ? 'Đang lưu…' : 'Lưu thay đổi'}</button>
      </form>
      <form className="profile-panel form-stack" onSubmit={savePassword}>
        <div><p className="eyebrow">BẢO MẬT</p><h2>Đổi mật khẩu</h2><p className="auth-copy">Dùng ít nhất 8 ký tự và không chia sẻ mật khẩu với người khác.</p></div>
        <label htmlFor="current-password">Mật khẩu hiện tại<input id="current-password" type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} required /></label>
        <label htmlFor="new-password">Mật khẩu mới<input id="new-password" type="password" autoComplete="new-password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} minLength={8} maxLength={72} required /></label>
        <button className="button button--secondary" disabled={savingPassword}>{savingPassword ? 'Đang đổi…' : 'Đổi mật khẩu'}</button>
      </form>
    </div>
  </section>
}

export default ProfilePage
