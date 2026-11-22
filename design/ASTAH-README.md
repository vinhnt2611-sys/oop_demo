# Sơ đồ Astah cho Logic Lab

Mở **LogicLab.asta** bằng Astah UML 12. Trên máy hiện tại, có thể nhấp đúp **Open-Astah.cmd** trong thư mục gốc project; launcher dùng runtime Astah và Java 25 đã tải riêng trong `.tools/astah12`, không đổi Java mặc định của máy.

Đây là file Astah native, có model UML và các đối tượng có thể chỉnh sửa; không phải ảnh được chèn vào file `.asta`.

## Nội dung

| Diagram | Mục đích |
|---|---|
| 01 UseCase | Actor Người dùng và 8 chức năng của ứng dụng, có system boundary |
| 02 GeneralClass | 22 kiểu Java của demo, nhóm theo UI, algorithm, model và expression; ẩn thuộc tính/phương thức |
| 03 DetailModel | Bảng chân trị, implicant, snapshot vòng ghép, bảng PI và kết quả |
| 04 DetailAlgorithm | Engine Quine–McCluskey, CoverSolver và Solution |
| 05 DetailExpression | Strategy SOP/POS, cây biểu thức; quan hệ kế thừa và hiện thực interface |
| 06 DetailUI | Điểm khởi chạy, controller, table model và các thao tác chính của frame |

Các lớp/record lồng nhau vẫn giữ owner tương ứng trong cây model. Các sơ đồ dùng chung model, nên sửa một lớp sẽ phản ánh ở các presentation khác của lớp đó.

## Cách đọc

- Association liền: quan hệ giữ tham chiếu giữa đối tượng.
- Dependency đứt, mũi tên thường: sử dụng kiểu khác qua tham số, kết quả hoặc lời gọi.
- Generalization liền, tam giác rỗng: kế thừa `NormalFormStrategy`.
- Realization đứt, tam giác rỗng: hiện thực `BooleanExpression`.
- `record`: kiểu Java record bất biến, có thành phần dữ liệu và accessor do Java sinh.

Chi tiết được lấy từ bản `dist/LogicLab.jar` của demo. Đã lược bỏ anonymous class, thành viên synthetic, các phương thức record `equals/hashCode/toString` và accessor trùng với thuộc tính. Sơ đồ UI lược bỏ danh sách Swing widget/helper dựng bố cục để tập trung vào trách nhiệm điều phối. Đây là bản thiết kế minh họa, chưa phải bộ nộp môn học.

Sơ đồ Use Case không coi các bước nội bộ như đếm bit hoặc brute force là thao tác của actor. Nạp ví dụ, ngẫu nhiên, sao chép và trợ giúp là tính năng bổ sung của demo.

## Tạo lại từ code

Source generator: `tools/CreateAstah.java`. Dùng API chính thức của Astah. Chạy `Generate-Astah.cmd` ở thư mục gốc, hoặc truyền đường dẫn Astah đã cài làm tham số đầu tiên. Cần JDK trong PATH; không đóng gói lại `astah-api.jar` hay phần mềm Astah trong project.

File đã được lưu, chạy kiểm tra model và mở lại thành công qua Astah API với đủ 6 diagram. Bản tạo bằng Astah 12 có thể không mở được trong phiên bản Astah cũ hơn.
