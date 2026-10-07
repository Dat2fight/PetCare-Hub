import { useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { Notification } from '../types'

const WS_URL = '/ws'

export const useWebSocket = (onNotification: (notification: Notification) => void) => {
  const clientRef = useRef<Client | null>(null)

  const connect = useCallback(() => {
    const token = localStorage.getItem('accessToken')
    if (!token) return

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      onConnect: () => {
        client.subscribe('/user/queue/notifications', (message) => {
          const notification: Notification = JSON.parse(message.body)
          onNotification(notification)
        })
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
      },
      reconnectDelay: 5000,
    })

    client.activate()
    clientRef.current = client
  }, [onNotification])

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
