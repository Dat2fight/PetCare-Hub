import { useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import api from '../../api/client';
import { usePermissions } from '../../contexts/PermissionContext';
import { useAdminResource } from '../../hooks/useAdminResource';
import { getErrorMessage } from '../../utils/getErrorMessage';
import { PageHeader, DataTable, PriceText, ErrorNotice, ConfirmDialog, AdminForm } from '../../components/admin/AdminUI';
import { ProductImage } from '../../components/common/ProductImage';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { useQueue, Filters, Row, ActionDialog, labels, timeText } from './OperationsUI';
import { catalogs } from './CatalogConfig';
import { CatalogForm } from './CatalogForms';
export function CatalogList() {
    const { resource = 'products' } = useParams();
    const def = catalogs[resource];
    const r = useQueue('/admin/catalog/' + resource);
    const { permissions } = usePermissions();
    const [selected, setSelected] = useState<number[]>([]);
    if (!def)
        return <PageHeader title="Không tìm thấy danh mục"/>;
    const canWrite = permissions.includes(def.permission);
    return <><PageHeader title={def.title} actions={canWrite ? <Link className="underline" to={`/admin/catalog/${resource}/new`}>Tạo mới</Link> : undefined}/>{resource === 'products' && <LowStock />}{resource === 'products' && canWrite && <div className="flex gap-4 flex-wrap mb-4"><Link className="underline" to={`/admin/product-batch?ids=${selected.join(',')}`}>Thay đổi hàng loạt ({selected.length} đã chọn)</Link><Link className="underline" to="/admin/product-import">Nhập CSV</Link></div>}{resource === 'promotions' && <p className="mb-4">Mã áp dụng cho tiền sản phẩm; không áp cho thú cưng hoặc gói chăm sóc.</p>}<Filters apply={q => { r.filter(q); setSelected([]); }} fields={[{ key: 'keyword', label: 'Tìm tên / mã' }, ...(def.fields.some(f => f.key === 'active') ? [{ key: 'active', label: 'Hoạt động', options: { '': 'Tất cả', true: 'Đang hoạt động', false: 'Ngừng hoạt động' } }] : [])]}/><DataTable {...r} retry={r.reload} onPage={r.setPage} onSort={r.setSort} columns={[...(resource === 'products' && canWrite ? [{ key: 'choose', label: 'Chọn', render: (x: Row) => <input type="checkbox" aria-label={`Chọn ${x.name}`} checked={selected.includes(x.id)} onChange={e => setSelected(e.target.checked ? [...selected, x.id] : selected.filter(id => id !== x.id))}/> }] : []), { key: 'id', label: 'ID', sort: 'id', render: x => x.id }, ...(resource === 'pets' || resource === 'products' ? [{ key: 'image', label: 'Ảnh', render: (x: Row) => <ProductImage src={x.imageUrl} alt={x.name} width={80} height={80} className="h-20 w-20 rounded-lg object-cover"/> }] : []), { key: 'name', label: 'Tên / mã', sort: resource === 'promotions' ? 'code' : 'name', render: x => <Link className="underline" to={`/admin/catalog/${resource}/${x.id}`}>{x.name || x.code}</Link> }, { key: 'price', label: 'Giá / thông tin', render: x => x.price != null ? <PriceText value={x.price}/> : resource === 'promotions' ? `${x.currentUses} lượt sử dụng` : x.speciesName || x.displayOrder || '—' }, { key: 'state', label: 'Trạng thái', render: x => x.active != null ? (x.active ? 'Hoạt động' : 'Đã ngừng') : x.orderId ? <Link className="underline" to={`/admin/orders/${x.orderId}`}>Giữ chỗ: {x.orderNumber}</Link> : x.availabilityStatus || '—' }]}/></>;
}
export function CatalogEdit() {
    const { resource = 'products', id } = useParams();
    const def = catalogs[resource];
    const fresh = id === 'new';
    const r = useAdminResource<Row>(`/admin/catalog/${resource}${fresh ? '?size=1' : '/' + id}`);
    const navigate = useNavigate();
    const { permissions } = usePermissions();
    const [remove, setRemove] = useState(false);
    const [release, setRelease] = useState(false);
    const [reason, setReason] = useState('');
    const [error, setError] = useState('');
    const [busy, setBusy] = useState(false);
    if (!def)
        return <PageHeader title="Không tìm thấy danh mục"/>;
    if (r.error)
        return <ErrorNotice message={r.error} retry={r.reload}/>;
    if (!r.data)
        return <p>Đang tải…</p>;
    const canWrite = permissions.includes(def.permission);
    const data: Row = fresh ? { id: 0 } : r.data;
    const destroy = async () => { if (busy)
        return; setBusy(true); setError(''); try {
        await api.delete(`/admin/catalog/${resource}/${id}`, { data: { reason } });
        navigate(`/admin/catalog/${resource}`);
    }
    catch (e) {
        setError(getErrorMessage(e));
    }
    finally {
        setBusy(false);
    } };
    return <><Link className="underline" to={`/admin/catalog/${resource}`}>← {def.title}</Link><PageHeader title={fresh ? 'Tạo ' + def.title.toLowerCase() : `${def.title} #${id}`} actions={!fresh && canWrite ? <Button variant="danger" onClick={() => setRemove(true)}>Xóa</Button> : undefined}/>{resource === 'pets' && <p className="my-4 rounded-xl bg-primary-50 p-4 text-sm">Hồ sơ tìm thú cưng phù hợp: nhập mức vận động, khả năng sống trong căn hộ, mức phù hợp với trẻ nhỏ và giờ chăm sóc tối thiểu từ đánh giá thực tế của từng bé. Chọn “Chưa đánh giá” hoặc để trống khi chưa biết; bé chỉ được gợi ý khi đủ cả 4 thông số. Sau khi lưu, hồ sơ được dùng trong lần tìm tiếp theo.</p>}{resource === 'pets' && !fresh && data.orderId && <div className="mb-5 rounded-xl border p-4"><p>Đơn giữ chỗ: <Link className="underline" to={`/admin/orders/${data.orderId}`}>{data.orderNumber}</Link> · Khách #{data.customerId}</p><p>Hết hạn: {timeText(data.expiresAt)}</p><Button variant="danger" onClick={() => setRelease(true)}>Hủy đơn và nhả giữ chỗ</Button></div>}{resource === 'promotions' && !fresh && <Usage id={id!}/>}{canWrite ? <CatalogForm key={resource + id} fields={def.fields} data={data} url={`/admin/catalog/${resource}${fresh ? '' : '/' + id}`} method={fresh ? 'post' : 'put'} onDone={() => navigate(`/admin/catalog/${resource}`)} warning={resource === 'products' ? 'Form không thay đổi số tồn. Dùng trang Kho để nhập/xuất/kiểm kê.' : resource === 'pets' ? 'Thú cưng có chủ hoặc từng gắn đơn không được sửa/xóa trực tiếp.' : undefined}/> : <dl className="space-y-3">{def.fields.map(f => <div key={f.key}><dt className="font-semibold">{f.label}</dt><dd className="break-words">{String(data[f.key] ?? '—')}</dd></div>)}</dl>}{remove && <ConfirmDialog title="Xóa dữ liệu" busy={busy} onClose={() => setRemove(false)}><AdminForm busy={busy} error={error} onSubmit={e => { e.preventDefault(); void destroy(); }}><p>Chỉ xóa khi không còn dữ liệu tham chiếu. Với dữ liệu đã sử dụng, hãy ngừng hoạt động.</p><Input label="Lý do xóa" required value={reason} maxLength={500} onChange={e => setReason(e.target.value)}/><Button variant="danger" type="submit" isLoading={busy}>Xác nhận xóa</Button></AdminForm></ConfirmDialog>}{release && <ActionDialog title="Hủy toàn bộ đơn giữ chỗ" url={`/admin/catalog/pets/${id}/release-reservation`} fields={[{ key: 'reason', label: 'Lý do' }]} warning="Toàn bộ đơn gồm thú cưng và sản phẩm sẽ bị hủy; hoàn tồn, mã và điểm đã dùng, thông báo cho khách." onClose={() => setRelease(false)} onDone={r.reload}/>}</>;
}
function Usage({ id }: {
    id: string;
}) { const r = useQueue('/admin/promotion-usages/' + id); return <details className="my-4"><summary>Đơn đã dùng mã</summary><DataTable {...r} retry={r.reload} onPage={r.setPage} columns={[{ key: 'order', label: 'Đơn', render: x => <Link className="underline" to={`/admin/orders/${x.orderId}`}>{x.orderNumber}</Link> }, { key: 'customer', label: 'Khách', render: x => `#${x.customerId}` }, { key: 'date', label: 'Thời điểm', render: x => timeText(x.createdAt) }]}/></details>; }
export function PetSupport() { const [input, setInput] = useState(''); const [id, setId] = useState(''); return <><PageHeader title="Hỗ trợ thú cưng của khách" description="Chỉ đọc; mỗi lần xem được ghi nhật ký. Bác sĩ chỉ xem thú cưng có lịch với mình."/><form onSubmit={e => { e.preventDefault(); setId(input); }} className="flex flex-wrap gap-3"><Input label="ID thú cưng" type="number" min={1} required value={input} onChange={e => setInput(e.target.value)}/><Button type="submit">Xem hồ sơ</Button></form>{id && <SupportResult key={id} id={id}/>}</>; }
function SupportResult({ id }: {
    id: string;
}) { const r = useAdminResource<Row>('/admin/pet-support/' + id); return r.error ? <ErrorNotice message={r.error} retry={r.reload}/> : r.data ? <section className="mt-4 rounded-xl border p-4"><h2>{r.data.name}</h2><p>{r.data.species} · {r.data.birthDate || 'Chưa có ngày sinh'} · {labels[r.data.healthStatus] || r.data.healthStatus}</p><p>Chủ: #{r.data.ownerId}</p></section> : <p>Đang tải…</p>; }
function LowStock() { const r = useAdminResource<{
    count: number;
    quantity: number;
}>('/admin/catalog/products/low-stock'); return r.error ? <ErrorNotice message={r.error} retry={r.reload}/> : r.data ? <p className="mb-4">{r.data.count} sản phẩm đang hoạt động chạm ngưỡng tồn thấp · <Link className="underline" to="/admin/inventory">Xem kho</Link></p> : <p>Đang tổng hợp tồn kho…</p>; }
