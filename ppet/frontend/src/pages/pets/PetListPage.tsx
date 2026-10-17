import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../../api/client';
import { PetDTO } from '../../services/petManagementService';

export const PetListPage = () => {
  const [pets, setPets] = useState<PetDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const controller = new AbortController(); setLoading(true); setError(false);
    apiClient.get('/pets', { params: { page, size: 20, availabilityStatus: 'AVAILABLE' }, signal: controller.signal })
      .then(({ data }) => { setPets(data.content); setTotalPages(data.totalPages); })
      .catch(() => { if (!controller.signal.aborted) setError(true); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [page, retry]);
  return <div className="max-w-7xl mx-auto p-8"><h1 className="text-3xl font-bold mb-8">Tìm người bạn bốn chân</h1>
    {loading ? <p>Đang tải…</p> : error ? <div role="alert">Không tải được thú cưng. <button onClick={() => setRetry(retry + 1)}>Thử lại</button></div> : pets.length === 0 ? <p>Chưa có thú cưng phù hợp.</p> :
      <div className="grid md:grid-cols-3 gap-6">{pets.map(pet => <Link className="bg-white rounded-2xl shadow p-5" key={pet.id} to={`/pets/${pet.id}`}>
        {pet.imageUrl && <img className="w-full h-64 object-cover rounded-xl" src={pet.imageUrl} alt={pet.name} />}
        <h2 className="text-xl font-bold mt-4">{pet.name}</h2><p>{pet.speciesName} · {pet.breedName}</p>
        <p>{pet.age == null ? 'Chưa có ngày sinh' : `${pet.age} tuổi`}</p>
        {pet.price != null && <p>{pet.price.toLocaleString('vi-VN')} ₫</p>}
      </Link>)}</div>}
    <nav className="flex gap-6 mt-8"><button disabled={page === 0} onClick={() => setPage(page - 1)}>Trang trước</button><span>{totalPages ? page + 1 : 0}/{totalPages}</span><button disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>Trang sau</button></nav>
  </div>;
};
