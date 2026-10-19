# Critical #1 — cấu hình và xoay khóa JWT

## 1. Hiện trạng và baseline

- Đã chạy `git status --short`, `git branch -a`, `git log --oneline -n 40`. Working tree ban đầu sạch; nhánh hiện tại `fix/high-bugs`, HEAD `7a2511f`. Tạo `fix/critical-jwt-secret` từ chính HEAD này. Không push, force, stash, reset hoặc viết lại lịch sử.
- Pha 1 chỉ đọc source/config/history và chạy baseline, chưa sửa source. Chỉ bắt đầu Pha 2 sau khi kết luận khả thi và thông báo lựa chọn phương án.
- Baseline backend: `cmd.exe /c ".\mvnw.cmd -q -Dnet.bytebuddy.experimental=true test"` → **110 test: 109 pass, 0 fail/error, 1 skip**. Skip là `SqlServerConstraintTest`, chưa bật Docker opt-in. Test dùng H2, không chạy script lên SQL Server thật.
- Host dùng Java 25, target compile vẫn Java 17; flag ByteBuddy phục vụ Mockito trên host hiện tại. Chạy Maven ngoài sandbox theo hạn chế Java/Maven đã xác nhận trong phiên làm việc. Không có lỗi test baseline cần miễn trừ.
- Baseline frontend `npm.cmd run build` → **pass**, Vite 7.3.7 + `tsc -b`. Không thay dependency, frontend source hoặc lockfile. Buildinfo sinh tự động được trả lại đúng nội dung HEAD.
- Phiên tiếp tục bắt đầu tại `f348ef3`, không làm lại bản sửa Java. Đã chạy `git status`, `git show --stat HEAD`, `git diff`, đối chiếu checklist và hoàn tất tài liệu/kiểm chứng. Các thay đổi chưa commit khi tiếp tục đúng là tài liệu, .gitignore, .env.example và buildinfo. Đã restore riêng buildinfo theo yêu cầu; không pop/drop hoặc thay đổi `stash@{0}` có sẵn.

## 2. Pha 1 — bằng chứng và tính khả thi

### 2.1. Khai báo, đọc và sử dụng khóa

| Vị trí trước sửa | Vai trò / phát hiện |
|---|---|
| `backend/src/main/resources/application.yml`, `app.jwt.secret` | Khóa literal công khai; 64 byte UTF-8; không phụ thuộc biến môi trường |
| `backend/src/main/resources/application-dev.yml`, `app.jwt.secret` | Tham chiếu JWT_SECRET từng có fallback công khai; 66 byte UTF-8 |
| `backend/src/main/resources/application-test.yml`, `app.jwt.secret` | Khóa test literal 63 byte UTF-8 nhưng nằm trong main resources, có thể được đóng gói cùng ứng dụng |
| `backend/target/classes/application{,-dev,-test}.yml` | Ba bản sao generated của cùng cấu hình, phát hiện khi quét cả file ignored; đã clean build để loại bản sao cũ |
| `config/JwtProperties.java` | `@ConfigurationProperties(prefix="app.jwt")`; getter `getSecret()`; trước sửa mới chỉ `@NotBlank`, không kiểm tra độ dài/phơi lộ ở startup |
| `security/JwtTokenProvider.java` | `getSigningKey()` lấy UTF-8 bytes → `Keys.hmacShaKeyFor`; `generateToken()` ký; `getUsernameFromJWT()`, `validateToken()`, `getExpirationFromJWT()` xác minh/parser dùng cùng khóa |
| `controller/AuthController.login()` | Sau xác thực mật khẩu gọi `generateToken()`, trả token cho frontend |
| `security/JwtAuthenticationFilter.doFilterInternal()` | Xác minh chữ ký, lấy subject rồi nạp quyền hiện tại bằng UserDetailsService; biết khóa cho phép tự ký subject của user có quyền nếu backend dùng khóa đó |
| `config/SecurityConfig` | Đăng ký JwtAuthenticationFilter, không có khóa riêng |
| `security/StompAuthorizationInterceptor` | Dùng JwtTokenProvider cho CONNECT/expiration; không có khóa riêng |
| `AuditSecurityTest`, `EnabledAccountTest`, `WebSocketDeliveryTest` | Sinh token tại thời điểm test; AuditSecurityTest đọc JwtProperties để kiểm tra expiration; không giữ token cũ cố định. StompAuthorizationTest mock provider |
| `frontend/src/api/client.ts`, `contexts/AuthContext.tsx`, `hooks/useWebSocket.ts` | Lưu/gửi accessToken/token, không có khóa ký. HTTP 401 xóa token và chuyển đăng nhập; socket reconnect gửi token hiện tại |
| `seed_pets.js` | Đăng nhập bằng credential từ môi trường và dùng token vừa nhận; không ký token hoặc nhúng token cũ |
| `.github/workflows/deploy.yml` | Chỉ npm build + GitHub Pages frontend, không chạy backend/Maven; GitHub token deploy không phải JWT signing secret |
| README, docs, scripts, `.env*`, compose/Docker | Không tìm thấy khóa JWT khác ngoài ba giá trị trên. Không có `.env` hay compose/Docker cấu hình backend trong workspace. Docs có mô tả refresh và token minh họa, không phải signing key |

