import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { PublicPetDTO, petManagementService } from '../../services/petManagementService';
import { useParams, Link, useLocation, useNavigate } from 'react-router-dom';
import apiClient from '../../api/client';
import { PublicPetDTO } from '../../services/petManagementService';
import { useAuth } from '../../contexts/AuthContext';
import { Button } from '../../components/common/Button';
import { PetPurchaseModal } from '../../components/pets/PetPurchaseModal';

export const PetDetailPage = () => {
  const { id } = useParams();
  const { user, isLoading: sessionLoading } = useAuth();
  const location = useLocation(); const navigate = useNavigate();
  const [purchaseOpen, setPurchaseOpen] = useState(false);
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
  if (!validId || (loadedId === id && notFound)) return <div role="alert" className="p-8">Không tìm thấy thú cưng. <Link to="/pets">Quay lại danh sách</Link></div>;
  if (loading || loadedId !== id) return <p className="p-8">Đang tải…</p>;
  if (error || !pet) return <div role="alert" className="p-8">{error || 'Không tìm thấy thú cưng'} <button onClick={() => setRetry(retry + 1)}>Thử lại</button> <Link to="/pets">Danh sách thú cưng</Link></div>;
  const customer = !user || (user.roles.includes('CUSTOMER') && !user.roles.some(role => ['ADMIN', 'SALES_STAFF', 'WAREHOUSE_STAFF', 'VETERINARIAN'].includes(role)));
  // Public detail only contains unowned sale pets; SOLD is a redacted status-only response.
  const canBuy = pet.availabilityStatus === 'AVAILABLE' && (pet.price ?? 0) > 0;
  const status = pet.availabilityStatus === 'PENDING' || pet.availabilityStatus === 'RESERVED' ? 'Đang được đặt' : pet.availabilityStatus === 'SOLD' ? 'Đã bán' : canBuy ? 'Còn bán' : 'Chưa mở bán';
  return <article className="max-w-4xl mx-auto p-8 bg-white rounded-2xl my-8"><Link to="/pets">Thú cưng</Link><h1 className="text-3xl font-bold my-4">{pet.name}</h1>
    {pet.imageUrl && <img src={pet.imageUrl} alt={pet.name} className="w-full max-h-96 object-contain" />}
    <p className="my-4">{pet.speciesName} · {pet.breedName} · {pet.age == null ? 'Chưa có tuổi' : `${pet.age} tuổi`}</p>
    {pet.price != null && <p className="text-xl font-bold">{pet.price.toLocaleString('vi-VN')} ₫</p>}
    <p>{pet.description}</p>
    {pet.availabilityStatus === 'RESERVED' && <p className="mt-4 font-semibold">Thú cưng đang được đặt</p>}
    {/* NEEDS DECISION: integrate the pet purchase branch before enabling checkout. */}
    <button disabled aria-disabled="true" className="mt-4 rounded-xl bg-gray-100 px-6 py-3 text-gray-600">
      {pet.availabilityStatus === 'RESERVED' ? 'Đang được đặt' : pet.availabilityStatus === 'SOLD' ? 'Đã bán' : 'Đặt mua — Sắp ra mắt'}
    </button>
    <p className="my-4 font-semibold">{status}</p>
    {customer && <Button disabled={!canBuy || sessionLoading} onClick={() => {
      if (!user) { navigate('/login', { state: { from: location } }); return; }
      setPurchaseOpen(true);
    }}>Đặt mua thú cưng</Button>}
    {purchaseOpen && user && customer && <PetPurchaseModal pet={pet} onClose={() => setPurchaseOpen(false)} onConflict={() => { setPurchaseOpen(false); setRetry(value => value + 1); }} />}
  </article>;
};
