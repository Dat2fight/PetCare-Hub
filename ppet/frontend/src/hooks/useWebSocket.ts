import { useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { Notification } from '../types'
import { useAuth } from '../contexts/AuthContext'

const WS_URL = '/ws'

export const useWebSocket = (onNotification: (notification: Notification) => void) => {
  const clientRef = useRef<Client | null>(null)
  const { user } = useAuth()

  const connect = useCallback(() => {
    const token = localStorage.getItem('accessToken') || localStorage.getItem('token')
    if (!token) return

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      onConnect: () => {
        if (user?.id) {
          client.subscribe(`/topic/notifications/${user.id}`, (message) => {
            try {
              const notification: Notification = JSON.parse(message.body)
              onNotification(notification)
            } catch (err) {
              console.error('Error parsing notification message:', err)
            }
          })
        }
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
      },
      reconnectDelay: 5000,
    })

    client.activate()
    clientRef.current = client
  }, [onNotification, user?.id])

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate()
      clientRef.current = null
    }
  }, [])

  useEffect(() => {
    return () => {
      disconnect()
    }
  }, [disconnect])

  return { connect, disconnect }
}
