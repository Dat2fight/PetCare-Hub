import { useState } from 'react';
import { getBusinessDate } from '../../utils/businessDate';
import { Link, useParams } from 'react-router-dom';
import { usePermissions } from '../../contexts/PermissionContext';
import { useAdminResource } from '../../hooks/useAdminResource';
import { PageHeader, DataTable, ErrorNotice, StatusBadge } from '../../components/admin/AdminUI';
import { Button } from '../../components/common/Button';
import { Row, useQueue, Filters, options, labels, ActionDialog } from './OperationsUI';
const appointmentStates = ['SCHEDULED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];
function dateText(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}
export function AppointmentsQueue() {
  const {
    permissions
  } = usePermissions();
  const r = useQueue('/admin/appointments');
  const [filters, setFilters] = useState('');
  const [calendar, setCalendar] = useState(false);
  const [week, setWeek] = useState(() => {
    const d = new Date(getBusinessDate() + 'T12:00:00');
    d.setDate(d.getDate() - (d.getDay() + 6) % 7);
    return dateText(d);
  });
  const weekQuery = (start: string, filter: string) => {
    const end = new Date(start + 'T12:00:00');
    end.setDate(end.getDate() + 6);
    const params = new URLSearchParams(filter);
    params.set('from', start);
    params.set('to', dateText(end));
    return params.toString();
  };
  const showWeek = (start: string) => {
    setWeek(start);
    r.filter(weekQuery(start, filters));
  };
  const apply = (value: string) => {
    setFilters(value);
    r.filter(calendar ? weekQuery(week, value) : value);
  };
  return <><PageHeader title="Lịch hẹn" actions={<Button variant="outline" onClick={() => {
      if (!calendar) showWeek(week);else r.filter(filters);
      setCalendar(!calendar);
    }}>Xem {calendar ? 'bảng' : 'lịch tuần'}</Button>} />{calendar ? <div className="mb-5 flex flex-wrap gap-3"><label>Tuần bắt đầu <input className="rounded-xl border p-2" type="date" value={week} onChange={e => {
          if (e.target.value) showWeek(e.target.value);
        }} /></label><p>Lịch hiển thị các bản ghi ở trang hiện tại; dùng phân trang phía dưới để xem đủ.</p></div> : null}<Filters key={String(calendar)} apply={apply} fields={[...(permissions.includes('APPOINTMENT_MANAGE') ? [{
      key: 'staff',
      label: 'ID nhân viên',
      type: 'number'
    }, {
      key: 'unassigned',
      label: 'Phân công',
      options: {
        '': 'Tất cả',
        'true': 'Chưa phân công'
      }
    }] : []), {
      key: 'serviceId',
      label: 'ID dịch vụ',
      type: 'number'
    }, {
      key: 'status',
      label: 'Trạng thái',
      options: options(appointmentStates)
    }, ...(!calendar ? [{
      key: 'from',
      label: 'Từ ngày',
      type: 'date'
    }, {
      key: 'to',
      label: 'Đến ngày',
      type: 'date'
    }] : [])].map(f => ({
      ...f,
      value: new URLSearchParams(filters).get(f.key) || ''
    }))} />
 {calendar && r.data && <div className="mb-5 grid gap-2 sm:grid-cols-2 xl:grid-cols-7">{Array.from({
        length: 7
      }, (_, i) => {
        const d = new Date(week + 'T12:00:00');
        d.setDate(d.getDate() + i);
        const day = dateText(d);
        return <section key={day} className="min-w-0 rounded-xl border bg-white p-3"><h2 className="mb-2 font-bold">{d.toLocaleDateString('vi-VN', {
              weekday: 'short',
              day: 'numeric',
              month: 'numeric'
            })}</h2>{r.data?.content.filter(a => a.date === day).map(a => <Link key={a.id} to={`/admin/appointments/${a.id}`} className="mb-2 block break-words rounded-lg bg-primary-50 p-2">{a.start}–{a.end}<br />{a.pet}<br />{a.service}<br />{a.staffName?.trim() || 'Chưa phân công'}</Link>)}</section>;
      })}</div>}
 <DataTable {...r} retry={r.reload} onPage={r.setPage} onSort={r.setSort} columns={[{
      key: 'id',
      label: 'Lịch hẹn',
      render: a => <Link className="underline" to={`/admin/appointments/${a.id}`}>#{a.id} · {a.pet}</Link>
    }, {
      key: 'service',
      label: 'Dịch vụ',
      render: a => a.service
    }, {
      key: 'date',
      label: 'Thời gian',
      sort: 'date',
      render: a => `${a.date} ${a.start}–${a.end}`
    }, {
      key: 'staff',
      label: 'Nhân viên',
      render: a => a.staffName?.trim() || <StatusBadge>Chưa phân công</StatusBadge>
    }, {
      key: 'status',
      label: 'Trạng thái',
      render: a => labels[a.status]
    }]} /></>;
}
function AssignDialog({
  id,
  onClose,
  onDone
}: {
  id: string;
  onClose: () => void;
  onDone: () => void;
}) {
  const r = useQueue(`/admin/appointments/${id}/eligible-staff`);
  return <ActionDialog key={r.data?.number ?? 0} title="Phân công nhân viên" url={`/admin/appointments/${id}/assign`} fields={[{
    key: 'staffId',
    label: 'Nhân viên (trang hiện tại)',
    type: 'number',
    options: Object.fromEntries((r.data?.content || []).map(s => [String(s.id), s.name]))
  }, {
    key: 'reason',
    label: 'Lý do'
  }]} warning="Máy chủ sẽ kiểm tra lại nhân viên đang hoạt động, đúng vai trò và không trùng lịch." onClose={onClose} onDone={onDone}>
  <DataTable {...r} retry={r.reload} onPage={r.setPage} columns={[{
      key: 'name',
      label: 'Nhân viên đủ vai trò',
      render: s => s.name
    }]} />
 </ActionDialog>;
}
export function AppointmentDetail() {
  const {
    id = ''
  } = useParams();
  const r = useAdminResource<Row>(`/admin/appointments/${id}`);
  const {
    permissions
  } = usePermissions();
  const [assign, setAssign] = useState(false);
  const [change, setChange] = useState(false);
  if (r.error) return <ErrorNotice message={r.error} retry={r.reload} />;
  if (!r.data) return <p>Đang tải lịch hẹn…</p>;
  const a = r.data;
  const transitions: Record<string, string[]> = {
    SCHEDULED: ['CONFIRMED', 'IN_PROGRESS', 'CANCELLED', 'NO_SHOW'],
    CONFIRMED: ['IN_PROGRESS', 'CANCELLED', 'NO_SHOW'],
    IN_PROGRESS: ['COMPLETED', 'CANCELLED']
  };
  return <><Link className="underline" to="/admin/appointments">← Lịch hẹn</Link><PageHeader title={`Lịch #${id} · ${a.pet}`} description={`${a.service} · ${a.date} ${a.start}–${a.end}`} /><p>Nhân viên: {a.staffName?.trim() || 'Chưa phân công'}</p><p>Trạng thái: {labels[a.status]}</p><p className="my-4 whitespace-pre-wrap break-words">{a.notes}</p><div className="flex flex-wrap gap-3">{transitions[a.status] && <Button onClick={() => setChange(true)}>Đổi trạng thái / hủy</Button>}{permissions.includes('APPOINTMENT_MANAGE') && ['SCHEDULED', 'CONFIRMED'].includes(a.status) && <Button variant="outline" onClick={() => setAssign(true)}>Phân công / đổi nhân viên</Button>}</div>{change && <ActionDialog title="Đổi trạng thái lịch hẹn" url={`/admin/appointments/${id}/status`} fields={[{
      key: 'status',
      label: 'Trạng thái mới',
      options: options(transitions[a.status], false)
    }, {
      key: 'reason',
      label: 'Lý do / ghi chú'
    }]} onClose={() => setChange(false)} onDone={r.reload} />}{assign && <AssignDialog id={id} onClose={() => setAssign(false)} onDone={r.reload} />}</>;
}
