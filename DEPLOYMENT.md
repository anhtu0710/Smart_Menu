# Triển khai SmartMenu

## Chuẩn bị

- Cài Docker Desktop hoặc Docker Engine trên máy chủ Linux và bảo đảm Docker đang chạy.
- Có domain HTTPS trỏ vào máy chủ. Docker Compose bên dưới phục vụ HTTP trên cổng `APP_PORT`; hãy đặt Cloudflare, Caddy, Nginx hoặc load balancer HTTPS ở phía trước.
- Có `GEMINI_API_KEY` thật. Khóa này chỉ đặt trên máy chủ trong `.env`, không commit vào Git.
- Có một token ngẫu nhiên dài để bảo vệ webhook thanh toán.

## Chạy local bằng Docker

1. Tạo file bí mật: `Copy-Item .env.example .env`.
2. Sửa `.env`: tối thiểu `DB_PASSWORD`, `GEMINI_API_KEY`, `FRONTEND_URL`, `PAYMENT_WEBHOOK_TOKEN`. Mật khẩu SQL Server cần chữ hoa, chữ thường, số, ký tự đặc biệt và ít nhất 8 ký tự.
3. Chạy: `docker compose -f docker-compose.prod.yml up --build -d`.
4. Mở `http://localhost:8080` (hoặc cổng trong `APP_PORT`). Service `db-init` tự tạo `SmartMenuDB` một lần nếu database chưa có; backend sau đó tự tạo/cập nhật schema trong SQL Server volume `sqlserver_data`. Không chạy `smartmenu-sqlserver.sql` vì file cũ có lệnh xóa bảng và không khớp hoàn toàn entity hiện tại.
5. Dừng stack khi cần: `docker compose -f docker-compose.prod.yml down`. Lệnh này giữ volume DB. Chỉ thêm `-v` khi chủ ý xóa toàn bộ dữ liệu.

## Kiểm thử với hai dữ liệu thô

Sau khi stack chạy, thực hiện:

```powershell
.\scripts\test-api.ps1 `
  -ExcelPath 'C:\Users\ASUS\Downloads\Moc_Nau_Coffee_Menu_Data.xlsx' `
  -MenuImagePath 'C:\Users\ASUS\Downloads\f5458cbc-4b4b-45a1-be19-c088c6a1ff76.png'
```

Script kiểm tra đăng ký, đăng nhập, phân quyền, upload hai file, phân tích, đọc lại kết quả, blueprint và tạo menu phong cách miễn phí.

## Public URL miễn phí để kiểm thử

Khi chỉ cần kiểm thử từ Internet trong lúc máy đang bật, sau khi Docker Desktop đã có trạng thái **Running**, chạy một lệnh tại thư mục dự án:

```powershell
.\scripts\start-public-tunnel.ps1
```

Script tự khởi động stack và in URL `https://...trycloudflare.com`. Nếu muốn thao tác thủ công, dùng:

```powershell
docker compose -f docker-compose.prod.yml --profile quick-tunnel up -d
docker compose -f docker-compose.prod.yml logs quick-tunnel
```

Lấy URL `https://...trycloudflare.com` trong log và mở URL đó trên điện thoại hoặc mạng khác. Chạy kiểm thử qua URL public bằng cách thêm `-ApiUrl 'https://...trycloudflare.com/api/v1'` vào `scripts/test-api.ps1`.

Cloudflare đôi khi cần một đến ba phút để DNS của Quick Tunnel mới hoạt động. Script mặc định sẽ chờ tối đa ba phút và **chỉ** in URL khi đã tự truy cập HTTPS thành công.

Quick Tunnel không cần tài khoản Cloudflare nhưng URL sẽ đổi sau khi container/tunnel khởi động lại, không có cam kết uptime và bất kỳ ai biết URL đều có thể truy cập. Chỉ dùng cho dữ liệu test; khi cần hostname ổn định hoặc dùng thật, tạo Cloudflare Tunnel có tài khoản/domain theo phần production bên dưới. Mạng chạy Docker phải cho phép kết nối đi ra Cloudflare Tunnel qua UDP hoặc TCP cổng 7844; script sẽ báo rõ nếu mạng đang chặn kết nối này.

## Đưa lên production

1. Trên máy chủ, clone repository và tạo `.env` từ `.env.example` với giá trị production.
2. Đặt reverse proxy HTTPS cho domain đến `http://127.0.0.1:8080`, rồi đặt `FRONTEND_URL` đúng domain `https://...` và giữ `SESSION_SECURE=true`.
3. Chạy `docker compose -f docker-compose.prod.yml up --build -d`.
4. Kiểm tra `docker compose -f docker-compose.prod.yml ps` và `docker compose -f docker-compose.prod.yml logs --tail=100 backend`.
5. Chạy script kiểm thử ở trên qua URL domain HTTPS (tham số `-ApiUrl 'https://ten-mien/api/v1'`).

`PAYMENT_MOCK_ENABLED` phải luôn là `false` ở production. Webhook thanh toán chỉ chấp nhận header `X-SmartMenu-Webhook-Token`; hãy cấu hình token cùng giá trị ở cổng thanh toán/reverse proxy. Trước khi thu tiền thật, cần thay cơ chế token chung này bằng bước xác minh chữ ký và số tiền theo tài liệu chính thức của nhà cung cấp thanh toán bạn chọn.

## Đẩy mã nguồn lên Git

```powershell
git init
git add .
git commit -m "Prepare SmartMenu for production"
git branch -M main
git remote add origin https://github.com/anhtu0710/Smart_Menu.git
git push -u origin main
```

Kiểm tra `git status` trước khi `git add .`: `.env`, uploads và output build backend đã được ignore. Tuyệt đối không commit Gemini key, password SQL Server hoặc webhook token.
