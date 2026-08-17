import { apiClient } from './client'
import type { Match, News, Stat } from './badminton'

export type Dashboard = {
  nextActivities: { sessionId: number; title: string; start: string; end: string; status: string }[]
  registrationCount: number
  recentMatches: Match[]
  leaderboard: Stat[]
  news: News[]
}

export async function getGroupDashboard(groupId: number, seasonId: number) {
  return (await apiClient.get<Dashboard>(`/dashboard/groups/${groupId}?seasonId=${seasonId}`)).data
}
