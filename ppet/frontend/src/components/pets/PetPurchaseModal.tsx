import { FormEvent, useRef, useState } from 'react';
import { isAxiosError } from 'axios';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { Input } from '../common/Input';
import { PublicPetDTO } from '../../services/petManagementService';
import { orderService } from '../../services/orderService';
import { useAuth } from '../../contexts/AuthContext';

export const PetPurchaseModal = ({ pet, onClose, onConflict }: { pet: PublicPetDTO; onClose: () => void; onConflict: () => void }) => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const submitting = useRef(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (submitting.current) return;
    const data = new FormData(event.currentTarget);
    const recipient = String(data.get('recipient') || '').trim();
    const phone = String(data.get('phone') || '').trim();
    const address = String(data.get('address') || '').trim();
    if (!recipient || !phone || !address || !data.get('confirmed')) { setError('Vui lòng nhập đủ thông tin và xác nhận trách nhiệm chăm sóc.'); return; }
    submitting.current = true; setBusy(true); setError('');
    try {
      // Existing order API stores contact and address together in shippingAddress, as product checkout does.
      await orderService.create({ items: [{ petId: pet.id, quantity: 1 }], paymentMethod: 'COD',
        shippingAddress: `${recipient}, ${phone}, ${address}`, notes: String(data.get('notes') || '').trim() });
      toast.success('Đã đặt mua thú cưng'); onClose(); navigate('/orders');
    } catch (failure) {
      if (isAxiosError(failure) && failure.response?.status === 409) {
        toast.error('Thú cưng vừa được người khác đặt'); onConflict();
      } else {
        const message = isAxiosError(failure) && typeof failure.response?.data?.message === 'string' ? failure.response.data.message : 'Không đặt được thú cưng. Hãy kiểm tra danh sách đơn hàng nếu kết nối bị gián đoạn trước khi thử lại.';
        setError(message);
      }
    } finally { submitting.current = false; setBusy(false); }
  };
  return <Modal title="Xác nhận đặt mua thú cưng" onClose={onClose} busy={busy}>
    <form onSubmit={submit} className="space-y-4">
      {pet.imageUrl && <img src={pet.imageUrl} alt={pet.name} className="w-full h-40 object-contain" />}
      <p className="font-semibold">{pet.name} · {pet.price?.toLocaleString('vi-VN')} ₫</p>
      <Input name="recipient" label="Người nhận" autoComplete="name" required maxLength={150} defaultValue={[user?.firstName, user?.lastName].filter(Boolean).join(' ')} />
      <Input name="phone" label="Số điện thoại" type="tel" autoComplete="tel" required maxLength={25} defaultValue={user?.phone || ''} />
      <Input name="address" label="Địa chỉ giao hàng" autoComplete="street-address" required maxLength={1700} />
      <Input name="notes" label="Ghi chú (không bắt buộc)" maxLength={2000} />
      <p>Thanh toán khi nhận hàng (COD).</p>
      <label className="flex gap-2 items-start"><input type="checkbox" name="confirmed" required className="mt-1" />
        Tôi đã kiểm tra thông tin thú cưng và xác nhận có thể tiếp nhận, chăm sóc thú cưng.</label>
      {error && <p role="alert" className="text-red-600">{error}</p>}
      <Button type="submit" fullWidth isLoading={busy}>Xác nhận đặt mua</Button>
    </form>
  </Modal>;
};
