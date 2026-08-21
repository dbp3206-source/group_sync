import { Camera, CheckCircle2, KeyRound, Trash2, UserRound, X } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { changePassword, deleteAvatar, getProfile, updateProfile, uploadAvatar } from '../api/profile'
import { getApiErrorMessage } from '../api/errors'
import { useAuth } from '../auth'
import Avatar from '../components/Avatar'

const AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/webp']
const MAX_AVATAR_BYTES = 512 * 1024

type SectionNotice = { kind: 'success' | 'error'; text: string } | null

async function prepareAvatar(sourceFile: File) {
  const image = await createImageBitmap(sourceFile)
  try {
    const side = Math.min(image.width, image.height)
    const startX = Math.floor((image.width - side) / 2)
    const startY = Math.floor((image.height - side) / 2)
    const canvas = document.createElement('canvas')
    canvas.width = 256
    canvas.height = 256
    const context = canvas.getContext('2d')
    if (!context) throw new Error('The image preview could not be prepared.')
    context.drawImage(image, startX, startY, side, side, 0, 0, 256, 256)
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/webp', 0.84))
    if (!blob) throw new Error('The image could not be compressed.')
    return new File([blob], 'knowledgeos-avatar.webp', { type: 'image/webp' })
  } finally {
    image.close()
  }
}

function Notice({ notice }: { notice: SectionNotice }) {
  if (!notice) return null
  return <div className={`profile-notice profile-notice--${notice.kind}`} role={notice.kind === 'error' ? 'alert' : 'status'}>{notice.kind === 'success' && <CheckCircle2 size={15} aria-hidden="true" />}{notice.text}</div>
}

