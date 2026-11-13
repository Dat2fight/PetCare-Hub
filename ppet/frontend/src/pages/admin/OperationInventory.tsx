import { useState } from 'react';
import { Link } from 'react-router-dom';
import { PageHeader, DataTable } from '../../components/admin/AdminUI';
import { Button } from '../../components/common/Button';
import { Row, useQueue, Filters, options, labels, ActionDialog, timeText } from './OperationsUI';
export function InventoryQueue() {
  const r = useQueue('/admin/inventory');
  const [item, setItem] = useState<Row>();
  return <><PageHeader title="Kho sản phẩm" actions={<Link className="underline" to="/admin/inventory/history">Lịch sử giao dịch</Link>} /><Filters apply={r.filter} fields={[{
      key: 'keyword',
      label: 'Tên hoặc mã sản phẩm'
    }, {
      key: 'low',
      label: 'Cảnh báo',
      options: {
        '': 'Tất cả',
        'true': 'Tồn kho thấp'
      }
    }]} /><DataTable {...r} retry={r.reload} onPage={r.setPage} onSort={r.setSort} columns={[{
      key: 'name',
      label: 'Sản phẩm',
      sort: 'name',
      render: i => <>{i.name}<p className="text-sm">{i.sku}</p></>
    }, {
      key: 'quantity',
      label: 'Tồn kho',
      sort: 'quantity',
      render: i => i.quantity
    }, {
      key: 'threshold',
      label: 'Ngưỡng cảnh báo',
      render: i => i.reorderPoint ?? 10
    }, {
      key: 'action',
      label: 'Thao tác',
      render: i => <Button variant="outline" onClick={() => setItem(i)}>Nhập / xuất / kiểm kê</Button>
    }]} />{item && <ActionDialog title={`Cập nhật kho: ${item.name}`} url="/admin/inventory/movements" body={{
      productId: item.productId
    }} fields={[{
      key: 'type',
      label: 'Loại giao dịch',
      options: options(['STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT'], false)
    }, {
      key: 'quantity',
      label: 'Số lượng',
      type: 'number'
    }, {
      key: 'reason',
      label: 'Lý do'
    }]} warning="Nhập/xuất là số lượng tăng/giảm; kiểm kê là số tồn thực tế mới. Không được xuất vượt tồn." onClose={() => setItem(undefined)} onDone={r.reload} />}</>;
}
export function InventoryHistory() {
  const r = useQueue('/admin/inventory/transactions');
  return <><PageHeader title="Lịch sử kho" /><Filters apply={r.filter} fields={[{
      key: 'productId',
      label: 'ID sản phẩm',
      type: 'number'
    }, {
      key: 'type',
      label: 'Loại',
      options: options(['STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT'])
    }, {
      key: 'actor',
      label: 'ID nhân viên',
      type: 'number'
    }, {
      key: 'from',
      label: 'Từ ngày',
      type: 'date'
    }, {
      key: 'to',
      label: 'Đến ngày',
      type: 'date'
    }]} /><DataTable {...r} retry={r.reload} onPage={r.setPage} onSort={r.setSort} columns={[{
      key: 'product',
      label: 'Sản phẩm',
      render: i => `#${i.productId}`
    }, {
      key: 'type',
      label: 'Loại / số lượng',
      render: i => `${labels[i.type]}: ${i.quantity}`
    }, {
      key: 'reason',
      label: 'Lý do',
      render: i => i.reason
    }, {
      key: 'source',
      label: 'Nguồn / tham chiếu',
      render: i => <>{i.source === 'SYSTEM' ? 'Hệ thống' : `Nhân viên #${i.actorId}`}{i.referenceId && <p>{i.referenceType}/{i.referenceId}</p>}</>
    }, {
      key: 'time',
      label: 'Thời gian',
      sort: 'createdAt',
      render: i => timeText(i.createdAt)
    }]} /></>;
}
