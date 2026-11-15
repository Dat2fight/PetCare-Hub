# API giai đoạn 5 — nội dung, cấu hình, xuất dữ liệu, trả hàng

Tiền tố `/api/v1`; JSON UTF-8. Quyền theo [ma trận](admin-phase-5-report.md#ma-trận-quyền). Page gồm `content,totalElements,totalPages,number,size`, size tối đa 100; sort chỉ `id`, riêng admin content thêm `title`. Lỗi dùng envelope/correlationId hiện hữu: 400 dữ liệu sai, 401 chưa đăng nhập, 403 thiếu quyền, 404 không thuộc sở hữu/không công khai, 409 xung đột trạng thái.

## CMS

| Method / endpoint | Input / output |
|---|---|
| GET /admin/content | `kind`, `keyword`, page; metadata bản nháp và publishedAt |
| GET /admin/content/{id} | `{id,slug,kind,title,body,configuration,published}` |
| POST /admin/content; PUT /admin/content/{id} | `{slug,kind,title,body,configuration}` → `{id}`; lưu nháp |
| POST /admin/content/{id}/publish; /unpublish | Không body; xuất bản phần đã lưu / gỡ bản công khai |
| GET /admin/content/{id}/revisions | Page `{id,snapshot,actorId,createdAt}`; snapshot là chuỗi JSON |
| POST /admin/content/{id}/restore | `{revisionId}`; phải thuộc nội dung này, chỉ phục hồi nháp |
| DELETE /admin/content/{id} | `{reason}`; chỉ khi chưa xuất bản |
| GET /content?kind=ARTICLE hoặc FAQ | Page `{id,slug,snapshot,publishedAt}`, chỉ bản công khai; snapshot là chuỗi JSON |
| GET /content/{slug} | `{slug,kind,content:{title,body,configuration},publishedAt}`; chưa xuất bản → 404 |
| GET /content/featured | `{products?:[{id,name,imageUrl,price}],pets?:[...]}`; chỉ hàng đang bán |

PAGE chỉ dùng `home`, `contact`, `terms`, `privacy`, `shipping-policy`, `return-policy`, `cancellation-policy`; ARTICLE/FAQ không dùng các mã đó. Slug lowercase, 1–100 ký tự, mẫu `[a-z0-9]+(?:-[a-z0-9]+)*`, unique và bất biến cùng kind sau khi tạo. title 1–200, body 1–20.000 ký tự. Từ chối trường lạ. configuration mặc định `{}`; chỉ home cho `{productIds:[1,2],petIds:[3]}`, tối đa 12 ID dương duy nhất mỗi loại. Kiểm đang bán khi lưu/publish, đọc public lọc lại hàng tắt/pet đã bán. Không có key → frontend dùng catalog mặc định; mảng rỗng → ẩn nhóm đó.

Nháp và snapshot công khai tách biệt; sửa/restore không lộ nháp. Giữ 20 revision save/restore gần nhất. SafeContent render text/heading/list bằng React, không HTML thô. Public page đặt document title và khôi phục khi rời trang. Chính sách chưa có dùng thông báo liên hệ cửa hàng, không tự sinh điều khoản pháp lý.

## Cấu hình

GET `/admin/settings` trả map khóa → số. PUT cùng URL nhận một hoặc nhiều khóa dưới đây; từ chối map rỗng, trường lạ, null, số âm; tiền tối đa hai chữ số thập phân.

| Khóa | Giới hạn | Dự phòng khi DB chưa có |
|---|---|---|
| shippingFee | 0–1.000.000.000 | APP_CHECKOUT_SHIPPING_FEE, 30000 |
| freeShippingFrom | 0–1.000.000.000 | APP_CHECKOUT_FREE_SHIPPING_FROM, 500000 |
| holdHours | Số nguyên 1–168 | OrderExpiryConfig hiện có |
| cancellationHours | Số nguyên 0–168 | BookingProperties hiện có |
| returnDays | Số nguyên 1–90 | APP_RETURNS_WINDOW_DAYS, 7 |

Cache read-only 5 giây, chỉ điền sau commit; write transaction đọc DB. Thay đổi thành công xóa cache tiến trình sau commit. Quote/đơn mới dùng cấu hình mới; đơn đã lưu shippingFee/expiresAt/returnWindowDays không bị tính lại. Hủy lịch đọc cấu hình hiện hành. Ngưỡng loyalty vẫn dùng `loyalty_rules` qua SettingsService, sửa tại API loyalty giai đoạn 3; không nhân bản cấu hình. Không cho sửa secret/JWT/DB.

## Xuất dữ liệu bất đồng bộ

- POST `/admin/data-exports`: `{kind:"revenue"|"inventory"|"customers",from?,to?,keyword?,requestKey:UUID}` → 202 `{id}`. Revenue cần from/to, kỳ bao gồm hai ngày và tối đa 366 ngày; kho/khách dùng keyword tên ≤100 ký tự. Replay cùng actor/key/payload trả id cũ; đổi payload/key cũ → 409. Một yêu cầu đang QUEUED và tối thiểu một phút giữa yêu cầu của mỗi admin.
- GET cùng URL: Page `{id,kind,status,rowCount,createdAt,expiresAt,consumedAt,errorCode}` của người tạo, không trả payload.
- POST `/{id}/download`: không body, attachment CSV/BOM UTF-8, `Cache-Control: no-store`. Chỉ người tạo, READY và chưa hết hạn; admin khác 404, đã dùng/chưa sẵn sàng/hết hạn 409. Payload bị xóa, CONSUMED trong transaction trước streaming; mạng đứt cần tạo yêu cầu mới.
- Worker một job/tick mỗi 5 giây, trễ khởi động 30 giây; khóa job chống xử lý trùng. Trần 20.000 dòng/10 triệu ký tự; vượt trần FAILED/EXPORT_LIMIT_EXCEEDED, input lỗi EXPORT_INPUT_INVALID, lỗi thực thi EXPORT_FAILED. Hạn 24 giờ từ khi tạo; worker xóa payload QUEUED/READY hết hạn; download kiểm deadline cả khi worker tắt. APP_EXPORTS_ENABLED bật/tắt; DATA_EXPORT hiện trong `/admin/system/status`.
- Revenue gồm ID/mã đơn/paidAt/tổng sau returnedAmount của từng đơn đủ điều kiện; kho gồm ID/tên/SKU/số lượng; khách gồm ID/tên/họ/enabled/createdAt, chỉ CUSTOMER chưa xóa. Không email/điện thoại/địa chỉ/mật khẩu/token. CSV chống công thức; không URL file công khai. Dữ liệu lấy lúc worker chạy. CSV nhanh `/admin/reports/export` giai đoạn 4 vẫn độc lập, đồng bộ 5.000 dòng, không dùng một lần. CSV không thay backup DB.

## Trả sản phẩm

- POST `/returns` (CUSTOMER): `{orderId,itemId,quantity,reason,requestKey:UUID}` → `{id}`. Khóa đơn của chính khách; reason 1–500 ký tự. Cần DELIVERED/COMPLETED, có deliveredAt, chưa quá deliveredAt + returnWindowDays (legacy null dùng 7 ngày), không đang hoàn toàn bộ. Chỉ PRODUCT trong đơn; tổng lượng ở các yêu cầu khác REJECTED không vượt đã mua. Cùng key/payload replay id cũ; đổi payload 409; đơn người khác 404.
- GET `/returns` trả Page của khách; GET `/admin/returns?status=REQUESTED|APPROVED|REJECTED|RECEIVED|REFUNDED` là hàng đợi nội bộ. Dòng gồm id/orderId/itemId/quantity/status/reason/decisionReason/maximumRefund/refundAmount/createdAt/updatedAt/itemName.
- POST `/admin/returns/{id}/transition`: `{action:"approve"|"reject"|"receive"|"refund",reason,amount?,reference?}`. Mọi bước cần reason. REQUESTED → APPROVED/REJECTED; APPROVED → RECEIVED/REJECTED; RECEIVED → REFUNDED. Receive xác nhận đã kiểm hàng thực tế đủ điều kiện nhập kho, cộng kho có ledger. Refund ghi nhận hoàn tiền mặt thủ công, amount ≥0 và reference 1–100. Replay trạng thái không lặp kho/tiền; refund replay phải trùng amount/reference. Các luồng cùng khóa Order; full-refund bị chặn nếu còn yêu cầu khác REJECTED.
- Trần hoàn = phần subtotal sản phẩm được trả trong tổng giá sản phẩm sau trừ discountAmount/pointsDiscount, làm tròn xuống. Không hoàn ship/tax, không vượt maximumRefund/tổng đơn còn lại. Tạo OrderPaymentEvent `RETURN_REFUND_{id}` unique, tăng returnedAmount, không đổi tổng đơn gốc. Không tự phục hồi voucher/điểm; điều chỉnh điểm thủ công có audit khi chính sách yêu cầu. Đổi hàng = trả hàng rồi tạo đơn mới.
- OrderDTO thêm `returnedAmount`, `returnUntil`; OrderItemDTO thêm `itemId` chỉ đọc. Frontend có form tại chi tiết đơn và `/returns`. Thông báo chỉ trên web.
- Doanh thu dashboard/chart/CSV cũ và mới trừ returnedAmount tại paidAt của đơn gốc, không tạo kỳ riêng ở ngày hoàn. Top sản phẩm vẫn là bán gộp trước trả hàng.

## Audit

Mỗi write admin thành công có một SUCCESS: CONTENT_DRAFT_SAVED/PUBLISHED/UNPUBLISHED/RESTORED/DELETED, SYSTEM_SETTINGS_UPDATED, DATA_EXPORT_REQUESTED/DOWNLOADED, RETURN_APPROVED/REJECTED/RECEIVED/REFUNDED. Replay cũng một audit/request, không lặp hiệu ứng. Lỗi theo filter FAILED/DENIED. Cấu hình ghi số trước → sau; không audit body CMS/lý do tự do/file/keyword cá nhân. Worker ghi job_runs, không audit từng dòng. Khách tạo yêu cầu không phải write admin. Không thêm nhập lại mật khẩu; xóa CMS và xử lý trả hàng bắt buộc lý do, UI có xác nhận.
