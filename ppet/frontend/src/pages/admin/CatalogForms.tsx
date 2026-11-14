import { useState, FormEvent } from 'react';
import api from '../../api/client';
import { getErrorMessage } from '../../utils/getErrorMessage';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select, ConfirmDialog, AdminForm, DataTable } from '../../components/admin/AdminUI';
import { useQueue } from './OperationsUI';
import { CatalogField, initialValues, formBody } from './CatalogConfig';
function Reference({ field, value, onChange }: {
    field: CatalogField;
    value: string;
    onChange: (v: string) => void;
}) {
    const r = useQueue('/admin/catalog/' + field.ref);
    const [open, setOpen] = useState(false);
    return <div className="min-w-0"><Input label={field.label + ' · ID'} type="number" min={1} value={value} required={field.required} onChange={e => onChange(e.target.value)}/><Button type="button" variant="outline" onClick={() => setOpen(!open)}>Chọn {field.label.toLowerCase()}</Button>{open && <div className="my-3 border rounded-xl p-2"><Input label="Tìm tên" onChange={e => r.filter(new URLSearchParams({ keyword: e.target.value }).toString())}/><DataTable {...r} retry={r.reload} onPage={r.setPage} columns={[{ key: 'name', label: 'Tên', render: x => <Button type="button" variant="outline" onClick={() => { onChange(String(x.id)); setOpen(false); }}>{x.name} · #{x.id}</Button> }]}/></div>}</div>;
}
export function Fields({ fields, values, setValues }: {
    fields: CatalogField[];
    values: Record<string, string>;
    setValues: (v: Record<string, string>) => void;
}) { return <div className="grid gap-4 sm:grid-cols-2">{fields.map(f => { const change = (value: string) => setValues({ ...values, [f.key]: value }); return f.ref ? <Reference key={f.key} field={f} value={values[f.key]} onChange={change}/> : f.type === 'textarea' ? <label key={f.key} className="sm:col-span-2">{f.label}<textarea className="block mt-2 rounded-xl border p-3 w-full" rows={4} maxLength={4000} value={values[f.key]} onChange={e => change(e.target.value)}/></label> : f.options || f.type === 'boolean' ? <Select key={f.key} label={f.label} value={values[f.key]} onChange={change} required={f.required}>{Object.entries(f.options || { true: 'Có', false: 'Không' }).map(([v, label]) => <option key={v} value={v}>{label}</option>)}</Select> : <Input key={f.key} label={f.label} type={f.type || 'text'} value={values[f.key]} onChange={e => change(e.target.value)} required={f.required} min={f.min} max={f.max} step={f.type === 'number' ? 'any' : undefined} maxLength={f.type === 'url' ? 500 : 255}/>; })}</div>; }
export function CatalogForm({ fields, data = {}, url, method = 'post', onDone, warning }: {
    fields: CatalogField[];
    data?: Record<string, unknown>;
    url: string;
    method?: 'post' | 'put';
    onDone: () => void;
    warning?: string;
}) {
    const [values, setValues] = useState(() => initialValues(fields, data));
    const [confirm, setConfirm] = useState(false);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const save = async () => { if (busy)
        return; setBusy(true); setError(''); try {
        await api[method](url, formBody(fields, values));
        onDone();
        setConfirm(false);
    }
    catch (e) {
        setError(getErrorMessage(e));
    }
    finally {
        setBusy(false);
    } };
    return <><AdminForm busy={busy} error={!confirm ? error : undefined} onSubmit={(e: FormEvent) => { e.preventDefault(); setConfirm(true); }}><Fields fields={fields} values={values} setValues={setValues}/><Button type="submit">Lưu thay đổi</Button></AdminForm>{confirm && <ConfirmDialog title="Xác nhận thay đổi" busy={busy} onClose={() => { if (!busy)
        setConfirm(false); }}><p className="mb-4">{warning || 'Kiểm tra lại thông tin. Thay đổi sẽ có hiệu lực và được ghi nhật ký.'}</p><AdminForm busy={busy} error={error} onSubmit={e => { e.preventDefault(); void save(); }}><Button type="submit" isLoading={busy}>Xác nhận lưu</Button></AdminForm></ConfirmDialog>}</>;
}
