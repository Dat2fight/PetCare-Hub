import { useState, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import api from '../../api/client';
import { useAuth } from '../../contexts/AuthContext';
import { useAdminResource } from '../../hooks/useAdminResource';
import { getErrorMessage } from '../../utils/getErrorMessage';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Modal } from '../../components/common/Modal';
import { AdminForm, ConfirmDialog, DataTable, FilterBar, Page, PageHeader, Select, StatusBadge } from '../../components/admin/AdminUI';

export interface AdminUser { id: number; username: string; email: string; firstName: string; lastName: string; enabled: boolean; roles: string[]; deletedAt: string | null; mustChangePassword: boolean }
export const roleNames: Record<string, string> = { ADMIN: 'Quản trị viên', CUSTOMER: 'Khách hàng', SALES_STAFF: 'Nhân viên bán hàng', WAREHOUSE_STAFF: 'Nhân viên kho', VETERINARIAN: 'Bác sĩ thú y' };
type Operation = 'lock' | 'unlock' | 'role' | 'delete' | 'revoke';
const operationNames: Record<Operation, string> = { lock: 'Khóa tài khoản', unlock: 'Mở khóa tài khoản', role: 'Đổi vai trò', delete: 'Xóa mềm tài khoản', revoke: 'Thu hồi mọi phiên' };

