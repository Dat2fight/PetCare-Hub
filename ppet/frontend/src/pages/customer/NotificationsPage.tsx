import { useEffect, useState } from 'react';
import apiClient from '../../api/client';
import toast from 'react-hot-toast';
import { useWebSocket } from '../../hooks/useWebSocket';
interface Notice { id: number; message: string; read: boolean; createdAt: string }
export const NotificationsPage = () => {
 const [items, setItems] = useState<Notice[]>([]); const [page,setPage] = useState(0); const [total,setTotal] = useState(0);
 const [loading,setLoading] = useState(true); const [error,setError] = useState(false); const [retry,setRetry] = useState(0); const [busy,setBusy] = useState(false);
 useWebSocket(() => setRetry(value => value + 1));
 useEffect(() => { const controller = new AbortController(); setLoading(true); setError(false);
  const load = () => apiClient.get('/notifications', {params:{page,size:20},signal:controller.signal}).then(({data}) => {setItems(data.content);setTotal(data.totalPages);setError(false);})
   .catch(() => {if (!controller.signal.aborted) setError(true);}).finally(() => {if (!controller.signal.aborted) setLoading(false);});
  void load(); const timer = window.setInterval(load,30000); return () => {controller.abort();window.clearInterval(timer);};
 },[page,retry]);
 const read = async (id?: number) => { setBusy(true); try { await apiClient.put(id ? `/notifications/${id}/read` : '/notifications/read-all'); setRetry(retry+1); toast.success('Đã đánh dấu đã đọc'); } catch {toast.error('Không cập nhật được thông báo');} finally {setBusy(false);} };
 return <div className="max-w-4xl mx-auto p-8"><h1 className="text-3xl font-bold mb-6">Thông báo</h1><button disabled={busy} onClick={() => read()}>Đọc tất cả</button>
 {loading ? <p>Đang tải…</p> : error ? <p role="alert">Không tải được thông báo. <button onClick={() => setRetry(retry+1)}>Thử lại</button></p> : !items.length ? <p>Chưa có thông báo.</p> : items.map(n => <article className="bg-white rounded-xl p-5 my-3" key={n.id}><p>{n.message}</p><p>{new Date(n.createdAt).toLocaleString('vi-VN')}</p>{!n.read && <button disabled={busy} onClick={() => read(n.id)}>Đánh dấu đã đọc</button>}</article>)}
 <nav className="flex gap-6"><button disabled={!page} onClick={() => setPage(page-1)}>Trang trước</button><button disabled={page+1 >= total} onClick={() => setPage(page+1)}>Trang sau</button></nav></div>;
};
