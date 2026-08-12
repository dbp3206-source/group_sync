import axios from 'axios'

export type ApiError = {
  code?: string
  message?: string
  fieldErrors?: Record<string, string>
}

export function getApiErrorMessage(error: unknown, fallback = 'Something went wrong.') {
  if (axios.isAxiosError<ApiError>(error)) {
    return error.response?.data?.message ?? fallback
  }
  return fallback
}
