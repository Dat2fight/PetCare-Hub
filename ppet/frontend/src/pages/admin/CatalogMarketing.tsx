import { useState } from 'react';
import api from '../../api/client';
import { useAdminResource } from '../../hooks/useAdminResource';
import { getErrorMessage } from '../../utils/getErrorMessage';
import { PageHeader, DataTable, ErrorNotice, ConfirmDialog } from '../../components/admin/AdminUI';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { CatalogForm, Fields } from './CatalogForms';
import { catalogs, CatalogField, initialValues, formBody } from './CatalogConfig';
import { Row, useQueue, ActionDialog, Filters } from './OperationsUI';
export function LoyaltyRulesPage() { const r = useAdminResource<Row>('/admin/loyalty-rules'); const [user, setUser] = useState(''); const [done, setDone] = useState(false); const fields: CatalogField[] = [{ key: 'earnEvery', label: 'Chi tiêu sản phẩm để tích 1 điểm (đ)', type: 'number', required: true, min: 0.01 }, { key: 'pointValue', label: 'Giá trị đổi 1 điểm (đ)', type: 'number', required: true, min: 0.01 }, ...['silver', 'gold', 'platinum'].map((key, i) => ({ key, label: ['Ngưỡng Bạc', 'Ngưỡng Vàng', 'Ngưỡng Bạch kim'][i], type: 'number', min: 1, required: true }))]; return <><PageHeader title="Quy tắc và điều chỉnh điểm" description="Chỉ ADMIN. Ngưỡng dựa trên điểm tích lũy; điều chỉnh thủ công chỉ đổi số dư sử dụng."/>{r.error ? <ErrorNotice message={r.error} retry={r.reload}/> : r.data ? <CatalogForm key={JSON.stringify(r.data)} fields={fields} data={r.data} url="/admin/loyalty-rules" method="put" onDone={r.reload}/> : <p>Đang tải…</p>}<section className="mt-8 border-t pt-5"><h2 className="text-xl font-bold mb-3">Điều chỉnh số dư khách</h2><Input label="ID khách hàng" type="number" min={1} value={user} onChange={e => { setUser(e.target.value); setDone(false); }}/>{Number(user) > 0 && <CatalogForm key={user} fields={[{ key: 'points', label: 'Số điểm (dương = cộng, âm = trừ)', type: 'number', min: -1000000, max: 1000000, required: true }, { key: 'reason', label: 'Lý do', required: true }]} url={`/admin/loyalty/${user}/adjust`} onDone={() => setDone(true)} warning="Điểm thay đổi ngay và có nhật ký. Không điều chỉnh âm vượt số dư; không tự tăng hạng thành viên."/>}{done && <p role="status">Đã điều chỉnh điểm.</p>}</section></>; }
export function SubscriptionsPage() { const r = useQueue('/admin/subscriptions'); const [row, setRow] = useState<Row>(); return <><PageHeader title="Khách đăng ký gói chăm sóc" description="Gói hiện có chưa lưu hạn mức/lượt dùng. Không coi dữ liệu thiếu là quyền sử dụng vô hạn."/><Filters apply={r.filter} fields={[{ key: 'packageId', label: 'ID gói', type: 'number' }]}/><DataTable {...r} retry={r.reload} onPage={r.setPage} columns={[{ key: 'name', label: 'Gói / thú cưng', render: x => <>{x.name}<p>{x.pet}</p></> }, { key: 'customer', label: 'Khách', render: x => `#${x.customerId}` }, { key: 'date', label: 'Thời hạn', render: x => `${x.startDate} → ${x.endDate}` }, { key: 'state', label: 'Trạng thái', render: x => x.status }, { key: 'remaining', label: 'Lượt còn lại', render: () => 'Chưa có dữ liệu hạn mức' }, { key: 'action', label: 'Thao tác', render: x => <Button variant="outline" onClick={() => setRow(x)}>Hủy / gia hạn</Button> }]}/>{row && <ActionDialog title={`Cập nhật đăng ký #${row.id}`} url={`/admin/subscriptions/${row.id}`} fields={[{ key: 'action', label: 'Thao tác', options: { cancel: 'Hủy', extend: 'Gia hạn' } }, { key: 'endDate', label: 'Ngày hết hạn mới (nếu gia hạn)', type: 'date', required: false }, { key: 'reason', label: 'Lý do' }]} onClose={() => setRow(undefined)} onDone={r.reload}/>}</>; }
const audienceFields: CatalogField[] = [{ key: 'group', label: 'Nhóm khách', options: { BIRTHDAY: 'Thú cưng sinh nhật tháng này', INACTIVE: 'Khách lâu không mua' }, default: 'BIRTHDAY' }, { key: 'days', label: 'Số ngày không mua', type: 'number', min: 1, max: 3650, default: '90' }, { key: 'message', label: 'Nội dung thông báo', required: true }];
export function CampaignsPage() {
    const fields = [...audienceFields, ...catalogs.promotions.fields];
    const [values, setValues] = useState(() => initialValues(fields));
    const [preview, setPreview] = useState<{
        count: number;
        overLimit: boolean;
        audienceToken: string;
    }>();
    const [key, setKey] = useState(() => crypto.randomUUID());
    const [confirm, setConfirm] = useState(false);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const [done, setDone] = useState(false);
    const check = async () => { setBusy(true); setError(''); try {
        const { data } = await api.get('/admin/campaign-audience', { params: { group: values.group, days: values.days } });
        setPreview(data);
    }
    catch (e) {
        setError(getErrorMessage(e));
    }
    finally {
        setBusy(false);
    } };
    const send = async () => { if (busy)
        return; setBusy(true); setError(''); try {
        await api.post('/admin/campaigns', { group: values.group, days: Number(values.days), requestKey: key, audienceToken: preview?.audienceToken, promotion: formBody(catalogs.promotions.fields, values), message: values.message });
        setDone(true);
        setPreview(undefined);
        setConfirm(false);
    }
    catch (e) {
        setError(getErrorMessage(e));
    }
    finally {
        setBusy(false);
    } };
    return <><PageHeader title="Chiến dịch ưu đãi" description="Tạo mã riêng cho nhóm đã xem trước và gửi thông báo web. Tối đa 500 khách, một chiến dịch mỗi 10 phút cho mỗi nhân viên."/><form onSubmit={e => { e.preventDefault(); void check(); }}><fieldset disabled={busy}><Fields fields={fields} values={values} setValues={v => { setValues(v); setPreview(undefined); setKey(crypto.randomUUID()); setDone(false); }}/><Button className="my-4" type="submit" isLoading={busy}>Xem trước người nhận</Button></fieldset></form>{error && <ErrorNotice message={error}/>}{done && <p role="status">Đã tạo mã và gửi thông báo.</p>}{preview && <div><p>{preview.count} khách phù hợp{preview.overLimit ? ' · Vượt giới hạn gửi' : ''}</p><Button disabled={busy || !preview.count || preview.overLimit} onClick={() => setConfirm(true)}>Tạo mã và gửi</Button></div>}{confirm && <ConfirmDialog title="Gửi ưu đãi cho nhóm khách" busy={busy} onClose={() => setConfirm(false)}><p className="mb-4">Gửi cho {preview?.count} khách. Chỉ khách trong danh sách này dùng được mã; không gửi email/SMS.</p><Button isLoading={busy} onClick={() => void send()}>Xác nhận gửi</Button>{error && <ErrorNotice message={error}/>}</ConfirmDialog>}</>;
}
