import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../../api/client';
export const OrdersPage = () => {
  const [orders, setOrders] = useState<{id: number; status: string; totalPrice: number}[]>([]);
  const [page, setPage] = useState(0); const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true); const [error, setError] = useState(false); const [retry, setRetry] = useState(0);
  useEffect(() => { const controller = new AbortController(); setLoading(true); setError(false);
    apiClient.get('/orders', { params: { page, size: 20 }, signal: controller.signal }).then(({data}) => { setOrders(data.content); setTotal(data.totalPages); })
      .catch(() => { if (!controller.signal.aborted) setError(true); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [page, retry]);
  return <div className="max-w-4xl mx-auto p-8"><h1 className="text-3xl font-bold mb-6">Đơn hàng của tôi</h1>
    {loading ? <p>Đang tải…</p> : error ? <p role="alert">Không tải được đơn hàng. <button onClick={() => setRetry(retry+1)}>Thử lại</button></p> : !orders.length ? <p>Chưa có đơn hàng.</p> : orders.map(order => <article key={order.id} className="p-6 bg-white rounded-xl mb-4"><Link to={`/orders/${order.id}`} className="text-primary-600">Đơn #{order.id}</Link> · {order.status} · {order.totalPrice.toLocaleString('vi-VN')} ₫</article>)}
    <nav className="flex gap-6"><button disabled={!page} onClick={() => setPage(page-1)}>Trang trước</button><span>{total ? page+1 : 0}/{total}</span><button disabled={page+1 >= total} onClick={() => setPage(page+1)}>Trang sau</button></nav>
  </div>;
};
