import apiClient from '../api/client';
import { Order, Page } from '../types';

export type OrderItemRequest = { productId: number; petId?: never; quantity: number };
export interface OrderCreateRequest {
  items: OrderItemRequest[];
  shippingAddress: string;
  paymentMethod?: 'COD';
  notes?: string;
  checkoutKey?: string;
  promotionCode?: string;
  pointsToUse?: number;
}
export const orderService = {
  create: async (request: OrderCreateRequest): Promise<Order> => {
    if (request.items.some(item => item.petId != null)) throw new Error('Mua thú cưng chưa khả dụng trên phiên bản này');
    const { data } = await apiClient.post<Order>('/orders', {
      checkoutKey: request.checkoutKey, promotionCode: request.promotionCode, pointsToUse: request.pointsToUse,
      shippingAddress: request.shippingAddress, paymentMethod: request.paymentMethod, notes: request.notes,
      items: request.items.map(item => ({ productId: item.productId, quantity: item.quantity }))
    });
    return data;
  },
  list: async (page: number, signal?: AbortSignal): Promise<Page<Order>> =>
    (await apiClient.get<Page<Order>>('/orders', { params: { page, size: 20 }, signal })).data,
  get: async (id: string, signal?: AbortSignal): Promise<Order> =>
    (await apiClient.get<Order>(`/orders/${encodeURIComponent(id)}`, { signal })).data,
  cancel: async (id: number): Promise<Order> => (await apiClient.post<Order>(`/orders/${id}/cancel`)).data
};
