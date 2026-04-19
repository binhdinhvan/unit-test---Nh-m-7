# Unit Tests – Chức năng Tra từ điển & Flashcard học tập

## Tổng quan

Module kiểm thử đơn vị cho chức năng **Tra từ điển (Dictionary)** tích hợp API bên ngoài và **Lưu Flashcard cá nhân (Student Dictionary)**.

| Chỉ số | Giá trị |
|---|---|
| Tổng số Test Case | 15 |
| Pass | 15 |
| Fail (Bug) | 0 |
| Framework | JUnit 5 + Mockito + WebClient Mock |
| Báo cáo Excel | `Unit_Testing_Report_Dictionary.xlsx` |

---

## Cấu trúc thư mục

```
dictionary-unit-tests/
├── src/                                    # Source code được kiểm thử
│   ├── DictionaryServiceImpl.java          # Tra từ qua API ngoài + cache DB
│   └── StudentDictionaryServiceImpl.java   # Lưu & quản lý Flashcard cá nhân
│
├── tests/                                  # File kiểm thử
│   └── DictionaryFeatureTests.java         # 15 test cases (JUnit 5 + Mockito)
│
├── export-report.js                        # Script xuất Excel
├── package.json
└── Unit_Testing_Report_Dictionary.xlsx     # Báo cáo kết quả
```

---

## Mô tả Test Cases

### Nhóm 1: `DictionaryServiceImpl` – Tra từ & Cache (DICT_001_TC → DICT_011_TC)
| Kịch bản | TC |
|---|---|
| Tra từ thành công (cache hit từ DB) | DICT_001_TC |
| Tra từ thành công (gọi API, lưu cache) | DICT_002_TC |
| Lỗi StudentProfile không tồn tại | DICT_003_TC |
| Lỗi không tìm thấy định nghĩa (API trả rỗng) | DICT_004_TC |
| Happy path trả về đầy đủ dữ liệu (IPA, audio, definition) | DICT_005_TC |
| Ánh xạ nhiều phần tử definitions thành công | DICT_006_TC |
| Kịch bản Index Out Of Bounds (audio rỗng) | DICT_007_TC → DICT_011_TC |

### Nhóm 2: `StudentDictionaryServiceImpl` – Flashcard cá nhân (DICT_012_TC → DICT_015_TC)
| Kịch bản | TC |
|---|---|
| Lưu flashcard lần đầu thành công | DICT_012_TC |
| Lấy danh sách flashcard của sinh viên | DICT_013_TC |
| Xóa flashcard thành công | DICT_014_TC |
| Kiểm tra trùng lặp flashcard | DICT_015_TC |

---

## Chạy báo cáo Excel

```bash
npm install
npm run export-report
```

Kết quả: file `Unit_Testing_Report_Dictionary.xlsx` sẽ được tạo/cập nhật trong thư mục này.
