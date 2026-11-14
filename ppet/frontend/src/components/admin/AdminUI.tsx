import { ReactNode, FormEvent, useId } from 'react';
import { Button } from '../common/Button';
import { Modal } from '../common/Modal';

export interface Page<T> { content: T[]; totalElements: number; number: number; size: number }
export const PageHeader = ({ title, description, actions }: { title: string; description?: string; actions?: ReactNode }) => <div className="mb-6 flex flex-wrap items-start justify-between gap-3"><div className="min-w-0"><h1 className="text-2xl font-bold break-words">{title}</h1>{description && <p className="mt-2 text-gray-600">{description}</p>}</div>{actions}</div>;
export const FilterBar = ({ children, onSubmit }: { children: ReactNode; onSubmit: (e: FormEvent) => void }) => <form onSubmit={onSubmit} className="mb-5 grid gap-3 rounded-2xl bg-white p-4 sm:grid-cols-2 xl:grid-cols-4">{children}</form>;
export const StatusBadge = ({ children }: { children: ReactNode }) => <span className="inline-block rounded-lg bg-primary-50 px-2 py-1 text-sm text-primary-800">{children}</span>;
export const StatCard = ({ label, value }: { label: string; value: ReactNode }) => <div className="min-w-0 rounded-2xl border bg-white p-5"><p className="text-sm text-gray-600">{label}</p><p className="mt-2 break-words text-2xl font-bold">{value}</p></div>;
export const PriceText = ({ value }: { value: number }) => <>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)}</>;
export const ErrorNotice = ({ message, retry }: { message: string; retry?: () => void }) => <div role="alert" className="my-3 rounded-xl bg-red-50 p-4 text-red-800 break-words">{message}{retry && <Button type="button" variant="outline" onClick={retry} className="mt-3">Thử lại</Button>}</div>;
export const AdminForm = ({ children, onSubmit, busy, error }: { children: ReactNode; onSubmit: (e: FormEvent) => void; busy: boolean; error?: string }) => <form onSubmit={onSubmit} aria-busy={busy}><fieldset disabled={busy} className="min-w-0 space-y-4">{children}</fieldset>{error && <ErrorNotice message={error} />}</form>;
export function Select({ label, value, onChange, children, required = false }: { label: string; value: string; onChange: (value: string) => void; children: ReactNode; required?: boolean }) {
  const id = useId();
  return <div className="min-w-0"><label htmlFor={id} className="mb-1.5 block text-sm font-medium">{label}</label><select id={id} value={value} onChange={e => onChange(e.target.value)} required={required} className="min-h-11 w-full min-w-0 rounded-xl border bg-gray-50 px-3 py-2">{children}</select></div>;
}
export function ConfirmDialog({ title, children, onClose, busy }: { title: string; children: ReactNode; onClose: () => void; busy: boolean }) {
  return <Modal title={title} onClose={onClose} busy={busy}>{children}</Modal>;
}
export interface Column<T> { key: string; label: string; render: (row: T) => ReactNode; sort?: string }
export function DataTable<T extends { id: number }>({ columns, page, loading, error, retry, onPage, sort, onSort }: {
  columns: Column<T>[]; page?: Page<T>; loading: boolean; error: string; retry: () => void; onPage: (page: number) => void;
  sort?: string; onSort?: (sort: string) => void;
}) {
  if (error) return <ErrorNotice message={error} retry={retry} />;
  if (loading) return <p role="status" className="p-6">Đang tải dữ liệu…</p>;
  if (!page?.content.length) return <p role="status" className="rounded-2xl border bg-white p-6">Không có dữ liệu phù hợp.</p>;
  return <div className="min-w-0">
    <table className="block w-full rounded-2xl border bg-white md:table"><thead className="hidden md:table-header-group"><tr>{columns.map(c => <th key={c.key} scope="col" className="p-3 text-left text-sm" aria-sort={sort?.startsWith(c.sort + ',') ? (sort.endsWith('desc') ? 'descending' : 'ascending') : undefined}>{c.sort && onSort ? <button type="button" className="min-h-11 underline" onClick={() => onSort(`${c.sort},${sort === `${c.sort},asc` ? 'desc' : 'asc'}`)}>{c.label}{sort?.startsWith(c.sort + ',') ? (sort.endsWith('desc') ? ' ↓' : ' ↑') : ''}</button> : c.label}</th>)}</tr></thead>
      <tbody className="block md:table-row-group">{page.content.map(row => <tr key={row.id} className="block border-b p-3 last:border-0 md:table-row md:p-0">{columns.map(c => <td key={c.key} className="block min-w-0 p-2 align-top break-words [overflow-wrap:anywhere] md:table-cell md:max-w-64 md:p-3"><span className="mb-1 block text-xs font-semibold text-gray-500 md:hidden">{c.label}</span>{c.render(row)}</td>)}</tr>)}</tbody></table>
    <nav aria-label="Phân trang" className="mt-4 flex flex-wrap items-center gap-3"><Button type="button" variant="outline" disabled={page.number === 0} onClick={() => onPage(page.number - 1)}>Trước</Button><span>Trang {page.number + 1}/{Math.max(1, Math.ceil(page.totalElements / page.size))} · {page.totalElements} bản ghi</span><Button type="button" variant="outline" disabled={(page.number + 1) * page.size >= page.totalElements} onClick={() => onPage(page.number + 1)}>Sau</Button></nav>
  </div>;
}
