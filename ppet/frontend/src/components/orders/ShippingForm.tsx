import { Input } from '../common/Input';
import type { ShippingInfo } from '../../services/orderService';

const fields = [
  ['recipientName', 'Người nhận', 'name', 100],
  ['phone', 'Số điện thoại', 'tel', 20],
  ['province', 'Tỉnh hoặc thành phố', 'address-level1', 100],
  ['district', 'Quận hoặc huyện', 'address-level2', 100],
  ['ward', 'Phường hoặc xã', 'address-level3', 100],
  ['addressLine', 'Số nhà và tên đường', 'address-line1', 200],
  ['note', 'Ghi chú giao hàng (tùy chọn)', 'off', 500],
] as const;

export function ShippingForm({ values, onChange, errors }: {
  values: ShippingInfo;
  onChange: (value: ShippingInfo) => void;
  errors: Record<string, string>;
}) {
  return <fieldset className="grid gap-3 min-w-0">
    <legend className="mb-2 font-semibold">Thông tin giao hàng</legend>
    {fields.map(([name, label, autoComplete, maxLength]) => <Input key={name}
      name={name} label={label} value={values[name]} required={name !== 'note'}
      type={name === 'phone' ? 'tel' : 'text'} autoComplete={autoComplete}
      maxLength={maxLength} error={errors[name]}
      onChange={event => onChange({ ...values, [name]: event.target.value })} />)}
  </fieldset>;
}
