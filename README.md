# Logic Lab — oop_demo

Ứng dụng Java desktop minh họa topic 9 **Logic Expression Normalizer**: nhập bảng chân trị, rút gọn bằng Quine–McCluskey và quan sát các bước tạo lời giải. Đây là bản demo để học và hình dung sản phẩm; chưa phải bộ hồ sơ nộp capstone.

## Sơ đồ Astah và ảnh PNG

[File Astah chỉnh sửa được](design/LogicLab.asta) · [Mã nguồn tạo Astah](design/tools/CreateAstah.java) · [Hướng dẫn sơ đồ](design/ASTAH-README.md)

Các ảnh dưới đây được xuất từ file Astah của project.

### Use Case

![Use Case](design/preview/LogicLab/requirements/01%20UseCase.png)

### General Class

![General Class](design/preview/LogicLab/02%20GeneralClass.png)

### Detail Model

![Detail Model](design/preview/LogicLab/03%20DetailModel.png)

### Detail Algorithm

![Detail Algorithm](design/preview/LogicLab/04%20DetailAlgorithm.png)

### Detail Expression

![Detail Expression](design/preview/LogicLab/05%20DetailExpression.png)

### Detail UI

![Detail UI](design/preview/LogicLab/06%20DetailUI.png)

## Chạy thử

Máy cần **Java 17 trở lên**. Bản này đã được biên dịch và kiểm tra bằng JDK 21, tạo bytecode tương thích Java 17.

Trên Windows, nhấp đúp **`Run-LogicLab.cmd`**. File `dist/LogicLab.jar` đã được tạo sẵn.

Hoặc chạy trong terminal tại thư mục project:

```powershell
java -jar dist/LogicLab.jar
```

Không cần Internet, tài khoản, database hoặc thư viện ngoài. GUI dùng Java Swing trong thư viện chuẩn.

## Thử trong 5 phút

1. Trong danh sách ví dụ, chọn **Ghép cơ bản: F = C**. Ứng dụng nạp các dòng 1, 3, 5, 7 của bảng 3 biến và tự tính.
2. Mở **Các vòng ghép**. Chọn từng cột để thấy `001`, `011`, `101`, `111` ghép dần thành `xx1`, tức `C`. Bảng ghi rõ term đã ghép và term trở thành PI.
3. Chọn ví dụ **Phải dùng brute force**. Mở **Bảng PI**: 6 PI, không có essential PI, chọn 3 PI để phủ các dòng 0, 1, 2, 5, 6, 7. Có 2 tập phủ cùng tối ưu.
4. Chuyển sang **POS**. Ứng dụng xử lý các dòng F = 0 để rút gọn F', rồi dùng De Morgan tạo POS của F. Cột `z` trong bảng PI là dòng 0 của F.
5. Chọn ví dụ **4 biến, nhiều vòng ghép**. Có 16 dòng đầu vào; cuộn bảng nếu cần. Bấm cột **F** để đổi 0/1, sau đó nhấn **Rút gọn biểu thức**. Kết quả cũ được xóa ngay khi đầu vào thay đổi.
6. Thử hai ví dụ **Hàm luôn bằng 0/1**. Mở **Hiểu thuật toán** khi cần tra SOP, POS, minterm, literal, PI hoặc essential PI.

Các nút bổ sung: **Ngẫu nhiên**, **Đặt tất cả = 0**, **Sao chép kết quả**. Có thể chọn một dòng trong bảng rồi nhấn Space để đổi F. Enter thực hiện rút gọn.

## Chức năng đã có

| Chức năng của topic 9 | Cách triển khai |
|---|---|
| 3 biến / 4 biến | Bảng 8 / 16 dòng, A là bit trái nhất |
| Nhập bảng chân trị | Chỉ 0 và 1; không có don't-care |
| SOP / POS | Hai strategy riêng, dùng chung thuật toán |
| Quine–McCluskey | Nhóm theo số bit 1, ghép, đánh dấu, loại trùng |
| Các cột trung gian | Lưu toàn bộ snapshot, các cặp ghép và PI của từng cột |
| Bảng PI | Các hàng PI, cột mục tiêu, đánh dấu phủ, tô màu PI được chọn |
| Essential PI | Nhận biết dòng mục tiêu chỉ được một PI phủ |
| Phủ tối thiểu | Brute force tập con PI bổ sung; ít PI nhất, sau đó ít literal nhất |
| Biểu thức cuối | Cây biểu thức có thể tính giá trị; định dạng SOP/POS |
| Kiểm chứng | Sau mỗi lần tính, so kết quả với toàn bộ bảng chân trị gốc |

