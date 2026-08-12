import { useEffect, useState } from 'react'
import { getApiErrorMessage } from '../api/errors'
import { getNotifications, markNotificationRead, type Notification } from '../api/notifications'

function NotificationsPage() {
  const [items, setItems] = useState<Notification[]>([])
  const [error, setError] = useState('')

  async function refresh() {
    setItems(await getNotifications())
  }

  useEffect(() => { refresh().catch((e) => setError(getApiErrorMessage(e, 'Could not load notifications.'))) }, [])

  async function markRead(item: Notification) {
    if (item.read) return
    try {
      await markNotificationRead(item.id)
      setItems((current) => current.map((entry) => entry.id === item.id ? { ...entry, read: true } : entry))
    } catch (e) { setError(getApiErrorMessage(e, 'Could not update notification.')) }
  }

  return <section>
    <div className="page-heading"><div><p className="eyebrow">Inbox</p><h1>Notifications</h1></div><span className="subtle">{items.filter((item) => !item.read).length} unread</span></div>
    {error && <div className="alert alert-danger">{error}</div>}
    <div className="notification-list">
      {items.length === 0 && <div className="page-panel empty-state">No notifications yet.</div>}
      {items.map((item) => <button className={`page-panel notification-row ${item.read ? '' : 'notification-unread'}`} key={item.id} onClick={() => markRead(item)}>
        <span className="notification-dot" aria-hidden="true" />
        <span><strong>{item.title}</strong><span>{item.message}</span><small>{new Date(item.createdAt).toLocaleString()} · {item.read ? 'Read' : 'Unread'}</small></span>
      </button>)}
    </div>
  </section>
}

export default NotificationsPage
