import apiClient from '../api/client';
import { Order, Page } from '../types';

export type OrderItemRequest = { productId: number; petId?: never; quantity: number }
  | { petId: number; productId?: never; quantity: 1 };
export interface OrderCreateRequest {
  items: OrderItemRequest[];
  shippingAddress: string;
  paymentMethod?: 'COD';
  notes?: string;
}
export const orderService = {
  create: async (request: OrderCreateRequest): Promise<Order> => {
    const { data } = await apiClient.post<Order>('/orders', {
      shippingAddress: request.shippingAddress, paymentMethod: request.paymentMethod, notes: request.notes,
      items: request.items.map(item => item.petId != null ? { petId: item.petId, quantity: 1 } : { productId: item.productId, quantity: item.quantity })
    });
    return data;
  },
  list: async (page: number, signal?: AbortSignal): Promise<Page<Order>> =>
    (await apiClient.get<Page<Order>>('/orders', { params: { page, size: 20 }, signal })).data,
  get: async (id: string, signal?: AbortSignal): Promise<Order> =>
    (await apiClient.get<Order>(`/orders/${encodeURIComponent(id)}`, { signal })).data,
  cancel: async (id: number): Promise<Order> => (await apiClient.post<Order>(`/orders/${id}/cancel`)).data
};
