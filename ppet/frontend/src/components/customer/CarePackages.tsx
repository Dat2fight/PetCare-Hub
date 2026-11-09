import { useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { isAxiosError } from 'axios';
import toast from 'react-hot-toast';
import { useAuth } from '../../contexts/AuthContext';
import { useResource } from '../../hooks/useResource';
import { newCheckoutKey, orderService } from '../../services/orderService';
import { Button } from '../common/Button';
import { mockImages } from '../../data/mockImages';

interface Benefits {includesGrooming:boolean; includesCheckup:boolean; includesVaccination:boolean}
interface Plan extends Benefits {id:number; name:string; description?:string; price:number; durationMonths:number}
interface Subscription extends Benefits {id:number; orderId?:number; name:string; petName:string; startDate:string; endDate:string; status:string}
const shell='max-w-5xl mx-auto px-4 py-10';
const card='min-w-0 break-words p-5 rounded-2xl bg-white shadow-sm space-y-3';
function PackagePhoto({plan}:{plan:Benefits}) {
  const src = plan.includesGrooming ? mockImages.grooming : mockImages.vet;
  return <img src={src} alt="" className="w-full h-44 object-cover rounded-xl" />;
}
function BenefitsList({plan}:{plan:Benefits}) {
  const items=[plan.includesGrooming && 'Chăm sóc lông',plan.includesCheckup && 'Khám sức khỏe',plan.includesVaccination && 'Tiêm phòng'].filter(Boolean);
  return items.length ? <ul className="list-disc pl-5">{items.map(b=><li key={String(b)}>{b}</li>)}</ul> : null;
}
function Feedback({error,retry}:{error?:string;retry:()=>void}) {
  return error ? <p role="alert">{error} <button onClick={retry}>Thử lại</button></p> : <p>Đang tải…</p>;
}
export function CarePackages() {
  const plans=useResource<Plan[]>('/care-packages');
  return <main className={shell}><h1 className="text-3xl font-bold mb-6">Gói chăm sóc định kỳ</h1>
    {!plans.data ? <Feedback error={plans.error} retry={plans.retry}/> : !plans.data.length ? <p>Chưa có gói đang bán.</p> :
    <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">{plans.data.map(p=><article className={card} key={p.id}>
      <PackagePhoto plan={p}/>
      <h2 className="text-xl font-bold"><Link to={'/care-packages/'+p.id}>{p.name}</Link></h2>
      <p>{p.price.toLocaleString('vi-VN')} ₫ · {p.durationMonths} tháng</p><BenefitsList plan={p}/>
      <Link className="inline-block underline text-primary-700" to={'/care-packages/'+p.id}>Xem chi tiết</Link></article>)}</div>}</main>;
}
function PackagePurchase({plan}:{plan:Plan}) {
  const {user}=useAuth(), navigate=useNavigate();
  const pets=useResource<{id:number;name:string}[]>('/pets/my-pets',user?.id);
  const [petId,setPetId]=useState(''), [busy,setBusy]=useState(false);
  const lock=useRef(false);
  async function buy() {
    if (lock.current || !petId) return;
    lock.current=true;setBusy(true);
    const storage='care-checkout:'+user?.id+':'+plan.id+':'+petId;
    try {
      const checkoutKey=sessionStorage.getItem(storage) || newCheckoutKey();
      sessionStorage.setItem(storage,checkoutKey);
      const order=await orderService.create({items:[{carePackageId:plan.id,beneficiaryPetId:Number(petId),quantity:1}],paymentMethod:'COD',checkoutKey});
      sessionStorage.removeItem(storage);
      navigate('/orders/'+order.id,{replace:true});
    } catch(err) {
      toast.error(isAxiosError(err) ? err.response?.data?.message || 'Chưa xác nhận được đơn. Vui lòng thử lại.' : 'Chưa thể tạo đơn.');
    } finally {lock.current=false;setBusy(false);}
  }
  if (!pets.data) return <Feedback error={pets.error} retry={pets.retry}/>;
  if (!pets.data.length) return <Link className="underline" to="/my-pets">Thêm thú cưng vào hồ sơ để mua gói</Link>;
  return <div className="space-y-4"><label className="block">Thú cưng sử dụng gói
    <select className="block w-full border rounded-lg p-2" value={petId} disabled={busy} onChange={e=>setPetId(e.target.value)}>
      <option value="">Chọn thú cưng</option>{pets.data.map(p=><option key={p.id} value={p.id}>{p.name}</option>)}</select></label>
    <p>Gói bắt đầu khi nhân viên xác nhận đã thu tiền mặt. Gói không tự gia hạn.</p>
    <Button disabled={!petId || busy} onClick={buy}>Đặt gói · {plan.price.toLocaleString('vi-VN')} ₫</Button></div>;
}
export function PackageDetail({checkout=false}:{checkout?:boolean}) {
  const {id}=useParams(), {user}=useAuth();
  const plan=useResource<Plan>('/care-packages/'+encodeURIComponent(id || ''));
  return <main className={shell}><Link to="/care-packages">← Các gói chăm sóc</Link>
    {!plan.data ? <Feedback error={plan.error} retry={plan.retry}/> : <article className={card}>
      {!checkout && <PackagePhoto plan={plan.data}/>}
      <h1 className="text-3xl font-bold">{plan.data.name}</h1><p>{plan.data.description}</p>
      <p>{plan.data.price.toLocaleString('vi-VN')} ₫ · {plan.data.durationMonths} tháng</p><BenefitsList plan={plan.data}/>
      {checkout ? <PackagePurchase key={id} plan={plan.data}/> : (!user || user.roles.includes('CUSTOMER')) &&
        <Link className="inline-block rounded-xl px-5 py-3 bg-primary-600 text-white" to={'/care-packages/'+id+'/checkout'}>Mua gói</Link>}
    </article>}</main>;
}
export function MyPackages() {
  const {user}=useAuth();
  const subs=useResource<Subscription[]>('/care-packages/subscriptions/me',user?.id);
  const labels:Record<string,string>={ACTIVE:'Đang sử dụng',EXPIRED:'Hết hạn',CANCELLED:'Đã hủy'};
  return <main className={shell}><h1 className="text-3xl font-bold mb-6">Gói của tôi</h1>
    {!subs.data ? <Feedback error={subs.error} retry={subs.retry}/> : !subs.data.length ? <p>Bạn chưa có gói đã kích hoạt. <Link className="underline" to="/care-packages">Xem các gói</Link></p> :
    <div className="grid sm:grid-cols-2 gap-5">{subs.data.map(s=><article key={s.id} className={card}><h2 className="text-xl font-bold">{s.name}</h2>
      <p>{s.petName} · {labels[s.status]}</p><p>Từ {s.startDate} đến hết {s.endDate}</p><BenefitsList plan={s}/>
      {s.orderId && <Link className="underline" to={'/orders/'+s.orderId}>Xem đơn mua gói</Link>}</article>)}</div>}</main>;
}
