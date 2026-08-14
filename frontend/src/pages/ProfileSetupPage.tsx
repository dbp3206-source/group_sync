import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '../api/errors'
import { updateProfile, uploadAvatar } from '../api/profile'
import { useAuth } from '../auth/AuthContext'

async function cropAvatar(sourceFile: File) {
  const image = await createImageBitmap(sourceFile)
  const side = Math.min(image.width, image.height)
  const startX = Math.floor((image.width - side) / 2)
  const startY = Math.floor((image.height - side) / 2)
  const canvas = document.createElement('canvas')
  canvas.width = 256
  canvas.height = 256
  const context = canvas.getContext('2d')
  if (!context) throw new Error('Không thể chuẩn bị ảnh đại diện.')
  context.drawImage(image, startX, startY, side, side, 0, 0, 256, 256)
  image.close()
  const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/webp', 0.84))
  if (!blob) throw new Error('Không thể nén ảnh đại diện.')
  return new File([blob], 'groupsync-avatar.webp', { type: 'image/webp' })
}

function ProfileSetupPage() {
  const { user, updateCurrentUser } = useAuth()
  const navigate = useNavigate()
  const inputRef = useRef<HTMLInputElement>(null)
  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [timeZone, setTimeZone] = useState(user?.timeZone ?? 'Asia/Ho_Chi_Minh')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  async function selectAvatar(file: File | undefined) {
    setError('')
    if (!file) return
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      setError('Hãy chọn ảnh PNG, JPEG hoặc WebP.')
      return
    }
    try {
      const cropped = await cropAvatar(file)
      if (cropped.size > 512 * 1024) {
        setError('Ảnh sau khi nén vẫn quá 512 KB. Hãy chọn ảnh khác.')
        return
      }
      setSelectedFile(cropped)
      setPreviewUrl(URL.createObjectURL(cropped))
    } catch (imageError) {
      setError(imageError instanceof Error ? imageError.message : 'Không thể đọc ảnh này.')
    }
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setError('')
    if (!selectedFile) {
      setError('Ảnh đại diện là bước bắt buộc để hoàn tất hồ sơ.')
      return
    }
    setSaving(true)
    try {
      await updateProfile({ displayName, timeZone })
      const updatedUser = await uploadAvatar(selectedFile)
      updateCurrentUser(updatedUser)
      navigate('/dashboard', { replace: true })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể lưu hồ sơ. Vui lòng thử lại.'))
    } finally {
      setSaving(false)
    }
  }

  return <section className="profile-setup-page">
    <div className="profile-setup-copy"><p className="eyebrow">BƯỚC CUỐI CÙNG</p><h1>Tạo dấu hiệu nhận biết của bạn.</h1><p className="intro">Ảnh đại diện giúp thành viên nhận ra bạn trong lịch nhóm, danh sách đăng ký và kết quả trận đấu.</p></div>
    <form className="profile-setup-card form-stack" onSubmit={submit}>
      {error && <div className="status-card status-card--error" role="alert">{error}</div>}
      <button type="button" className="avatar-upload" onClick={() => inputRef.current?.click()} aria-label="Chọn ảnh đại diện">
        {previewUrl ? <img src={previewUrl} alt="Ảnh đại diện đã chọn" /> : <span>{displayName.slice(0, 1).toUpperCase() || '?'}</span>}
        <b>Chọn ảnh</b>
      </button>
      <input ref={inputRef} className="visually-hidden" type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => selectAvatar(event.target.files?.[0])} />
      <p className="avatar-help">Ảnh được cắt vuông và nén trước khi lưu. PNG, JPEG hoặc WebP; tối đa 512 KB.</p>
      <label htmlFor="setup-name">Tên hiển thị<input id="setup-name" value={displayName} onChange={(event) => setDisplayName(event.target.value)} minLength={2} maxLength={100} required /></label>
      <label htmlFor="setup-time-zone">Múi giờ<select id="setup-time-zone" value={timeZone} onChange={(event) => setTimeZone(event.target.value)}><option value="Asia/Ho_Chi_Minh">Việt Nam (GMT+7)</option><option value="Asia/Bangkok">Bangkok (GMT+7)</option><option value="Asia/Singapore">Singapore (GMT+8)</option></select></label>
      <button className="button button--primary" disabled={saving}>{saving ? 'Đang hoàn tất…' : 'Hoàn tất hồ sơ'}</button>
    </form>
  </section>
}

export default ProfileSetupPage
