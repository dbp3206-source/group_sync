import axios from 'axios'

export type ApiError = {
  code?: string
  message?: string
  fieldErrors?: Record<string, string>
}

export function isWakeUpError(error: unknown) {
  if (!axios.isAxiosError(error)) return false
  return error.response?.status === 502
    || error.response?.status === 503
    || error.code === 'ECONNABORTED'
    || !error.response
}

export function getApiErrorMessage(error: unknown, fallback = 'Something went wrong.') {
  if (axios.isAxiosError<ApiError>(error)) {
    const response = error.response
    if (!response) return 'KnowledgeOS could not reach the server. Please try again in a moment.'
    const data = response.data
    if (data?.fieldErrors) {
      const details = Object.entries(data.fieldErrors).map(([field, message]) => `${field}: ${message}`).join(' ')
      return `${data.message ?? 'Please check the form.'} ${details}`
    }
    if (response.status === 403) return 'Security token expired. Refresh the page and try again.'
    if (response.status === 502 || response.status === 503) return 'KnowledgeOS is waking up. Please try again in a moment.'
    return data?.message ?? fallback
  }
  return fallback
}
