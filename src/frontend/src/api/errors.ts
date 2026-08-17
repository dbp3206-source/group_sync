import axios from 'axios'

export type ApiError = {
  code?: string
  message?: string
  fieldErrors?: Record<string, string>
}

export function getApiErrorMessage(error: unknown, fallback = 'Something went wrong.') {
  if (axios.isAxiosError<ApiError>(error)) {
    const response = error.response
    if (!response) return 'GroupSync server is not running. Start it with scripts/start-groupsync.ps1, then try again.'
    const data = response.data
    if (data?.fieldErrors) {
      const details = Object.entries(data.fieldErrors).map(([field, message]) => `${field}: ${message}`).join(' ')
      return `${data.message ?? 'Please check the form.'} ${details}`
    }
    if (response.status === 403) return 'Security token expired. Refresh the page and try again.'
    return data?.message ?? fallback
  }
  return fallback
}
