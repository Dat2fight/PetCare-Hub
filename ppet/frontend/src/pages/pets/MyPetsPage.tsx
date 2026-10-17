import { useEffect, useState, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../../api/client';
import { PetDTO } from '../../services/petManagementService';
import toast from 'react-hot-toast';
export const MyPetsPage = () => {
 const [pets,setPets] = useState<PetDTO[]>([]); const [species,setSpecies] = useState<{id:number;name:string}[]>([]);
 const [loading,setLoading] = useState(true);const [error,setError] = useState(false);const [retry,setRetry] = useState(0);
 const [editing,setEditing] = useState<PetDTO|null>(null);const [show,setShow] = useState(false);const [busy,setBusy] = useState(false);
 useEffect(() => {const controller = new AbortController();setLoading(true);setError(false);
  Promise.all([apiClient.get<PetDTO[]>('/pets/my-pets',{signal:controller.signal}),apiClient.get('/species',{signal:controller.signal})])
   .then(([p,s]) => {setPets(p.data);setSpecies(s.data);}).catch(() => {if(!controller.signal.aborted)setError(true);}).finally(() => {if(!controller.signal.aborted)setLoading(false);});
  return () => controller.abort();
 },[retry]);
 const save = async (e: FormEvent<HTMLFormElement>) => {e.preventDefault();const data = new FormData(e.currentTarget);setBusy(true);
  const payload = {name:data.get('name'),speciesId:Number(data.get('speciesId')),breedId:editing?.breedId,birthDate:data.get('birthDate') || null,description:data.get('description'),healthStatus:editing?.healthStatus || 'HEALTHY',availabilityStatus:editing?.availabilityStatus || 'UNAVAILABLE'};
  try {if(editing) await apiClient.put(`/pets/my-pets/${editing.id}`,payload);else await apiClient.post('/pets/my-pets',payload);setShow(false);setRetry(retry+1);toast.success('Đã lưu thú cưng');}
  catch {toast.error('Không lưu được thú cưng. Vui lòng kiểm tra thông tin.');}finally{setBusy(false);}
 };
 return <div className="max-w-5xl mx-auto p-8"><h1 className="text-3xl font-bold mb-6">Thú cưng của tôi</h1><button onClick={() => {setEditing(null);setShow(true);}}>+ Thêm thú cưng</button>
 {show && <form key={editing?.id || 'new'} onSubmit={save} className="grid gap-4 p-6 bg-white rounded-xl my-6"><label>Tên <input name="name" required maxLength={100} defaultValue={editing?.name}/></label>
 <label>Loài <select name="speciesId" required defaultValue={editing?.speciesId}>{species.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select></label>
 <label>Ngày sinh <input name="birthDate" type="date" defaultValue={editing?.birthDate}/></label><label>Ghi chú <textarea name="description" defaultValue={editing?.description}/></label>
 <button disabled={busy}>{busy?'Đang lưu…':'Lưu'}</button><button type="button" onClick={() => setShow(false)}>Đóng</button></form>}
 {loading?<p>Đang tải…</p>:error?<p role="alert">Không tải được thú cưng. <button onClick={() => setRetry(retry+1)}>Thử lại</button></p>:!pets.length?<p>Bạn chưa có thú cưng nào.</p>:pets.map(pet => <article key={pet.id} className="bg-white p-6 rounded-xl my-4"><h2 className="text-xl font-bold">{pet.name}</h2><p>{pet.speciesName} · {pet.breedName}</p><div className="flex gap-5"><Link to={`/my-pets/${pet.id}/health`}>Hồ sơ sức khỏe</Link><Link to={`/my-pets/${pet.id}/vaccinations`}>Tiêm phòng</Link><button onClick={() => {setEditing(pet);setShow(true);}}>Chỉnh sửa</button></div></article>)}
 </div>;
};
