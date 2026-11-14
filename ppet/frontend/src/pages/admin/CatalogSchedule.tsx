import { useState } from 'react';
import api from '../../api/client';
import { getBusinessDate } from '../../utils/businessDate';
import { getErrorMessage } from '../../utils/getErrorMessage';
import { PageHeader, DataTable, ConfirmDialog, AdminForm } from '../../components/admin/AdminUI';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { CatalogForm } from './CatalogForms';
import { CatalogField } from './CatalogConfig';
import { useQueue, Row, ActionDialog, Filters } from './OperationsUI';
const kinds: Record<string, string> = { hours: 'Giờ mở cửa', holidays: 'Ngày đóng cửa', shifts: 'Ca nhân viên', 'time-off': 'Lịch nghỉ nhân viên' };
export default function CatalogSchedule() { const [kind, setKind] = useState('hours'); return <><PageHeader title="Lịch làm việc" description="Thay đổi ảnh hưởng giờ trống và đặt lịch mới. Lịch đã đặt được giữ nguyên cho đến khi bạn chủ động chọn hủy."/><nav className="flex flex-wrap gap-2 mb-5">{Object.entries(kinds).map(([key, name]) => <Button key={key} variant={kind === key ? 'primary' : 'outline'} onClick={() => setKind(key)}>{name}</Button>)}</nav><ScheduleTable key={kind} kind={kind}/><Affected /></>; }
function ScheduleTable({ kind }: {
    kind: string;
}) {
    const r = useQueue('/admin/schedules/' + kind);
    const [week, setWeek] = useState(getBusinessDate());
    const [staff, setStaff] = useState('');
    const [edit, setEdit] = useState<Row>();
    const [create, setCreate] = useState(false);
    const [remove, setRemove] = useState<Row>();
    const [reason, setReason] = useState('');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const [calendar, setCalendar] = useState(false);
    const fields: CatalogField[] = [...(['hours', 'shifts'].includes(kind) ? [{ key: 'weekday', label: 'Thứ (1 = thứ Hai, 7 = Chủ nhật)', type: 'number', min: 1, max: 7, required: true }] : [{ key: 'workDate', label: 'Ngày', type: 'date', required: true }]), ...(['shifts', 'time-off'].includes(kind) ? [{ key: 'staffId', label: 'ID nhân viên', type: 'number', min: 1, required: true }] : []), ...(kind === 'holidays' ? [] : [{ key: 'startTime', label: 'Bắt đầu', type: 'time', required: true, default: '09:00' }, { key: 'endTime', label: 'Kết thúc', type: 'time', required: true, default: '17:00' }, { key: 'closed', label: 'Nghỉ cả ngày', type: 'boolean', default: 'false' }]), { key: 'reason', label: 'Lý do', required: true }];
    const del = async () => { setBusy(true); setError(''); try {
        await api.delete(`/admin/schedules/${kind}/${remove?.id}`, { data: { reason } });
        setRemove(undefined);
        r.reload();
    }
    catch (e) {
        setError(getErrorMessage(e));
    }
    finally {
        setBusy(false);
    } };
    return <><div className="flex flex-wrap gap-3 mb-3"><Button onClick={() => { setCreate(true); setEdit(undefined); }}>Thêm {kinds[kind].toLowerCase()}</Button><Button variant="outline" onClick={() => setCalendar(!calendar)}>Xem {calendar ? 'bảng' : 'theo thứ'}</Button></div><form className="flex gap-3 flex-wrap my-4" onSubmit={e => { e.preventDefault(); const end = new Date(week + 'T12:00:00'); end.setDate(end.getDate() + 6); const to = [end.getFullYear(), String(end.getMonth() + 1).padStart(2, '0'), String(end.getDate()).padStart(2, '0')].join('-'); r.filter(new URLSearchParams({ from: week, to, ...(staff ? { staff } : {}) }).toString()); setCalendar(true); }}><Input label="Tuần bắt đầu" type="date" required value={week} onChange={e => setWeek(e.target.value)}/>{['shifts', 'time-off'].includes(kind) && <Input label="ID nhân viên" type="number" min={1} value={staff} onChange={e => setStaff(e.target.value)}/>}<Button type="submit">Xem tuần đã chọn</Button></form>{calendar && <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-7 my-4">{Array.from({ length: 7 }, (_, i) => <section className="border rounded-xl p-3 min-w-0" key={i}><h2>{i === 6 ? 'Chủ nhật' : 'Thứ ' + (i + 2)}</h2>{r.data?.content.filter(x => x.weekday === i + 1 || x.workDate && (new Date(x.workDate + 'T12:00:00').getDay() + 6) % 7 === i).map(x => <p key={x.id} className="break-words">{x.workDate} {x.staffId ? '#' + x.staffId : ''} · {x.closed ? 'Nghỉ' : x.startTime + '–' + x.endTime}</p>)}</section>)}</div>}<DataTable {...r} retry={r.reload} onPage={r.setPage} onSort={r.setSort} columns={[{ key: 'id', label: 'ID', render: x => x.id }, { key: 'day', label: 'Thứ / ngày', render: x => x.workDate || `Thứ ${x.weekday === 7 ? 'CN' : x.weekday + 1}` }, { key: 'staff', label: 'Nhân viên', render: x => x.staffId || 'Toàn cửa hàng' }, { key: 'time', label: 'Giờ / ghi chú', render: x => <>{x.closed ? 'Nghỉ cả ngày' : `${x.startTime}–${x.endTime}`}<p>{x.reason}</p></> }, { key: 'actions', label: 'Thao tác', render: x => <div className="flex flex-wrap gap-2"><Button variant="outline" onClick={() => { setEdit(x); setCreate(false); }}>Sửa</Button><Button variant="danger" onClick={() => setRemove(x)}>Xóa</Button></div> }]}/><p className="my-3 text-sm">Lịch và bảng hiển thị trang hiện tại; dùng phân trang để xem hết.</p>{(create || edit) && <section className="my-5 rounded-xl border p-4"><h2 className="font-bold mb-3">{create ? 'Thêm mới' : 'Sửa lịch #' + edit?.id}</h2><CatalogForm key={kind + (edit?.id || 'new')} fields={fields} data={edit} url={`/admin/schedules/${kind}${edit ? '/' + edit.id : ''}`} method={edit ? 'put' : 'post'} onDone={() => { setCreate(false); setEdit(undefined); r.reload(); }}/><Button className="mt-3" variant="outline" onClick={() => { setCreate(false); setEdit(undefined); }}>Đóng form</Button></section>}{remove && <ConfirmDialog title="Xóa cấu hình lịch" busy={busy} onClose={() => setRemove(undefined)}><AdminForm busy={busy} error={error} onSubmit={e => { e.preventDefault(); void del(); }}><p>Xóa có thể mở lại giờ đặt lịch theo cấu hình còn lại hoặc giờ mặc định.</p><Input label="Lý do xóa" required value={reason} onChange={e => setReason(e.target.value)}/><Button type="submit" variant="danger" isLoading={busy}>Xác nhận xóa</Button></AdminForm></ConfirmDialog>}</>;
}
function Affected() { const [date, setDate] = useState(getBusinessDate()); const [query, setQuery] = useState(''); const [ids, setIds] = useState<number[]>([]); const [confirm, setConfirm] = useState(false); return <section className="border-t mt-8 pt-5"><h2 className="text-xl font-bold">Lịch hẹn bị ảnh hưởng trong ngày</h2><form className="my-4 flex flex-wrap gap-3" onSubmit={e => { e.preventDefault(); setQuery(date); setIds([]); }}><Input type="date" label="Ngày cần kiểm tra" required value={date} onChange={e => setDate(e.target.value)}/><Button type="submit">Xem lịch đã đặt</Button></form>{query && <AffectedRows key={query} date={query} ids={ids} setIds={setIds}/>}<Button variant="danger" disabled={!ids.length} onClick={() => setConfirm(true)}>Hủy các lịch đã chọn ({ids.length})</Button>{confirm && <ActionDialog title="Hủy lịch và thông báo khách" url="/admin/schedule-cancel" body={{ date: query, ids }} fields={[{ key: 'reason', label: 'Lý do hủy' }]} warning="Chỉ hủy các lịch được chọn trong ngày này. Khách sẽ nhận thông báo trên web." onClose={() => setConfirm(false)} onDone={() => { setIds([]); setQuery(''); }}/>}</section>; }
function AffectedRows({ date, ids, setIds }: {
    date: string;
    ids: number[];
    setIds: (v: number[]) => void;
}) { const r = useQueue('/admin/schedule-affected', 'date=' + date); return <DataTable {...r} retry={r.reload} onPage={r.setPage} columns={[{ key: 'choose', label: 'Chọn', render: x => <input type="checkbox" aria-label={'Chọn lịch ' + x.id} checked={ids.includes(x.id)} onChange={e => setIds(e.target.checked ? [...ids, x.id] : ids.filter(id => id !== x.id))}/> }, { key: 'id', label: 'Lịch', render: x => `#${x.id} · ${x.pet}` }, { key: 'time', label: 'Thời gian', render: x => `${x.start}–${x.end}` }, { key: 'service', label: 'Dịch vụ', render: x => x.service }]}/>; }
export function StaffPerformance() { const r = useQueue('/admin/staff-performance', `from=${getBusinessDate()}&to=${getBusinessDate()}`); return <><PageHeader title="Hiệu suất nhân viên"/><Filters apply={r.filter} fields={[{ key: 'from', label: 'Từ ngày', type: 'date', value: getBusinessDate() }, { key: 'to', label: 'Đến ngày', type: 'date', value: getBusinessDate() }]}/><DataTable {...r} retry={r.reload} onPage={r.setPage} columns={[{ key: 'name', label: 'Nhân viên', render: x => `${x.name} #${x.id}` }, { key: 'total', label: 'Số lịch', render: x => x.total }, { key: 'complete', label: 'Hoàn tất', render: x => x.completed }, { key: 'cancel', label: 'Tỷ lệ hủy', render: x => `${x.total ? (100 * x.cancelled / x.total).toFixed(1) : 0}%` }, { key: 'noShow', label: 'Tỷ lệ vắng', render: x => `${x.total ? (100 * x.noShow / x.total).toFixed(1) : 0}%` }]}/></>; }
