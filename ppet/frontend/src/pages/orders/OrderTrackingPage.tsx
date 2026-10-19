import { useEffect, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { isAxiosError } from 'axios';
import apiClient from '../../api/client';
import toast from 'react-hot-toast';
import { OrdersPage } from './OrdersPage';

interface OrderDetail {
  id: number; orderNumber: string; status: string; totalPrice: number;
  shippingAddress: string; paymentMethod: string; paymentStatus: string; createdAt: string;
  items: { productId: number | null; productName: string | null; quantity: number; price: number }[];
}
const statusNames: Record<string,string> = { PENDING:'Chờ xác nhận', CONFIRMED:'Đã xác nhận', PROCESSING:'Đang chuẩn bị', SHIPPING:'Đang giao', DELIVERED:'Đã giao', CANCELLED:'Đã hủy' };
export const OrderTrackingPage = () => {
  const { id }=useParams(); const [params]=useSearchParams(); const orderId=id || params.get('orderId');
  const [order,setOrder]=useState<OrderDetail|null>(null); const [loading,setLoading]=useState(true);
  const [error,setError]=useState(''); const [retry,setRetry]=useState(0); const [busy,setBusy]=useState(false); const mutating=useRef(false);
  useEffect(()=>{
    if(!orderId)return;
    const controller=new AbortController(); setLoading(true);setError('');setOrder(null);
    apiClient.get<OrderDetail>(`/orders/${encodeURIComponent(orderId)}`,{signal:controller.signal})
      .then(({data})=>{if(!controller.signal.aborted)setOrder(data);})
      .catch(error=>{if(!controller.signal.aborted)setError(isAxiosError(error) && error.response?.status===404 ? 'Đơn hàng không tồn tại.' : isAxiosError(error) && error.response?.status===403 ? 'Bạn không có quyền xem đơn này.' : 'Không tải được đơn hàng.');})
      .finally(()=>{if(!controller.signal.aborted)setLoading(false);});
    return()=>controller.abort();
  },[orderId,retry]);
  const cancel=async()=>{
    if(!order || mutating.current)return; mutating.current=true;setBusy(true);
    try{const {data}=await apiClient.post<OrderDetail>(`/orders/${order.id}/cancel`);setOrder(data);toast.success('Đã hủy đơn hàng');}
    catch(error){toast.error(isAxiosError(error) && typeof error.response?.data?.message==='string' ? error.response.data.message : 'Không hủy được đơn hàng.');setRetry(n=>n+1);}
    finally{mutating.current=false;setBusy(false);}
  };
  // Preserve the old tracking URL without an ID as the user's order list.
  if(!orderId)return <OrdersPage/>;
  return <div className="max-w-4xl mx-auto p-8"><Link to="/orders" className="text-primary-600">← Đơn hàng của tôi</Link>
    {loading ? <p>Đang tải…</p> : error ? <p role="alert">{error} <button onClick={()=>setRetry(n=>n+1)}>Thử lại</button></p> : order && <article className="bg-white rounded-2xl p-6 mt-4 space-y-4">
      <h1 className="text-2xl font-bold">Đơn {order.orderNumber}</h1><p>{statusNames[order.status] || order.status}</p>
      <p>Ngày đặt: {new Date(order.createdAt).toLocaleString('vi-VN')}</p><p>Giao đến: {order.shippingAddress}</p>
      <p>{order.paymentMethod} · {order.paymentStatus==='COMPLETED' ? 'Đã thanh toán' : order.paymentStatus==='PENDING' ? 'Chưa thanh toán' : order.paymentStatus}</p>
      {order.items.map((item,index)=><p key={index}>{item.productName || `Sản phẩm #${item.productId}`} × {item.quantity}: {(item.price*item.quantity).toLocaleString('vi-VN')} ₫</p>)}
      <p className="font-bold">Tổng đơn: {order.totalPrice.toLocaleString('vi-VN')} ₫</p>
      {['PENDING','CONFIRMED','PROCESSING'].includes(order.status) && <button disabled={busy} onClick={cancel} className="border rounded p-3">{busy?'Đang hủy…':'Hủy đơn hàng'}</button>}
    </article>}
  </div>;
};
