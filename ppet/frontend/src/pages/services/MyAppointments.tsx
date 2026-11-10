import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { isAxiosError } from 'axios';
import apiClient from '../../api/client';
import { Button } from '../../components/common/Button';
import { Modal } from '../../components/common/Modal';
import { useResource } from '../../hooks/useResource';
import type { Appointment } from '../../services/bookingService';
import type { Page } from '../../types';
import { formatBusinessTime } from '../../utils/businessDate';

const labels = { SCHEDULED: 'Đã đặt', CONFIRMED: 'Đã xác nhận', IN_PROGRESS: 'Đang thực hiện',
  NO_SHOW: 'Vắng mặt', COMPLETED: 'Đã hoàn tất', CANCELLED: 'Đã hủy' };
const frame = 'max-w-3xl mx-auto px-4 py-8 space-y-4';

function downloadCalendar(appointment: Appointment) {
  const stamp = (date: Date) => date.toISOString().replace(/[-:]/g, '').replace(/\.\d+Z$/, 'Z');
  const escape = (value: string) => value.replace(/\\/g, '\\\\').replace(/\r?\n|\r/g, '\\n').replace(/([;,])/g, '\\$1');
  const text = ['BEGIN:VCALENDAR','VERSION:2.0','PRODID:-//PetCare Hub//VI','BEGIN:VEVENT',
    'UID:' + appointment.code + '@petcarehub', 'DTSTAMP:' + stamp(new Date()),
    'DTSTART:' + stamp(new Date(appointment.startsAt)), 'DTEND:' + stamp(new Date(appointment.endsAt)),
    'SUMMARY:' + escape(appointment.serviceName + ' · ' + appointment.petName), 'END:VEVENT','END:VCALENDAR',''].join('\r\n');
  const url = URL.createObjectURL(new Blob([text], {type:'text/calendar;charset=utf-8'}));
  const link = document.createElement('a'); link.href = url; link.download = appointment.code + '.ics'; link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function AppointmentCard({ a }: { a: Appointment }) {
  return <div className="space-y-1"><h2 className="font-bold">{a.serviceName} · {a.petName}</h2>
    <p>{a.code} · {labels[a.status]}</p><p>{formatBusinessTime(a.appointmentTime, true)} ({a.timezone})</p>
    <p>{a.staffName} · {a.price?.toLocaleString('vi-VN')} ₫</p></div>;
}

export function AppointmentConfirmation() {
  const { id } = useParams();
  const result = useResource<Appointment>('/appointments/' + id);
  return <section className={frame}><h1 className="text-2xl font-bold">Chi tiết lịch hẹn</h1>
    {result.error ? <p role="alert">{result.error} <button onClick={result.retry}>Thử lại</button></p>
      : !result.data ? <p>Đang tải lịch hẹn…</p> : <>
        <AppointmentCard a={result.data} />
        {result.data.cancellable && <p>Bạn có thể hủy đến {formatBusinessTime(result.data.cancelUntil,true)}.</p>}
        {result.data.status !== 'CANCELLED' && <Button onClick={() => downloadCalendar(result.data!)}>Thêm vào lịch (.ics)</Button>}
      </>}
    <nav className="flex flex-wrap gap-4 underline"><Link to="/my-appointments">Xem lịch hẹn của tôi</Link><Link to="/services">Đặt lịch khác</Link></nav>
  </section>;
}

export function MyAppointments() {
  const [filter,setFilter] = useState('upcoming'), [page,setPage] = useState(0);
  const result = useResource<Page<Appointment>>('/appointments/me?filter='+filter+'&page='+page+'&size=10');
  const [selected,setSelected] = useState<Appointment>(), [busy,setBusy] = useState(false);
  const [error,setError] = useState(''), [reason,setReason] = useState('');
  async function cancel() {
    if (!selected || busy) return;
    setBusy(true); setError('');
    try {
      await apiClient.post('/appointments/'+selected.id+'/cancel',{reason});
      setSelected(undefined); result.retry();
    } catch (failure) {
      const data = isAxiosError(failure) ? failure.response?.data : undefined;
      setError((data?.message || 'Chưa hủy được lịch.') + (data?.correlationId ? ' · Mã lỗi: '+data.correlationId : ''));
      result.retry();
    } finally { setBusy(false); }
  }
  return <section className={frame}><h1 className="font-bold text-2xl">Lịch hẹn của tôi</h1>
    <div className="flex flex-wrap gap-3">{Object.entries({upcoming:'Sắp tới',past:'Đã qua',cancelled:'Đã hủy'}).map(([key,label]) =>
      <button key={key} aria-pressed={filter===key} className={filter===key?'font-bold underline':''}
        onClick={() => {setFilter(key);setPage(0);}}>{label}</button>)}</div>
    {result.error ? <p role="alert">{result.error} <button onClick={result.retry}>Thử lại</button></p>
      : !result.data ? <p>Đang tải…</p> : !result.data.content.length ? <p>Chưa có lịch hẹn trong mục này.</p>
      : result.data.content.map(a => <article key={a.id} className="rounded-xl bg-white p-4">
        <AppointmentCard a={a}/><div className="flex gap-4 mt-3">
          <Link className="underline" to={'/appointments/'+a.id+'/confirmation'}>Chi tiết</Link>
          {a.cancellable && <button onClick={() => {setSelected(a);setReason('');setError('');}}>Hủy lịch</button>}
        </div></article>)}
    {result.data && (result.data.totalPages > 1 || page > 0) && <div className="flex gap-4">
      <button disabled={!page} onClick={()=>setPage(page-1)}>Trang trước</button>
      <span>Trang {page+1}</span><button disabled={page+1>=result.data.totalPages} onClick={()=>setPage(page+1)}>Trang sau</button>
    </div>}
    {selected && <Modal title={'Hủy lịch '+selected.code} busy={busy} onClose={()=>setSelected(undefined)}>
      <p>{selected.serviceName} · {formatBusinessTime(selected.appointmentTime,true)}</p>
      <label>Lý do (tùy chọn)<textarea className="block w-full border p-2" maxLength={500} value={reason}
        onChange={event=>setReason(event.target.value)}/></label>
      {error && <p role="alert">{error}</p>}<Button disabled={busy} onClick={cancel}>Xác nhận hủy lịch</Button>
    </Modal>}
  </section>;
}
