# Kiểm tra thủ công sau bản vá

Các lệnh dưới đây dành cho **DB QA/staging riêng**, có fixture `qa_`, sau khi người vận hành đã cấu hình và khởi động backend. Không chạy trên database dev đang dùng hoặc production. Những mutation dưới đây chỉ là hướng dẫn, chưa được thực thi trong nhiệm vụ này. PowerShell 7 dùng `-SkipHttpErrorCheck`; không in object login, token hoặc mật khẩu.

## Chuẩn bị và BUG-001

```powershell
$qaBase = 'http://localhost:8081/api/v1'
function Invoke-QaApi($method, $path, $body=$null, $token=$null) {
  $args = @{ Uri="$qaBase$path"; Method=$method; SkipHttpErrorCheck=$true }
  if ($token) { $args.Headers=@{ Authorization="Bearer $token" } }
  if ($null -ne $body) { $args.ContentType='application/json'; $args.Body=($body|ConvertTo-Json -Depth 8) }
  $r=Invoke-WebRequest @args
  $parsed=$null
  try { $parsed=$r.Content|ConvertFrom-Json } catch { }
  return @{ Status=[int]$r.StatusCode; Json=$parsed }
}
$qaStamp=[DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$qaPassword=[Guid]::NewGuid().ToString()+'Aa9!'
$qaNameA="qa_customer_a_$qaStamp"
$qaNameB="qa_customer_b_$qaStamp"
foreach($name in @($qaNameA,$qaNameB)) {
  $registration=Invoke-QaApi POST '/auth/register' @{username=$name;email="$name@example.test";password=$qaPassword;firstName='qa_First';lastName='qa_Last';role='SALES_STAFF'}
  "Register status: $($registration.Status)" # 200
}
$qaLoginA=Invoke-QaApi POST '/auth/login' @{username=$qaNameA;password=$qaPassword}
$qaLoginB=Invoke-QaApi POST '/auth/login' @{username=$qaNameB;password=$qaPassword}
$qaTokenA=$qaLoginA.Json.token; $qaTokenB=$qaLoginB.Json.token
$qaIdA=$qaLoginA.Json.user.id; $qaIdB=$qaLoginB.Json.user.id
"Granted roles: $($qaLoginA.Json.user.roles -join ',')" # CUSTOMER
(Invoke-QaApi GET '/admin/users' $null $qaTokenA).Status # 403
```

Đổi role payload thành ADMIN/VETERINARIAN/chuỗi lạ với username/email QA mới: vẫn CUSTOMER. Tạo nhân viên chỉ qua `/admin/staff` bởi QA ADMIN. Luồng này nhận role nhân viên thật, không nhận ADMIN. PATCH `/admin/users/{id}/role` từ CUSTOMER phải 403; hạ quyền admin hiện hữu bị 403.

## BUG-002 — toàn bộ write catalog

Các ID nhập bên dưới phải là fixture QA của từng loại, không dùng ID dữ liệu nghiệp vụ. API đúng sẽ từ chối trước mutation.

```powershell
$qaProductId=[long](Read-Host 'ID product qa_')
$qaCategoryId=[long](Read-Host 'ID category qa_')
$qaPromotionId=[long](Read-Host 'ID promotion qa_')
$cases=@(
  @{Path='/products';Id=$qaProductId;Body=@{name='qa_denied';price=1;categoryId=$qaCategoryId}},
  @{Path='/categories';Id=$qaCategoryId;Body=@{name='qa_denied'}},
  @{Path='/promotions';Id=$qaPromotionId;Body=@{code="qa_denied_$qaStamp";discountType='PERCENTAGE';discountValue=1}}
)
foreach($case in $cases) {
  (Invoke-QaApi POST $case.Path $case.Body $qaTokenA).Status # 403
  (Invoke-QaApi PUT "$($case.Path)/$($case.Id)" $case.Body $qaTokenA).Status # 403
  (Invoke-QaApi DELETE "$($case.Path)/$($case.Id)" $null $qaTokenA).Status # 403
}
```

Không token: 401; VETERINARIAN: 403; SALES_STAFF/ADMIN được qua kiểm quyền, sau đó có thể nhận lỗi validation/nghiệp vụ. Promotion giữ SALES_STAFF theo chính sách base.

## BUG-003 — dữ liệu thật của hai chủ

Tạo trước fixture pet/lịch/hồ sơ/thông báo **cho tài khoản B vừa tạo** bằng công cụ QA được cấp quyền; không chỉ kiểm với ID không tồn tại. B phải đọc được chính fixture đó. Test tự động QaAppointmentScopeTest/PrivatePetDataTest đã tạo các hàng H2 này.

```powershell
$qaPetB=[long](Read-Host 'ID pet qa_ thuộc B')
$qaRecordB=[long](Read-Host 'ID medical-record qa_ của pet B')
$qaNoticeB=[long](Read-Host 'ID notification qa_ của B')
(Invoke-QaApi GET "/appointments/user/$qaIdB" $null $qaTokenA).Status # 403
(Invoke-QaApi GET '/appointments' $null $qaTokenA).Status # 403
(Invoke-QaApi GET '/appointments/me' $null $qaTokenA).Status # 200, chỉ lịch A
(Invoke-QaApi GET "/appointments/pet/$qaPetB" $null $qaTokenA).Status # 404
(Invoke-QaApi GET "/notifications/$qaIdB" $null $qaTokenA).Status # 403
(Invoke-QaApi PUT "/notifications/$qaNoticeB/read" $null $qaTokenA).Status # 404
(Invoke-QaApi GET "/medical-records/$qaRecordB" $null $qaTokenA).Status # 404
(Invoke-QaApi GET "/medical-records/pet/$qaPetB" $null $qaTokenA).Status # 404
(Invoke-QaApi GET "/vaccinations/pet/$qaPetB" $null $qaTokenA).Status # 404
(Invoke-QaApi GET "/pets/my-pets/$qaPetB" $null $qaTokenB).Status # 200
```

