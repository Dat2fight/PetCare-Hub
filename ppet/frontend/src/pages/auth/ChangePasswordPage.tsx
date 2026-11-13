import { useState, FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import api from '../../api/client';
import { useAuth } from '../../contexts/AuthContext';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { getErrorMessage } from '../../utils/getErrorMessage';

export default function ChangePasswordPage() {
  const { user, logout } = useAuth(); const navigate = useNavigate();
  const [currentPassword, setCurrent] = useState(''); const [newPassword, setNew] = useState(''); const [confirm, setConfirm] = useState('');
  const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  if (!user) return <Navigate to="/login" replace />;
  const submit = async (event: FormEvent) => {
    event.preventDefault(); if (busy) return;
    if (newPassword !== confirm) { setError('Hai mật khẩu mới chưa khớp.'); return; }
    if (new TextEncoder().encode(newPassword).length > 72) { setError('Mật khẩu tối đa 72 byte UTF-8.'); return; }
    setBusy(true); setError('');
    try { await api.post('/auth/password', { currentPassword, newPassword }); logout(); navigate('/login', { replace: true, state: { passwordChanged: true } }); }
    catch (error) { setError(getErrorMessage(error)); } finally { setBusy(false); }
  };
  return <div className="mx-auto max-w-lg p-4 sm:p-8"><h1 className="text-2xl font-bold">Đổi mật khẩu</h1><p className="my-4">{user.mustChangePassword ? 'Bạn cần đổi mật khẩu tạm trước khi sử dụng tài khoản.' : 'Đổi mật khẩu sẽ đăng xuất tất cả phiên hiện tại.'}</p><form onSubmit={submit} className="space-y-4"><Input label="Mật khẩu hiện tại" type="password" autoComplete="current-password" required value={currentPassword} onChange={e => setCurrent(e.target.value)} /><Input label="Mật khẩu mới" type="password" autoComplete="new-password" required minLength={8} maxLength={72} value={newPassword} onChange={e => setNew(e.target.value)} /><Input label="Nhập lại mật khẩu mới" type="password" autoComplete="new-password" required value={confirm} onChange={e => setConfirm(e.target.value)} />{error && <p role="alert" className="break-words text-red-700">{error}</p>}<Button isLoading={busy} type="submit">Đổi mật khẩu và đăng xuất</Button><Button type="button" variant="outline" disabled={busy} onClick={logout}>Đăng xuất</Button></form></div>;
}
