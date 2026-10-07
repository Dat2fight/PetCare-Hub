import apiClient from '../api/client'
import { Notification, Page } from '../types'

export const notificationService = {
  getAll: (page = 0, size = 20) =>
    apiClient.get<Page<Notification>>('/notifications', { params: { page, size } }),

  markAsRead: (id: number) =>
    apiClient.put(`/notifications/${id}/read`),

  markAllAsRead: () =>
    apiClient.put('/notifications/read-all'),

  getUnreadCount: () =>
    apiClient.get<{ count: number }>('/notifications/unread-count'),
}
