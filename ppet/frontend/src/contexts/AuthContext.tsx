import React, { createContext, useContext, useState, ReactNode, useEffect, useRef } from 'react'
import { isAxiosError } from 'axios'
import { User } from '../types'
import apiClient from '../api/client'

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  sessionError: string | null
  retrySession: () => void
  login: (token: string, user: User) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [sessionError, setSessionError] = useState<string | null>(null)
  const [retry, setRetry] = useState(0)
  const restoreRequest = useRef<AbortController | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    restoreRequest.current = controller
    const fetchUser = async () => {
      setIsLoading(true)
      setSessionError(null)
      const token = localStorage.getItem('accessToken') || localStorage.getItem('token')
      try {
        if (token) {
          const response = await apiClient.get('/auth/me', { signal: controller.signal })
          if (!controller.signal.aborted) setUser(response.data)
        } else {
          setUser(null)
        }
      } catch (error) {
        if (controller.signal.aborted) return
        if (isAxiosError(error) && error.response?.status === 401) {
          // apiClient owns token cleanup and the login redirect for 401 responses.
          setUser(null)
        } else {
          setSessionError('Chưa thể xác minh phiên đăng nhập. Vui lòng thử lại.')
        }
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }

    fetchUser()
    return () => controller.abort()
  }, [retry])

  const retrySession = () => { if (!isLoading) setRetry(value => value + 1) }

  const isAuthenticated = user !== null

  const login = (token: string, userData: User) => {
    restoreRequest.current?.abort()
    localStorage.setItem('accessToken', token)
    localStorage.setItem('token', token)
    setUser(userData)
    setSessionError(null)
    setIsLoading(false)
  }

  const logout = () => {
    restoreRequest.current?.abort()
    localStorage.removeItem('accessToken')
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    setUser(null)
    setSessionError(null)
    setIsLoading(false)
  }

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, isLoading, sessionError, retrySession, login, logout }}>
      {sessionError && <div role="alert" className="p-4 bg-amber-50 text-amber-900">
        {sessionError} <button type="button" onClick={retrySession} disabled={isLoading}>Thử lại</button>
      </div>}
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