VETERINARIAN/SALES_STAFF GET appointments chỉ thấy lịch được phân công; ADMIN thấy tất cả. Clinical read/ghi: chủ chỉ đọc, VETERINARIAN/ADMIN theo ma trận. UserId chéo là 403; ID tài nguyên riêng của người khác là 404.

## BUG-007 — không có API tự sửa số dư cho khách

```powershell
(Invoke-QaApi POST "/loyalty/account/$qaIdB/spend?points=1&description=qa_test" $null $qaTokenA).Status # 403
(Invoke-QaApi POST "/loyalty/account/$qaIdB/spend?points=-100&description=qa_test" $null $qaTokenA).Status # 403 trước validation
(Invoke-QaApi POST '/loyalty/account/spend?points=1&description=qa_test' $null $qaTokenA).Status # 403
$qaAdminCredential=Get-Credential -Message 'Tài khoản ADMIN chỉ dùng ở DB QA'
$qaAdminLogin=Invoke-QaApi POST '/auth/login' @{username=$qaAdminCredential.UserName;password=$qaAdminCredential.GetNetworkCredential().Password}
$qaAdminToken=$qaAdminLogin.Json.token
foreach($points in @(-100,0,1000001)) {
  (Invoke-QaApi POST "/loyalty/account/$qaIdB/spend?points=$points&description=qa_test" $null $qaAdminToken).Status # 400
}
```

So số dư và ledger trước/sau invalid request: không đổi. Test H2 BalanceAndCartConcurrencyTest kiểm hai lần trừ 80 từ 100 chỉ một thành công, dư 20 và một REDEEM. Chưa chạy lock/race trên SQL Server; test opt-in cần SQL Server container riêng.

## BUG-005 — public DTO

```powershell
$qaPets=Invoke-QaApi GET '/pets?size=100'
$qaLeaks=@($qaPets.Json.content|Where-Object { $_.PSObject.Properties.Name -contains 'ownerId' -or $_.PSObject.Properties.Name -contains 'healthStatus' })
"Private keys found: $($qaLeaks.Count)" # 0
(Invoke-QaApi GET "/pets/$qaPetB").Status # 404
(Invoke-QaApi GET "/pets?ownerId=$qaIdB&availability=SOLD").Status # 200; không nới query public
```

Fixture gồm pet AVAILABLE không chủ, pet có chủ dù AVAILABLE, RESERVED và SOLD: list chỉ pet AVAILABLE không chủ. Detail RESERVED chưa có chủ hiển thị “đang được đặt”; SOLD/owned 404. Quiz không trả healthStatus/ownerId và không lưu kết quả cho pet owned.

## BUG-004 — đơn thật và lỗi frontend

```powershell
(Invoke-QaApi POST '/orders' @{} $qaTokenA).Status # 400
$qaOrder=Invoke-QaApi POST '/orders' @{
  userId=$qaIdB;total=0.01;shippingAddress='qa_Người nhận, qa_000, qa_Địa chỉ';paymentMethod='COD';
  items=@(@{productId=$qaProductId;quantity=1;price=0.01})
} $qaTokenA
"Status=$($qaOrder.Status); orderId=$($qaOrder.Json.id); orderStatus=$($qaOrder.Json.status)" # 200, id dương, PENDING
(Invoke-QaApi GET "/orders/$($qaOrder.Json.id)" $null $qaTokenB).Status # 404
```

Giá phải bằng DB fixture, chủ phải là A, paymentStatus PENDING. Quantity=0/thiếu địa chỉ →400; vượt tồn →409; hàng inactive →404. Kiểm cart chỉ bị trừ phần mua thành công; item thứ hai thất bại phải rollback toàn bộ.

Kiểm UI tại `/PetCare-Hub/`: guest mở checkout chuyển login với state.from và quay lại sau login; giỏ rỗng không có nút đặt. Trên môi trường QA, dùng DevTools request override/mock response cho POST orders lần lượt 400/409/500 hoặc 200 với `{}`: form và lỗi vẫn hiển thị, không toast thành công/không điều hướng. Response hợp lệ id dương mới tới `/orders/{id}`, trang tracking GET lại đơn thật. Loading ngăn bấm đúp; mất mạng sau commit thì kiểm danh sách đơn trước khi thử lại. Không có idempotency server trong base này.

## BUG-006 và các lệnh gate

Context thiếu/ngắn JWT được kiểm tự động, không cần khởi động hoặc ghi SQL Server:

```powershell
Set-Location .\ppet\backend
.\mvnw.cmd '-Dtest=JwtSecretConfigurationTest,QaSecretConfigurationTest,AdminBootstrapTest' test
.\mvnw.cmd clean test
Set-Location ..\frontend
npx tsc --noEmit
npm run lint
npm run build
git restore --worktree -- tsconfig.app.tsbuildinfo
```

Để kiểm startup bằng tay trên DB QA với schema hợp lệ, buộc secret trống và Hibernate validate (không dùng dev DB):

```powershell
# Tại ppet/backend; DB_* trỏ DB QA, bootstrap tắt.
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--app.jwt.secret= --spring.jpa.hibernate.ddl-auto=validate --app.bootstrap.enabled=false'
```

Kết quả: lỗi cấu hình nhắc JWT_SECRET, context không start; không có giá trị secret trong lỗi. Khóa hợp lệ qua context test; bootstrap thiếu/disabled không tạo admin, bật đủ cấu hình thì lưu BCrypt. Xem SECRET-OPERATIONS.md để cấp biến đúng và rotate; không dùng khóa test cho runtime.
