import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../api/client';
import { getErrorMessage } from '../../utils/getErrorMessage';
import { PageHeader, DataTable, ErrorNotice, Select, ConfirmDialog } from '../../components/admin/AdminUI';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { CatalogForm } from './CatalogForms';
import { CatalogField } from './CatalogConfig';
import { Row, useQueue, timeText } from './OperationsUI';
export function NotificationComposer() {
    const [group, setGroup] = useState('SINGLE');
    const [userId, setUserId] = useState('');
    const [role, setRole] = useState('CUSTOMER');
    const [tier, setTier] = useState('BRONZE');
    const [days, setDays] = useState('90');
    const [template, setTemplate] = useState<Row>();
    const [title, setTitle] = useState('');
    const [message, setMessage] = useState('');
    const [preview, setPreview] = useState<Row>();
    const [confirm, setConfirm] = useState(false);
    const [key, setKey] = useState(() => crypto.randomUUID());
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const [batch, setBatch] = useState<number>();
    const reset = () => { setPreview(undefined); setKey(crypto.randomUUID()); setBatch(undefined); };
    const audience = () => ({ group, ...(group === 'SINGLE' ? { userId: Number(userId) } : group === 'ROLE' ? { role } : group === 'TIER' ? { tier } : { days: Number(days) }) });
    const check = async () => { setBusy(true); setError(''); try {
        setPreview((await api.get('/admin/notifications/audience-preview', { params: audience() })).data);
    }
    catch (e) {
        setError(getErrorMessage(e));
    }
    finally {
        setBusy(false);
    } };
    const send = async () => { setBusy(true); setError(''); try {
        const { data } = await api.post('/admin/notifications/send', { ...audience(), ...(template ? { templateId: template.id } : { title, message }), requestKey: key, audienceToken: preview?.audienceToken });
        setBatch(data.id);
        setKey(crypto.randomUUID());
        setPreview(undefined);
        setConfirm(false);
    }
    catch (e) {
        setError(getErrorMessage(e));
    }
    finally {
        setBusy(false);
    } };
    return <><PageHeader title="Gửi thông báo trên web" description="Tối đa 500 người mỗi đợt, một đợt mỗi phút. Máy chủ gửi theo lô; theo dõi kết quả ở lịch sử." actions={<Link className="underline" to="/admin/notifications/history">Lịch sử gửi</Link>}/><form onSubmit={e => { e.preventDefault(); void check(); }}><fieldset disabled={busy} className="space-y-4"><Select label="Nhóm người nhận" value={group} onChange={v => { setGroup(v); reset(); }}><option value="SINGLE">Một tài khoản</option><option value="ROLE">Theo vai trò</option><option value="TIER">Theo hạng thành viên</option><option value="INACTIVE">Khách lâu không mua</option></Select>{group === 'SINGLE' ? <Input type="number" min={1} label="ID người nhận" required value={userId} onChange={e => { setUserId(e.target.value); reset(); }}/> : group === 'ROLE' ? <Select label="Vai trò" value={role} onChange={v => { setRole(v); reset(); }}>{['CUSTOMER', 'ADMIN', 'SALES_STAFF', 'WAREHOUSE_STAFF', 'VETERINARIAN'].map(r => <option key={r}>{r}</option>)}</Select> : group === 'TIER' ? <Select label="Hạng" value={tier} onChange={v => { setTier(v); reset(); }}>{['BRONZE', 'SILVER', 'GOLD', 'PLATINUM'].map(r => <option key={r}>{r}</option>)}</Select> : <Input label="Không mua trong số ngày" type="number" min={1} max={3650} value={days} onChange={e => { setDays(e.target.value); reset(); }}/>}<TemplatePicker choose={t => { setTemplate(t); reset(); }}/>{template ? <div className="border rounded-xl p-4"><p className="font-bold">{template.title}</p><p className="whitespace-pre-wrap break-words">{template.body}</p><Button type="button" variant="outline" onClick={() => { setTemplate(undefined); reset(); }}>Soạn nội dung riêng</Button></div> : <><Input label="Tiêu đề" maxLength={200} required value={title} onChange={e => { setTitle(e.target.value); reset(); }}/><label className="block">Nội dung<textarea className="block border rounded-xl p-3 w-full" rows={5} maxLength={1500} required value={message} onChange={e => { setMessage(e.target.value); reset(); }}/></label><p>Chỉ văn bản thuần. Có thể dùng biến {'{firstName}'} do máy chủ thay bằng tên người nhận.</p><div className="border rounded-xl p-4"><h2 className="font-bold">Xem nội dung trước khi gửi</h2><p>{title}</p><p className="whitespace-pre-wrap break-words">{message}</p></div></>}<Button type="submit" isLoading={busy}>Xem trước người nhận</Button></fieldset></form>{error && <ErrorNotice message={error}/>}{preview && <div className="my-4"><p>{preview.overLimit ? 'Ít nhất ' : ''}{preview.count} người nhận{preview.overLimit ? ' · Vượt giới hạn' : ''}</p><Button disabled={busy || !preview.count || preview.overLimit} onClick={() => setConfirm(true)}>Xếp hàng gửi</Button></div>}{batch && <p role="status" className="my-4">Đã xếp hàng đợt #{batch}. <Link className="underline" to="/admin/notifications/history">Xem tiến độ gửi</Link></p>}{confirm && <ConfirmDialog title="Xác nhận gửi thông báo" busy={busy} onClose={() => setConfirm(false)}><p className="mb-4">Gửi thông báo cho {preview?.count} tài khoản đã xem trước. Không gửi email/SMS.</p><Button isLoading={busy} onClick={() => void send()}>Xác nhận xếp hàng</Button>{error && <ErrorNotice message={error}/>}</ConfirmDialog>}</>;
}
function TemplatePicker({ choose }: {
    choose: (r: Row) => void;
}) { const r = useQueue('/admin/notifications/templates'); return <details><summary className="cursor-pointer underline">Chọn mẫu có sẵn</summary><DataTable {...r} retry={r.reload} onPage={r.setPage} columns={[{ key: 'title', label: 'Mẫu', render: t => <>{t.code}<p>{t.title}</p></> }, { key: 'use', label: 'Sử dụng', render: t => <Button type="button" variant="outline" disabled={!t.active || t.purpose !== 'GENERAL'} onClick={() => choose(t)}>{t.purpose === 'GENERAL' ? 'Chọn' : 'Dành cho job tự động'}</Button> }]}/></details>; }
export function NotificationHistory() { const r = useQueue('/admin/notifications/history'); return <><PageHeader title="Lịch sử gửi thông báo" description="QUEUED: đang chờ/đang gửi; COMPLETED: đã xử lý hết. SENT là thông báo đã lưu trên web, không phải xác nhận khách đã đọc. Tài khoản bị khóa/xóa trước lúc gửi được bỏ qua." actions={<Button variant="outline" disabled={r.loading} onClick={r.reload}>Làm mới lịch sử</Button>}/><DataTable {...r} retry={r.reload} onPage={r.setPage} columns={[{ key: 'id', label: 'Đợt', render: x => `#${x.id}` }, { key: 'status', label: 'Trạng thái', render: x => x.status }, { key: 'count', label: 'Tiến độ', render: x => `${x.sent} đã gửi · ${x.skipped} bỏ qua / ${x.total}` }, { key: 'time', label: 'Tạo / hoàn tất', render: x => <>{timeText(x.createdAt)}<p>{x.finishedAt ? timeText(x.finishedAt) : '—'}</p></> }, { key: 'error', label: 'Thử lại / lỗi', render: x => `${x.attempts} · ${x.errorCode || 'Không có'}` }]}/></>; }
const fields: CatalogField[] = [{ key: 'code', label: 'Mã mẫu', required: true }, { key: 'purpose', label: 'Mục đích', options: { GENERAL: 'Thông báo chung', APPOINTMENT: 'Nhắc lịch hẹn', VACCINATION: 'Nhắc tiêm' }, default: 'GENERAL' }, { key: 'title', label: 'Tiêu đề', required: true }, { key: 'body', label: 'Nội dung', type: 'textarea' }, { key: 'active', label: 'Đang bật', type: 'boolean', default: 'true' }];
export function NotificationTemplates() { const r = useQueue('/admin/notifications/templates'); const [editing, setEditing] = useState<Row>(); const [create, setCreate] = useState(false); const [remove, setRemove] = useState<Row>(); const [reason, setReason] = useState(''); const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const destroy = async () => { setBusy(true); setError(''); try {
    await api.delete('/admin/notifications/templates/' + remove?.id, { data: { reason } });
    setRemove(undefined);
    r.reload();
}
catch (e) {
    setError(getErrorMessage(e));
}
finally {
    setBusy(false);
} }; return <><PageHeader title="Mẫu thông báo" description="Biến chung: {firstName}; nhắc hẹn: {petName}, {appointmentTime}; nhắc tiêm: {petName}, {dueDate}. Mẫu hệ thống chỉ tắt, không xóa; tắt sẽ dừng nhắc tự động tương ứng." actions={<Button onClick={() => { setCreate(true); setEditing(undefined); }}>Tạo mẫu</Button>}/><DataTable {...r} retry={r.reload} onPage={r.setPage} columns={[{ key: 'code', label: 'Mã / loại', render: x => <>{x.code}<p>{x.purpose}</p></> }, { key: 'title', label: 'Nội dung', render: x => <>{x.title}<p className="whitespace-pre-wrap">{x.body}</p></> }, { key: 'active', label: 'Bật', render: x => x.active ? 'Có' : 'Không' }, { key: 'action', label: 'Thao tác', render: x => <div className="flex flex-wrap gap-2"><Button variant="outline" onClick={() => { setEditing(x); setCreate(false); }}>Sửa</Button><Button variant="danger" onClick={() => { setRemove(x); setReason(''); setError(''); }}>Xóa</Button></div> }]}/>{(editing || create) && <section className="my-6 border rounded-xl p-4"><CatalogForm key={editing?.id || 'new'} fields={fields} data={editing} url={'/admin/notifications/templates' + (editing ? '/' + editing.id : '')} method={editing ? 'put' : 'post'} onDone={() => { setCreate(false); setEditing(undefined); r.reload(); }}/><Button variant="outline" className="mt-3" onClick={() => { setEditing(undefined); setCreate(false); }}>Đóng form</Button></section>}{remove && <ConfirmDialog title="Xóa mẫu thông báo" busy={busy} onClose={() => setRemove(undefined)}><form onSubmit={e => { e.preventDefault(); void destroy(); }}><Input label="Lý do xóa" required maxLength={500} value={reason} onChange={e => setReason(e.target.value)}/><Button type="submit" variant="danger" isLoading={busy}>Xác nhận xóa</Button>{error && <ErrorNotice message={error}/>}</form></ConfirmDialog>}</>; }
