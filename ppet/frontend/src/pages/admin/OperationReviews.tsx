import { useState } from 'react';
import { useAdminResource } from '../../hooks/useAdminResource';
import { PageHeader, DataTable } from '../../components/admin/AdminUI';
import { Button } from '../../components/common/Button';
import { Row, useQueue, Filters, options, labels, ActionDialog, Field } from './OperationsUI';
export function ReviewsQueue() {
  const r = useQueue('/admin/reviews', 'status=PENDING');
  const count = useAdminResource<{
    pending: number;
  }>('/admin/reviews/count');
  const [selected, setSelected] = useState<Row>();
  const fields: Field[] = [{
    key: 'action',
    label: 'Thao tác',
    options: options(['APPROVED', 'REJECTED', 'HIDDEN'], false)
  }, {
    key: 'reason',
    label: 'Lý do'
  }];
  return <><PageHeader title="Duyệt đánh giá" description={count.data ? `${count.data.pending} đánh giá chờ duyệt` : undefined} /><Filters apply={r.filter} fields={[{
      key: 'status',
      label: 'Trạng thái',
      value: 'PENDING',
      options: {
        PENDING: 'Chờ duyệt',
        APPROVED: 'Đã duyệt',
        REJECTED: 'Từ chối',
        HIDDEN: 'Đã ẩn',
        ALL: 'Tất cả'
      }
    }, {
      key: 'type',
      label: 'Đối tượng',
      options: {
        '': 'Tất cả',
        PRODUCT: 'Sản phẩm',
        PET: 'Thú cưng',
        SERVICE: 'Dịch vụ'
      }
    }, {
      key: 'target',
      label: 'ID đối tượng',
      type: 'number'
    }]} /><DataTable {...r} retry={r.reload} onPage={r.setPage} onSort={r.setSort} columns={[{
      key: 'target',
      label: 'Đối tượng',
      render: r => `${r.type} #${r.targetId}`
    }, {
      key: 'rating',
      label: 'Số sao',
      sort: 'rating',
      render: r => `${r.rating}/5`
    }, {
      key: 'comment',
      label: 'Nội dung',
      render: r => <p className="whitespace-pre-wrap break-words">{r.comment}</p>
    }, {
      key: 'status',
      label: 'Kiểm duyệt',
      render: r => <>{labels[r.status || 'PENDING']}<p>{r.reason}</p></>
    }, {
      key: 'action',
      label: 'Thao tác',
      render: r => <Button variant="outline" onClick={() => setSelected(r)}>Duyệt / từ chối / ẩn</Button>
    }]} />{selected && <ActionDialog title={`Kiểm duyệt đánh giá #${selected.id}`} url={`/admin/reviews/${selected.id}/moderate`} fields={fields} onClose={() => setSelected(undefined)} onDone={() => {
      r.reload();
      count.reload();
    }} />}</>;
}
