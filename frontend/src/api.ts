const API_BASE = '/api'
export type User = {
  id: number
  username: string
  email: string
  fullName: string
  role: 'ADMIN' | 'TEACHER' | 'STUDENT'
  active: boolean
}

export type Course = {
  id: number
  courseCode: string
  title: string
  category: string | null
  description: string | null
  syllabus: string | null
  fee: number
  durationWeeks: number
  creditHours: number
  startDate: string | null
  endDate: string | null
  enrollmentDeadline: string | null
  capacity: number
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  enrollmentOpen: boolean
  teacherId: number | null
  teacherName: string | null
  enrolledCount: number
}

export type Enrollment = {
  enrollmentId: number
  courseId: number
  courseCode: string
  courseTitle: string
  fee: number
  status: 'ACTIVE' | 'DROPPED'
  enrolledAt: string
  droppedAt: string | null
}

export type Dashboard = Record<string, number>

export function token() {
  return localStorage.getItem('sms_token')
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (!(options.body instanceof FormData) && options.body) headers.set('Content-Type', 'application/json')
  if (token()) headers.set('Authorization', `Bearer ${token()}`)
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  if (!response.ok) {
    let message = `Request failed (${response.status})`
    try {
      const body = await response.json()
      message = body.message || body.detail || body.error || message
    } catch {
      // Keep the fallback message for non-JSON responses.
    }
    throw new Error(message)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export function money(value: number) {
  return new Intl.NumberFormat('en-BD', {
    style: 'currency',
    currency: 'BDT',
    maximumFractionDigits: 2,
  }).format(value)
}
