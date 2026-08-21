export function getInitials(displayName: string) {
  const words = displayName.trim().split(/\s+/u).filter(Boolean)
  if (!words.length) return '?'
  if (words.length > 1) return `${Array.from(words[0])[0] || ''}${Array.from(words[1])[0] || ''}`.toLocaleUpperCase()
  return Array.from(words[0]).slice(0, 2).join('').toLocaleUpperCase() || '?'
}
