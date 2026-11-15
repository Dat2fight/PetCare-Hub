import { lazy, Suspense, useState, ReactNode } from 'react';
import { Link, NavLink, Route, Routes, useLocation, useParams } from 'react-router-dom';
import { usePermissions } from '../../contexts/PermissionContext';
import { useAuth } from '../../contexts/AuthContext';
import { useAdminResource } from '../../hooks/useAdminResource';
import { ErrorNotice, PageHeader, StatCard } from '../../components/admin/AdminUI';
import { catalogs } from './CatalogConfig';
import { Button } from '../../components/common/Button';
const CatalogList = lazy(() => import('./CatalogPage').then(m => ({ default: m.CatalogList })));
const CatalogEdit = lazy(() => import('./CatalogPage').then(m => ({ default: m.CatalogEdit })));
const PetSupport = lazy(() => import('./CatalogPage').then(m => ({ default: m.PetSupport })));
const CatalogBatch = lazy(() => import('./CatalogBatch'));
const Schedule = lazy(() => import('./CatalogSchedule'));
const StaffPerformance = lazy(() => import('./CatalogSchedule').then(m => ({ default: m.StaffPerformance })));
const LoyaltyRules = lazy(() => import('./CatalogMarketing').then(m => ({ default: m.LoyaltyRulesPage })));
const Subscriptions = lazy(() => import('./CatalogMarketing').then(m => ({ default: m.SubscriptionsPage })));
const Campaigns = lazy(() => import('./CatalogMarketing').then(m => ({ default: m.CampaignsPage })));
function CatalogGuard({ children }: {
    children: ReactNode;
}) { const { resource = '' } = useParams(); const { permissions } = usePermissions(); const c = catalogs[resource]; return c && (permissions.includes(c.permission) || !!c.readPermission && permissions.includes(c.readPermission)) ? <div key={resource}>{children}</div> : <PageHeader title="403 — Không có quyền truy cập"/>; }
const Users = lazy(() => import('./AdminUsersPage'));
const OrdersQueue = lazy(() => import('./OperationOrders').then(m => ({ default: m.OrdersQueue })));
const OrderDetail = lazy(() => import('./OperationOrders').then(m => ({ default: m.OrderDetail })));
const OrderPrint = lazy(() => import('./OperationOrders').then(m => ({ default: m.OrderPrint })));
const InventoryQueue = lazy(() => import('./OperationInventory').then(m => ({ default: m.InventoryQueue })));
const InventoryHistory = lazy(() => import('./OperationInventory').then(m => ({ default: m.InventoryHistory })));
const AppointmentsQueue = lazy(() => import('./OperationAppointments').then(m => ({ default: m.AppointmentsQueue })));
const AppointmentDetail = lazy(() => import('./OperationAppointments').then(m => ({ default: m.AppointmentDetail })));
const ReviewsQueue = lazy(() => import('./OperationReviews').then(m => ({ default: m.ReviewsQueue })));
const Detail = lazy(() => import('./AdminUserDetail'));
const Audit = lazy(() => import('./AdminAuditPage'));
const InsightsDashboard = lazy(() => import('./InsightsDashboard'));
const InsightsSystem = lazy(() => import('./InsightsSystem'));
const NotificationComposer = lazy(() => import('./InsightsNotifications').then(m => ({default:m.NotificationComposer})));
const NotificationHistory = lazy(() => import('./InsightsNotifications').then(m => ({default:m.NotificationHistory})));
const NotificationTemplates = lazy(() => import('./InsightsNotifications').then(m => ({default:m.NotificationTemplates})));
const menu = [{ to: '/admin', label: 'Tổng quan', permission: 'PORTAL_ACCESS' }, {to:"/admin/notifications",label:"Gửi thông báo",permission:"SYSTEM_MANAGE"},{to:"/admin/notifications/templates",label:"Mẫu thông báo",permission:"SYSTEM_MANAGE"},{to:"/admin/notifications/history",label:"Lịch sử thông báo",permission:"SYSTEM_MANAGE"},{to:"/admin/system",label:"Hệ thống",permission:"SYSTEM_MANAGE"},...Object.entries(catalogs).map(([key, c]) => ({ to: '/admin/catalog/' + key, label: c.title, permission: c.readPermission || c.permission })), { to: '/admin/pet-support', label: 'Hỗ trợ thú cưng', permission: 'ASSIGNED_MEDICAL_MANAGE' }, { to: '/admin/schedules', label: 'Lịch làm việc', permission: 'APPOINTMENT_MANAGE' }, { to: '/admin/staff-performance', label: 'Hiệu suất nhân viên', permission: 'REPORT_VIEW' }, { to: '/admin/loyalty-rules', label: 'Quy tắc điểm', permission: 'SYSTEM_MANAGE' }, { to: '/admin/subscriptions', label: 'Đăng ký gói', permission: 'PACKAGE_MANAGE' }, { to: '/admin/campaigns', label: 'Chiến dịch ưu đãi', permission: 'PROMOTION_MANAGE' }, { to: '/admin/orders', label: 'Đơn hàng', permission: 'ORDER_MANAGE' }, { to: '/admin/inventory', label: 'Kho', permission: 'INVENTORY_MANAGE' }, { to: '/admin/appointments', label: 'Lịch hẹn', permission: 'ASSIGNED_APPOINTMENT_MANAGE' }, { to: '/admin/reviews', label: 'Đánh giá', permission: 'REVIEW_MANAGE' }, { to: '/admin/users', label: 'Người dùng', permission: 'USER_MANAGE' }, { to: '/admin/audit-logs', label: 'Nhật ký thao tác', permission: 'AUDIT_VIEW' }];
function Guard({ permission, children }: {
    permission: string;
    children: ReactNode;
}) {
    const { permissions } = usePermissions();
    return permissions.includes(permission) ? children : <PageHeader title="403 — Không có quyền truy cập" description="Tài khoản của bạn không được phép sử dụng chức năng này."/>;
}
function PendingReviews() {
    const r = useAdminResource<{
        pending: number;
    }>('/admin/reviews/count');
    return <div className="my-4">{r.error ? <ErrorNotice message={r.error} retry={r.reload}/> : <Link to="/admin/reviews"><StatCard label="Đánh giá chờ duyệt" value={r.data?.pending ?? '…'}/></Link>}</div>;
}
function Home() {
    const { permissions } = usePermissions();
    if (permissions.includes('REPORT_VIEW')) return <InsightsDashboard/>;
    return <><PageHeader title="Cổng quản trị PetCare Hub" description="Các chức năng được hiển thị theo quyền tài khoản do máy chủ cung cấp."/><p className="rounded-2xl border bg-white p-5">Tài khoản nội bộ đã được xác thực. Chọn chức năng vận hành trong menu để bắt đầu.</p>{permissions.includes('REVIEW_MANAGE') && <PendingReviews/>}</>;
}
export default function AdminPortal() {
    const { user, logout } = useAuth();
    const { permissions, loading, error, retry } = usePermissions();
    const [open, setOpen] = useState(false);
    const location = useLocation();
    if (loading)
        return <p role="status" className="p-6">Đang xác minh quyền…</p>;
    if (error)
        return <div className="p-6"><ErrorNotice message={error} retry={retry}/></div>;
    if (!permissions.includes('PORTAL_ACCESS'))
        return <div className="p-6"><PageHeader title="403 — Không có quyền truy cập"/><Link to="/" className="underline">Về trang chủ</Link></div>;
    const current = menu.find(m => m.to === location.pathname.replace(/\/+$/, ''))?.label || 'Chi tiết';
    return <div className="min-h-screen bg-gray-50 text-gray-900">
    <header className="flex min-w-0 flex-wrap items-center justify-between gap-3 border-b bg-white p-4"><Link to="/admin" className="text-lg font-bold">PetCare Hub · Quản trị</Link><div className="flex flex-wrap items-center gap-2"><span className="max-w-40 truncate">{user?.firstName}</span><Button variant="outline" size="sm" onClick={logout}>Đăng xuất</Button><Button className="lg:hidden" variant="outline" size="sm" aria-expanded={open} aria-controls="admin-menu" onClick={() => setOpen(!open)}>Menu</Button></div></header>
    <div className="mx-auto grid max-w-[1600px] min-w-0 lg:grid-cols-[230px_minmax(0,1fr)]">
      <aside id="admin-menu" className={`${open ? 'block' : 'hidden'} min-w-0 border-b bg-white p-4 lg:block lg:min-h-[calc(100vh-80px)] lg:border-r`}><nav aria-label="Quản trị" className="space-y-2">{menu.filter(m => permissions.includes(m.permission)).map(m => <NavLink key={m.to} to={m.to} end={m.to === '/admin'} onClick={() => setOpen(false)} className={({ isActive }) => `block min-h-11 rounded-xl px-3 py-3 ${isActive ? 'bg-primary-100 font-semibold text-primary-900' : 'hover:bg-gray-100'}`}>{m.label}</NavLink>)}<Link className="block min-h-11 px-3 py-3 underline" to="/change-password">Đổi mật khẩu</Link><Link className="block min-h-11 px-3 py-3 underline" to="/">Về cửa hàng</Link></nav></aside>
      <div className="min-w-0 p-3 sm:p-6"><nav aria-label="Đường dẫn" className="mb-5 text-sm text-gray-600"><Link to="/admin" className="underline">Quản trị</Link> / {current}</nav><Suspense fallback={<p role="status">Đang tải trang…</p>}><Routes><Route path="notifications" element={<Guard permission="SYSTEM_MANAGE"><NotificationComposer/></Guard>}/><Route path="notifications/templates" element={<Guard permission="SYSTEM_MANAGE"><NotificationTemplates/></Guard>}/><Route path="notifications/history" element={<Guard permission="SYSTEM_MANAGE"><NotificationHistory/></Guard>}/><Route path="system" element={<Guard permission="SYSTEM_MANAGE"><InsightsSystem/></Guard>}/><Route path="catalog/:resource" element={<CatalogGuard><CatalogList /></CatalogGuard>}/><Route path="catalog/:resource/:id" element={<CatalogGuard><CatalogEdit /></CatalogGuard>}/><Route path="product-batch" element={<Guard permission="CATALOG_MANAGE"><CatalogBatch /></Guard>}/><Route path="product-import" element={<Guard permission="CATALOG_MANAGE"><CatalogBatch csv/></Guard>}/><Route path="pet-support" element={<Guard permission="ASSIGNED_MEDICAL_MANAGE"><PetSupport /></Guard>}/><Route path="schedules" element={<Guard permission="APPOINTMENT_MANAGE"><Schedule /></Guard>}/><Route path="staff-performance" element={<Guard permission="REPORT_VIEW"><StaffPerformance /></Guard>}/><Route path="loyalty-rules" element={<Guard permission="SYSTEM_MANAGE"><LoyaltyRules /></Guard>}/><Route path="subscriptions" element={<Guard permission="PACKAGE_MANAGE"><Subscriptions /></Guard>}/><Route path="campaigns" element={<Guard permission="PROMOTION_MANAGE"><Campaigns /></Guard>}/><Route path="orders" element={<Guard permission="ORDER_MANAGE"><OrdersQueue /></Guard>}/><Route path="orders/:id" element={<Guard permission="ORDER_MANAGE"><OrderDetail /></Guard>}/><Route path="orders/:id/print" element={<Guard permission="ORDER_MANAGE"><OrderPrint /></Guard>}/><Route path="inventory" element={<Guard permission="INVENTORY_MANAGE"><InventoryQueue /></Guard>}/><Route path="inventory/history" element={<Guard permission="INVENTORY_MANAGE"><InventoryHistory /></Guard>}/><Route path="appointments" element={<Guard permission="ASSIGNED_APPOINTMENT_MANAGE"><AppointmentsQueue /></Guard>}/><Route path="appointments/:id" element={<Guard permission="ASSIGNED_APPOINTMENT_MANAGE"><AppointmentDetail /></Guard>}/><Route path="reviews" element={<Guard permission="REVIEW_MANAGE"><ReviewsQueue /></Guard>}/><Route index element={<Home />}/><Route path="users" element={<Guard permission="USER_MANAGE"><Users /></Guard>}/><Route path="users/:id" element={<Guard permission="USER_MANAGE"><Detail /></Guard>}/><Route path="audit-logs" element={<Guard permission="AUDIT_VIEW"><Audit /></Guard>}/><Route path="*" element={<PageHeader title="404 — Không tìm thấy trang quản trị"/>}/></Routes></Suspense></div>
    </div>
  </div>;
}