Các đường dẫn Java trong bảng dưới `backend/src/main/java/com/petcarehub/`. Các test dưới `backend/src/test/java/com/petcarehub/`.

Lệnh kiểm chứng: `rg -l --hidden` tìm `jwt/signing key/refresh/blacklist`; kiểm kê file bằng `rg --files --hidden --no-ignore`; Python đọc cấu hình với giá trị secret/password bị che; `rg -l -F -f - --hidden --no-ignore -g '!**/.git/**'` quét chính xác ba khóa lấy trong bộ nhớ. Chỉ xuất tên file, không xuất khóa. Phạm vi quét chính xác gồm cả generated/ignored files.

### 2.2. Property và thuật toán

- **Medium #35 đã được sửa**: code đọc `JwtProperties.expirationMs` tương ứng `app.jwt.expiration-ms` ở base/dev/test; base 24 giờ, dev/test 1 giờ. Secret đều là `app.jwt.secret`. Không cần đổi property expiration hoặc claims để sửa #1.
- Các property refresh-expiration/refresh-expiration-ms trong YAML không được JwtProperties đọc. Không sửa những property không dùng đó trong lượt này.
- Thư viện **JJWT 0.12.6**, xác nhận `backend/pom.xml` và bytecode dependency bằng `C:/Users/CHIEN/.jdks/ms-21.0.12.1/bin/javap.exe -c -classpath .../jjwt-api-0.12.6.jar io.jsonwebtoken.security.Keys`. `javap` không có trong PATH nên đã dùng đường dẫn JDK có sẵn; không nâng thư viện.
- Secret được dùng như **UTF-8 bytes, không Base64 decode**. `Keys.hmacShaKeyFor` yêu cầu ít nhất **32 byte/256 bit**. Với cách `signWith(key)` hiện tại: 32–47 byte → HS256; 48–63 → HS384; ≥64 → HS512. Hai khóa runtime cũ chọn HS512; khóa test cũ chọn HS384. Test mới xác nhận cả ba ngưỡng mà không đổi cách chọn thuật toán.
- Claims vẫn đúng `sub`, `iat`, `exp`; không thêm role claim, refresh token, blacklist hay API mới.

### 2.3. Lịch sử và ảnh hưởng xoay khóa

- `git log --all -S` phát hiện các commit thay đổi số lần xuất hiện: `c570fe9`, `adfb3ef`, `13313f1`, `d8b8cff`, `9aa11c0`.
- `c570fe9` đã chứa cả ba file `ppet/backend/src/main/resources/application.yml`, `application-dev.yml`, `application-test.yml`. `adfb3ef`/`d8b8cff` còn có bản sao dưới prefix `PetCare-Hub/ppet/...`; hai commit xóa thư mục trùng vẫn giữ khóa trong đường dẫn `ppet/...`.
- Kiểm tra từng revision reachable tìm được **55 commit còn chứa khóa** (kể cả HEAD baseline `7a2511f`), với hash + file cụ thể trong [HISTORY.md](HISTORY.md). Các commit con không sửa secret vẫn thừa kế nó; không chỉ những commit xuất hiện trong `git log -S` mới chứa khóa.
- **Khóa cũ đã bị lộ vĩnh viễn**. Xóa ở HEAD không xóa bản sao trong clone, lịch sử hoặc artifact cũ. Không viết lại lịch sử trong công việc này.
- Đổi khóa làm mọi token ký bằng khóa trước mất hiệu lực với instance mới; tất cả instance phải chuyển khóa/restart để instance cũ không tiếp tục nhận token cũ. Không có grace period chấp nhận khóa đã lộ.
- Không tìm thấy endpoint refresh hay blacklist/revocation implementation. `docs/api.md` và `docs/architecture.md` mô tả refresh, nhưng AuthController thực tế chỉ có login/register/me. Frontend xóa `refreshToken` khi logout/401 nhưng không có luồng refresh thực thi. Đây là hiện trạng để đánh giá rotation, không sửa tính năng khác.
- Test/script không phụ thuộc token cũ. Frontend người dùng đang giữ token cũ sẽ phải đăng nhập lại; socket cũng cần reconnect bằng token mới. Không cần sửa frontend contract.

