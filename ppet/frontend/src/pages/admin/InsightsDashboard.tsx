import { isAxiosError } from 'axios';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../api/client';
import { useAdminResource } from '../../hooks/useAdminResource';
import { getBusinessDate } from '../../utils/businessDate';
import { getErrorMessage } from '../../utils/getErrorMessage';
import { PageHeader, StatCard, PriceText, ErrorNotice, Select, ConfirmDialog } from '../../components/admin/AdminUI';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { Row } from './OperationsUI';
const today = getBusinessDate();
const Skeleton = () => <div role="status" aria-label="Đang tải dữ liệu" className="animate-pulse h-32 rounded-xl bg-gray-200 my-4"/>;
export default function InsightsDashboard() { const [period, setPeriod] = useState('month'); const [from, setFrom] = useState(today.slice(0, 8) + '01'); const [to, setTo] = useState(today); const [query, setQuery] = useState(`from=${from}&to=${to}`); const select = (value: string) => { setPeriod(value); if (value === 'custom')
    return; const date = new Date(today + 'T12:00:00'); if (value === 'week')
    date.setDate(date.getDate() - (date.getDay() + 6) % 7); const first = value === 'month' ? today.slice(0, 8) + '01' : value === 'today' ? today : [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-'); setFrom(first); setTo(today); setQuery(`from=${first}&to=${today}`); }; return <><PageHeader title="Tổng quan và báo cáo" description="Doanh thu chỉ tính đơn đã thanh toán, theo ngày thu tiền tại cửa hàng."/><form className="grid sm:grid-cols-2 xl:grid-cols-4 gap-3 mb-5" onSubmit={e => { e.preventDefault(); setQuery(new URLSearchParams({ from, to }).toString()); }}><Select label="Kỳ báo cáo" value={period} onChange={select}><option value="today">Hôm nay</option><option value="week">Tuần này</option><option value="month">Tháng này</option><option value="custom">Tùy chọn</option></Select><Input label="Từ ngày" type="date" required value={from} onChange={e => { setFrom(e.target.value); setPeriod('custom'); }}/><Input label="Đến ngày" type="date" required value={to} onChange={e => { setTo(e.target.value); setPeriod('custom'); }}/><Button type="submit">Xem báo cáo</Button></form><Summary query={query}/><Alerts /><Revenue query={query}/><Rankings query={query}/><Exports query={query}/></>; }
function Summary({ query }: {
    query: string;
}) { const r = useAdminResource<Row>('/admin/dashboard/summary?period=custom&' + query); const labels: Record<string, string> = { revenue: 'Doanh thu đã thu', orders: 'Đơn tạo trong kỳ', pending: 'Đơn trong kỳ còn chờ xử lý', customers: 'Khách đăng ký trong kỳ', appointmentsToday: 'Lịch hẹn hôm nay (so hôm qua)' }; return r.error ? <ErrorNotice message={r.error} retry={r.reload}/> : r.loading || !r.data ? <Skeleton /> : <section aria-label="Chỉ số tổng quan"><p className="mb-3 text-sm">{r.data.definition}</p><p className="mb-3 text-sm">So sánh {r.data.previousFrom} → {r.data.previousTo}; múi giờ {r.data.zone}. Mốc trước bằng 0 và kỳ này có phát sinh: không tính phần trăm.</p><div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{Object.entries(labels).map(([key, label]) => { const m = r.data!.metrics[key]; return <div key={key}><StatCard label={label} value={key === 'revenue' ? <PriceText value={m.current}/> : m.current}/><p className="text-sm p-2">Chênh lệch: {key === 'revenue' ? <PriceText value={m.change}/> : m.change} · {m.percent == null ? 'Chưa có nền so sánh' : m.percent + '%'}</p></div>; })}</div></section>; }
function Alerts() { const r = useAdminResource<Row[]>('/admin/dashboard/alerts'); return <section className="my-7"><h2 className="font-bold text-xl mb-3">Cần xử lý</h2>{r.error ? <ErrorNotice message={r.error} retry={r.reload}/> : r.loading ? <Skeleton /> : <div className="grid gap-3 sm:grid-cols-2">{r.data?.map(a => <Link key={a.key} to={a.href} className="block p-4 border rounded-xl bg-white underline">{a.label}: {a.count}</Link>)}</div>}</section>; }
function Revenue({ query }: {
    query: string;
}) { const [granularity, setGranularity] = useState('day'); const [table, setTable] = useState(false); const r = useAdminResource<{
    date: string;
    revenue: number;
    orders: number;
}[]>(`/admin/reports/revenue?granularity=${granularity}&${query}`); const max = Math.max(1, ...(r.data || []).map(x => x.revenue)); return <section className="my-7"><h2 className="font-bold text-xl mb-3">Doanh thu theo thời gian</h2><Select label="Chi tiết biểu đồ" value={granularity} onChange={setGranularity}><option value="day">Theo ngày</option><option value="month">Theo tháng</option></Select>{r.error ? <ErrorNotice message={r.error} retry={r.reload}/> : r.loading ? <Skeleton /> : <><svg viewBox="0 0 800 230" className="w-full my-4 rounded-xl border bg-white" role="img" aria-label="Biểu đồ doanh thu, bảng số liệu đầy đủ ở bên dưới"><title>Doanh thu theo thời gian</title><line x1="10" y1="210" x2="790" y2="210" stroke="currentColor"/>{r.data?.map((x, i) => { const width = 780 / (r.data?.length || 1); return <rect key={x.date} x={10 + i * width} y={210 - x.revenue / max * 190} width={Math.max(.5, width * .75)} height={x.revenue / max * 190} fill="#059669"><title>{x.date}: {x.revenue.toLocaleString('vi-VN')} đ · {x.orders} đơn</title></rect>; })}</svg><p>Trục ngang: {r.data?.[0]?.date} → {r.data?.[r.data.length - 1]?.date}; mức cao nhất: <PriceText value={max === 1 && r.data?.every(x => !x.revenue) ? 0 : max}/>.</p><Button variant="outline" className="my-3" aria-expanded={table} onClick={() => setTable(!table)}>{table ? 'Ẩn bảng' : 'Xem bảng số liệu'}</Button>{table && <div className="overflow-auto"><table className="w-full bg-white text-left"><caption className="sr-only">Doanh thu và số đơn theo ngày hoặc tháng</caption><thead><tr><th scope="col">Mốc</th><th scope="col">Doanh thu</th><th scope="col">Số đơn</th></tr></thead><tbody>{r.data?.map(x => <tr key={x.date}><th scope="row" className="p-2">{x.date}</th><td><PriceText value={x.revenue}/></td><td>{x.orders}</td></tr>)}</tbody></table></div>}</>}</section>; }
function Rankings({ query }: {
    query: string;
}) { const r = useAdminResource<Row>('/admin/reports/rankings?' + query); return <section className="my-7"><h2 className="font-bold text-xl mb-3">Sản phẩm và dịch vụ nổi bật</h2>{r.error ? <ErrorNotice message={r.error} retry={r.reload}/> : r.loading || !r.data ? <Skeleton /> : <><p>Đơn hủy: {r.data.cancelled}/{r.data.totalOrders} ({Number(r.data.cancellationPercent).toFixed(1)}%) · Hết hạn: {r.data.expired} ({Number(r.data.expiryPercent).toFixed(1)}%, nằm trong số đơn hủy).</p><div className="grid gap-4 sm:grid-cols-2 mt-3"><div className="border rounded-xl p-4 bg-white"><h3 className="font-bold">Sản phẩm bán chạy</h3><p className="text-sm">Theo số lượng từ đơn đủ điều kiện doanh thu; không gồm thú cưng.</p>{!r.data.products.length ? <p>Chưa có dữ liệu.</p> : <ol>{r.data.products.map((x: Row) => <li className="py-2" key={x.id}>{x.name}: {x.quantity} sản phẩm</li>)}</ol>}</div><div className="border rounded-xl p-4 bg-white"><h3 className="font-bold">Dịch vụ được đặt</h3><p className="text-sm">Theo ngày hẹn, loại lịch đã hủy.</p>{!r.data.services.length ? <p>Chưa có dữ liệu.</p> : <ol>{r.data.services.map((x: Row) => <li className="py-2" key={x.id}>{x.name}: {x.count} lịch</li>)}</ol>}</div></div></>}</section>; }
function Exports({ query }: {
    query: string;
}) { const [kind, setKind] = useState('revenue'); const [keyword, setKeyword] = useState(''); const [confirm, setConfirm] = useState(false); const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const download = async () => { setBusy(true); setError(''); try {
    const response = await api.get('/admin/reports/export?' + query, { params: { kind, keyword }, responseType: 'blob' });
    const url = URL.createObjectURL(response.data);
    const a = document.createElement('a');
    a.href = url;
    a.download = `petcare-${kind}-${getBusinessDate()}.csv`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    setConfirm(false);
}
catch (e) {
    if (isAxiosError(e) && e.response?.data instanceof Blob) {
        try {
            e.response.data = JSON.parse(await e.response.data.text());
        }
        catch { /* Keep the HTTP correlation header if body decoding fails. */ }
    }
    setError(getErrorMessage(e));
}
finally {
    setBusy(false);
} }; return <section className="border-t pt-5 my-7"><h2 className="font-bold text-xl mb-3">Xuất CSV</h2><p>Tối đa 5000 dòng; UTF-8 cho Excel, có nhật ký truy cập. Khách hàng chỉ gồm ID, họ tên, trạng thái và ngày tạo.</p><div className="grid gap-3 my-4 sm:grid-cols-2"><Select label="Loại xuất" value={kind} onChange={setKind}><option value="revenue">Doanh thu theo ngày của kỳ đã chọn</option><option value="inventory">Tồn kho hiện tại</option><option value="customers">Khách hàng</option></Select>{kind !== 'revenue' && <Input label="Lọc tên trước khi xuất" value={keyword} onChange={e => setKeyword(e.target.value)}/>}</div><Button onClick={() => setConfirm(true)}>Tải CSV</Button>{confirm && <ConfirmDialog title="Xác nhận xuất dữ liệu" busy={busy} onClose={() => setConfirm(false)}><p className="mb-4">Tải dữ liệu theo loại và bộ lọc đã chọn. Thao tác được ghi audit.</p><Button isLoading={busy} onClick={() => void download()}>Xác nhận tải</Button>{error && <ErrorNotice message={error}/>}</ConfirmDialog>}</section>; }
