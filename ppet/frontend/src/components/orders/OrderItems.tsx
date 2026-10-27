import { OrderItem } from '../../types';

export const OrderItems = ({ items }: { items: OrderItem[] }) => <ul className="space-y-3">
  {items.map((item, index) => <li key={index} className="flex items-center gap-3">
    {item.imageUrl && <img src={item.imageUrl} alt="" className="h-16 w-16 rounded-lg object-cover" />}
    <p>{item.petId != null ? item.petName || `Thú cưng #${item.petId}` : item.productName || `Sản phẩm #${item.productId}`}
      {' '}× {item.quantity}: {(item.price * item.quantity).toLocaleString('vi-VN')} ₫</p>
  </li>)}
</ul>;
