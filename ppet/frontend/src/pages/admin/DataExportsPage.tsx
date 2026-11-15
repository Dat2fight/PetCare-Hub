import { useState } from 'react';
import { isAxiosError } from 'axios';
import api from '../../api/client';
import { getErrorMessage } from '../../utils/getErrorMessage';
import { getBusinessDate } from '../../utils/businessDate';
import { PageHeader, Select, DataTable, ConfirmDialog, ErrorNotice } from '../../components/admin/AdminUI';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { Row, useQueue, timeText } from './OperationsUI';
export default function DataExportsPage() {
    const r = useQueue('/admin/data-exports');
    const [kind, setKind] = useState('customers');
    const [from, setFrom] = useState(getBusinessDate());
    const [to, setTo] = useState(getBusinessDate());
    const [keyword, setKeyword] = useState('');
    const [key, setKey] = useState(() => crypto.randomUUID());
    const [confirm, setConfirm] = useState<'create' | Row>();
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const reset = () => setKey(crypto.randomUUID());
    const perform = async () => { setBusy(true); setError(''); try {
        if (confirm === 'create') {
            await api.post('/admin/data-exports', { kind, ...(kind === 'revenue' ? { from, to } : { keyword }), requestKey: key });
            reset();
        }
        else {
            const { data } = await api.post('/admin/data-exports/' + confirm!.id + '/download', {}, { responseType: 'blob' });
            const url = URL.createObjectURL(data);
            const a = document.createElement('a');
            a.href = url;
            a.download = `petcare-${confirm!.kind}-${getBusinessDate()}.csv`;
            a.click();
            setTimeout(() => URL.revokeObjectURL(url), 1000);
        }
        setConfirm(undefined);
        r.reload();
    }
    catch (e) {
        if (isAxiosError(e) && e.response?.data instanceof Blob) {
            try {
                e.response.data = JSON.parse(await e.response.data.text());
            }
            catch { /* use HTTP error */ }
        }
        setError(getErrorMessage(e));
    }
    finally {
        setBusy(false);
    } };
    return <><PageHeader title="Xuất dữ liệu theo yêu cầu" description="Tối đa 20.000 dòng, chuẩn bị bất đồng bộ; chỉ người tạo được tải một lần trong 24 giờ. Lần tải được tiêu thụ ngay khi máy chủ chấp nhận; nếu mất mạng, tạo yêu cầu mới." actions={<Button variant="outline" onClick={r.reload}>Làm mới tiến độ</Button>}/><form className="space-y-3 mb-6" onSubmit={e => { e.preventDefault(); setConfirm('create'); }}><Select label="Loại dữ liệu" value={kind} onChange={v => { setKind(v); reset(); }}><option value="customers">Khách hàng (ID, họ tên, trạng thái, ngày tạo)</option><option value="inventory">Tồn kho</option><option value="revenue">Đơn đủ điều kiện doanh thu</option></Select>{kind === 'revenue' ? <><Input label="Từ ngày" type="date" required value={from} onChange={e => { setFrom(e.target.value); reset(); }}/><Input label="Đến ngày" type="date" required value={to} onChange={e => { setTo(e.target.value); reset(); }}/></> : <Input label="Lọc tên" maxLength={100} value={keyword} onChange={e => { setKeyword(e.target.value); reset(); }}/>}<Button type="submit">Yêu cầu xuất</Button></form><DataTable {...r} retry={r.reload} onPage={r.setPage} columns={[{ key: 'id', label: 'Đợt', render: x => `#${x.id} · ${x.kind}` }, { key: 'status', label: 'Trạng thái', render: x => <>{x.status}<p>{x.errorCode}</p></> }, { key: 'count', label: 'Số dòng', render: x => x.rowCount ?? 'Chưa xác định' }, { key: 'expiry', label: 'Hết hạn', render: x => timeText(x.expiresAt) }, { key: 'download', label: 'Tải', render: x => <Button variant="outline" disabled={x.status !== 'READY'} onClick={() => setConfirm(x)}>Tải một lần</Button> }]}/>{confirm && <ConfirmDialog title={confirm === 'create' ? 'Xác nhận yêu cầu xuất' : 'Xác nhận tải một lần'} busy={busy} onClose={() => setConfirm(undefined)}><Button isLoading={busy} onClick={() => void perform()}>Xác nhận</Button>{error && <ErrorNotice message={error}/>}</ConfirmDialog>}</>;
}
