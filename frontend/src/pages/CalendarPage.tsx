import { useEffect, useMemo, useState } from 'react'
import FullCalendar from '@fullcalendar/react'
import timeGridPlugin from '@fullcalendar/timegrid'
import dayGridPlugin from '@fullcalendar/daygrid'
import interactionPlugin, { type DateClickArg, type EventResizeDoneArg } from '@fullcalendar/interaction'
import type { EventClickArg, EventDropArg, EventInput } from '@fullcalendar/core'
import viLocale from '@fullcalendar/core/locales/vi'
import { createBusyEvent, createRecurringSchedule, deleteBusyEvent, deleteRecurringSchedule, getBusyEvents, getCalendarItems, getRecurringSchedules, updateBusyEvent, type BusyEvent, type CalendarItem, type WeeklySchedule } from '../api/calendar'
import { getApiErrorMessage } from '../api/errors'
import { useAuth } from '../auth/AuthContext'

const weekdays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const weekdayLabels: Record<string, string> = { MONDAY: 'T2', TUESDAY: 'T3', WEDNESDAY: 'T4', THURSDAY: 'T5', FRIDAY: 'T6', SATURDAY: 'T7', SUNDAY: 'CN' }

function toLocalInput(value: string) {
  const date = new Date(value)
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}
function localIso(value: string) { return new Date(value).toISOString() }
function dateOnly(date: Date) { const pad = (part: number) => String(part).padStart(2, '0'); return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` }

function CalendarPage() {
  const { user } = useAuth()
  const [items, setItems] = useState<CalendarItem[]>([])
  const [manualEvents, setManualEvents] = useState<BusyEvent[]>([])
  const [schedules, setSchedules] = useState<WeeklySchedule[]>([])
  const [range, setRange] = useState({ from: '', to: '' })
  const [mode, setMode] = useState<'event' | 'schedule' | null>(null)
  const [editingEvent, setEditingEvent] = useState<BusyEvent | null>(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)
  const [eventTitle, setEventTitle] = useState('')
  const [eventStart, setEventStart] = useState('')
  const [eventEnd, setEventEnd] = useState('')
  const [eventDescription, setEventDescription] = useState('')
  const [eventCategory, setEventCategory] = useState('PERSONAL')
  const [eventLocation, setEventLocation] = useState('')
  const [scheduleTitle, setScheduleTitle] = useState('')
  const [scheduleDays, setScheduleDays] = useState<string[]>(['MONDAY'])
  const [scheduleStart, setScheduleStart] = useState('09:00')
  const [scheduleEnd, setScheduleEnd] = useState('10:00')
  const [validFrom, setValidFrom] = useState(dateOnly(new Date()))
  const [validUntil, setValidUntil] = useState(dateOnly(new Date(Date.now() + 90 * 86400000)))

  async function refresh(from = range.from, to = range.to) {
    if (!from || !to) return
    const [nextItems, nextManual, nextSchedules] = await Promise.all([
      getCalendarItems(`${from}T00:00:00+07:00`, `${to}T23:59:59+07:00`), getBusyEvents(), getRecurringSchedules(),
    ])
    setItems(nextItems); setManualEvents(nextManual); setSchedules(nextSchedules)
  }
  useEffect(() => { if (range.from && range.to) refresh().catch((requestError) => setError(getApiErrorMessage(requestError, 'Không thể tải lịch.'))) }, [range])

  const calendarEvents = useMemo<EventInput[]>(() => items.map((item) => ({
    id: `${item.sourceType}-${item.sourceId}-${item.start}`,
    title: item.title,
    start: item.start,
    end: item.end,
    editable: item.sourceType === 'MANUAL',
    classNames: [`calendar-event--${item.sourceType.toLowerCase()}`],
    extendedProps: { sourceType: item.sourceType, sourceId: item.sourceId },
  })), [items])

  function clearEventForm() { setEditingEvent(null); setEventTitle(''); setEventStart(''); setEventEnd(''); setEventDescription(''); setEventCategory('PERSONAL'); setEventLocation('') }
  function openEvent(start?: Date, end?: Date) {
    clearEventForm()
    if (start) { setEventStart(toLocalInput(start.toISOString())); setEventEnd(toLocalInput((end ?? new Date(start.getTime() + 60 * 60000)).toISOString())) }
    setMode('event')
  }
  function editEvent(event: BusyEvent) {
    setEditingEvent(event); setEventTitle(event.title); setEventStart(toLocalInput(event.start)); setEventEnd(toLocalInput(event.end)); setEventDescription(event.description ?? ''); setEventCategory(event.category ?? 'PERSONAL'); setEventLocation(event.location ?? ''); setMode('event')
  }
  function onDateClick(arg: DateClickArg) { openEvent(arg.date, new Date(arg.date.getTime() + 60 * 60000)) }
  function onEventClick(arg: EventClickArg) {
    if (arg.event.extendedProps.sourceType !== 'MANUAL') { setMessage('Hoạt động này được sinh từ nhóm hoặc lịch lặp lại. Hãy chỉnh tại nguồn của hoạt động.'); return }
    const event = manualEvents.find((entry) => entry.id === arg.event.extendedProps.sourceId)
    if (event) editEvent(event)
  }
  async function onEventDrop(arg: EventDropArg | EventResizeDoneArg) {
    const event = manualEvents.find((entry) => entry.id === arg.event.extendedProps.sourceId)
    if (!event || !arg.event.start || !arg.event.end) return
    setError(''); setMessage('')
    try {
      await updateBusyEvent(event.id, { title: event.title, start: arg.event.start.toISOString(), end: arg.event.end.toISOString(), description: event.description ?? undefined, category: event.category ?? 'PERSONAL', location: event.location ?? undefined, visibility: 'PRIVATE', reminderMinutes: event.reminderMinutes ?? undefined })
      setMessage('Đã cập nhật thời gian sự kiện.'); await refresh()
    } catch (requestError) { arg.revert(); setError(getApiErrorMessage(requestError, 'Không thể thay đổi thời gian sự kiện.')) }
  }
  async function saveEvent(event: React.FormEvent) {
    event.preventDefault(); setError(''); setMessage(''); setSaving(true)
    const payload = { title: eventTitle, start: localIso(eventStart), end: localIso(eventEnd), description: eventDescription || undefined, category: eventCategory || undefined, location: eventLocation || undefined, visibility: 'PRIVATE', reminderMinutes: undefined }
    try {
      if (editingEvent) await updateBusyEvent(editingEvent.id, payload); else await createBusyEvent(payload)
      setMessage(editingEvent ? 'Đã lưu thay đổi sự kiện.' : 'Đã thêm lịch cá nhân.'); setMode(null); clearEventForm(); await refresh()
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể lưu sự kiện.')) } finally { setSaving(false) }
  }
  async function saveSchedule(event: React.FormEvent) {
    event.preventDefault(); setError(''); setMessage(''); setSaving(true)
    try {
      await createRecurringSchedule({ title: scheduleTitle, weekdays: scheduleDays, startTime: `${scheduleStart}:00`, endTime: `${scheduleEnd}:00`, validFrom, validUntil, timezone: user?.timeZone ?? 'Asia/Ho_Chi_Minh', description: undefined, category: 'RECURRING', location: undefined, visibility: 'PRIVATE', reminderMinutes: undefined, frequency: 'WEEKLY' })
      setScheduleTitle(''); setMessage('Đã thêm lịch lặp lại.'); setMode(null); await refresh()
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể thêm lịch lặp lại.')) } finally { setSaving(false) }
  }
  async function removeEvent(id: number) { if (!confirm('Xóa lịch cá nhân này?')) return; try { await deleteBusyEvent(id); setMessage('Đã xóa lịch cá nhân.'); await refresh() } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể xóa sự kiện.')) } }
  async function removeSchedule(id: number) { if (!confirm('Xóa lịch lặp lại này?')) return; try { await deleteRecurringSchedule(id); setMessage('Đã xóa lịch lặp lại.'); await refresh() } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không thể xóa lịch lặp lại.')) } }

  return <section className="schedule-page">
    <header className="schedule-header"><div><p className="eyebrow">LỊCH CỦA TÔI</p><h1>Giữ nhịp của bạn.</h1><p>Lịch riêng chỉ hiển thị trạng thái bận/rảnh cho nhóm. Nội dung và ghi chú của bạn vẫn là riêng tư.</p></div><div className="schedule-actions"><button className="button button--secondary" onClick={() => setMode('schedule')}>Thêm lịch lặp lại</button><button className="button button--primary" onClick={() => openEvent()}>Thêm lịch cá nhân</button></div></header>
    {(error || message) && <div className={`status-card ${error ? 'status-card--error' : 'status-card--success'}`} role={error ? 'alert' : 'status'}>{error || message}</div>}
    <div className="calendar-legend"><span><b className="legend-dot legend-dot--manual"/> Lịch cá nhân</span><span><b className="legend-dot legend-dot--recurring"/> Lịch lặp lại</span><span><b className="legend-dot legend-dot--study"/> Buổi học</span><span><b className="legend-dot legend-dot--badminton"/> Cầu lông</span></div>
    <div className="calendar-surface"><FullCalendar plugins={[timeGridPlugin, dayGridPlugin, interactionPlugin]} initialView="timeGridWeek" firstDay={1} locale={viLocale} allDaySlot={false} slotMinTime="06:00:00" slotMaxTime="23:00:00" height="auto" headerToolbar={{ left: 'prev,next today', center: 'title', right: 'timeGridWeek,timeGridDay,dayGridMonth' }} buttonText={{ today: 'Hôm nay', week: 'Tuần', day: 'Ngày', month: 'Tháng' }} events={calendarEvents} editable eventClick={onEventClick} eventDrop={onEventDrop} eventResize={onEventDrop} dateClick={onDateClick} datesSet={(arg) => setRange({ from: dateOnly(arg.start), to: dateOnly(new Date(arg.end.getTime() - 86400000)) })} /></div>
    <div className="schedule-sources"><article><div className="panel-heading"><div><p className="eyebrow">LỊCH CÁ NHÂN</p><h2>Đã lưu</h2></div><span>{manualEvents.length}</span></div>{manualEvents.length ? <div className="source-list">{manualEvents.map((event) => <div key={event.id}><button onClick={() => editEvent(event)}><strong>{event.title}</strong><span>{new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(event.start))}</span></button><button className="source-delete" onClick={() => removeEvent(event.id)} aria-label={`Xóa ${event.title}`}>×</button></div>)}</div> : <p>Chưa có lịch cá nhân nào.</p>}</article><article><div className="panel-heading"><div><p className="eyebrow">LỊCH LẶP LẠI</p><h2>Đầu vào cố định</h2></div><span>{schedules.length}</span></div>{schedules.length ? <div className="source-list">{schedules.map((schedule) => <div key={schedule.id}><span><strong>{schedule.title}</strong><span>{schedule.weekdays.map((day) => weekdayLabels[day]).join(' · ')} · {schedule.startTime.slice(0, 5)}–{schedule.endTime.slice(0, 5)}</span></span><button className="source-delete" onClick={() => removeSchedule(schedule.id)} aria-label={`Xóa ${schedule.title}`}>×</button></div>)}</div> : <p>Thêm lớp học hoặc lịch làm việc cố định để availability chính xác hơn.</p>}</article></div>
    {mode && <div className="modal-backdrop" role="presentation" onMouseDown={() => !saving && setMode(null)}>{mode === 'event' ? <form className="schedule-modal form-stack" onSubmit={saveEvent} onMouseDown={(event) => event.stopPropagation()}><div className="modal-heading"><div><p className="eyebrow">LỊCH CÁ NHÂN</p><h2>{editingEvent ? 'Chỉnh sửa sự kiện' : 'Thêm sự kiện'}</h2></div><button type="button" className="modal-close" onClick={() => setMode(null)} aria-label="Đóng">×</button></div><label htmlFor="event-title">Tên sự kiện<input id="event-title" value={eventTitle} onChange={(event) => setEventTitle(event.target.value)} required autoFocus /></label><div className="two-fields"><label htmlFor="event-start">Bắt đầu<input id="event-start" type="datetime-local" value={eventStart} onChange={(event) => setEventStart(event.target.value)} required /></label><label htmlFor="event-end">Kết thúc<input id="event-end" type="datetime-local" value={eventEnd} onChange={(event) => setEventEnd(event.target.value)} required /></label></div><label htmlFor="event-category">Loại<select id="event-category" value={eventCategory} onChange={(event) => setEventCategory(event.target.value)}><option value="PERSONAL">Cá nhân</option><option value="CLASS">Lớp học</option><option value="WORK">Công việc</option></select></label><label htmlFor="event-location">Địa điểm <small>(không bắt buộc)</small><input id="event-location" value={eventLocation} onChange={(event) => setEventLocation(event.target.value)} /></label><label htmlFor="event-description">Ghi chú <small>(không bắt buộc)</small><textarea id="event-description" value={eventDescription} onChange={(event) => setEventDescription(event.target.value)} rows={3} /></label><div className="modal-actions">{editingEvent && <button type="button" className="button button--secondary" onClick={() => removeEvent(editingEvent.id)}>Xóa</button>}<button className="button button--primary" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu sự kiện'}</button></div></form> : <form className="schedule-modal form-stack" onSubmit={saveSchedule} onMouseDown={(event) => event.stopPropagation()}><div className="modal-heading"><div><p className="eyebrow">LỊCH LẶP LẠI</p><h2>Thêm lịch cố định</h2></div><button type="button" className="modal-close" onClick={() => setMode(null)} aria-label="Đóng">×</button></div><p className="auth-copy">Dùng cho lớp học, công việc hoặc lịch cố định. Nhóm chỉ biết bạn bận, không biết nội dung.</p><label htmlFor="schedule-title">Tên lịch<input id="schedule-title" value={scheduleTitle} onChange={(event) => setScheduleTitle(event.target.value)} placeholder="Ví dụ: Lớp OOP" required autoFocus /></label><fieldset className="weekday-picker"><legend>Ngày lặp lại</legend>{weekdays.map((day) => <label key={day}><input type="checkbox" checked={scheduleDays.includes(day)} onChange={(event) => setScheduleDays(event.target.checked ? [...scheduleDays, day] : scheduleDays.filter((value) => value !== day))} />{weekdayLabels[day]}</label>)}</fieldset><div className="two-fields"><label htmlFor="schedule-start">Bắt đầu<input id="schedule-start" type="time" value={scheduleStart} onChange={(event) => setScheduleStart(event.target.value)} required /></label><label htmlFor="schedule-end">Kết thúc<input id="schedule-end" type="time" value={scheduleEnd} onChange={(event) => setScheduleEnd(event.target.value)} required /></label></div><div className="two-fields"><label htmlFor="schedule-from">Từ ngày<input id="schedule-from" type="date" value={validFrom} onChange={(event) => setValidFrom(event.target.value)} required /></label><label htmlFor="schedule-until">Đến ngày<input id="schedule-until" type="date" value={validUntil} onChange={(event) => setValidUntil(event.target.value)} required /></label></div><div className="modal-actions"><button className="button button--primary" disabled={saving || scheduleDays.length === 0}>{saving ? 'Đang lưu…' : 'Lưu lịch lặp lại'}</button></div></form>}</div>}
  </section>
}

export default CalendarPage