Chữ `x` trong mẫu ghép nghĩa là biến bị loại, không phải cho phép don't-care đầu vào. Biểu thức tối thiểu không nhất thiết là duy nhất. Tiêu chí phụ “ít literal nhất” và cách chọn ổn định khi hòa là quyết định thiết kế của bản demo.

## Biên dịch và kiểm thử

Cần **JDK 17+**, các lệnh `java`, `javac`, `jar` có trong PATH. Trong PowerShell:

```powershell
.\build.cmd
.\test.cmd
```

Trong Command Prompt, dùng `build.cmd` và `test.cmd`. Không dùng Maven/Gradle; không tải dependency. Script chạy từ đúng thư mục của nó và không thay đổi thiết lập hệ thống.

`test.cmd` biên dịch lại source, chạy kiểm thử headless, trả exit code khác 0 khi thất bại. Xem [TESTING.md](TESTING.md) để biết phạm vi kiểm thử và kết quả đã ghi nhận.

## Cấu trúc

```text
logic-normalizer-demo/
  Run-LogicLab.cmd            Mở bản JAR đã đóng gói
  build.cmd                  Biên dịch và đóng gói bằng JDK
  test.cmd                   Kiểm thử đầy đủ
  dist/LogicLab.jar           Bản chạy
  sourcecode/src/logic/
    Main.java                Điểm khởi chạy
    model/                   Bảng chân trị, cube, snapshot, bảng PI, kết quả
    algorithm/               Quine–McCluskey và brute-force cover
    expression/              Cây biểu thức và strategy SOP/POS
    ui/                      Swing GUI, table model, controller
  sourcecode/test/logic/      Bộ kiểm thử và bộ giải độc lập
  design/architecture.md     Use case, sơ đồ lớp và giải thích OOP
  design/LogicLab.asta       6 sơ đồ Astah native, chỉnh sửa được
  design/ASTAH-README.md     Hướng dẫn đọc và tạo lại sơ đồ
  WALKTHROUGH.md             Một ví dụ giải tay để đối chiếu
```

## Thiết kế OOP

- `TruthTable` đóng gói và sao chép dữ liệu đầu vào để giữ tính bất biến.
- `Implicant` là đối tượng giá trị: mẫu nhị phân, vị trí x, kiểm tra phủ và ghép.
- `NormalFormStrategy` là lớp trừu tượng; `SopStrategy` và `PosStrategy` kế thừa, override hành vi. Engine gọi qua kiểu cha để thể hiện đa hình.
- `QuineMcCluskeyEngine` sử dụng `CoverSolver`, tạo các đối tượng kết quả có cấu trúc.
- Cây `BooleanExpression` biểu diễn AND, OR, literal và hằng; cùng một kết quả được dùng cho cả hiển thị lẫn tính giá trị.
- GUI không triển khai thuật toán. Các package model/algorithm/expression không phụ thuộc Swing.

## Phạm vi bản demo

Bản này có source, chương trình chạy, kiểm thử, sơ đồ Mermaid, file Astah native với 6 sơ đồ và hướng dẫn. Trên máy đã chuẩn bị demo, mở Astah bằng `Open-Astah.cmd`; trên máy khác, mở `design/LogicLab.asta` bằng Astah UML 12 đã cài. Xem `design/ASTAH-README.md` để biết nội dung. Chưa có report Word, slide, video hoặc thông tin/phân công thành viên. Các việc đó cần làm theo nhóm thực tế nếu phát triển thành bộ nộp môn học. Repo `oop_demo` dùng để lưu bản minh họa này.

Source của bản demo do trợ lý AI tạo trong phiên làm việc này, không có mã thư viện bên ngoài được sao chép vào project. Đây là tài liệu minh họa để đọc, thử và học thiết kế; khi làm bài chính thức, tuân thủ yêu cầu tự viết và khai báo nguồn của môn học.
