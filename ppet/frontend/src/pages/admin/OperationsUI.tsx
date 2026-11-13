import { formatBusinessTime } from '../../utils/businessDate';
import { useState, FormEvent, ReactNode } from 'react';
import api from '../../api/client';
import { useAdminResource } from '../../hooks/useAdminResource';
import { getErrorMessage } from '../../utils/getErrorMessage';
import { Page, FilterBar, Select, ConfirmDialog, AdminForm } from '../../components/admin/AdminUI';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
export type Row = {
  id: number;
  [key: string]: any;
};
export const labels: Record<string, string> = {
  PENDING: 'Chờ xử lý',
  CONFIRMED: 'Đã xác nhận',
  PROCESSING: 'Đang chuẩn bị',
  SHIPPING: 'Đang giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
  COMPLETED: 'Hoàn tất',
  FAILED: 'Thất bại',
  REFUNDED: 'Đã hoàn tiền',
  SCHEDULED: 'Đã đặt lịch',
  IN_PROGRESS: 'Đang thực hiện',
  NO_SHOW: 'Vắng mặt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
  HIDDEN: 'Đã ẩn',
  STOCK_IN: 'Nhập kho',
  STOCK_OUT: 'Xuất kho',
  ADJUSTMENT: 'Kiểm kê',
  CASH_RECEIVED: 'Đã thu tiền mặt',
  CASH_REFUNDED: 'Đã hoàn tiền mặt'
};
export type Field = {
  key: string;
  label: string;
  type?: string;
  options?: Record<string, string>;
  required?: boolean;
  value?: string;
};
export function useQueue(path: string, initial = '') {
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState(initial);
  const [sort, setSort] = useState('id,desc');
  const r = useAdminResource<Page<Row>>(`${path}?page=${page}&size=20&sort=${sort}&${filter}`);
  return {
    ...r,
    page: r.data,
    setPage,
    sort,
    setSort: (value: string) => {
      setSort(value);
      setPage(0);
    },
    filter: (value: string) => {
      setFilter(value);
      setPage(0);
    }
  };
}
export function Filters({
  fields,
  apply
}: {
  fields: Field[];
  apply: (query: string) => void;
}) {
  const [values, setValues] = useState<Record<string, string>>(() => Object.fromEntries(fields.map(f => [f.key, f.value || ''])));
  return <FilterBar onSubmit={e => {
    e.preventDefault();
    apply(new URLSearchParams(Object.fromEntries(Object.entries(values).filter(([, v]) => v !== ''))).toString());
  }}>{fields.map(f => f.options ? <Select key={f.key} label={f.label} value={values[f.key]} onChange={v => setValues({
      ...values,
      [f.key]: v
    })}>{Object.entries(f.options).map(([v, label]) => <option value={v} key={v}>{label}</option>)}</Select> : <Input key={f.key} label={f.label} type={f.type || 'text'} value={values[f.key]} onChange={e => setValues({
      ...values,
      [f.key]: e.target.value
    })} maxLength={100} />)}<Button type="submit" className="self-end">Lọc</Button></FilterBar>;
}
export const options = (values: string[], all = true) => Object.fromEntries([...(all ? [['', 'Tất cả']] : []), ...values.map(v => [v, labels[v] || v])]);
export function ActionDialog({
  title,
  url,
  fields = [],
  body = {},
  onClose,
  onDone,
  warning,
  children
}: {
  title: string;
  url: string;
  fields?: Field[];
  body?: Record<string, unknown>;
  onClose: () => void;
  onDone: () => void;
  warning?: string;
  children?: ReactNode;
}) {
  const [values, setValues] = useState<Record<string, string>>(() => Object.fromEntries(fields.map(f => [f.key, f.value || ''])));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    try {
      const data = {
        ...body,
        ...Object.fromEntries(fields.map(f => [f.key, f.type === 'number' ? Number(values[f.key]) : values[f.key]]))
      };
      await api.post(url, data);
      onDone();
      onClose();
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };
  return <ConfirmDialog title={title} busy={busy} onClose={onClose}>{children}<AdminForm busy={busy} error={error} onSubmit={submit}><p>{warning || 'Thao tác sẽ được ghi nhật ký. Vui lòng kiểm tra trước khi xác nhận.'}</p>{fields.map(f => f.options ? <Select key={f.key} label={f.label} value={values[f.key]} onChange={v => setValues({
        ...values,
        [f.key]: v
      })} required={f.required !== false}><option value="">Chọn…</option>{Object.entries(f.options).map(([v, label]) => <option key={v} value={v}>{label}</option>)}</Select> : <Input key={f.key} label={f.label} type={f.type || 'text'} required={f.required !== false} min={f.type === 'number' ? 0 : undefined} maxLength={f.key === 'reference' ? 100 : 500} value={values[f.key]} onChange={e => setValues({
        ...values,
        [f.key]: e.target.value
      })} />)}<Button type="submit" isLoading={busy}>Xác nhận</Button></AdminForm></ConfirmDialog>;
}
export const timeText = (value?: string) => value ? formatBusinessTime(value, true) : "—";
