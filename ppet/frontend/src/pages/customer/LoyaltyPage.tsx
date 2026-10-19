import { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import apiClient from '../../api/client';
interface Account { id: number; currentPoints: number; tier: string; nextTier: string | null; pointsToNextTier: number; thresholds: Record<string, number> }
interface Transaction { id: number; points: number; description: string; transactionDate: string }
export const LoyaltyPage = () => {
  const { user } = useAuth(); const [account, setAccount] = useState<Account | null>(null); const [history, setHistory] = useState<Transaction[]>([]);
  const [page, setPage] = useState(0); const [error, setError] = useState(false); const [loading, setLoading] = useState(true); const [retry, setRetry] = useState(0);
  useEffect(() => { if (!user) return; const controller = new AbortController(); setLoading(true); setError(false);
    (async () => { try { const {data} = await apiClient.get<Account>('/loyalty/account', {signal: controller.signal});
      const tx = await apiClient.get<Transaction[]>('/loyalty/transactions', {params: {page, size:20}, signal: controller.signal});
      if (!controller.signal.aborted) { setAccount(data); setHistory(tx.data); }
    } catch { if (!controller.signal.aborted) setError(true); } finally { if (!controller.signal.aborted) setLoading(false); } })();
    return () => controller.abort();
  }, [user?.id, page, retry]);
  return <div className="max-w-4xl mx-auto p-8"><h1 className="text-3xl font-bold mb-6">Điểm thưởng và hạng thành viên</h1>
    {loading ? <p>Đang tải…</p> : error ? <p role="alert">Không tải được điểm thưởng. <button onClick={() => setRetry(retry+1)}>Thử lại</button></p> : account && <>
      <p className="text-2xl">{account.currentPoints} điểm · {account.tier}</p><p>{account.nextTier ? `Còn ${account.pointsToNextTier} điểm để lên ${account.nextTier}` : 'Đã đạt hạng cao nhất'}</p>
      <p>{Object.entries(account.thresholds).map(([tier, points]) => `${tier}: ${points}`).join(' · ')}</p>
      <h2 className="text-xl mt-6">Lịch sử điểm</h2>{!history.length ? <p>Chưa có giao dịch điểm.</p> : history.map(tx => <p key={tx.id}>{tx.description} · {tx.points} điểm · {new Date(tx.transactionDate).toLocaleDateString('vi-VN')}</p>)}
      <nav className="flex gap-6 mt-6"><button disabled={!page} onClick={() => setPage(page-1)}>Trang trước</button><button disabled={history.length < 20} onClick={() => setPage(page+1)}>Trang sau</button></nav>
    </>}
  </div>;
};
