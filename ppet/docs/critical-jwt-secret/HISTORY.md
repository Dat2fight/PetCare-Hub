# Lịch sử chứa khóa JWT cũ

Chỉ lưu hash commit và đường dẫn, không chứa giá trị khóa. Kiểm chứng bằng `git log --all -S` và `git grep -l -F` cho từng revision của `git rev-list --all`; pattern được truyền nội bộ, không in ra console.

Tổng: **55 commit reachable** chứa ít nhất một trong ba khóa cũ tại thời điểm đánh giá. `git log -S` cho các thay đổi số lần xuất hiện: `c570fe9`, `adfb3ef`, `13313f1`, `d8b8cff`, `9aa11c0`. Các commit sau đó vẫn thừa kế khóa dù không sửa dòng secret. Không viết lại lịch sử.

## Nhóm đường dẫn 1

Commit: `7a2511f`, `8917238`, `a971c2b`, `cbc7cad`, `30a352f`, `1f3304f`, `94088d6`, `1951a99`, `e44094e`, `4c37f7a`, `46b7cb0`, `86d1dd1`, `cfe10c5`, `53eb80d`, `5d9de2c`, `233053e`, `11cb032`, `06015d0`, `e468c47`, `b4286c6`, `dbaad01`, `18ab8ba`, `a84b24a`, `353eb81`, `163fc02`, `468f6a9`, `6a686da`, `53ccccd`, `af77964`, `fded878`, `34df62d`, `99b3e00`, `9aa11c0`, `13313f1`, `33eb427`, `3960646`, `c008760`, `1d7beb5`, `1c82a87`, `fa34711`, `63fd128`, `7dbb7e9`, `374fa93`, `f140e93`, `9c931c4`, `1eafc85`, `c223645`, `e6ec119`, `d0943ba`, `d9a156a`, `7789b0f`, `3371526`, `c570fe9`.

Các file chứa khóa trong mỗi commit của nhóm:

- `ppet/backend/src/main/resources/application-dev.yml`
- `ppet/backend/src/main/resources/application-test.yml`
- `ppet/backend/src/main/resources/application.yml`

## Nhóm đường dẫn 2

Commit: `d8b8cff`, `adfb3ef`.

Các file chứa khóa trong mỗi commit của nhóm:

- `PetCare-Hub/ppet/backend/src/main/resources/application-dev.yml`
- `PetCare-Hub/ppet/backend/src/main/resources/application-test.yml`
- `PetCare-Hub/ppet/backend/src/main/resources/application.yml`
- `ppet/backend/src/main/resources/application-dev.yml`
- `ppet/backend/src/main/resources/application-test.yml`
- `ppet/backend/src/main/resources/application.yml`
