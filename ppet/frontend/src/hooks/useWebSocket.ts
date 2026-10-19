import { useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { Notification } from '../types'
import { useAuth } from '../contexts/AuthContext'

type NotificationEvent = Pick<Notification, 'id' | 'message' | 'read' | 'createdAt'>

export const useWebSocket = (onNotification: (notification: NotificationEvent) => void) => {
  const clientRef = useRef<Client | null>(null)
  const callbackRef = useRef(onNotification)
  const { user } = useAuth()
  useEffect(() => { callbackRef.current = onNotification }, [onNotification])

  const disconnect = useCallback(() => {
    const client = clientRef.current
    clientRef.current = null
    if (client) void client.deactivate()
  }, [])

  const connect = useCallback(() => {
    if (!user?.id || clientRef.current?.active) return
    const client = new Client({
      webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_URL?.trim() || '/ws'),
      beforeConnect: async () => {
        const token = localStorage.getItem('accessToken') || localStorage.getItem('token')
        if (!token) { await client.deactivate(); return }
        client.connectHeaders = { Authorization: `Bearer ${token}` }
      },
      onConnect: () => {
        client.subscribe('/user/queue/notifications', message => {
          try {
            const notification: NotificationEvent = JSON.parse(message.body)
            callbackRef.current(notification)
          } catch { console.warn('Invalid notification message') }
        })
      },
      // Do not log STOMP frames: headers may contain authentication information.
      onStompError: () => { void client.deactivate() },
      reconnectDelay: 5000,
    })
    clientRef.current = client
    client.activate()
  }, [user?.id])

  useEffect(() => {
    connect()
    return disconnect
  }, [connect, disconnect])

  return { connect, disconnect }
}
