# Ví dụ rút gọn và đối chiếu giao diện

Chọn 3 biến A, B, C; A là bit trái nhất. Nhập F = 1 ở các dòng **1, 3, 5, 7**, các dòng khác bằng 0. Đây là ví dụ 01 trong ứng dụng.

| Dòng | A | B | C | F |
|---|---|---|---|---|
| 0 | 0 | 0 | 0 | 0 |
| 1 | 0 | 0 | 1 | 1 |
| 2 | 0 | 1 | 0 | 0 |
| 3 | 0 | 1 | 1 | 1 |
| 4 | 1 | 0 | 0 | 0 |
| 5 | 1 | 0 | 1 | 1 |
| 6 | 1 | 1 | 0 | 0 |
| 7 | 1 | 1 | 1 | 1 |

## SOP

Biểu thức ban đầu:

```text
F = A'B'C + A'BC + AB'C + ABC
```

**Cột 0:** nhóm theo số bit 1.

```text
Nhóm 1: 001
Nhóm 2: 011, 101
Nhóm 3: 111
```

**Cột 1:** ghép các cặp khác đúng một bit.

```text
001 + 011 -> 0x1   (A'C)
001 + 101 -> x01   (B'C)
011 + 111 -> x11   (BC)
101 + 111 -> 1x1   (AC)
```

**Cột 2:** hai cặp hợp lệ đều tạo cùng kết quả; chỉ giữ một term.

```text
0x1 + 1x1 -> xx1
x01 + x11 -> xx1
```

Không ghép tiếp được. `xx1` là PI duy nhất, tương ứng **C**. PI này phủ cả 1, 3, 5, 7 nên cũng là essential PI.

```text
F = C
```

Giao diện hiện 3 cột, 1 PI, 1 PI được chọn và 1 literal. Kiểm chứng: ở cả 8 dòng, F đúng bằng C.

## POS

Các dòng F=0 là **0, 2, 4, 6**. Đó là các minterm của F'. Ghép chúng được `xx0`, tức **F' = C'**. Lấy phủ định lại:

```text
F = (C')' = C
```

SOP và POS có thể cho cùng biểu thức trong ví dụ này. Đây không phải lỗi: một literal cũng là một SOP/POS hợp lệ.

## Ví dụ cần brute force

Nạp ví dụ 02: **F = Σm(0,1,2,5,6,7)**. Có 6 PI:

```text
P1 = 00x = A'B'
P2 = 0x0 = A'C'
P3 = 11x = AB
P4 = 1x1 = AC
P5 = x01 = B'C
P6 = x10 = BC'
```

Mỗi minterm được hai PI phủ nên không có essential PI. Duyệt 64 tập con của 6 PI, tìm được hai tập tối ưu (3 PI, 6 literal):

```text
{P2, P3, P5}: F = A'C' + AB + B'C
{P1, P4, P6}: F = A'B' + AC + BC'
```

Ứng dụng chọn tập đầu theo thứ tự duyệt ổn định. Hai kết quả đều đúng; bảng PI cho thấy tại sao phải chọn thêm PI thay vì chỉ lấy essential PI.
