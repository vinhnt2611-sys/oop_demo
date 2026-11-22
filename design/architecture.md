# Thiết kế Logic Lab

Sơ đồ Mermaid dưới đây mô tả bản demo thực tế. Đây không phải file Astah để nộp môn học.

## Use case

```mermaid
flowchart LR
    User[Người dùng] --> Select[Chọn 3 hoặc 4 biến]
    User --> Input[Nhập bảng chân trị 0/1]
    User --> Form[Chọn SOP hoặc POS]
    User --> Run[Rút gọn biểu thức]
    Run --> Result[Xem kết quả cuối]
    Run --> Rounds[Xem các cột ghép trung gian]
    Run --> Chart[Xem bảng PI và tập phủ]
    User --> Examples[Nạp ví dụ hoặc tạo ngẫu nhiên]
    User --> Help[Đọc giải thích thuật ngữ]
    User --> Copy[Sao chép kết quả]
```

Các chức năng tạo ví dụ, ngẫu nhiên, trợ giúp và sao chép là phần bổ sung để thuận tiện học tập. Không có actor quản trị, đăng nhập hay database.

## Tổng quan lớp

```mermaid
classDiagram
    class Main
    class NormalizerFrame
    class TruthTableModel
    class MainController
    class QuineMcCluskeyEngine
    class CoverSolver
    class TruthTable
    class Implicant
    class CombinationRound
    class PrimeImplicantChart
    class MinimizationResult
    class NormalFormStrategy {
        <<abstract>>
    }
    class SopStrategy
    class PosStrategy
    class BooleanExpression {
        <<interface>>
    }
    class Literal
    class Constant
    class And
    class Or
    Main --> NormalizerFrame
    NormalizerFrame *-- TruthTableModel
    NormalizerFrame *-- MainController
    NormalizerFrame --> MinimizationResult
    TruthTableModel ..> TruthTable : snapshot
    MainController *-- QuineMcCluskeyEngine
    QuineMcCluskeyEngine *-- CoverSolver
    QuineMcCluskeyEngine ..> TruthTable
    QuineMcCluskeyEngine ..> NormalFormStrategy
    QuineMcCluskeyEngine ..> MinimizationResult : creates
    NormalFormStrategy <|-- SopStrategy
    NormalFormStrategy <|-- PosStrategy
    NormalFormStrategy ..> BooleanExpression : builds
    MinimizationResult o-- TruthTable
    MinimizationResult o-- CombinationRound
    MinimizationResult o-- PrimeImplicantChart
    MinimizationResult o-- BooleanExpression
    PrimeImplicantChart o-- Implicant
    CombinationRound o-- Implicant
    BooleanExpression <|.. Literal
    BooleanExpression <|.. Constant
    BooleanExpression <|.. And
    BooleanExpression <|.. Or
    And o-- BooleanExpression
    Or o-- BooleanExpression
```

`CombinationRound.Entry`, `CombinationRound.Merge` và `CoverSolver.Solution` là các record lồng nhau dùng để lưu snapshot/đáp án. Các loại biểu thức cũng là record lồng trong `BooleanExpression`. Các snapshot và giá trị bất biến có thể chia sẻ đối tượng, nên sơ đồ dùng aggregation cho quan hệ giữ tham chiếu; không khẳng định quyền sở hữu vòng đời độc quyền.

## Các lớp quan trọng

```mermaid
classDiagram
    class TruthTable {
        -int variableCount
        -boolean[] outputs
        +variableCount() int
        +rowCount() int
        +output(int row) boolean
        +rowsWithValue(boolean value) List
    }
    class Implicant {
        +int variableCount
        +int value
        +int mask
        +combine(Implicant other) Optional
        +covers(int row) boolean
        +coveredRows() List
        +pattern() String
        +literalCount() int
    }
    class QuineMcCluskeyEngine {
        -CoverSolver coverSolver
        +minimize(TruthTable input, NormalFormStrategy form) MinimizationResult
    }
    class CoverSolver {
        +solve(PrimeImplicantChart chart) Solution
    }
    class NormalFormStrategy {
        <<abstract>>
        +name() String
        +targetValue() boolean
        +targetRows(TruthTable table) List
        +expressionFor(Implicant term) BooleanExpression
        +build(List chosen) BooleanExpression
    }
    class PrimeImplicantChart {
        -List targets
        -List primes
        +coverageMask(int index) int
        +targetMask() int
        +essentialIndices() Set
    }
    class MinimizationResult {
        +TruthTable input
        +NormalFormStrategy form
        +List rounds
        +PrimeImplicantChart chart
        +Solution solution
        +BooleanExpression expression
        +selectedPrimes() List
        +literalCount() int
        +equivalentToInput() boolean
    }
    QuineMcCluskeyEngine --> CoverSolver
    QuineMcCluskeyEngine ..> NormalFormStrategy
    CoverSolver ..> PrimeImplicantChart
```

Các trường record được biểu diễn là thành phần giá trị trên sơ đồ; trong Java, trường record thực tế là `private final` và được đọc bằng accessor cùng tên. Collection được sao chép bằng `List.copyOf`/`Set.copyOf`; mảng đầu vào được clone.

## Luồng tính toán

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant UI as NormalizerFrame
    participant Controller as MainController
    participant Engine as QuineMcCluskeyEngine
    participant Form as NormalFormStrategy
    participant Cover as CoverSolver
    User->>UI: Nhập F, chọn dạng, nhấn Rút gọn
    UI->>Controller: minimize(snapshot, strategy)
    Controller->>Engine: minimize(input, strategy)
    Engine->>Form: targetRows(input)
    Form-->>Engine: Dòng 1 (SOP) hoặc dòng 0 (POS)
    Engine->>Engine: Ghép và lưu các cột, thu thập PI
    Engine->>Cover: solve(chart)
    Cover-->>Engine: Essential PI và tập phủ bổ sung tối thiểu
    Engine->>Form: build(chosen)
    Form-->>Engine: Cây biểu thức SOP/POS của F
    Engine->>Engine: Kiểm chứng tất cả dòng đầu vào
    Engine-->>Controller: MinimizationResult
    Controller-->>UI: MinimizationResult
    UI-->>User: Kết quả, các vòng ghép, bảng PI
```

## Các quyết định triển khai

- Với tối đa 16 dòng, dùng bit mask `int` để biểu diễn tập dòng được phủ. Bit `i` là dòng `i`.
- Trong `Implicant`, bit 1 của `mask` là vị trí `x`; các bit cố định lưu trong `value`. Hai cube chỉ ghép khi cùng mask và khác đúng một bit value.
- PI được sắp xếp theo pattern để ID `P1`, `P2` và thứ tự duyệt ổn định.
- Brute force tối ưu số PI trước, số literal sau. Nếu hòa, giữ đáp án đầu tiên và đếm số tập cùng tối ưu.
- POS tối thiểu F' rồi lấy phủ định bằng cách đổi dấu literal và nối OR thành AND. Hằng 0/1 xử lý bằng đối tượng `Constant`.
- Khi người dùng thay đổi đầu vào, xóa trạng thái kết quả cũ và yêu cầu tính lại. Chọn ví dụ, ngẫu nhiên và đổi SOP/POS sẽ tính ngay để tiện khám phá.
- Tính trên Swing EDT vì giới hạn 4 biến và kích thước bài toán nhỏ; không có I/O mạng hoặc công việc dài trong thao tác này.