### 2.4. Local, CI và deploy

- Không còn default profile dev trong cấu hình hiện hành. Dev chỉ bật khi chọn rõ ràng. Baseline process, Windows User/Machine không có `JWT_SECRET`, `APP_JWT_SECRET`, `SPRING_PROFILES_ACTIVE`; chỉ kiểm tra hiện diện, không đọc/in giá trị secret ngoài workspace.
- Workflow duy nhất build frontend, không cần signing key. Không có backend deploy/compose để cập nhật. Không có quyền quan sát cấu hình của server đã triển khai hay secret store bên ngoài; **không suy luận rằng chúng chưa được cấu hình**.
- Sau sửa, cách chạy local/server trước đây dựa vào literal/fallback sẽ dừng cho tới khi cấp khóa mới. Đây là thay đổi vận hành có chủ đích. `mvn test` vẫn tự chạy với cấu hình chỉ thuộc test classpath; npm build không cần JWT secret.

### 2.5. So sánh và quyết định

| Phương án | Ưu điểm | Nhược điểm / tác động |
|---|---|---|
| A: bắt buộc môi trường mọi profile, kể cả automated tests | Một quy tắc fail-fast; không mặc định | Mọi developer/CI test phải cấp secret dù chỉ chạy H2; tăng nguy cơ dùng nhầm production secret cho test |
| B: runtime bắt buộc, dev tự sinh khóa mỗi restart | Local chạy ngay, không khóa công khai | Mất phiên sau restart; các instance dev có khóa khác nhau; nhánh hành vi riêng dễ gây nhầm cấu hình |
| **C: A cho mọi profile trong artifact chạy thật; automated tests dùng tài nguyên riêng** | Dev/server cùng fail-fast; test tự chạy với khóa giả ngẫu nhiên, không cần production secret; không có khóa test trong artifact | Cần cấp JWT_SECRET trước khi chạy dev/server; phải clean artifact cũ khi chuyển tài nguyên test |

**Chọn C. Kết luận: KHẢ THI**, với điều kiện vận hành cấp khóa mới, đồng bộ mọi instance và chấp nhận đăng nhập lại. Profile dev không có cơ chế tự sinh khóa trong mã chạy thật. Thêm denylist fingerprint của ba khóa đã lộ để ngay cả cấu hình môi trường vô tình tái sử dụng chúng cũng bị từ chối.

## 3. Pha 2 — thay đổi và test

| Commit | File đã đổi | Nội dung |
|---|---|---|
| `f348ef3` — `fix(#1): require a fresh JWT secret and fail fast on unsafe configuration` | JwtProperties, JwtTokenProvider, application.yml, application-dev.yml, chuyển application-test.yml main → test resources, JwtSecretConfigurationTest | Một binding app.jwt.secret từ JWT_SECRET, không fallback; fail-fast constructor; chặn thiếu/rỗng/blank/placeholder/<32 byte/ba khóa đã lộ; cache signing key hợp lệ; bỏ secret khỏi toString; test-only random key |
| `fix(#1): document JWT key provisioning and rotation` (commit chứa báo cáo này) | `.gitignore`, root README, `ppet/README.md`, `backend/.env.example`, REPORT/HISTORY | Hướng dẫn sinh/cấp/rotate khóa, ignore .env toàn repo, sample CHANGE_ME bị từ chối, bằng chứng lịch sử và giới hạn kiểm chứng |

`JwtSecretConfigurationTest`: **17 case** gồm context thiếu/rỗng/blank/khóa ngắn/placeholder, biên 31 byte, các ngưỡng 32/48/64 byte, ký+xác thực+claims/expiration, token ký bằng khóa ngẫu nhiên khác, token hết hạn, toString không chứa secret, profile base/dev fail khi thiếu và khởi tạo được khi cấp JWT_SECRET.

Ngoài JUnit, đã lấy ba khóa cũ nội bộ từ commit baseline và chạy provider trong tiến trình Java tạm: **cả ba bị từ chối**, thông báo không chứa giá trị. Không ghi khóa vào file/console; chương trình kiểm tra tạm đã xóa. Fingerprint lưu trong code không phải signing key hay fallback.

