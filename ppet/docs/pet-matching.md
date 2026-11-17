# Tìm thú cưng phù hợp: dữ liệu và quy tắc

Hệ thống đối chiếu câu trả lời với hồ sơ riêng của từng thú cưng trong bảng `pets`.
Không suy ra tính cách từ mô tả, giống, tuổi, cân nặng; không có dữ liệu ngẫu nhiên hay danh sách ID cố định.

## Nhập và cập nhật hồ sơ

Trong **Quản trị → Thú cưng đăng bán → Tạo mới / chỉnh sửa**, nhập từ đánh giá thực tế:

| Trường | Giá trị |
|---|---|
| `activityLevel` | `LOW`, `MEDIUM`, `HIGH`: nhu cầu vận động thấp, vừa, cao |
| `suitableForApartment` | Có / Không / Chưa đánh giá |
| `goodWithChildren` | Có / Không / Chưa đánh giá |
| `minimumDailyCareHours` | Giờ chăm sóc tối thiểu mỗi ngày, lớn hơn 0 đến 24, tối đa 2 số thập phân |

Không tự chọn giá trị khi chưa đánh giá. Hồ sơ thiếu bất kỳ trường nào không được xếp hạng.
Thú cưng vẫn có thể đăng bán bình thường trong catalog khi chưa đủ hồ sơ gợi ý.
Thông tin cập nhật được đọc lại từ database trong lần gửi khảo sát tiếp theo.
Kết quả đã mở là ảnh chụp tại thời điểm tìm; gửi lại khảo sát để tính lại.

## Điều kiện và điểm

Chỉ xét thú cưng chưa có chủ, trạng thái `AVAILABLE`, không gắn đơn giữ chỗ và có đủ hồ sơ.
Ưu tiên các bé đáp ứng môi trường sống, trẻ nhỏ, số giờ chăm sóc và nhu cầu vận động.
Chỉ khi không có bé nào đáp ứng đầy đủ, trả các bé gần phù hợp nhất theo điểm đối chiếu.
Mỗi kết quả dự phòng có `fallback=true` và `unmetCriteria` nêu đầy đủ điều kiện chưa đáp ứng;
giao diện hiển thị rõ “Gần phù hợp · Cần cân nhắc”, kể cả khi điểm bằng 0.
Không trộn kết quả dự phòng vào danh sách khi vẫn có bé đáp ứng đầy đủ.

- Không gian: 20 điểm nếu đáp ứng. Nhà riêng không yêu cầu tiêu chí căn hộ; không suy đoán có sân hay diện tích.
- Gia đình: 20 điểm nếu đáp ứng. Không có trẻ nhỏ thì không yêu cầu tiêu chí trẻ nhỏ.
- Chăm sóc: min(30, làm tròn xuống(30 × giờ khách có / giờ tối thiểu bé cần)). Thiếu ít thời gian được điểm cao hơn thiếu nhiều; thừa giờ không được thưởng thêm.
- Vận động: 30 điểm nếu trùng mức, 15 điểm nếu lệch một mức, 0 điểm nếu lệch hai mức.

Đây là thang điểm đối chiếu công khai, **không phải xác suất, mô hình khoa học hay bảo đảm về hành vi**.
Giải thích kết quả nêu dữ liệu của bé, câu trả lời của khách và điểm từng tiêu chí.
Sắp theo điểm giảm dần, ID tăng dần khi bằng điểm; trả tối đa 10 bé. Lưu kết quả và khảo sát trong cùng giao dịch.

## Nâng cấp và kiểm chứng

Flyway `V22__pet_matching_profiles.sql` thêm bốn cột nullable và ràng buộc SQL Server.
Chạy bản backend mới với Flyway bật để nâng cấp trước khi dùng giao diện mới.
Không sửa migration cũ. Không tự điền thông số cho dữ liệu cũ; người quản trị cần đánh giá và bổ sung.

`MatchingProfileTest` kiểm tra API quản trị tạo/sửa/lưu/đọc hồ sơ và ảnh hưởng tới API gợi ý;
bao gồm dữ liệu thiếu, sai, điều kiện loại trừ và không suy đoán từ mô tả/tuổi/cân nặng.
`MatchingTransactionTest` kiểm tra thứ hạng, phân quyền, tính riêng tư và rollback.
`FlywayMigrationTest` kiểm tra nâng cấp từ V21, giữ dữ liệu cũ chưa đánh giá và ràng buộc trên SQL Server thật.

Chạy backend tests bằng `./mvnw.cmd test`; kiểm thử SQL Server riêng bằng `tools/test-sqlserver.ps1`
(harness chỉ tạo/xóa database thử nghiệm mang tên ngẫu nhiên, không dùng database ứng dụng).
