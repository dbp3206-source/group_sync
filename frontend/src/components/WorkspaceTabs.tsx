import { BarChart3, CalendarCheck, GraduationCap, LayoutDashboard, Trophy } from 'lucide-react'
import { Link, useLocation } from 'react-router-dom'

type WorkspaceTabsProps = { groupId: number; type: 'STUDY' | 'BADMINTON' }

function WorkspaceTabs({ groupId, type }: WorkspaceTabsProps) {
  const { pathname } = useLocation()
  const activityPath = type === 'BADMINTON' ? `/badminton?groupId=${groupId}` : `/study?groupId=${groupId}`
  const tabs = [
    { label: 'Tổng quan', to: `/groups/${groupId}`, icon: LayoutDashboard, active: pathname === `/groups/${groupId}` },
    { label: 'Lịch chung', to: `/groups/${groupId}/availability`, icon: CalendarCheck, active: pathname.includes('/availability') },
    { label: type === 'BADMINTON' ? 'Thi đấu' : 'Buổi học', to: activityPath, icon: type === 'BADMINTON' ? BarChart3 : GraduationCap, active: pathname === (type === 'BADMINTON' ? '/badminton' : '/study') },
    ...(type === 'BADMINTON' ? [{ label: 'Giải đấu', to: `/tournaments?groupId=${groupId}`, icon: Trophy, active: pathname === '/tournaments' }] : []),
  ]
  return <nav className="workspace-tabs" aria-label="Không gian nhóm">{tabs.map(({ label, to, icon: Icon, active }) => <Link key={label} className={active ? 'is-active' : ''} to={to}><Icon size={17} aria-hidden="true" /><span>{label}</span></Link>)}</nav>
}

export default WorkspaceTabs
