# SmartMenu

SmartMenu là ứng dụng phân tích dữ liệu kinh doanh quán cà phê, đọc ảnh menu hiện tại và đề xuất menu mới. Frontend dùng Angular, backend dùng Spring Boot và dữ liệu vận hành trên SQL Server.

## Chạy bằng Docker

1. Tạo cấu hình cục bộ: `Copy-Item .env.example .env`.
2. Cập nhật các giá trị bí mật trong `.env`. Không commit file này.
3. Chạy stack:

```powershell
docker compose -f docker-compose.prod.yml up --build -d
```

Truy cập ứng dụng tại `http://localhost:8080`. Compose tự tạo database `SmartMenuDB`, khởi tạo schema và lưu dữ liệu SQL Server trong volume Docker.

## Kiểm thử tích hợp

Sau khi stack sẵn sàng, bộ kiểm thử sau xác nhận đăng ký/đăng nhập, phân quyền, upload Excel và ảnh menu, phân tích, blueprint/menu, giới hạn gói Plus, giao dịch thanh toán ở trạng thái `PENDING` và cách ly dữ liệu giữa người dùng:

```powershell
.\scripts\test-api.ps1 `
  -ExcelPath 'C:\Users\ASUS\Downloads\Moc_Nau_Coffee_Menu_Data.xlsx' `
  -MenuImagePath 'C:\Users\ASUS\Downloads\f5458cbc-4b4b-45a1-be19-c088c6a1ff76.png'
```

Các test đơn vị và build frontend:

```powershell
# Backend
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd' test

# Frontend
npm test -- --watch=false
npm run build
```

## Triển khai production

Xem hướng dẫn và checklist secrets/HTTPS tại [DEPLOYMENT.md](DEPLOYMENT.md). Trước khi phục vụ người dùng thật, bắt buộc cấu hình domain HTTPS, `GEMINI_API_KEY` và nhà cung cấp thanh toán có xác minh chữ ký hợp lệ.
