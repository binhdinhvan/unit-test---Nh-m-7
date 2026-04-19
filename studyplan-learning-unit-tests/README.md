# Unit Tests – Chức năng Học bài & Theo dõi tiến độ

## Tổng quan

Module kiểm thử đơn vị cho chức năng **Học bài (Lesson)**, **Tiến độ học tập (LessonProgress)** và **Tiến độ bài kiểm tra (TestProgress)** của hệ thống.

| Chỉ số | Giá trị |
|---|---|
| Tổng số Test Case | 40 |
| Pass | 32 |
| Fail (Phát hiện Bug) | 8 |
| Framework | JUnit 5 + Mockito |
| Báo cáo Excel | `Unit_Testing_Report_StudyPlanLearning.xlsx` |

---

## Cấu trúc thư mục

```
studyplan-learning-unit-tests/
├── src/                                   # Source code được kiểm thử
│   ├── LessonServiceImpl.java             # Xử lý bài học & nội dung
│   ├── LessonProgressServiceImpl.java     # Theo dõi tiến độ bài học
│   └── TestProgressServiceImpl.java       # Theo dõi tiến độ bài kiểm tra
│
├── tests/                                 # File kiểm thử
│   └── StudyPlanLearningFeatureTests.java # 40 test cases (JUnit 5 + Mockito)
│
├── export-report.js                       # Script xuất Excel
├── inject-comments.js                     # Script đồng bộ Javadoc từ MD
├── package.json
└── Unit_Testing_Report_StudyPlanLearning.xlsx  # Báo cáo kết quả
```

---

## Mô tả Test Cases

### Nhóm 1: `LessonServiceImpl` – Quản lý bài học (TC_SPL_001 → TC_SPL_020)
- Lấy danh sách bài học, chi tiết bài học
- Kiểm soát quyền truy cập, mở khóa bài tiếp theo
- **Bug phát hiện:** `TC_SPL_009`, `TC_SPL_011`, `TC_SPL_013`, `TC_SPL_014`

### Nhóm 2: `LessonProgressServiceImpl` – Tiến độ bài học (TC_SPL_021 → TC_SPL_030)
- Cập nhật, truy xuất tiến độ học tập
- **Bug phát hiện:** `TC_SPL_019`, `TC_SPL_022`

### Nhóm 3: `TestProgressServiceImpl` – Tiến độ bài kiểm tra (TC_SPL_031 → TC_SPL_040)
- Cập nhật và theo dõi kết quả bài kiểm tra trong lộ trình
- **Bug phát hiện:** `TC_SPL_039`, `TC_SPL_040`

---

## Chạy báo cáo Excel

```bash
npm install
npm run export-report
```

Kết quả: file `Unit_Testing_Report_StudyPlanLearning.xlsx` sẽ được tạo/cập nhật trong thư mục này.
