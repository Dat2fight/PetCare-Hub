import React, { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { isAxiosError } from 'axios';
import toast from 'react-hot-toast';
import apiClient from '../../api/client';
import { Button } from '../../components/common/Button';
import { FiTrash2, FiMinus, FiPlus, FiArrowRight } from 'react-icons/fi';

interface CartItem { id: number; productId: number; productName: string; quantity: number; price: number }
interface Cart { items: CartItem[] }
const money = (value: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);

export const CartPage: React.FC = () => {
  const [cart, setCart] = useState<Cart>({ items: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [retry, setRetry] = useState(0);
  const mutating = useRef(false);
  const navigate = useNavigate();

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError('');
    apiClient.get<Cart>('/cart', { signal: controller.signal })
      .then(({ data }) => setCart(data))
      .catch(() => { if (!controller.signal.aborted) setError('Không tải được giỏ hàng. Vui lòng thử lại.'); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [retry]);

  const mutate = async (operation: () => Promise<unknown>) => {
    if (mutating.current) return;
    mutating.current = true; setBusy(true);
    try {
      await operation();
      const { data } = await apiClient.get<Cart>('/cart');
      setCart(data); setError('');
    } catch (err) {
      const message = isAxiosError(err) ? err.response?.data?.message : undefined;
      toast.error(message || 'Không đồng bộ được giỏ hàng. Vui lòng tải lại trước khi thanh toán.');
      // A failed response may follow a successful write; require a fresh read before checkout.
      setError('Vui lòng tải lại giỏ hàng để xác nhận dữ liệu mới nhất.');
    } finally { mutating.current = false; setBusy(false); }
  };
  const update = (item: CartItem, quantity: number) => mutate(() => apiClient.put(`/cart/items/${item.id}`, { quantity }));
  // No promotion is applied by the cart API; do not invent a client-side discount.
  const total = Math.max(0, cart.items.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0));
  const disabled = loading || busy || !!error;

  return <div className="bg-background min-h-screen py-10">
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <h1 className="text-3xl font-bold mb-8">Giỏ hàng</h1>
      {loading && <p role="status">Đang tải giỏ hàng...</p>}
      {error && <div role="alert" className="mb-6 text-red-600">{error} <button disabled={busy} className="underline" onClick={() => setRetry(x => x + 1)}>Tải lại</button></div>}
      {!loading && !error && cart.items.length === 0 && <p className="mb-6">Giỏ hàng đang trống.</p>}
      <div className="flex flex-col lg:flex-row gap-8">
        <div className="lg:w-2/3 space-y-4">
          {cart.items.map(item => <div key={item.id} className="bg-white rounded-2xl p-6 shadow-xs border border-gray-100 flex items-center gap-6">
            <div className="flex-1">
              <Link to={`/products/${item.productId}`} className="font-semibold hover:text-primary-600">{item.productName}</Link>
              <div className="font-bold text-primary-600 mt-2">{money(Number(item.price))}</div>
            </div>
            <div className="flex items-center border rounded-xl">
              <button aria-label={`Giảm ${item.productName}`} disabled={disabled || item.quantity <= 1} onClick={() => void update(item, item.quantity - 1)} className="p-2 disabled:opacity-40"><FiMinus /></button>
              <span className="w-8 text-center">{item.quantity}</span>
              <button aria-label={`Tăng ${item.productName}`} disabled={disabled} onClick={() => void update(item, item.quantity + 1)} className="p-2 disabled:opacity-40"><FiPlus /></button>
            </div>
            <button aria-label={`Xóa ${item.productName}`} disabled={disabled} onClick={() => void mutate(() => apiClient.delete(`/cart/items/${item.id}`))} className="p-2 text-red-500 disabled:opacity-40"><FiTrash2 /></button>
          </div>)}
          <div className="flex justify-between gap-4">
            <Link to="/products" className="text-primary-600">+ Tiếp tục mua sắm</Link>
            {cart.items.length > 0 && <button disabled={disabled} onClick={() => void mutate(() => apiClient.delete('/cart'))} className="text-red-600 disabled:opacity-40">Xóa tất cả</button>}
          </div>
        </div>
        <div className="lg:w-1/3 bg-white rounded-3xl p-6 shadow-card border border-gray-100 h-fit">
          <h2 className="text-xl font-bold mb-6">Tóm tắt đơn hàng</h2>
          <div className="flex justify-between mb-6"><span>Tổng cộng</span><strong className="text-primary-600">{money(total)}</strong></div>
          <Button fullWidth size="lg" disabled={disabled || cart.items.length === 0} onClick={() => navigate('/checkout')}>
            Thanh toán ngay <FiArrowRight className="ml-2" />
          </Button>
          <Link to="/orders" className="block mt-4 text-primary-600">Xem đơn hàng của tôi</Link>
        </div>
      </div>
    </div>
  </div>;
};
