import { useState, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../../api/client';
import { PublicPetDTO } from '../../services/petManagementService';

interface Match { pet: PublicPetDTO; score: number; explanation: string }
export const PetMatchingPage = () => {
  const [home, setHome] = useState('APARTMENT');
  const [children, setChildren] = useState(false);
  const [activity, setActivity] = useState('MEDIUM');
  const [hours, setHours] = useState(2);
  const [results, setResults] = useState<Match[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setLoading(true); setError(''); setResults(null);
    try { const { data } = await apiClient.post<Match[]>('/pet-matching/submit', { livingEnvironment: home, houseSize: home, presenceOfChildren: children, activityLevel: activity, availableCareTime: hours }); setResults(data); }
    catch { setError('Không tải được kết quả. Vui lòng thử lại.'); }
    finally { setLoading(false); }
  };
  return <div className="max-w-4xl mx-auto p-8"><h1 className="text-3xl font-bold mb-6">Tìm thú cưng phù hợp</h1>
    <form onSubmit={submit} className="grid gap-5 bg-white p-6 rounded-2xl">
      <label>Môi trường sống <select value={home} onChange={e => setHome(e.target.value)}><option value="APARTMENT">Căn hộ</option><option value="HOUSE">Nhà riêng</option></select></label>
      <label><input type="checkbox" checked={children} onChange={e => setChildren(e.target.checked)} /> Gia đình có trẻ nhỏ</label>
      <label>Mức vận động <select value={activity} onChange={e => setActivity(e.target.value)}><option value="LOW">Thấp</option><option value="MEDIUM">Vừa</option><option value="HIGH">Cao</option></select></label>
      <label>Số giờ chăm sóc mỗi ngày <input type="number" min="0" max="24" required value={hours} onChange={e => setHours(Number(e.target.value))} /></label>
      <button className="bg-primary-600 text-white rounded-xl p-3" disabled={loading}>{loading ? 'Đang phân tích…' : 'Tìm thú cưng'}</button>
    </form>
    {error && <p role="alert">{error}</p>}
    {results?.length === 0 && <p>Chưa có thú cưng phù hợp.</p>}
    {results?.map(result => <article key={result.pet.id} className="bg-white p-6 rounded-2xl my-4"><Link to={`/pets/${result.pet.id}`} className="font-bold">{result.pet.name} · {result.pet.breedName}</Link><p>Độ phù hợp: {result.score}%</p><p>{result.explanation}</p></article>)}
  </div>;
};
