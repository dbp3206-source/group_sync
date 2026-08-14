import { useEffect, useState } from 'react'

type AvatarProps = {
  displayName: string
  avatarUrl?: string
  className?: string
}

function Avatar({ displayName, avatarUrl, className = '' }: AvatarProps) {
  const [showImage, setShowImage] = useState(Boolean(avatarUrl))
  useEffect(() => setShowImage(Boolean(avatarUrl)), [avatarUrl])
  const initial = displayName.trim().slice(0, 1).toUpperCase() || '?'
  return <span className={`avatar-fallback ${className}`} aria-hidden="true">
    {showImage && avatarUrl ? <img src={avatarUrl} alt="" onError={() => setShowImage(false)} /> : initial}
  </span>
}

export default Avatar