Không sửa JwtAuthenticationFilter, SecurityConfig, WebSocket authorization, controller roles, seed, API/claims, frontend source hoặc thư viện. Không xóa hay bỏ qua test cũ.

### Checklist đối chiếu khi tiếp tục

| Mục | Trạng thái khi tiếp tục | Bằng chứng / phần hoàn tất trong phiên này |
|---|---|---|
| Xóa khóa cứng/fallback, một property secret | Xong từ phiên trước | Base dùng app.jwt.secret tham chiếu JWT_SECRET không fallback; dev kế thừa; cấu hình test đã chuyển khỏi main resources |
| Fail-fast thiếu/rỗng/ngắn/mẫu; từ chối khóa đã lộ | Xong từ phiên trước | Constructor JwtTokenProvider; 17 case JwtSecretConfigurationTest; không đổi claims |
| Dev chạy local an toàn | Mã đã xong; hướng dẫn chưa commit | Dev cần khóa môi trường như server; README bổ sung lệnh CSPRNG và hướng dẫn nạp biến |
| .env.example, README, .gitignore | Còn dở: đã soạn, chưa commit | Rà lại nội dung mẫu, kiểm tra ignore bằng git check-ignore, đưa vào commit tài liệu |
| Workflow/compose | Đã đánh giá; không cần thay đổi | Workflow chỉ npm/GitHub Pages; không có compose/backend deploy trong repo |
| Test JUnit bắt buộc | Xong từ phiên trước | Thiếu/ngắn/mẫu fail, hợp lệ ký/xác thực, khác khóa và hết hạn bị từ chối |
| Quét cuối và kiểm chứng sau gián đoạn | Chưa chốt khi tiếp tục | Chạy lại toàn repo trừ .git, Maven test và frontend build; restore buildinfo sau build |

## 4. Kết quả kiểm chứng

| Lệnh / mốc | Kết quả |
|---|---|
| Baseline mvn test | 110 tổng: 109 pass, 0 fail/error, 1 skip |
| Test JWT tập trung: `mvnw.cmd -q -Dnet.bytebuddy.experimental=true -Dtest=JwtSecretConfigurationTest clean test` | 17 pass, 0 fail/error/skip |
| Toàn bộ mvn test sau commit mã | **127 tổng: 126 pass, 0 fail/error, 1 skip** |
| Frontend baseline / sau sửa | `npm.cmd run build` pass / pass; không đổi frontend source |

Sau mỗi commit đều chạy toàn bộ Maven test; kết quả sau commit tài liệu được báo trong phần bàn giao của chat. Skip SQL Server như baseline, không phải pass. Không kiểm thử đăng nhập bằng browser hoặc kết nối SQL Server thật trong lượt này. Các warning context startup trong test thiếu khóa là hành vi được kiểm thử, không phải test fail.

Đã xác nhận `target/classes/application-test.yml` không còn sau clean; cấu hình test chỉ ở `target/test-classes`. Phiên tiếp tục đã quét toàn repo trừ `.git`, gồm cả file ignored/generated: **không có giá trị của ba khóa cũ và không có fallback JWT** (hai lệnh rg đều exit 1: không có kết quả khớp). Pattern khóa được lấy từ Git trong bộ nhớ, chỉ xuất file path nếu tìm thấy. `.env.example` và REPORT/HISTORY đã được đọc và quét: không có khóa/token thật, chỉ có placeholder, tên biến và lệnh hướng dẫn. `git check-ignore` xác nhận .env/.env.production bị bỏ qua còn .env.example được phép commit. Những placeholder trong test/.env.example là có chủ đích, không phải khóa dùng để chạy server.

## 5. Hướng dẫn vận hành

