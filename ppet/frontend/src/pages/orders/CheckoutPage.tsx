import { useEffect, useRef, useState, FormEvent } from 'react';
import { isAxiosError } from 'axios';
import { useNavigate, Link } from 'react-router-dom';
import apiClient from '../../api/client';
import toast from 'react-hot-toast';
interface Item { id:number; productId:number; productName:string; quantity:number; price:number }
export const CheckoutPage = () => {
 const navigate=useNavigate();const [items,setItems]=useState<Item[]>([]);const [loading,setLoading]=useState(true);const [error,setError]=useState(false);const [busy,setBusy]=useState(false);
 const submitting=useRef(false);const [submitError,setSubmitError]=useState('');
 useEffect(() => {const controller=new AbortController(); apiClient.get('/cart',{signal:controller.signal}).then(({data})=>setItems(data.items || [])).catch(()=>{if(!controller.signal.aborted)setError(true);}).finally(()=>{if(!controller.signal.aborted)setLoading(false);});return()=>controller.abort();},[]);
 const submit=async(e:FormEvent<HTMLFormElement>)=>{e.preventDefault();if(submitting.current || !items.length)return;submitting.current=true;setBusy(true);setSubmitError('');const data=new FormData(e.currentTarget);
  try{const response=await apiClient.post('/orders',{shippingAddress:String(data.get('shippingAddress') || '').trim(),paymentMethod:data.get('paymentMethod'),items:items.map(i=>({productId:i.productId,quantity:i.quantity}))});toast.success('Đã đặt đơn hàng COD');navigate(`/orders/${response.data.id}`,{replace:true});}
  catch(error){const message=isAxiosError(error) && typeof error.response?.data?.message==='string' ? error.response.data.message : 'Không đặt được đơn hàng. Kiểm tra danh sách đơn nếu kết nối bị gián đoạn trước khi thử lại.';setSubmitError(message);toast.error(message);}finally{submitting.current=false;setBusy(false);}
 };
 if(loading)return <p className="p-8">Đang tải giỏ hàng…</p>;if(error)return <div role="alert" className="p-8">Không tải được giỏ hàng. <Link to="/cart">Quay lại giỏ hàng</Link></div>;
 return <div className="max-w-4xl mx-auto p-8"><h1 className="text-3xl font-bold mb-6">Đặt hàng</h1>{!items.length?<p>Giỏ hàng trống.</p>:<form onSubmit={submit} className="grid gap-5 bg-white p-6 rounded-2xl">
 {items.map(i=><p key={i.id}>{i.productName} × {i.quantity}: {(i.price*i.quantity).toLocaleString('vi-VN')} ₫</p>)}
 <p className="font-bold">Tổng: {items.reduce((sum,i)=>sum+i.price*i.quantity,0).toLocaleString('vi-VN')} ₫</p>
 <label>Người nhận, số điện thoại và địa chỉ giao hàng <textarea name="shippingAddress" required maxLength={2000} className="block border rounded w-full p-3"/></label>
 <label>Phương thức thanh toán <select name="paymentMethod" className="block border rounded p-3"><option value="COD">Thanh toán khi nhận hàng (COD)</option></select></label>
 {submitError && <p role="alert" className="text-red-600">{submitError} <Link to="/orders">Xem đơn hàng</Link></p>}
 <button disabled={busy} className="bg-primary-600 text-white p-3 rounded-xl">{busy?'Đang đặt…':'Đặt hàng'}</button></form>}</div>;
};