export default function AdminUsersPage() {
  const { user: current, logout } = useAuth();
  const [page, setPage] = useState(0); const [sort, setSort] = useState('id,asc');
  const [keyword, setKeyword] = useState(''); const [roleFilter, setRoleFilter] = useState(''); const [status, setStatus] = useState('');
  const [filters, setFilters] = useState('');
  const resource = useAdminResource<Page<AdminUser>>(`/admin/users?page=${page}&size=20&sort=${sort}${filters}`);
  const [operation, setOperation] = useState<{ user: AdminUser; type: Operation }>();
  const [reason, setReason] = useState(''); const [password, setPassword] = useState(''); const [role, setRole] = useState('CUSTOMER');
  const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [notice, setNotice] = useState('');
  const [create, setCreate] = useState(false); const [temporary, setTemporary] = useState<{ username: string; password: string }>();
  const [staff, setStaff] = useState({ username: '', email: '', firstName: '', lastName: '', phone: '', role: 'SALES_STAFF' });
  const open = (user: AdminUser, type: Operation) => { setOperation({ user, type }); setReason(''); setPassword(''); setError(''); setRole(user.roles[0]); };
  const apply = async (event: FormEvent) => {
    event.preventDefault(); if (!operation || busy) return;
    setBusy(true); setError(''); const base = `/admin/users/${operation.user.id}`;
    try {
      const data = { reason, currentPassword: password };
      if (operation.type === 'role') await api.patch(`${base}/role`, { ...data, role });
      else if (operation.type === 'delete') await api.delete(base, { data });
      else if (operation.type === 'revoke') await api.post(`${base}/revoke-sessions`, data);
      else await api.patch(`${base}/status`, { ...data, enabled: operation.type === 'unlock' });
      if (operation.type === 'revoke' && operation.user.id === current?.id) { logout(); return; }
      setNotice(`${operationNames[operation.type]} thành công.`); setOperation(undefined); resource.reload();
    } catch (error) { setError(getErrorMessage(error)); } finally { setBusy(false); setPassword(''); }
  };
  const provision = async (event: FormEvent) => {
    event.preventDefault(); if (busy) return; setBusy(true); setError('');
    try {
      const { data } = await api.post<{ user: AdminUser; temporaryPassword: string }>('/admin/staff', staff);
      setCreate(false); setTemporary({ username: data.user.username, password: data.temporaryPassword }); resource.reload();
    } catch (error) { setError(getErrorMessage(error)); } finally { setBusy(false); }
  };
  const needsPassword = operation && ((operation.type === 'delete' && operation.user.roles.some(r => r !== 'CUSTOMER')) || (operation.type === 'lock' && operation.user.roles.includes('ADMIN')) || (operation.type === 'role' && (operation.user.roles.includes('ADMIN') || role === 'ADMIN')));
  return <>
    <PageHeader title="Người dùng" description="Quản lý tài khoản, quyền truy cập và phiên đăng nhập." actions={<Button onClick={() => { setCreate(true); setError(''); setStaff({ username: '', email: '', firstName: '', lastName: '', phone: '', role: 'SALES_STAFF' }); }}>Tạo nhân viên</Button>} />
    {notice && <p role="status" className="mb-4 rounded-xl bg-green-50 p-3">{notice}</p>}
    <FilterBar onSubmit={e => { e.preventDefault(); setPage(0); setFilters(`&${new URLSearchParams({ keyword, ...(roleFilter ? { role: roleFilter } : {}), ...(status ? { status } : {}) })}`); }}>
      <Input label="Tên hoặc email" value={keyword} maxLength={100} onChange={e => setKeyword(e.target.value)} />
      <Select label="Vai trò" value={roleFilter} onChange={setRoleFilter}><option value="">Tất cả</option>{Object.entries(roleNames).map(([key, name]) => <option key={key} value={key}>{name}</option>)}</Select>
      <Select label="Trạng thái" value={status} onChange={setStatus}><option value="">Chưa xóa</option><option value="ACTIVE">Đang hoạt động</option><option value="LOCKED">Đã khóa</option><option value="DELETED">Đã xóa</option><option value="ALL">Tất cả</option></Select>
      <Button type="submit" className="self-end">Áp dụng bộ lọc</Button>
    </FilterBar>
    <DataTable {...resource} page={resource.data} retry={resource.reload} onPage={setPage} sort={sort} onSort={value => { setSort(value); setPage(0); }} columns={[
      { key: 'name', label: 'Tài khoản', sort: 'username', render: u => <><Link className="font-semibold underline" to={`/admin/users/${u.id}`}>{u.lastName} {u.firstName}</Link><p className="text-sm">{u.username}</p></> },
      { key: 'email', label: 'Email', sort: 'email', render: u => u.email },
      { key: 'role', label: 'Vai trò', render: u => u.roles.map(r => roleNames[r] || r).join(', ') },
      { key: 'status', label: 'Trạng thái', sort: 'enabled', render: u => <StatusBadge>{u.deletedAt ? 'Đã xóa' : !u.enabled ? 'Đã khóa' : u.mustChangePassword ? 'Cần đổi mật khẩu' : 'Hoạt động'}</StatusBadge> },
      { key: 'actions', label: 'Thao tác', render: u => u.deletedAt ? <span>Dữ liệu lịch sử được giữ lại.</span> : <div className="flex flex-wrap gap-2">{(['role', u.enabled ? 'lock' : 'unlock', 'delete', 'revoke'] as Operation[]).map(type => <Button key={type} size="sm" variant="outline" disabled={u.id === current?.id && type !== 'revoke'} title={u.id === current?.id && type !== 'revoke' ? 'Không được thay đổi chính mình' : undefined} onClick={() => open(u, type)}>{operationNames[type]}</Button>)}{u.id === current?.id && <small>Không được tự đổi vai trò, khóa hoặc xóa.</small>}</div> },
    ]} />
    {operation && <ConfirmDialog title={operationNames[operation.type]} busy={busy} onClose={() => setOperation(undefined)}><AdminForm onSubmit={apply} busy={busy} error={error}>
      <p className="break-words">Tài khoản: <strong>{operation.user.username}</strong>. Phiên đăng nhập cũ sẽ bị thu hồi.</p>
      {operation.type === 'delete' && <p>Đơn hàng và lịch sử được giữ lại. Lịch hẹn tương lai của nhân viên sẽ chờ phân công lại.</p>}
      {operation.type === 'role' && <><Select label="Vai trò mới" value={role} onChange={setRole}>{Array.from(new Set([...operation.user.roles, 'CUSTOMER', 'ADMIN'])).map(r => <option key={r} value={r}>{roleNames[r]}</option>)}</Select><p className="text-sm">Vai trò nhân viên mới chỉ được cấp qua chức năng tạo nhân viên.</p></>}
      <Input label="Lý do" value={reason} onChange={e => setReason(e.target.value)} required maxLength={500} />
      {needsPassword && <Input label="Mật khẩu của bạn để xác nhận" type="password" value={password} onChange={e => setPassword(e.target.value)} autoComplete="current-password" required maxLength={72} />}
      <Button type="submit" variant="danger" isLoading={busy}>Xác nhận {operationNames[operation.type].toLowerCase()}</Button>
    </AdminForm></ConfirmDialog>}
    {create && <Modal title="Tạo tài khoản nhân viên" busy={busy} onClose={() => setCreate(false)}><AdminForm onSubmit={provision} busy={busy} error={error}>
      {([['username', 'Tên đăng nhập'], ['email', 'Email'], ['lastName', 'Họ'], ['firstName', 'Tên'], ['phone', 'Số điện thoại']] as const).map(([key, label]) => <Input key={key} label={label} type={key === 'email' ? 'email' : 'text'} value={staff[key]} required={key !== 'phone'} maxLength={key === 'username' ? 50 : key === 'email' ? 255 : key === 'phone' ? 20 : 100} onChange={e => setStaff({ ...staff, [key]: e.target.value })} />)}
      <Select label="Vai trò" value={staff.role} onChange={role => setStaff({ ...staff, role })}>{['SALES_STAFF', 'WAREHOUSE_STAFF', 'VETERINARIAN'].map(r => <option key={r} value={r}>{roleNames[r]}</option>)}</Select>
      <p>Mật khẩu tạm chỉ hiển thị một lần. Nhân viên phải đổi mật khẩu khi đăng nhập.</p><Button type="submit" isLoading={busy}>Tạo tài khoản</Button>
    </AdminForm></Modal>}
    {temporary && <Modal title="Mật khẩu tạm — chỉ hiển thị lần này" onClose={() => setTemporary(undefined)}><p>Hãy chuyển mật khẩu riêng cho nhân viên và lưu trước khi đóng.</p><Input label="Tên đăng nhập" readOnly value={temporary.username} /><Input label="Mật khẩu tạm" readOnly value={temporary.password} /><Button className="mt-4" onClick={async () => { try { await navigator.clipboard.writeText(temporary.password); setNotice('Đã sao chép mật khẩu tạm.'); } catch { setNotice('Không thể sao chép tự động; hãy chọn và sao chép mật khẩu trong ô.'); } }}>Sao chép mật khẩu</Button></Modal>}
  </>;
}