1. **Sinh khóa mới riêng cho từng môi trường** bằng CSPRNG. Hướng dẫn PowerShell và Bash không in kết quả nằm ở [README](../../README.md#khóa-ký-jwt-bắt-buộc). Với Bash:
   ```bash
   export JWT_SECRET="$(openssl rand -base64 64 | tr -d '\r\n')"
   ```
   Không thực thi lệnh sinh khóa trong tác vụ audit này; đây chỉ là hướng dẫn, không kèm kết quả. Khóa tối thiểu 32 byte UTF-8, khuyến nghị 64 byte random biểu diễn Base64; backend không decode Base64.
2. **Local Windows:** cấp `$env:JWT_SECRET` qua script CSPRNG trong README hoặc secret store/IDE launcher rồi chạy `.\mvnw.cmd spring-boot:run` trong cùng terminal. Chọn dev riêng nếu cần. Không ghi khóa vào source hay shell command chứa literal. `.env.example` chỉ có `CHANGE_ME`; Spring Boot không tự nạp `.env`.
3. **CI:** npm build hiện không cần JWT. Maven automated tests tự có test-only random key. Nếu job khởi động backend thật, dùng GitHub Secrets → `env.JWT_SECRET` theo snippet README; không echo hoặc bật tracing. Không thay workflow frontend thành backend job trong phạm vi này.
4. **Server:** cấp JWT_SECRET bằng môi trường của service/secret manager. Không đặt trong command line, Git, image hoặc frontend/VITE_*. Cùng môi trường/cluster dùng cùng khóa; lưu khóa ổn định để restart bình thường không tự hủy phiên.
5. **Rotation:** drain/dừng tất cả instance dùng khóa cũ, cấp khóa mới, restart tất cả instance (bao gồm websocket sessions). Không để rolling deployment giữ instance nhận khóa đã lộ; người dùng phải đăng nhập lại.
6. **Kiểm tra startup:** tại môi trường thử, bỏ biến hoặc dùng placeholder/khóa ngắn → phải fail-fast; khôi phục khóa hợp lệ → ứng dụng chạy và login được. Kiểm tra log chỉ có hướng dẫn đặt JWT_SECRET, không có giá trị. Unit/context tests đã kiểm tra đường fail-fast không cần DB.

## 6. Việc bắt buộc mà code không làm thay

- Coi mọi khóa cũ đã lộ vĩnh viễn; **không tái sử dụng ở bất kỳ môi trường nào**. Denylist giúp chặn ba giá trị đã biết, không thể phát hiện mọi khóa yếu/khóa lộ khác.
- Cấp khóa mới thực tế ở local/server/secret store và restart toàn bộ instance. Code không tự sửa cấu hình môi trường bên ngoài; token cũ mất hiệu lực và người dùng cần đăng nhập lại.
- Nếu từng triển khai bằng khóa mặc định, kiểm tra log truy cập và hoạt động tài khoản admin để tìm dấu hiệu lạm dụng. Không suy diễn rằng đã có hoặc chưa có sự cố chỉ từ source code.
- Nhóm quyết định có làm sạch lịch sử Git/artifact cũ hay không. Tác vụ này không sửa lịch sử; làm sạch lịch sử cũng không thay thế rotation vì các bản clone có thể đã tồn tại.

## 7. Checklist kiểm tra tay

| Tình huống | Kết quả mong đợi |
|---|---|
| Chạy base/dev không cấp JWT_SECRET (không có external override app.jwt.secret) | Startup thất bại; thông báo tên biến, không lộ giá trị |
| Cấp khóa dưới 32 byte, chuỗi trắng hoặc CHANGE_ME | Startup thất bại |
| Cấp lại bất kỳ khóa cũ từng nằm trong repository | Startup thất bại do denylist fingerprint |
| Cấp khóa mới hợp lệ | Startup thành công, đăng nhập được, REST và STOMP tiếp tục dùng cùng token contract |
| Giữ token trước rotation, restart toàn bộ bằng khóa khác | Token cũ bị từ chối; đăng nhập lại nhận token mới dùng được |
| Restart giữ nguyên khóa hợp lệ | Token còn hạn vẫn dùng được; dev không tự đổi khóa |
| Kiểm tra log và artifact | Không lộ secret/token; không có application-test.yml trong artifact chạy thật |

## 8. Rủi ro còn lại và phạm vi không xử lý

- Fail-fast làm môi trường chưa cấp secret không chạy được, đúng mục tiêu an toàn. Chất lượng entropy/bảo vệ secret store và phối hợp rotation vẫn là trách nhiệm vận hành; kiểm tra độ dài không chứng minh khóa đủ ngẫu nhiên.
- Không quan sát được các server, GitHub Secrets hay deployment ngoài workspace. Không thể xác nhận khóa đang chạy thật đã được thay hay mọi token cũ đã bị loại khỏi mọi instance.
- Đã đọc hiện trạng #2 (bootstrap riêng, demo dev opt-in), #11 (enabled check), #7 (STOMP JWT), phân quyền hiện hành để phân tích tác động; **không sửa các lỗi này trong lượt #1**, không nâng Spring Security #24.
- Property refresh không có implementation tương ứng; không thêm refresh/blacklist. Các mô tả refresh trong tài liệu cũ và các vấn đề High/Medium/Low khác không được sửa kèm.
- Bản sao khóa trong lịch sử/clone/artifact phát hành trước đây vẫn tồn tại. Xóa fallback ở HEAD và chặn fingerprint không thu hồi được bản sao đã phát tán.
