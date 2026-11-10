import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { PublicPetDTO } from '../../services/petManagementService';
import apiClient from '../../api/client';

export function PetPurchaseModal({ pet, onClose, onConflict }: { pet: PublicPetDTO; onClose: () => void; onConflict: () => void }) {
  const navigate = useNavigate();
  const pending = useRef(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const add = async () => {
    if (pending.current) return;
    pending.current = true;
    setBusy(true);
    try {
      await apiClient.post('/cart/items', { petId: pet.id, quantity: 1 });
      navigate('/cart');
    } catch (err) {
      if (isAxiosError(err) && [404, 409].includes(err.response?.status || 0)) onConflict();
      else setError('Thêm vào giỏ chưa thành công. Vui lòng thử lại.');
    } finally {
      pending.current = false;
      setBusy(false);
    }
  };
  return <Modal title={pet.name} onClose={onClose}>
    <p>{pet.price?.toLocaleString('vi-VN')} ₫</p>
    <p>Thú cưng được giữ chỗ khi bạn đặt đơn COD.</p>
    {error && <p role="alert">{error}</p>}
    <Button disabled={busy} onClick={add}>Thêm vào giỏ</Button>
  </Modal>;
}
