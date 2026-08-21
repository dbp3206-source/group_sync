import { useEffect, useState } from 'react'
import { getInitials } from '../utils/avatar'

type AvatarProps = {
  displayName: string
  avatarUrl?: string | null
  className?: string
  decorative?: boolean
}

function Avatar({ displayName, avatarUrl, className = '', decorative = true }: AvatarProps) {
  const [showImage, setShowImage] = useState(Boolean(avatarUrl))
  useEffect(() => setShowImage(Boolean(avatarUrl)), [avatarUrl])
  const label = displayName.trim() ? `${displayName.trim()} avatar` : 'Account avatar'
  const fallbackVisible = !showImage || !avatarUrl
  return <span className={`avatar-fallback ${className}`} aria-hidden={decorative || undefined} role={!decorative && fallbackVisible ? 'img' : undefined} aria-label={!decorative && fallbackVisible ? label : undefined}>
    {showImage && avatarUrl ? <img src={avatarUrl} alt={decorative ? '' : label} onError={() => setShowImage(false)} /> : getInitials(displayName)}
  </span>
}

export default Avatar
