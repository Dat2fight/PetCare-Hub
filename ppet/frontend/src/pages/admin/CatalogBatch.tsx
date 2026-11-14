import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../../api/client';
import { getErrorMessage } from '../../utils/getErrorMessage';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { PageHeader, DataTable, ErrorNotice, ConfirmDialog, Select, PriceText } from '../../components/admin/AdminUI';
import { Row } from './OperationsUI';
export default function CatalogBatch({ csv = false }: {
    csv?: boolean;
}) {
    const [params] = useSearchParams();
    const [ids, setIds] = useState(params.get('ids') || '');
    const [action, setAction] = useState('ACTIVE');
    const [value, setValue] = useState('0');
    const [active, setActive] = useState('false');
    const [file, setFile] = useState('');
    const [preview, setPreview] = useState<{
        rows: Row[];
        previewToken: string;
        valid?: boolean;
    }>();
    const [confirm, setConfirm] = useState(false);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const [done, setDone] = useState(false);
    const reset = () => { setPreview(undefined); setDone(false); };
    const run = async (apply: boolean) => { if (busy)
        return; setBusy(true); setError(''); try {
        const body = csv ? { csv: file } : { ids: ids.split(',').map(x => Number(x.trim())), action, value: Number(value), active: active === 'true' };
        const { data } = await api.post(`/admin/catalog/products/${csv ? 'csv' : 'bulk'}-${apply ? 'apply' : 'preview'}`, { ...body, ...(apply ? { previewToken: preview?.previewToken } : {}) });
        if (apply) {
            setDone(true);
            setPreview(undefined);
            setConfirm(false);
        }
        else
            setPreview(data);
    }
    catch (e) {
        setError(getErrorMessage(e));
    }
    finally {
        setBusy(false);
    } };
    return <><PageHeader title={csv ? 'Nhập sản phẩm từ CSV' : 'Thay đổi sản phẩm hàng loạt'}/>{csv ? <><p className="mb-3 break-words">Tối đa 200 sản phẩm, 200 KB. Header: sku,name,price,categoryId,imageUrl,description. SKU hiện có sẽ được cập nhật; tồn kho không đổi.</p><label>Chọn file CSV<input className="block w-full my-3" type="file" accept=".csv,text/csv" disabled={busy} onChange={async (e) => { reset(); setFile(''); const f = e.target.files?.[0]; if (!f)
        return; if (f.size > 200000) {
        setError('File tối đa 200 KB');
        return;
    } setFile(await f.text()); setError(''); }}/></label></> : <div className="grid gap-3 sm:grid-cols-2"><Input label="ID sản phẩm, cách nhau bằng dấu phẩy (tối đa 100)" value={ids} onChange={e => { setIds(e.target.value); reset(); }}/><Select label="Thao tác" value={action} onChange={v => { setAction(v); reset(); }}><option value="ACTIVE">Bật/tắt</option><option value="PRICE_AMOUNT">Tăng/giảm theo số tiền</option><option value="PRICE_PERCENT">Tăng/giảm theo %</option></Select>{action === 'ACTIVE' ? <Select label="Trạng thái mới" value={active} onChange={v => { setActive(v); reset(); }}><option value="true">Hoạt động</option><option value="false">Ngừng hoạt động</option></Select> : <Input label="Giá trị thay đổi (âm = giảm)" type="number" step="any" value={value} onChange={e => { setValue(e.target.value); reset(); }}/>}</div>}<Button className="my-4" isLoading={busy} disabled={csv && !file} onClick={() => void run(false)}>Kiểm tra / xem trước</Button>{error && <ErrorNotice message={error}/>}{done && <p role="status">Đã áp dụng thành công.</p>}{preview && <><DataTable page={{ content: preview.rows, totalElements: preview.rows.length, number: 0, size: preview.rows.length || 1 }} loading={false} error="" retry={() => void run(false)} onPage={() => { }} columns={csv ? [{ key: 'id', label: 'Dòng dữ liệu', render: r => r.id }, { key: 'sku', label: 'SKU', render: r => r.sku || '—' }, { key: 'result', label: 'Kết quả', render: r => r.message }] : [{ key: 'name', label: 'Sản phẩm', render: r => r.name }, { key: 'old', label: 'Trước', render: r => <><PriceText value={r.before}/> · {r.activeBefore ? 'Bật' : 'Tắt'}</> }, { key: 'new', label: 'Sau', render: r => <><PriceText value={r.after}/> · {r.activeAfter ? 'Bật' : 'Tắt'}</> }]}/><Button className="my-4" disabled={busy || preview.valid === false} onClick={() => setConfirm(true)}>Áp dụng kết quả</Button></>}{confirm && <ConfirmDialog title="Xác nhận áp dụng" busy={busy} onClose={() => setConfirm(false)}><p className="mb-4">Áp dụng toàn bộ các dòng đã xem trước. Nếu dữ liệu đã thay đổi, bạn cần xem trước lại.</p><Button isLoading={busy} onClick={() => void run(true)}>Xác nhận áp dụng</Button>{error && <ErrorNotice message={error}/>}</ConfirmDialog>}</>;
}
