import { FormEvent, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import apiClient from '../../api/client';
import { useResource } from '../../hooks/useResource';
import { useAuth } from '../../contexts/AuthContext';
import { Button } from '../common/Button';

interface Review { id:number; targetType:string; targetId:number; rating:number; comment:string; status:string; moderationReason?:string }
interface Page<T> {content:T[];totalPages:number}
const labels:Record<string,string> = {PENDING:'Chờ duyệt', APPROVED:'Đã duyệt', REJECTED:'Bị từ chối', HIDDEN:'Đã ẩn'};

function ReviewForm({initial, save}: {initial?:Review; save:(rating:number, comment:string)=>Promise<void>}) {
  const [busy,setBusy] = useState(false);
  const submit = async (event:FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (busy) return;
    const form = new FormData(event.currentTarget);
    setBusy(true);
    try {await save(Number(form.get('rating')), String(form.get('comment')).trim()); toast.success('Đã gửi đánh giá để xét duyệt');}
    catch {toast.error('Chưa lưu được đánh giá. Vui lòng thử lại.');}
    finally {setBusy(false);}
  };
  return <form className="grid gap-3" onSubmit={submit}>
    <label>Số sao <select name="rating" defaultValue={initial?.rating || 5}>{[1,2,3,4,5].map(n=><option key={n}>{n}</option>)}</select></label>
    <label>Nội dung <textarea required name="comment" maxLength={2000} defaultValue={initial?.comment} className="block w-full border rounded p-2"/></label>
    <Button disabled={busy} type="submit">Gửi đánh giá</Button>
  </form>;
}

export function Reviews({type,id}:{type:'PRODUCT'|'SERVICE';id:number}) {
  const {user}=useAuth();
  const [revision,setRevision]=useState(0);
  const params=new URLSearchParams({targetType:type,targetId:String(id)});
  const reviews=useResource<Review[]>('/reviews?'+params,revision);
  const stats=useResource<{count:number;average:number|null}>('/reviews/summary?'+params,revision);
  const eligible=useResource<boolean>(user?.roles.includes('CUSTOMER') ? '/reviews/eligibility?'+params : null,revision);
  return <section className="p-4 space-y-4 bg-white rounded-xl">
    <h2 className="font-bold text-xl">Đánh giá</h2>
    {reviews.error || stats.error ? <button onClick={()=>setRevision(n=>n+1)}>Không tải được đánh giá. Thử lại</button> :
    !reviews.data || !stats.data ? <p>Đang tải…</p> : stats.data.count===0 ? <p>Chưa có đánh giá đã duyệt.</p> :
    <><strong>{stats.data.average?.toFixed(1)} / 5 · {stats.data.count} đánh giá</strong>
    {reviews.data.map(r=><p key={r.id} className="break-words">{r.rating} ★ · {r.comment}</p>)}</>}
    {eligible.error ? <button onClick={eligible.retry}>Thử lại kiểm tra quyền đánh giá</button> : eligible.data ?
    <ReviewForm save={async(rating,comment)=>{await apiClient.post('/reviews',{targetType:type,targetId:id,rating,comment});setRevision(n=>n+1);}}/> :
    <p>Khách đã hoàn tất giao dịch có thể gửi một đánh giá.</p>}
  </section>;
}

export function MyReviews() {
  const [page,setPage]=useState(0);
  const [edit,setEdit]=useState<number>();
  const [busy,setBusy]=useState(false);
  const list=useResource<Page<Review>>('/reviews/me?size=12&sort=id,desc&page='+page);
  const remove=async(id:number)=>{
    if (busy || !window.confirm('Xóa đánh giá này?')) return;
    setBusy(true);
    try {await apiClient.delete('/reviews/me/'+id);setPage(0);list.retry();} catch {toast.error('Chưa xóa được đánh giá');} finally {setBusy(false);}
  };
  return <main className="mx-auto max-w-3xl p-4 space-y-4"><h1 className="text-2xl font-bold">Đánh giá của tôi</h1>
    {list.error ? <button onClick={list.retry}>Lỗi tải đánh giá. Thử lại</button> : !list.data ? 'Đang tải…' : !list.data.content.length ? 'Bạn chưa có đánh giá.' :
    list.data.content.map(r=><article key={r.id} className="bg-white p-4 space-y-2 break-words"><p>{r.rating} ★ · {labels[r.status]}</p><p>{r.comment}</p>
    {r.moderationReason && <p>Lý do: {r.moderationReason}</p>}
    <button onClick={()=>setEdit(edit===r.id?undefined:r.id)}>Sửa</button> <button disabled={busy} onClick={()=>remove(r.id)}>Xóa</button>
    {edit===r.id && <ReviewForm initial={r} save={async(rating,comment)=>{await apiClient.put('/reviews/me/'+r.id,{rating,comment});setEdit(undefined);list.retry();}}/>}</article>)}
    <button disabled={!page} onClick={()=>setPage(page-1)}>Trang trước</button> <button disabled={page+1 >= (list.data?.totalPages||0)} onClick={()=>setPage(page+1)}>Trang sau</button>
  </main>;
}

export function ServiceDetail() {
  const {id}=useParams();
  const service=useResource<{id:number;name:string;description:string;active:boolean}>('/services/'+id);
  if(service.error) return <button onClick={service.retry}>Không tải được dịch vụ. Thử lại</button>;
  if(!service.data) return <p>Đang tải…</p>;
  if(!service.data.active) return <p>Dịch vụ đã ngừng hoạt động.</p>;
  return <div className="mx-auto max-w-3xl p-4 space-y-4"><h1 className="text-2xl font-bold">{service.data.name}</h1>
    <p>{service.data.description}</p><Link to={'/services/booking?serviceId='+id}>Đặt lịch</Link>
    <Reviews type="SERVICE" id={service.data.id}/></div>;
}
