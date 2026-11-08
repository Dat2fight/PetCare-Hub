import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { PublicPetDTO } from '../../services/petManagementService';

export const PetPurchaseModal = ({ onClose }: { pet: PublicPetDTO; onClose: () => void; onConflict: () => void }) => (
  <Modal title="Đặt mua thú cưng — Sắp ra mắt" onClose={onClose}>
    <p role="status">Mua thú cưng trực tuyến chưa khả dụng trên phiên bản này.</p>
    <Button type="button" disabled>Đặt mua — Sắp ra mắt</Button>
  </Modal>
);
