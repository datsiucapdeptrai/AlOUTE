# Tính năng video đã thêm vào FAKEBOOK

## Chức năng

- Đăng bài kèm video MP4, WebM, OGV hoặc MOV (tối đa 100 MB).
- Ảnh poster video JPG, PNG hoặc GIF (tối đa 5 MB).
- Tiêu đề và thể loại video.
- Phát video trên bảng tin và trang danh sách video.
- Đếm lượt xem duy nhất theo tài khoản khi người dùng bắt đầu phát video.
- Like/bỏ like và bình luận bằng AJAX, không tải lại trang.
- Chia sẻ bằng chức năng Share của trình duyệt; nếu không hỗ trợ sẽ sao chép liên kết.
- Trang chi tiết video tại `/videos/{postId}`.
- Danh sách video tại `/videos`, lọc theo thể loại và phân trang 3 video/trang.
- Hiển thị số video theo từng thể loại.

## Chạy dự án

1. Giải nén dự án và mở thư mục `FAKEBOOK` trong Spring Tool Suite.
2. Chọn **Maven > Update Project > Force Update**.
3. Chọn **Project > Clean**.
4. Chạy ứng dụng bằng **Run As > Spring Boot App**.
5. Đăng nhập, vào **Bảng tin**, mở mục **Thêm video** để đăng.

Hibernate đang dùng `spring.jpa.hibernate.ddl-auto=update`, vì vậy lần chạy đầu sẽ tự tạo bảng `video_categories`, `post_views` và bổ sung các cột video cho bảng `posts`.

## Lưu ý

- File tải lên nằm trong thư mục cấu hình bởi `app.upload-dir` (mặc định là `uploads`).
- Có thể đổi giới hạn file trong `src/main/resources/application.properties`.
- Lượt xem chỉ tăng một lần cho mỗi tài khoản/video.
