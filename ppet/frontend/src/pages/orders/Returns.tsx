import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../api/client';
import { Order } from '../../types';
import { useResource } from '../../hooks/useResource';
import { getErrorMessage } from '../../utils/getErrorMessage';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select, ConfirmDialog, ErrorNotice, PriceText } from '../../components/admin/AdminUI';
const statuses: Record<string, string> = { REQUESTED: 'Đã gửi yêu cầu', APPROVED: 'Đã duyệt, chờ nhận hàng', REJECTED: 'Đã từ chối', RECEIVED: 'Đã nhận hàng, chờ hoàn tiền', REFUNDED: 'Đã hoàn tiền' };
export function RequestReturn({ order }: {
    order: Order;
}) {
    const [open, setOpen] = useState(false);
    const [confirm, setConfirm] = useState(false);
    const [item, setItem] = useState('');
    const [quantity, setQuantity] = useState('1');
    const [reason, setReason] = useState('');
    const [key, setKey] = useState(() => crypto.randomUUID());
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const [done, setDone] = useState(false);
    const reset = () => { setKey(crypto.randomUUID()); setDone(false); };
    const send = async () => { setBusy(true); setError(''); try {
        await api.post('/returns', { orderId: order.id, itemId: Number(item), quantity: Number(quantity), reason, requestKey: key });
        setDone(true);
        setOpen(false);
        setConfirm(false);
        reset();
        setDone(true);
    }
    catch (e) {
        setError(getErrorMessage(e));
    }
    finally {
        setBusy(false);
    } };
    const products = order.items.filter(i => i.productId && i.itemId);
    return <section className="border-t pt-4"><Link className="underline" to="/returns">Theo dõi yêu cầu trả hàng</Link>{!!order.returnedAmount && <p>Đã hoàn cho sản phẩm: <PriceText value={order.returnedAmount}/></p>}{products.length > 0 && order.status === 'DELIVERED' && order.paymentStatus === 'COMPLETED' && <><p className="my-3">{order.returnUntil ? 'Hạn gửi yêu cầu: ' + order.returnUntil.replace('T', ' ') : 'Đơn cũ chưa có mốc giao hàng để xác định hạn trả; vui lòng liên hệ cửa hàng.'}</p>{order.returnUntil && <Button variant="outline" onClick={() => setOpen(!open)}>Yêu cầu trả sản phẩm</Button>}{open && <form className="space-y-3 mt-4" onSubmit={e => { e.preventDefault(); setConfirm(true); }}><Select required label="Sản phẩm cần trả" value={item} onChange={v => { setItem(v); reset(); }}><option value="">Chọn sản phẩm</option>{products.map(i => <option key={i.itemId} value={i.itemId}>{i.productName} · đã mua {i.quantity}</option>)}</Select><Input required label="Số lượng trả" type="number" min={1} max={products.find(i => String(i.itemId) === item)?.quantity} value={quantity} onChange={e => { setQuantity(e.target.value); reset(); }}/><Input required label="Lý do trả" maxLength={500} value={reason} onChange={e => { setReason(e.target.value); reset(); }}/><Button type="submit">Xem lại yêu cầu</Button></form>}</>}{done && <p role="status" className="my-3">Đã ghi nhận yêu cầu. Theo dõi tiến độ ở danh sách trả hàng.</p>}{confirm && <ConfirmDialog title="Xác nhận yêu cầu trả hàng" busy={busy} onClose={() => setConfirm(false)}><p className="mb-3">{quantity} sản phẩm · {reason}. Cửa hàng sẽ kiểm tra và thông báo kết quả trước khi nhận hàng.</p><Button isLoading={busy} onClick={() => void send()}>Gửi yêu cầu</Button>{error && <ErrorNotice message={error}/>}</ConfirmDialog>}</section>;
}
type ReturnRow = {
    id: number;
    orderId: number;
    itemName: string;
    quantity: number;
    status: string;
    reason: string;
    decisionReason?: string;
    refundAmount?: number;
};
export function MyReturns() { const [page, setPage] = useState(0); const r = useResource<{
    content: ReturnRow[];
    totalElements: number;
}>('/returns?page=' + page + '&size=20'); return <section className="max-w-4xl mx-auto px-4 py-8"><h1 className="text-3xl font-bold mb-5">Yêu cầu trả hàng của tôi</h1><Link to="/orders" className="underline">Xem đơn hàng để tạo yêu cầu</Link><Button variant="outline" className="ml-3" onClick={r.retry}>Làm mới</Button>{r.error ? <ErrorNotice message={r.error} retry={r.retry}/> : !r.data ? <p>Đang tải…</p> : <><div className="space-y-4 my-5">{!r.data.content.length && <p>Chưa có yêu cầu trả hàng.</p>}{r.data.content.map(x => <article className="border rounded-xl bg-white p-4 break-words" key={x.id}><h2 className="font-bold">#{x.id} · {x.itemName} × {x.quantity}</h2><p>{statuses[x.status]}</p><p>Lý do: {x.reason}</p>{x.decisionReason && <p>Phản hồi cửa hàng: {x.decisionReason}</p>}{x.refundAmount != null && <p>Đã hoàn: <PriceText value={x.refundAmount}/></p>}<Link to={'/orders/' + x.orderId} className="underline">Đơn #{x.orderId}</Link></article>)}</div><div className="flex gap-3"><Button disabled={!page} onClick={() => setPage(page - 1)}>Trước</Button><Button disabled={(page + 1) * 20 >= r.data.totalElements} onClick={() => setPage(page + 1)}>Sau</Button></div></>}</section>; }
