import apiClient from '../api/client';
import { Order, Page } from '../types';

export type OrderItemRequest = { productId?: number; petId?: number; carePackageId?: number; beneficiaryPetId?: number; quantity: number };
export interface OrderCreateRequest {
  items: OrderItemRequest[];
  shippingAddress?: string;
  paymentMethod?: 'COD';
  notes?: string;
  checkoutKey?: string;
  promotionCode?: string;
  pointsToUse?: number;
}
// Web Crypto exposes getRandomValues on HTTP development origins as well.
export function newCheckoutKey(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('');
  return [hex.slice(0,8), hex.slice(8,12), hex.slice(12,16), hex.slice(16,20), hex.slice(20)].join('-');
}

export const orderService = {
  create: async (request: OrderCreateRequest): Promise<Order> => {
    const { data } = await apiClient.post<Order>('/orders', {
      checkoutKey: request.checkoutKey, promotionCode: request.promotionCode, pointsToUse: request.pointsToUse,
      shippingAddress: request.shippingAddress, paymentMethod: request.paymentMethod, notes: request.notes,
      items: request.items.map(item => ({ productId: item.productId, petId: item.petId, carePackageId: item.carePackageId, beneficiaryPetId: item.beneficiaryPetId, quantity: item.quantity }))
    });
    return data;
  },
  list: async (page: number, signal?: AbortSignal): Promise<Page<Order>> =>
    (await apiClient.get<Page<Order>>('/orders', { params: { page, size: 20 }, signal })).data,
  get: async (id: string, signal?: AbortSignal): Promise<Order> =>
    (await apiClient.get<Order>(`/orders/${encodeURIComponent(id)}`, { signal })).data,
  cancel: async (id: number): Promise<Order> => (await apiClient.post<Order>(`/orders/${id}/cancel`)).data
};
