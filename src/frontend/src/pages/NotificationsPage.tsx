import { useEffect, useState } from 'react'
import { getApiErrorMessage } from '../api/errors'
import { getNotifications, markNotificationRead, type Notification } from '../api/notifications'

function NotificationsPage() {
  const [items, setItems] = useState<Notification[]>([])
  const [error, setError] = useState('')

  async function refresh() {
    setItems(await getNotifications())
  }

  useEffect(() => { refresh().catch((e) => setError(getApiErrorMessage(e, 'Không thể tải thông báo.'))) }, [])

  async function markRead(item: Notification) {
    if (item.read) return
    try {
      await markNotificationRead(item.id)
      setItems((current) => current.map((entry) => entry.id === item.id ? { ...entry, read: true } : entry))
    } catch (e) { setError(getApiErrorMessage(e, 'Không thể cập nhật thông báo.')) }
  }

  return <section>
    <div className="page-heading"><div><p className="eyebrow">HỘP THƯ</p><h1>Thông báo</h1></div><span className="subtle">{items.filter((item) => !item.read).length} chưa đọc</span></div>
    {error && <div className="alert alert-danger">{error}</div>}
    <div className="notification-list">
      {items.length === 0 && <div className="page-panel empty-state">Chưa có thông báo mới.</div>}
      {items.map((item) => <button className={`page-panel notification-row ${item.read ? '' : 'notification-unread'}`} key={item.id} onClick={() => markRead(item)}>
        <span className="notification-dot" aria-hidden="true" />
        <span><strong>{item.title}</strong><span>{item.message}</span><small>{new Date(item.createdAt).toLocaleString('vi-VN')} · {item.read ? 'Đã đọc' : 'Chưa đọc'}</small></span>
      </button>)}
    </div>
  </section>
}

export default NotificationsPage
