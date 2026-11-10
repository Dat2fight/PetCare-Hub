import { useEffect, useRef, useState, FormEvent } from 'react';
import { isAxiosError } from 'axios';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import apiClient from '../../api/client';
import { ShippingForm } from '../../components/orders/ShippingForm';
import { newCheckoutKey, orderService, validateShipping, type ShippingInfo, type OrderCreateRequest } from '../../services/orderService';
import toast from 'react-hot-toast';
interface Item { id:number; productId?:number; productName?:string; petId?:number; petName?:string; quantity:number; price:number }
interface Quote {
  subtotal: number; discount: number; pointsDiscount: number; shippingFee: number; total: number;
  availablePoints: number; maxRedeemablePoints: number;
}
const money = (v: number) => v.toLocaleString('vi-VN') + ' ₫';
export const CheckoutPage = () => {
 const {user} = useAuth();
 const [shipping,setShipping] = useState<ShippingInfo>(() => ({
   recipientName: [user?.lastName, user?.firstName].filter(Boolean).join(' '), phone: user?.phone ?? '',
   province: '', district: '', ward: '', addressLine: '', note: ''
 }));
 const [shippingErrors,setShippingErrors] = useState<Record<string,string>>({});
 const navigate=useNavigate();const [items,setItems]=useState<Item[]>([]);const [loading,setLoading]=useState(true);const [error,setError]=useState(false);const [busy,setBusy]=useState(false);
 const storage = 'checkout:pending:' + user?.id;
 const [pending, setPending] = useState<OrderCreateRequest | null>(() => {
   try { return JSON.parse(sessionStorage.getItem(storage) || 'null'); } catch { return null; }
 });
 const pendingKey = pending?.checkoutKey;
 useEffect(() => {
   if (!pendingKey) return;
   const controller = new AbortController();
   apiClient.get('/orders', { params: { page: 0, size: 20, sort: 'id,desc' }, signal: controller.signal })
     .then(({data}) => {
       const existing = data.content.find((order: {checkoutKey?: string}) => order.checkoutKey === pendingKey);
       if (!controller.signal.aborted && existing) {
         sessionStorage.removeItem(storage);
         navigate('/orders/' + existing.id, { replace: true });
       }
     }).catch(() => {});
   return () => controller.abort();
 }, [pendingKey, navigate, storage]);
 const submitting=useRef(false);const [submitError,setSubmitError]=useState('');
 useEffect(() => {const controller=new AbortController(); apiClient.get('/cart',{signal:controller.signal}).then(({data})=>setItems(data.items || [])).catch(()=>{if(!controller.signal.aborted)setError(true);}).finally(()=>{if(!controller.signal.aborted)setLoading(false);});return()=>controller.abort();},[]);
 const [draftCode, setDraftCode] = useState('');
 const [promotionCode, setPromotionCode] = useState('');
 const [points, setPoints] = useState(0);
 const [quote, setQuote] = useState<Quote | null>(null);
 const [quoteError, setQuoteError] = useState('');
 const [retry, setRetry] = useState(0);
 useEffect(() => {
   if (!items.length) return;
   const controller = new AbortController();
   setQuote(null);
   setQuoteError('');
   const timer = setTimeout(() => {
     apiClient.post<Quote>('/orders/quote', {
       items: items.map(i => ({ productId: i.productId, petId: i.petId, quantity: i.quantity })),
       promotionCode, pointsToUse: points
     }, { signal: controller.signal }).then(({data}) => {
       if (!controller.signal.aborted) setQuote(data);
     }).catch(error => {
       if (!controller.signal.aborted) setQuoteError(error.response?.data?.message || 'Không tính được tổng đơn.');
     });
   }, 250);
   return () => { controller.abort(); clearTimeout(timer); };
 }, [items, points, promotionCode, retry]);
 const placeOrder = async (request: OrderCreateRequest) => {
  if(submitting.current) return;
  submitting.current=true;setBusy(true);setSubmitError('');
  try {
   sessionStorage.setItem(storage, JSON.stringify(request));
   setPending(request);
   const response = await orderService.create(request);
   const orderId: unknown = response?.id;
   if (typeof orderId !== 'number' || !Number.isSafeInteger(orderId) || orderId <= 0) throw new Error('Phản hồi đơn hàng không hợp lệ');
   sessionStorage.removeItem(storage); setPending(null);
   toast.success('Đã đặt đơn hàng COD');
   navigate(`/orders/${orderId}`, { replace: true });
  }
  catch(error){
    if (isAxiosError(error) && error.response?.data?.fieldErrors) {
      setShippingErrors(Object.fromEntries(Object.entries(error.response.data.fieldErrors)
        .map(([key,value]) => [key.replace(/^shipping\./, ''), String(value)])));
    }
    if (isAxiosError(error) && error.response && error.response.status < 500) {
      sessionStorage.removeItem(storage); setPending(null);
    }
    const message=isAxiosError(error) && typeof error.response?.data?.message==='string' ? error.response.data.message : 'Không đặt được đơn hàng. Kiểm tra danh sách đơn nếu kết nối bị gián đoạn trước khi thử lại.';setSubmitError(message);toast.error(message);}finally{submitting.current=false;setBusy(false);}
 };
 const submit = (event: FormEvent<HTMLFormElement>) => {
   event.preventDefault();
   if (submitting.current || pending || !quote || !items.length) return;
   const errors = validateShipping(shipping); setShippingErrors(errors);
   if (Object.keys(errors).length) return;
   void placeOrder({ shipping, paymentMethod: 'COD', promotionCode, pointsToUse: points,
     items: items.map(i => ({ productId: i.productId, petId: i.petId, quantity: i.quantity })),
     checkoutKey: newCheckoutKey() });
 };
 if (pending) return <section className="max-w-xl mx-auto p-6" aria-live="polite">
   <h1>Chúng tôi đang kiểm tra đơn của bạn</h1>
   {submitError && <p role="alert">{submitError}</p>}
   <button disabled={busy} onClick={() => void placeOrder(pending)}>{busy ? 'Đang xử lý…' : 'Thử lại'}</button>
   <Link className="ml-4 underline" to="/orders">Xem đơn hàng của tôi</Link>
 </section>;
 if(loading)return <p className="p-8">Đang tải giỏ hàng…</p>;if(error)return <div role="alert" className="p-8">Không tải được giỏ hàng. <Link to="/cart">Quay lại giỏ hàng</Link></div>;
 return <div className="max-w-4xl mx-auto p-8"><h1 className="text-3xl font-bold mb-6">Đặt hàng</h1>{!items.length?<p>Giỏ hàng trống.</p>:<form onSubmit={submit} className="grid gap-5 bg-white p-6 rounded-2xl">
 {items.map(i=><p key={i.id}>{i.petName || i.productName} × {i.quantity}: {(i.price*i.quantity).toLocaleString('vi-VN')} ₫</p>)}
 <label>Mã giảm giá <input className="block max-w-full border rounded p-2" value={draftCode} maxLength={50} onChange={e=>setDraftCode(e.target.value)} /></label>
 <button type="button" onClick={()=>setPromotionCode(draftCode.trim())}>Áp dụng mã</button>
 <label>Điểm muốn dùng (1 điểm = 100 ₫) <input className="block max-w-full border rounded p-2" type="number" min="0" step="1" value={points} onChange={e=>setPoints(Number(e.target.value))} /></label>
 {quote ? <div aria-live="polite">
   <p>Số dư {quote.availablePoints} điểm · Có thể dùng {quote.maxRedeemablePoints} điểm</p>
   <p>Tiền hàng {money(quote.subtotal)}</p><p>Giảm theo mã {money(quote.discount)}</p>
   <p>Giảm từ điểm {money(quote.pointsDiscount)}</p><p>Phí giao hàng {money(quote.shippingFee)}</p>
   <strong>Tổng thanh toán {money(quote.total)}</strong>
 </div> : quoteError ? <div role="alert">{quoteError} <button type="button" onClick={()=>setRetry(n=>n+1)}>Thử lại</button></div> : <p role="status">Đang tính tổng đơn…</p>}
 <ShippingForm values={shipping} onChange={setShipping} errors={shippingErrors} />
 <label className="min-w-0">Phương thức thanh toán <select name="paymentMethod" className="block w-full min-w-0 max-w-full border rounded p-3"><option value="COD">Thanh toán khi nhận hàng (COD)</option></select></label>
 {items.some(i => i.petId) && <label><input type="checkbox" required /> Tôi đồng ý đặt đơn COD. Nếu sau 24 giờ đơn chưa được xác nhận, toàn bộ đơn sẽ bị hủy và thú cưng được nhả giữ chỗ.</label>}
 {submitError && <p role="alert" className="text-red-600">{submitError} <Link to="/orders">Xem đơn hàng</Link></p>}
 <button disabled={busy || !quote || draftCode.trim() !== promotionCode} className="bg-primary-600 text-white p-3 rounded-xl">{busy?'Đang đặt…':'Đặt hàng'}</button></form>}</div>;
};
