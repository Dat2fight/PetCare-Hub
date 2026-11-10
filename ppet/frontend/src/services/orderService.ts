import { isAxiosError } from 'axios';
import apiClient from '../api/client';
import { Order, Page } from '../types';

export type OrderItemRequest = { productId?: number; petId?: number; carePackageId?: number; beneficiaryPetId?: number; quantity: number };
export interface ShippingInfo {
  recipientName: string; phone: string; province: string; district: string;
  ward: string; addressLine: string; note: string;
}

export function validateShipping(values: ShippingInfo): Record<string, string> {
  const errors: Record<string, string> = {};
  const name = values.recipientName.trim().replace(/\s+/gu, ' ');
  if (name.length < 2 || name.length > 100 || !/^(?=.*\p{L})[\p{L}\p{M} .'’-]+$/u.test(name))
    errors.recipientName = 'Nhập họ tên từ 2 đến 100 ký tự.';
  if (!/^(0|\+84)[35789]\d{8}$/.test(values.phone.replace(/\s/g, '')))
    errors.phone = 'Nhập số di động Việt Nam bắt đầu bằng 0 hoặc +84.';
  for (const key of ['province', 'district', 'ward'] as const) {
    if (!values[key].trim() || values[key].trim().length > 100) errors[key] = 'Nhập từ 1 đến 100 ký tự.';
  }
  const address = values.addressLine.trim().replace(/\s+/gu, ' ');
  if (address.length < 5 || address.length > 200 || !/\p{L}/u.test(address) || !address.includes(' '))
    errors.addressLine = 'Nhập địa chỉ từ 5 đến 200 ký tự, có chữ và ít nhất hai từ.';
  if (values.note.trim().length > 500) errors.note = 'Ghi chú tối đa 500 ký tự.';
  return errors;
}

export interface OrderCreateRequest {
  items: OrderItemRequest[];
  shipping?: ShippingInfo;
  paymentMethod?: 'COD';
  notes?: string;
  checkoutKey: string;
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
    const { checkoutKey, ...body } = request;
    for (let attempt = 0; ; attempt++) {
      try {
        const { data } = await apiClient.post<Order>('/orders', body, {
          headers: { 'Idempotency-Key': checkoutKey }, timeout: 15000,
        });
        if (!Number.isSafeInteger(data.id) || data.id <= 0) throw new Error('Phản hồi đơn hàng không hợp lệ');
        return data;
      } catch (error) {
        if (!isAxiosError(error) || (error.response && error.response.status < 500) || attempt >= 2) throw error;
        await new Promise(resolve => setTimeout(resolve, 350 * 2 ** attempt));
      }
    }
  },
  list: async (page: number, signal?: AbortSignal): Promise<Page<Order>> =>
    (await apiClient.get<Page<Order>>('/orders', { params: { page, size: 20 }, signal })).data,
  get: async (id: string, signal?: AbortSignal): Promise<Order> =>
    (await apiClient.get<Order>(`/orders/${encodeURIComponent(id)}`, { signal })).data,
  cancel: async (id: number): Promise<Order> => (await apiClient.post<Order>(`/orders/${id}/cancel`)).data
};
