import React, { createContext, useContext, useState, ReactNode, useEffect } from 'react'
import { User } from '../types'
import apiClient from '../api/client'

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (token: string, user: User) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const fetchUser = async () => {
      const token = localStorage.getItem('accessToken')
      if (token) {
        try {
          const response = await apiClient.get('/auth/me')
          setUser(response.data)
        } catch (error) {
          console.error("Failed to restore session", error)
          localStorage.removeItem('accessToken')
        }
      }
      setIsLoading(false)
    }

    fetchUser()
  }, [])

  const isAuthenticated = user !== null

  const login = (token: string, userData: User) => {
    localStorage.setItem('accessToken', token)
    setUser(userData)
  }

  const logout = () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, isLoading, login, logout }}>
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
