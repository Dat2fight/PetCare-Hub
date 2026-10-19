import { useEffect, useState } from 'react';
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
  useEffect(() => {
    const controller = new AbortController(); setPet(null); setError(''); setLoading(true);
    apiClient.get<PublicPetDTO>(`/pets/${id}`, { signal: controller.signal }).then(({ data }) => setPet(data))
      .catch(error => { if (!controller.signal.aborted) setError(error.response?.status === 404 ? 'Không tìm thấy thú cưng' : 'Không thể tải thông tin thú cưng'); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [id, retry]);
  if (loading) return <p className="p-8">Đang tải…</p>;
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
    <p className="my-4 font-semibold">{status}</p>
    {customer && <Button disabled={!canBuy || sessionLoading} onClick={() => {
      if (!user) { navigate('/login', { state: { from: location } }); return; }
      setPurchaseOpen(true);
    }}>Đặt mua thú cưng</Button>}
    {purchaseOpen && user && customer && <PetPurchaseModal pet={pet} onClose={() => setPurchaseOpen(false)} onConflict={() => { setPurchaseOpen(false); setRetry(value => value + 1); }} />}
  </article>;
};
