import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAdminResource } from '../../hooks/useAdminResource';
import { PageHeader, DataTable, PriceText, ErrorNotice, StatusBadge } from '../../components/admin/AdminUI';
import { Button } from '../../components/common/Button';
import { Row, useQueue, Filters, options, labels, ActionDialog, Field, timeText } from './OperationsUI';
export function OrdersQueue() {
  const r = useQueue('/admin/orders');
  return <><PageHeader title="Đơn hàng" description="Quản lý đơn, thu tiền mặt và hoàn tiền thủ công." /><Filters apply={r.filter} fields={[{
      key: 'keyword',
      label: 'Mã đơn hoặc tên khách'
    }, {
      key: 'status',
      label: 'Trạng thái đơn',
      options: options(['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPING', 'DELIVERED', 'CANCELLED'])
    }, {
      key: 'payment',
      label: 'Thanh toán',
      options: options(['PENDING', 'COMPLETED', 'REFUNDED', 'FAILED'])
    }, {
      key: 'refunds',
      label: 'Hàng đợi hoàn tiền',
      options: {
        '': 'Tất cả',
        'true': 'Chờ hoàn tiền'
      }
    }, {
      key: 'from',
      label: 'Từ ngày',
      type: 'date'
    }, {
      key: 'to',
      label: 'Đến ngày',
      type: 'date'
    }]} /><DataTable {...r} retry={r.reload} onPage={r.setPage} onSort={r.setSort} columns={[{
      key: 'number',
      label: 'Mã đơn',
      render: o => <Link className="underline" to={`/admin/orders/${o.id}`}>{o.orderNumber}</Link>
    }, {
      key: 'customer',
      label: 'Khách hàng',
      render: o => o.customer
    }, {
      key: 'status',
      label: 'Trạng thái',
      sort: 'status',
      render: o => <StatusBadge>{labels[o.status]}</StatusBadge>
    }, {
      key: 'payment',
      label: 'Thanh toán',
      render: o => labels[o.paymentStatus]
    }, {
      key: 'total',
      label: 'Tổng tiền',
      sort: 'total',
      render: o => <PriceText value={o.total} />
    }]} /></>;
}
export function OrderDetail() {
  const {
    id
  } = useParams();
  const r = useAdminResource<Row>(`/admin/orders/${id}`);
  const notes = useQueue(`/admin/orders/${id}/notes`);
  const payments = useQueue(`/admin/orders/${id}/payments`);
  const [dialog, setDialog] = useState<{
    title: string;
    path: string;
    body?: Record<string, unknown>;
    fields?: Field[];
    warning?: string;
  }>();
  if (r.error) return <ErrorNotice message={r.error} retry={r.reload} />;
  if (!r.data) return <p role="status">Đang tải đơn…</p>;
  const d = r.data,
    o = d.order;
  const next: Record<string, [string, string]> = {
    PENDING: ['confirm', 'Xác nhận đơn'],
    CONFIRMED: ['process', 'Chuẩn bị hàng'],
    PROCESSING: ['ship', 'Giao hàng'],
    SHIPPING: ['deliver', 'Xác nhận đã giao và thu tiền']
  };
  const reason: Field = {
    key: 'reason',
    label: 'Lý do'
  };
  const refresh = () => {
    r.reload();
    notes.reload();
    payments.reload();
  };
  return <><Link to="/admin/orders" className="underline">← Đơn hàng</Link><PageHeader title={o.orderNumber} description={`${labels[o.status]} · ${labels[o.paymentStatus]}`} actions={<Link className="underline" to={`/admin/orders/${id}/print`}>Phiếu giao / in</Link>} /><div className="mb-6 flex flex-wrap gap-2">{next[o.status] && <Button onClick={() => setDialog({
        title: next[o.status][1],
        path: 'transition',
        body: {
          action: next[o.status][0]
        }
      })}>{next[o.status][1]}</Button>}{['PENDING', 'CONFIRMED', 'PROCESSING'].includes(o.status) && <Button variant="danger" onClick={() => setDialog({
        title: 'Hủy toàn bộ đơn',
        path: 'transition',
        body: {
          action: 'cancel'
        },
        fields: [reason],
        warning: 'Hủy toàn bộ đơn, nhả thú cưng, hoàn tồn kho, voucher và điểm đã dùng.'
      })}>Hủy đơn</Button>}{o.paymentStatus === 'COMPLETED' && ['DELIVERED', 'CANCELLED'].includes(o.status) && <Button variant="outline" onClick={() => setDialog(d.refundRequestedAt || o.status === 'CANCELLED' ? {
        title: 'Xác nhận đã hoàn tiền',
        path: 'mark-refunded',
        fields: [{
          key: 'reference',
          label: 'Mã biên nhận hoàn tiền'
        }, reason],
        warning: 'Chỉ xác nhận sau khi đã trả tiền thực tế. Thao tác không tự hoàn kho hay chuyển chủ thú cưng.'
      } : {
        title: 'Lập yêu cầu hoàn tiền',
        path: 'request-refund',
        fields: [{
          key: 'body',
          label: 'Lý do hoàn tiền'
        }]
      })}>{d.refundRequestedAt || o.status === 'CANCELLED' ? 'Đánh dấu đã hoàn' : 'Yêu cầu hoàn tiền'}</Button>}</div>
 <OrderSummary order={o} total={d.total} tax={d.taxAmount} /><p>Đã thu: {d.paidAt ? timeText(d.paidAt) : 'Chưa có mốc thu tiền'} · Đã hoàn: {d.refundedAt ? timeText(d.refundedAt) : 'Chưa hoàn'}</p>{d.refundReference && <p className="break-all">Biên nhận: {d.refundReference}</p>}
 <h2 className="mt-8 mb-3 text-xl font-bold">Ghi chú nội bộ</h2><Button variant="outline" onClick={() => setDialog({
      title: 'Thêm ghi chú nội bộ',
      path: 'notes',
      fields: [{
        key: 'body',
        label: 'Nội dung'
      }]
    })}>Thêm ghi chú</Button><DataTable {...notes} retry={notes.reload} onPage={notes.setPage} columns={[{
      key: 'body',
      label: 'Ghi chú',
      render: n => n.body
    }, {
      key: 'actor',
      label: 'Nhân viên',
      render: n => `#${n.actorId}`
    }, {
      key: 'date',
      label: 'Thời gian',
      render: n => timeText(n.createdAt)
    }]} />
 <h2 className="mt-8 mb-3 text-xl font-bold">Lịch sử thu / hoàn tiền</h2><p className="mb-3">Đối chiếu nội bộ tiền mặt theo dữ liệu hệ thống. Đơn cũ có thể chỉ có mốc thu tiền, chưa có bản ghi lịch sử riêng.</p><DataTable {...payments} retry={payments.reload} onPage={payments.setPage} columns={[{
      key: 'type',
      label: 'Giao dịch',
      render: e => labels[e.type]
    }, {
      key: 'amount',
      label: 'Số tiền',
      render: e => <PriceText value={e.amount} />
    }, {
      key: 'reference',
      label: 'Biên nhận',
      render: e => e.reference || 'Tiền mặt'
    }, {
      key: 'time',
      label: 'Thời gian',
      render: e => timeText(e.occurredAt)
    }]} />
 {dialog && <ActionDialog {...dialog} url={`/admin/orders/${id}/${dialog.path}`} onClose={() => setDialog(undefined)} onDone={refresh} />}</>;
}
function OrderSummary({
  order: o,
  total,
  tax
}: {
  order: any;
  total: number;
  tax?: number;
}) {
  return <section className="rounded-2xl border bg-white p-4"><h2 className="text-xl font-bold mb-3">Chi tiết đơn</h2><ul className="space-y-3">{o.items.map((item: any, i: number) => <li key={i} className="flex flex-wrap justify-between gap-2 border-b py-2"><span>{item.packageName || item.petName || item.productName || 'Mặt hàng'} × {item.quantity}</span><PriceText value={item.price * item.quantity} /></li>)}</ul><p className="mt-3">Phí giao: <PriceText value={o.shippingFee || 0} /></p><p>Giảm giá: <PriceText value={(o.discountAmount || 0) + (o.pointsDiscount || 0)} /></p><p>Thuế: <PriceText value={tax || 0} /></p><p className="mt-3 font-bold">Tổng: <PriceText value={total} /></p><p className="mt-4 break-words">Người nhận: {o.shipping?.recipientName || 'Nhận tại cửa hàng'} · {o.shipping?.phone || ''}</p><p className="break-words">{o.shippingAddress}</p><p className="break-words">{o.shipping?.note}</p><p className="break-words">{o.notes}</p><dl className="mt-4 grid gap-2 sm:grid-cols-2">{[['createdAt', 'Tạo đơn'], ['confirmedAt', 'Xác nhận'], ['processingAt', 'Chuẩn bị'], ['shippedAt', 'Giao hàng'], ['deliveredAt', 'Hoàn tất'], ['cancelledAt', 'Hủy đơn']].map(([key, label]) => <div key={key}><dt>{label}</dt><dd>{timeText(o[key])}</dd></div>)}</dl></section>;
}
export function OrderPrint() {
  const {
    id
  } = useParams();
  const r = useAdminResource<Row>(`/admin/orders/${id}/print`);
  return <>{r.error ? <ErrorNotice message={r.error} retry={r.reload} /> : r.data ? <><div className="print:hidden mb-4"><Button onClick={() => window.print()}>In / Lưu PDF</Button><Link className="ml-4 underline" to={`/admin/orders/${id}`}>Về đơn hàng</Link></div><div id="admin-invoice"><h1 className="text-2xl font-bold mb-4">PetCare Hub · Phiếu đơn {r.data.order.orderNumber}</h1><OrderSummary order={r.data.order} total={r.data.total} tax={r.data.taxAmount} /></div></> : <p>Đang tải phiếu…</p>}</>;
}
