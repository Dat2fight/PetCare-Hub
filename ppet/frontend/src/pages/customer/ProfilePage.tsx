import { useState, FormEvent } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import apiClient from '../../api/client';
import toast from 'react-hot-toast';
export const ProfilePage = () => {
 const {user,login,isLoading} = useAuth(); const [busy,setBusy] = useState(false);
 const save = async (e: FormEvent<HTMLFormElement>) => {e.preventDefault(); const data = Object.fromEntries(new FormData(e.currentTarget));setBusy(true);
  try { await apiClient.put('/users/me',data);const response = await apiClient.get('/auth/me');login(localStorage.getItem('accessToken') || '',response.data);toast.success('Đã cập nhật hồ sơ'); }
  catch {toast.error('Không cập nhật được hồ sơ. Vui lòng kiểm tra thông tin.');} finally {setBusy(false);}
 };
 if(isLoading) return <p>Đang tải…</p>; if(!user) return <Navigate to="/login" replace />;
 return <div className="max-w-3xl mx-auto p-8"><h1 className="text-3xl font-bold mb-6">Hồ sơ cá nhân</h1><form onSubmit={save} className="grid gap-5 bg-white p-6 rounded-2xl">
 <p>Tên đăng nhập: {user.username}</p>
 <label>Tên <input name="firstName" required maxLength={100} defaultValue={user.firstName}/></label>
 <label>Họ <input name="lastName" required maxLength={100} defaultValue={user.lastName}/></label>
 <label>Email <input name="email" type="email" required maxLength={255} defaultValue={user.email}/></label>
 <label>Số điện thoại <input name="phone" maxLength={20} defaultValue={user.phone}/></label>
 <button disabled={busy} className="bg-primary-600 text-white rounded-xl p-3">{busy?'Đang lưu…':'Cập nhật hồ sơ'}</button></form></div>;
};
