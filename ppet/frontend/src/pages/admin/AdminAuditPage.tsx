import { useState } from 'react';
import { useAdminResource } from '../../hooks/useAdminResource';
import { DataTable, Page, PageHeader, FilterBar, StatusBadge } from '../../components/admin/AdminUI';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
interface Audit { id: number; occurredAt: string; actorUserId: number; actorRole: string; action: string; targetType: string; targetId: number | null; result: string; details: string; correlationId: string; ip: string }
export default function AdminAuditPage() {
  const [page, setPage] = useState(0); const [filters, setFilters] = useState('');
  const [form, setForm] = useState({ actor: '', action: '', targetType: '', target: '', from: '', to: '' });
  const r = useAdminResource<Page<Audit>>(`/admin/audit-logs?page=${page}&size=20${filters}`);
  return <><PageHeader title="Nhật ký thao tác" description="Theo dõi người thực hiện, đối tượng và kết quả. Nhật ký không thể sửa hoặc xóa." />
    <FilterBar onSubmit={e => { e.preventDefault(); setPage(0); setFilters(`&${new URLSearchParams(Object.fromEntries(Object.entries(form).filter(([, value]) => value)))}`); }}>
      {([['actor', 'ID người thực hiện'], ['action', 'Mã thao tác'], ['targetType', 'Loại đối tượng'], ['target', 'ID đối tượng'], ['from', 'Từ ngày'], ['to', 'Đến ngày']] as const).map(([key, label]) => <Input key={key} label={label} type={key === 'from' || key === 'to' ? 'date' : key === 'actor' || key === 'target' ? 'number' : 'text'} value={form[key]} onChange={e => setForm({ ...form, [key]: e.target.value })} />)}
      <Button type="submit">Lọc nhật ký</Button>
    </FilterBar>
    <DataTable {...r} page={r.data} retry={r.reload} onPage={setPage} columns={[
      { key: 'time', label: 'Thời gian', render: a => new Date(a.occurredAt).toLocaleString('vi-VN') },
      { key: 'actor', label: 'Người thực hiện', render: a => <>#{a.actorUserId}<p className="text-xs">{a.actorRole}</p></> },
      { key: 'action', label: 'Thao tác / đối tượng', render: a => <>{a.action}<p>{a.targetType}{a.targetId ? ` #${a.targetId}` : ''}</p></> },
      { key: 'result', label: 'Kết quả', render: a => <StatusBadge>{({ SUCCESS: 'Thành công', DENIED: 'Bị từ chối', FAILED: 'Thất bại' })[a.result] || a.result}</StatusBadge> },
      { key: 'details', label: 'Chi tiết', render: a => <details><summary className="cursor-pointer py-2 underline">Xem chi tiết</summary><pre className="whitespace-pre-wrap break-all text-xs">{a.details}</pre><p className="mt-2 text-xs break-all">Mã đối chiếu: {a.correlationId}</p><p className="text-xs">IP: {a.ip || 'Không có'}</p></details> },
    ]} /></>;
}
