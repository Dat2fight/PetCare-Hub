import { lazy, Suspense, useState, ReactNode } from 'react';
import { Link, NavLink, Route, Routes, useLocation } from 'react-router-dom';
import { usePermissions } from '../../contexts/PermissionContext';
import { useAuth } from '../../contexts/AuthContext';
import { useAdminResource } from '../../hooks/useAdminResource';
import { ErrorNotice, PageHeader, StatCard, PriceText } from '../../components/admin/AdminUI';
import { Button } from '../../components/common/Button';
const Users = lazy(() => import('./AdminUsersPage'));
const Detail = lazy(() => import('./AdminUserDetail'));
const Audit = lazy(() => import('./AdminAuditPage'));
const menu = [{ to: '/admin', label: 'Tổng quan', permission: 'PORTAL_ACCESS' }, { to: '/admin/users', label: 'Người dùng', permission: 'USER_MANAGE' }, { to: '/admin/audit-logs', label: 'Nhật ký thao tác', permission: 'AUDIT_VIEW' }];

function Guard({ permission, children }: { permission: string; children: ReactNode }) {
  const { permissions } = usePermissions();
  return permissions.includes(permission) ? children : <PageHeader title="403 — Không có quyền truy cập" description="Tài khoản của bạn không được phép sử dụng chức năng này." />;
}
function Dashboard() {
  const r = useAdminResource<{ totalUsers: number; totalOrders: number; totalPets: number; totalRevenue: number }>('/admin/dashboard');
  if (r.error) return <ErrorNotice message={r.error} retry={r.reload} />;
  if (!r.data) return <p role="status">Đang tải tổng quan…</p>;
  return <div className="grid gap-4 sm:grid-cols-2"><StatCard label="Tổng tài khoản" value={r.data.totalUsers} /><StatCard label="Tổng đơn hàng" value={r.data.totalOrders} /><StatCard label="Tổng thú cưng" value={r.data.totalPets} /><StatCard label="Doanh thu đã thanh toán từ đầu tháng" value={<PriceText value={r.data.totalRevenue} />} /></div>;
}
function Home() {
  const { permissions } = usePermissions();
  return <><PageHeader title="Cổng quản trị PetCare Hub" description="Các chức năng được hiển thị theo quyền tài khoản do máy chủ cung cấp." />{permissions.includes('REPORT_VIEW') ? <Dashboard /> : <p className="rounded-2xl border bg-white p-5">Tài khoản nội bộ đã được xác thực. Các màn hình vận hành sẽ được bổ sung trong giai đoạn tiếp theo.</p>}</>;
}
export default function AdminPortal() {
  const { user, logout } = useAuth(); const { permissions, loading, error, retry } = usePermissions();
  const [open, setOpen] = useState(false); const location = useLocation();
  if (loading) return <p role="status" className="p-6">Đang xác minh quyền…</p>;
  if (error) return <div className="p-6"><ErrorNotice message={error} retry={retry} /></div>;
  if (!permissions.includes('PORTAL_ACCESS')) return <div className="p-6"><PageHeader title="403 — Không có quyền truy cập" /><Link to="/" className="underline">Về trang chủ</Link></div>;
  const current = menu.find(m => m.to === location.pathname)?.label || 'Chi tiết người dùng';
  return <div className="min-h-screen bg-gray-50 text-gray-900">
    <header className="flex min-w-0 flex-wrap items-center justify-between gap-3 border-b bg-white p-4"><Link to="/admin" className="text-lg font-bold">PetCare Hub · Quản trị</Link><div className="flex flex-wrap items-center gap-2"><span className="max-w-40 truncate">{user?.firstName}</span><Button variant="outline" size="sm" onClick={logout}>Đăng xuất</Button><Button className="lg:hidden" variant="outline" size="sm" aria-expanded={open} aria-controls="admin-menu" onClick={() => setOpen(!open)}>Menu</Button></div></header>
    <div className="mx-auto grid max-w-[1600px] min-w-0 lg:grid-cols-[230px_minmax(0,1fr)]">
      <aside id="admin-menu" className={`${open ? 'block' : 'hidden'} min-w-0 border-b bg-white p-4 lg:block lg:min-h-[calc(100vh-80px)] lg:border-r`}><nav aria-label="Quản trị" className="space-y-2">{menu.filter(m => permissions.includes(m.permission)).map(m => <NavLink key={m.to} to={m.to} end={m.to === '/admin'} onClick={() => setOpen(false)} className={({ isActive }) => `block min-h-11 rounded-xl px-3 py-3 ${isActive ? 'bg-primary-100 font-semibold text-primary-900' : 'hover:bg-gray-100'}`}>{m.label}</NavLink>)}<Link className="block min-h-11 px-3 py-3 underline" to="/change-password">Đổi mật khẩu</Link><Link className="block min-h-11 px-3 py-3 underline" to="/">Về cửa hàng</Link></nav></aside>
      <div className="min-w-0 p-3 sm:p-6"><nav aria-label="Đường dẫn" className="mb-5 text-sm text-gray-600"><Link to="/admin" className="underline">Quản trị</Link> / {current}</nav><Suspense fallback={<p role="status">Đang tải trang…</p>}><Routes><Route index element={<Home />} /><Route path="users" element={<Guard permission="USER_MANAGE"><Users /></Guard>} /><Route path="users/:id" element={<Guard permission="USER_MANAGE"><Detail /></Guard>} /><Route path="audit-logs" element={<Guard permission="AUDIT_VIEW"><Audit /></Guard>} /><Route path="*" element={<PageHeader title="404 — Không tìm thấy trang quản trị" />} /></Routes></Suspense></div>
    </div>
  </div>;
}
