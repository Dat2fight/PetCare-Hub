import { isAxiosError } from 'axios';

export function getErrorMessage(error: unknown): string {
  if (!isAxiosError(error)) return 'Không thể hoàn tất thao tác. Vui lòng thử lại.';
  const body = error.response?.data;
  const message = typeof body?.message === 'string' ? body.message : 'Không kết nối được máy chủ. Vui lòng thử lại.';
  const correlation = body?.correlationId || error.response?.headers['x-correlation-id'];
  return correlation ? `${message} — Mã lỗi: ${correlation}` : message;
}
