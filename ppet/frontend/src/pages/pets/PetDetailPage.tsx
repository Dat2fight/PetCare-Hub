import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import apiClient from '../../api/client';
import { PublicPetDTO } from '../../services/petManagementService';

export const PetDetailPage = () => {
  const { id } = useParams();
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
  return <article className="max-w-4xl mx-auto p-8 bg-white rounded-2xl my-8"><Link to="/pets">Thú cưng</Link><h1 className="text-3xl font-bold my-4">{pet.name}</h1>
    {pet.imageUrl && <img src={pet.imageUrl} alt={pet.name} className="w-full max-h-96 object-contain" />}
    <p className="my-4">{pet.speciesName} · {pet.breedName} · {pet.age == null ? 'Chưa có tuổi' : `${pet.age} tuổi`}</p>
    {pet.price != null && <p className="text-xl font-bold">{pet.price.toLocaleString('vi-VN')} ₫</p>}
    <p>{pet.description}</p>
  </article>;
};
