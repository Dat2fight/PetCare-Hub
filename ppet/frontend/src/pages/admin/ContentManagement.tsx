import { useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import api from '../../api/client';
import { useAdminResource } from '../../hooks/useAdminResource';
import { getErrorMessage } from '../../utils/getErrorMessage';
import { SafeContent } from '../../components/common/SafeContent';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { PageHeader, DataTable, ConfirmDialog, ErrorNotice, Select, AdminForm } from '../../components/admin/AdminUI';
import { Row, useQueue, Filters, ActionDialog } from './OperationsUI';
export function ContentQueue() { const r = useQueue('/admin/content'); return <><PageHeader title="Nội dung website" actions={<Link className="underline" to="/admin/content/new">Tạo nội dung</Link>}/><Filters apply={r.filter} fields={[{ key: 'keyword', label: 'Tìm tiêu đề' }, { key: 'kind', label: 'Loại', options: { '': 'Tất cả', PAGE: 'Trang cố định', ARTICLE: 'Bài viết', FAQ: 'Câu hỏi thường gặp' } }]}/><DataTable {...r} retry={r.reload} onPage={r.setPage} columns={[{ key: 'title', label: 'Tiêu đề', render: c => <Link className="underline" to={'/admin/content/' + c.id}>{c.title}</Link> }, { key: 'slug', label: 'Đường dẫn', render: c => c.slug }, { key: 'kind', label: 'Loại', render: c => c.kind }, { key: 'state', label: 'Xuất bản', render: c => c.publishedAt ? 'Đang công khai (có thể có nháp mới)' : 'Bản nháp' }]}/></>; }
export function ContentEditor() { const { id } = useParams(); return id === 'new' ? <Editor key="new"/> : <LoadedEditor id={id!}/>; }
function LoadedEditor({ id }: {
    id: string;
}) { const r = useAdminResource<Row>('/admin/content/' + id); return r.error ? <ErrorNotice message={r.error} retry={r.reload}/> : !r.data ? <p>Đang tải…</p> : <Editor key={JSON.stringify(r.data)} data={r.data} reload={r.reload}/>; }
function Editor({ data, reload }: {
    data?: Row;
    reload?: () => void;
}) {
    const navigate = useNavigate();
    const [slug, setSlug] = useState(data?.slug || '');
    const [kind, setKind] = useState(data?.kind || 'PAGE');
    const [title, setTitle] = useState(data?.title || '');
    const [body, setBody] = useState(data?.body || '');
    const [products, setProducts] = useState((data?.configuration?.productIds || []).join(','));
    const [pets, setPets] = useState((data?.configuration?.petIds || []).join(','));
    const [confirm, setConfirm] = useState<'save' | 'delete'>();
    const [action, setAction] = useState<'publish' | 'unpublish'>();
    const [reason, setReason] = useState('');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const save = async () => { setBusy(true); setError(''); try {
        if (confirm === 'delete') {
            await api.delete('/admin/content/' + data!.id, { data: { reason } });
            navigate('/admin/content');
        }
        else {
            const ids = (v: string) => v.trim() ? v.split(',').map(x => Number(x.trim())) : [];
            const payload = { slug, kind, title, body, configuration: slug === 'home' ? { productIds: ids(products), petIds: ids(pets) } : {} };
            const result = await api[data ? 'put' : 'post']('/admin/content' + (data ? '/' + data.id : ''), payload);
            setConfirm(undefined);
            if (data)
                reload?.();
            else
                navigate('/admin/content/' + result.data.id);
        }
    }
    catch (e) {
        setError(getErrorMessage(e));
    }
    finally {
        setBusy(false);
    } };
    return <><PageHeader title={data ? 'Soạn nội dung' : 'Tạo nội dung'} description="Lưu nháp không thay đổi bản công khai. Khôi phục phiên bản cũng chỉ thay đổi nháp; bấm xuất bản khi đã duyệt."/><p className="mb-4">Trang cố định: home, contact, terms, privacy, shipping-policy, return-policy, cancellation-policy. Bài viết/FAQ dùng slug riêng. Nội dung hỗ trợ #, ## và - đầu dòng; HTML hiển thị dưới dạng chữ.</p><AdminForm busy={busy} error={error} onSubmit={e => { e.preventDefault(); setConfirm('save'); }}><Input label="Slug" required disabled={!!data} maxLength={100} value={slug} onChange={e => setSlug(e.target.value)}/>{!data && <Select label="Loại nội dung" value={kind} onChange={setKind}><option value="PAGE">Trang cố định</option><option value="ARTICLE">Bài viết</option><option value="FAQ">Câu hỏi thường gặp</option></Select>}<Input label="Tiêu đề" required maxLength={200} value={title} onChange={e => setTitle(e.target.value)}/><label className="block">Nội dung<textarea required maxLength={20000} rows={10} className="w-full border rounded-xl p-3" value={body} onChange={e => setBody(e.target.value)}/></label>{slug === 'home' && <><Input label="ID sản phẩm nổi bật (cách nhau bằng dấu phẩy, tối đa 12)" value={products} onChange={e => setProducts(e.target.value)}/><Input label="ID thú cưng nổi bật (cách nhau bằng dấu phẩy, tối đa 12)" value={pets} onChange={e => setPets(e.target.value)}/><p>Chọn ID đang bán ở Danh mục. Để trống sẽ ẩn nhóm nổi bật tương ứng khi xuất bản.</p></>}<Button type="submit">Lưu bản nháp</Button></AdminForm><section className="border rounded-xl p-4 my-5"><h2 className="font-bold text-xl mb-3">Xem trước: {title}</h2><SafeContent text={body}/></section>{data && <><div className="flex flex-wrap gap-3"><Button onClick={() => setAction('publish')}>Xuất bản bản nháp đã lưu</Button><Button variant="outline" disabled={!data.published} onClick={() => setAction('unpublish')}>Hủy xuất bản</Button><Button variant="danger" disabled={data.published} onClick={() => setConfirm('delete')}>Xóa bản nháp</Button></div><Revisions id={data.id} reload={reload!}/></>}{confirm && <ConfirmDialog title={confirm === 'delete' ? 'Xóa nội dung' : 'Lưu bản nháp'} busy={busy} onClose={() => setConfirm(undefined)}><AdminForm busy={busy} error={error} onSubmit={e => { e.preventDefault(); void save(); }}>{confirm === 'delete' && <Input required label="Lý do xóa" maxLength={500} value={reason} onChange={e => setReason(e.target.value)}/>}<Button type="submit">Xác nhận</Button></AdminForm></ConfirmDialog>}{action && <ActionDialog title={action === 'publish' ? 'Xuất bản nội dung' : 'Hủy xuất bản'} url={'/admin/content/' + data!.id + '/' + action} onClose={() => setAction(undefined)} onDone={reload!}/>}</>;
}
function Revisions({ id, reload }: {
    id: number;
    reload: () => void;
}) { const r = useQueue('/admin/content/' + id + '/revisions'); const [selected, setSelected] = useState<Row>(); return <section className="my-6"><h2 className="text-xl font-bold">20 phiên bản gần nhất</h2><DataTable {...r} retry={r.reload} onPage={r.setPage} columns={[{ key: 'id', label: 'Phiên bản', render: x => `#${x.id}` }, { key: 'preview', label: 'Nội dung', render: x => <details><summary>Xem</summary><SafeContent text={JSON.parse(x.snapshot).body}/></details> }, { key: 'restore', label: 'Khôi phục', render: x => <Button variant="outline" onClick={() => setSelected(x)}>Khôi phục nháp</Button> }]}/>{selected && <ActionDialog title="Khôi phục thành bản nháp" url={'/admin/content/' + id + '/restore'} body={{ revisionId: selected.id }} onClose={() => setSelected(undefined)} onDone={reload}/>}</section>; }
