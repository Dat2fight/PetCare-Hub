import { useRef, useState } from 'react';
import { FiHeart } from 'react-icons/fi';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import apiClient from '../../api/client';
import { useAuth } from '../../contexts/AuthContext';
import { useResource } from '../../hooks/useResource';

type Kind = 'PRODUCT' | 'PET';
interface Match {id:number; targetId:number}
interface Wish extends Match {type:Kind; name:string; imageUrl?:string; price?:number; available:boolean}

export function useWishlistStatus(type:Kind, ids:number[]) {
  const {user} = useAuth();
  const query = user?.roles.includes('CUSTOMER') && ids.length ? '/wishlist/status?type='+type+'&ids='+ids.join(',') : null;
  return useResource<Match[]>(query, user?.id ?? 0);
}

export function HeartButton({type,id,status}:{type:Kind;id:number;status:ReturnType<typeof useWishlistStatus>}) {
  const {user,isLoading} = useAuth();
  const location=useLocation(), navigate=useNavigate();
  const original=status.data?.find(row=>row.targetId===id)?.id;
  const [override,setOverride]=useState<{key:string;value:number|null}>();
  const key=[user?.id,type,id].join('-');
  const value=override?.key===key ? override.value : original;
  const lock=useRef(false);
  const [busy,setBusy]=useState(false);
  if (user && !user.roles.includes('CUSTOMER')) return null;
  async function toggle() {
    if (!user) {navigate('/login',{state:{from:location}});return;}
    if (status.error) {status.retry();return;}
    if (lock.current) return;
    lock.current=true; setBusy(true);
    setOverride({key,value:value ? null : -1});
    try {
      if (value) await apiClient.delete('/wishlist/'+value);
      else {
        const {data}=await apiClient.post<Match>('/wishlist',{type,targetId:id});
        setOverride({key,value:data.id});
      }
    } catch {
      setOverride({key,value:value ?? null});
      toast.error('Chưa cập nhật được mục yêu thích.');
    } finally {lock.current=false;setBusy(false);}
  }
  const label=status.error ? 'Tải lại trạng thái yêu thích' : value ? 'Bỏ yêu thích' : 'Thêm vào yêu thích';
  return <button type="button" className="min-w-11 min-h-11 inline-flex items-center justify-center rounded-full bg-white text-primary-700 shadow-sm"
    aria-label={label} title={label} aria-pressed={Boolean(value)} disabled={isLoading || busy || Boolean(user && !status.data && !status.error)} onClick={toggle}>
    <FiHeart fill={value ? 'currentColor' : 'none'}/></button>;
}

export function WishlistPage() {
  const [page,setPage]=useState(0), [busy,setBusy]=useState<number|null>(null);
  const {user}=useAuth();
  const list=useResource<{content:Wish[];totalPages:number}>('/wishlist?size=12&page='+page,user?.id);
  async function act(item:Wish, remove:boolean) {
    if (busy!==null) return;
    setBusy(item.id);
    try {
      if (remove) {
        await apiClient.delete('/wishlist/'+item.id);
        if (page && list.data?.content.length===1) setPage(page-1); else list.retry();
      } else {
        await apiClient.post('/cart/items',{[item.type==='PET'?'petId':'productId']:item.targetId,quantity:1});
        toast.success('Đã thêm vào giỏ');
      }
    } catch {toast.error('Không thể cập nhật. Vui lòng thử lại.');list.retry();}
    finally {setBusy(null);}
  }
  return <main className="max-w-6xl mx-auto p-5"><h1 className="text-3xl font-bold my-5">Danh sách yêu thích</h1>
    {list.error ? <p role="alert">{list.error} <button onClick={list.retry}>Thử lại</button></p> : !list.data ? <p>Đang tải…</p> :
      <>{!list.data.content.length && <p>Bạn chưa lưu mục nào.</p>}
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">{list.data.content.map(item=><article key={item.id} className="p-5 rounded-2xl bg-white shadow-sm">
        {item.imageUrl && <img className="w-full aspect-video object-cover rounded-lg" src={item.imageUrl} alt=""/>}
        <h2 className="font-bold">{item.available ? <Link to={(item.type==='PET'?'/pets/':'/products/')+item.targetId}>{item.name}</Link> : item.name}</h2>
        {item.price!=null && <p>{item.price.toLocaleString('vi-VN')} ₫</p>}
        <div className="flex gap-4 flex-wrap">{item.available ? <button disabled={busy!==null} onClick={()=>act(item,false)}>Thêm vào giỏ</button> : <span>Hiện không còn bán</span>}
        <button disabled={busy!==null} onClick={()=>act(item,true)}>Bỏ yêu thích</button></div></article>)}</div>
      {list.data.totalPages>1 && <nav className="flex gap-5 my-6"><button disabled={!page} onClick={()=>setPage(page-1)}>Trang trước</button>
      <span>{page+1}/{list.data.totalPages}</span><button disabled={page+1>=list.data.totalPages} onClick={()=>setPage(page+1)}>Trang sau</button></nav>}</>}
  </main>;
}
