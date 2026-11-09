import { HeartButton, useWishlistStatus } from '../../components/customer/Wishlist';
import { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { PetPurchaseModal } from '../../components/pets/PetPurchaseModal';
import { Button } from '../../components/common/Button';
import { useLocation, useNavigate, useParams, Link } from 'react-router-dom';
import { PublicPetDTO, petManagementService } from '../../services/petManagementService';

export const PetDetailPage = () => {
  const {user, isLoading} = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const { id } = useParams();
  const [pet, setPet] = useState<PublicPetDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  const [loadedId, setLoadedId] = useState(id);
  const [notFound, setNotFound] = useState(false);
  const validId = !!id && /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id));
  useEffect(() => {
    const controller = new AbortController(); setPet(null); setError(''); setLoading(true); setNotFound(false); setLoadedId(id);
    if (!validId) { setNotFound(true); setLoading(false); return () => controller.abort(); }
    petManagementService.getPublicPet(Number(id), controller.signal).then(data => { if (!controller.signal.aborted) setPet(data); })
      .catch(error => { if (!controller.signal.aborted) { if (error.response?.status === 404) setNotFound(true); else setError('Không thể tải thông tin thú cưng'); } })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [id, retry, validId]);
  const hearts=useWishlistStatus('PET',pet?[pet.id]:[]);
  if (!validId || (loadedId === id && notFound)) return <div role="alert" className="p-8">Không tìm thấy thú cưng. <Link to="/pets">Quay lại danh sách</Link></div>;
  if (loading || loadedId !== id) return <p className="p-8">Đang tải…</p>;
  if (error || !pet) return <div role="alert" className="p-8">{error || 'Không tìm thấy thú cưng'} <button onClick={() => setRetry(retry + 1)}>Thử lại</button> <Link to="/pets">Danh sách thú cưng</Link></div>;
  return <article className="max-w-4xl mx-auto p-8 bg-white rounded-2xl my-8"><Link to="/pets">Thú cưng</Link><h1 className="text-3xl font-bold my-4">{pet.name}</h1>
    {pet.imageUrl && <img src={pet.imageUrl} alt={pet.name} className="w-full max-h-96 object-contain" />}
    <p className="my-4">{pet.speciesName} · {pet.breedName} · {pet.age == null ? 'Chưa có tuổi' : `${pet.age} tuổi`}</p>
    {pet.price != null && <p className="text-xl font-bold">{pet.price.toLocaleString('vi-VN')} ₫</p>}
    <p>{pet.description}</p><HeartButton type="PET" id={pet.id} status={hearts}/>
    {pet.availabilityStatus === 'AVAILABLE' && Number(pet.price) > 0
      ? (!user || user.roles.every(role => role === 'CUSTOMER')) && <Button disabled={isLoading}
          onClick={()=>user ? setOpen(true) : navigate('/login', {state:{from:location}})}>Thêm vào giỏ</Button>
      : <p>{['RESERVED','PENDING'].includes(pet.availabilityStatus) ? 'Đang được đặt' : 'Hiện chưa thể mua'}</p>}
    {open && <PetPurchaseModal pet={pet} onClose={()=>setOpen(false)} onConflict={()=>{setOpen(false);setRetry(n=>n+1);}}/>}
  </article>;
};
