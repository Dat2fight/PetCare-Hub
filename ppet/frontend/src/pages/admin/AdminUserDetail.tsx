import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAdminResource } from '../../hooks/useAdminResource';
import { AdminUser, roleNames } from './AdminUsersPage';
import { PageHeader, DataTable, Page, ErrorNotice, StatCard, PriceText } from '../../components/admin/AdminUI';

interface Overview { user: AdminUser; orders: Page<{ id: number; number: string; status: string; total: number; createdAt: string }>; appointments: Page<{ id: number; date: string; time: string; status: string }>; pets: Page<{ id: number; name: string; dateOfBirth: string }>; loyalty: { availablePoints: number; membershipLevel: string } }
export default function AdminUserDetail() {
  const { id } = useParams(); const [ordersPage, orders] = useState(0); const [appointmentsPage, appointments] = useState(0); const [petsPage, pets] = useState(0);
  const r = useAdminResource<Overview>(`/admin/users/${id}/overview?ordersPage=${ordersPage}&appointmentsPage=${appointmentsPage}&petsPage=${petsPage}&size=10`);
  if (r.error) return <ErrorNotice message={r.error} retry={r.reload} />;
  if (!r.data) return <p role="status">Đang tải hồ sơ…</p>;
  const data = r.data;
  return <><Link to="/admin/users" className="underline">← Người dùng</Link><PageHeader title={`${data.user.lastName} ${data.user.firstName}`} description={`${data.user.username} · ${data.user.roles.map(role => roleNames[role]).join(', ')}`} /><p className="mb-5 break-all">{data.user.email}</p><div className="mb-6 grid gap-3 sm:grid-cols-2"><StatCard label="Điểm hiện có" value={data.loyalty.availablePoints} /><StatCard label="Hạng thành viên" value={data.loyalty.membershipLevel} /></div>
    <section className="mb-8"><h2 className="mb-3 text-xl font-bold">Đơn hàng</h2><DataTable page={data.orders} loading={r.loading} error={r.error} retry={r.reload} onPage={orders} columns={[{ key: 'number', label: 'Mã đơn', render: o => o.number }, { key: 'status', label: 'Trạng thái', render: o => o.status }, { key: 'total', label: 'Tổng tiền', render: o => <PriceText value={o.total} /> }]} /></section>
    <section className="mb-8"><h2 className="mb-3 text-xl font-bold">Lịch hẹn</h2><DataTable page={data.appointments} loading={r.loading} error={r.error} retry={r.reload} onPage={appointments} columns={[{ key: 'id', label: 'Mã lịch', render: a => a.id }, { key: 'date', label: 'Thời gian', render: a => `${a.date} ${a.time}` }, { key: 'status', label: 'Trạng thái', render: a => a.status }]} /></section>
    <section><h2 className="mb-3 text-xl font-bold">Thú cưng</h2><DataTable page={data.pets} loading={r.loading} error={r.error} retry={r.reload} onPage={pets} columns={[{ key: 'name', label: 'Tên', render: p => p.name }, { key: 'date', label: 'Ngày sinh', render: p => p.dateOfBirth || 'Chưa cập nhật' }]} /></section><p className="mt-5 text-sm text-gray-500">Truy cập hồ sơ này được ghi vào nhật ký quản trị.</p>
  </>;
}