function ProfilePage() {
  const { user, updateCurrentUser } = useAuth()
  const inputRef = useRef<HTMLInputElement>(null)
  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [selectedAvatar, setSelectedAvatar] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState('')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [accountNotice, setAccountNotice] = useState<SectionNotice>(null)
  const [avatarNotice, setAvatarNotice] = useState<SectionNotice>(null)
  const [passwordNotice, setPasswordNotice] = useState<SectionNotice>(null)
  const [savingAccount, setSavingAccount] = useState(false)
  const [savingAvatar, setSavingAvatar] = useState(false)
  const [savingPassword, setSavingPassword] = useState(false)

  useEffect(() => () => { if (previewUrl) URL.revokeObjectURL(previewUrl) }, [previewUrl])
  useEffect(() => setDisplayName(user?.displayName ?? ''), [user?.displayName])

  async function saveAccount(event: React.FormEvent) {
    event.preventDefault()
    setAccountNotice(null)
    const name = displayName.trim()
    if (name.length < 2 || name.length > 100) {
      setAccountNotice({ kind: 'error', text: 'Display name must contain 2 to 100 characters.' })
      return
    }
    setSavingAccount(true)
    try {
      const updated = await updateProfile({ displayName: name, timeZone: user?.timeZone || 'Asia/Ho_Chi_Minh' })
      updateCurrentUser(updated)
      setDisplayName(updated.displayName)
      setAccountNotice({ kind: 'success', text: 'Display name updated.' })
    } catch (requestError) {
      setAccountNotice({ kind: 'error', text: getApiErrorMessage(requestError, 'Display name could not be updated.') })
    } finally {
      setSavingAccount(false)
    }
  }

  async function selectAvatar(file: File | undefined) {
    setAvatarNotice(null)
    if (!file) return
    if (!AVATAR_TYPES.includes(file.type)) {
      setAvatarNotice({ kind: 'error', text: 'Choose a PNG, JPEG, or WebP image.' })
      return
    }
    try {
      const prepared = await prepareAvatar(file)
      if (prepared.size > MAX_AVATAR_BYTES) {
        setAvatarNotice({ kind: 'error', text: 'The prepared avatar is larger than 512 KB. Choose another image.' })
        return
      }
      setSelectedAvatar(prepared)
      setPreviewUrl(URL.createObjectURL(prepared))
    } catch (imageError) {
      setAvatarNotice({ kind: 'error', text: imageError instanceof Error ? imageError.message : 'This image could not be read.' })
    }
  }

  function cancelPreview() {
    setSelectedAvatar(null)
    setPreviewUrl('')
    if (inputRef.current) inputRef.current.value = ''
    setAvatarNotice(null)
  }

  async function saveAvatar() {
    if (!selectedAvatar) return
    setAvatarNotice(null)
    setSavingAvatar(true)
    try {
      const updated = await uploadAvatar(selectedAvatar)
      updateCurrentUser(updated)
      cancelPreview()
      setAvatarNotice({ kind: 'success', text: 'Avatar updated.' })
    } catch (requestError) {
      setAvatarNotice({ kind: 'error', text: getApiErrorMessage(requestError, 'Avatar could not be uploaded.') })
    } finally {
      setSavingAvatar(false)
    }
  }

  async function removeAvatar() {
    if (!user?.avatarUrl || !window.confirm('Remove your current avatar?')) return
    setAvatarNotice(null)
    setSavingAvatar(true)
    try {
      await deleteAvatar()
      updateCurrentUser({ ...user, avatarUrl: null })
      try {
        updateCurrentUser(await getProfile())
      } catch {
        // The confirmed deletion is reflected locally even if the optional refresh is unavailable.
      }
      cancelPreview()
      setAvatarNotice({ kind: 'success', text: 'Avatar removed. Your initials are now shown.' })
    } catch (requestError) {
      setAvatarNotice({ kind: 'error', text: getApiErrorMessage(requestError, 'Avatar could not be removed.') })
    } finally {
      setSavingAvatar(false)
    }
  }

  async function savePassword(event: React.FormEvent) {
    event.preventDefault()
    setPasswordNotice(null)
    if (newPassword.length < 8 || newPassword.length > 72) {
      setPasswordNotice({ kind: 'error', text: 'New password must contain 8 to 72 characters.' })
      return
    }
    if (newPassword !== confirmPassword) {
      setPasswordNotice({ kind: 'error', text: 'New password and confirmation do not match.' })
      return
    }
    setSavingPassword(true)
    try {
      await changePassword(currentPassword, newPassword)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setPasswordNotice({ kind: 'success', text: 'Password changed.' })
    } catch (requestError) {
      setPasswordNotice({ kind: 'error', text: getApiErrorMessage(requestError, 'Password could not be changed.') })
    } finally {
      setSavingPassword(false)
    }
  }

  if (!user) return null

  return <section className="profile-page">
    <header className="profile-header">
      <Avatar className="avatar-fallback--profile" displayName={user.displayName} avatarUrl={user.avatarUrl} decorative={false} />
      <div><p className="eyebrow">ACCOUNT</p><h1>{user.displayName}</h1><p>{user.email}</p></div>
    </header>

    <div className="profile-layout">
      <section className="profile-panel profile-panel--avatar" aria-labelledby="avatar-heading">
        <div className="profile-section-heading"><Camera size={18} aria-hidden="true" /><div><h2 id="avatar-heading">Avatar</h2><p>Optional. Your initials remain available as a fallback.</p></div></div>
        <div className="profile-avatar-editor">
          {previewUrl ? <img className="profile-avatar-preview" src={previewUrl} alt="Selected avatar preview" /> : <Avatar className="avatar-fallback--editor" displayName={user.displayName} avatarUrl={user.avatarUrl} decorative={false} />}
          <div className="profile-avatar-actions">
            <button type="button" className="button button--secondary" onClick={() => inputRef.current?.click()} disabled={savingAvatar}><Camera size={16} aria-hidden="true" />{user.avatarUrl ? 'Change image' : 'Choose image'}</button>
            {user.avatarUrl && !previewUrl && <button type="button" className="profile-text-button profile-text-button--danger" onClick={removeAvatar} disabled={savingAvatar}><Trash2 size={15} aria-hidden="true" />Remove</button>}
          </div>
        </div>
        <input ref={inputRef} className="visually-hidden" type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => { void selectAvatar(event.target.files?.[0]); event.currentTarget.value = '' }} />
        <p className="profile-help">PNG, JPEG, or WebP. The image is cropped to a square and must remain at or below 512 KB.</p>
        {previewUrl && <div className="profile-preview-actions"><button type="button" className="button button--primary" onClick={saveAvatar} disabled={savingAvatar}>{savingAvatar ? 'Uploading...' : 'Use this image'}</button><button type="button" className="button button--secondary" onClick={cancelPreview} disabled={savingAvatar}><X size={15} aria-hidden="true" />Cancel</button></div>}
        <Notice notice={avatarNotice} />
      </section>

      <form className="profile-panel form-stack" onSubmit={saveAccount} aria-labelledby="account-heading">
        <div className="profile-section-heading"><UserRound size={18} aria-hidden="true" /><div><h2 id="account-heading">Account</h2><p>The name shown across KnowledgeOS.</p></div></div>
        <label htmlFor="profile-name">Display name<input id="profile-name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} minLength={2} maxLength={100} autoComplete="name" required /></label>
        <div className="profile-readonly-field"><span>Email</span><strong>{user.email}</strong><small>Email changes are not available in this account surface.</small></div>
        <button className="button button--primary profile-submit" disabled={savingAccount}>{savingAccount ? 'Saving...' : 'Save display name'}</button>
        <Notice notice={accountNotice} />
      </form>

      <form className="profile-panel profile-panel--security form-stack" onSubmit={savePassword} aria-labelledby="security-heading">
        <div className="profile-section-heading"><KeyRound size={18} aria-hidden="true" /><div><h2 id="security-heading">Password</h2><p>Use 8 to 72 characters and keep it private.</p></div></div>
        <div className="profile-password-grid">
          <label htmlFor="current-password">Current password<input id="current-password" type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} required /></label>
          <label htmlFor="new-password">New password<input id="new-password" type="password" autoComplete="new-password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} minLength={8} maxLength={72} required /></label>
          <label htmlFor="confirm-password">Confirm new password<input id="confirm-password" type="password" autoComplete="new-password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} minLength={8} maxLength={72} required /></label>
        </div>
        <button className="button button--secondary profile-submit" disabled={savingPassword}>{savingPassword ? 'Changing...' : 'Change password'}</button>
        <Notice notice={passwordNotice} />
      </form>
    </div>
  </section>
}

export default ProfilePage
