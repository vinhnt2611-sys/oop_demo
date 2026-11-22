# Kiểm thử Logic Lab

Đã chạy thành công `test.cmd` trên JDK 21 của máy hiện tại. Source được biên dịch với `--release 17`.

## Kết quả đã ghi nhận

```text
PASS: all 256 functions with 3 variables, SOP and POS
PASS: all 65536 functions with 4 variables, SOP and POS
PASS: 65,792 functions / 131,584 minimizations / 14,533,126 assertions, 11.97s
```

Thời gian thay đổi theo máy. Bộ test thất bại bằng `AssertionError` và exit code khác 0 nếu có sai lệch.

## Những gì được kiểm chứng

1. **Tương đương:** đánh giá cây biểu thức kết quả trên toàn bộ 8/16 dòng; so với bảng chân trị gốc, cho cả SOP và POS.
2. **Đầy đủ PI:** oracle liệt kê toàn bộ `3^n` cube bằng mã tam phân, xác định cube hợp lệ từ tập dòng được phủ, rồi chọn các cube cực đại. So sánh tập mẫu PI này với đầu ra engine.
3. **Tối thiểu toàn cục:** oracle dùng tất cả cube hợp lệ, không dùng PI hoặc CoverSolver của engine, giải bài toán phủ bằng quy hoạch động trên các dòng còn thiếu. So chi phí `(số term, số literal)` theo thứ tự từ điển với kết quả engine.
4. **Quá trình hiển thị:** kiểm tra số cột, không có term trùng trong một cột, dấu đã ghép đúng, cặp ghép đúng vị trí x/khác một bit, đầu ra ghép đúng cột kế tiếp và không dừng sớm.
5. **Trường hợp đặc biệt:** hàm hằng 0/1 được bao gồm trong duyệt đầy đủ; kiểm tra riêng ví dụ không có essential PI, có 2 nghiệm tối ưu, 64 tập con được duyệt.
6. **Dữ liệu và nhập liệu:** bảng chân trị sao chép dữ liệu, từ chối số biến không hỗ trợ; model GUI đổi F, chuyển 3/4 biến và reset dữ liệu đúng.

Đây là phép duyệt đầy đủ mọi bảng chân trị 3/4 biến, không chỉ kiểm tra ngẫu nhiên. Phần kiểm thử tự động chạy headless và không giả lập thao tác chuột của người dùng. Giao diện kết quả và bảng PI cũng đã được mở kiểm tra trực tiếp trên Windows; không suy ra rằng mọi tương tác GUI đều được bao phủ bởi bộ kiểm thử thuật toán.
