# Unit Tests – Chức năng Làm bài kiểm tra đầu vào & Nhận lộ trình

## Tổng quan

Module kiểm thử đơn vị cho chức năng **Placement Test (Kiểm tra đầu vào)** và **Enrollment (Phân loại lộ trình học)** của hệ thống.

| Chỉ số | Giá trị |
|---|---|
| Tổng số Test Case | 38 |
| Pass | 31 |
| Fail (Phát hiện Bug) | 7 |
| Framework | JUnit 5 + Mockito |
| Báo cáo Excel | `Unit_Testing_Report_StudyPlan.xlsx` |

---

## Cấu trúc thư mục

```
studyplan-unit-tests/
├── src/                              # Source code được kiểm thử
│   ├── TestAttemptServiceImpl.java   # Xử lý nộp bài & tính điểm
│   ├── AssessmentServiceImpl.java    # Nạp đề kiểm tra đầu vào
│   └── EnrollmentServeceImpl.java    # Phân loại lộ trình theo điểm
│
├── tests/                            # File kiểm thử
│   └── StudyPlanFeatureTests.java    # 38 test cases (JUnit 5 + Mockito)
│
├── export-report.js                  # Script xuất Excel
├── inject-comments.js                # Script đồng bộ Javadoc từ MD
├── package.json
└── Unit_Testing_Report_StudyPlan.xlsx  # Báo cáo kết quả
```

---

## Mô tả Test Cases

### Nhóm 1: `TestAttemptServiceImpl` – Nộp bài & Tính điểm (SP_001 → SP_018)
| Loại | Số lượng |
|---|---|
| Happy Path | 5 |
| Validation / Edge Case | 6 |
| **Bug / Fail** | **7** |

**Các lỗi phát hiện được:**
- `SP_011_TC` – NPE khi `assessmentAttemptRequests = null`
- `SP_012_TC` – NPE khi `answerRequests = null` trong một assessment
- `SP_015_TC` – Lỗ hổng gian lận điểm: server tin `isCorrect` từ client
- `SP_016_TC` – Sinh viên `firstLogin=false` vẫn được nộp bài lại
- `SP_017_TC` – Cheat điểm: nộp ít câu nhưng đạt điểm tối đa
- `SP_018_TC` – Nộp câu hỏi thuộc Assessment khác dễ hơn
- `SP_019_TC` – `RuntimeException` thô thay vì `AppException` domain

### Nhóm 2: `AssessmentServiceImpl` – Nạp đề (SP_019 → SP_021)

### Nhóm 3: `EnrollmentServeceImpl` – Phân loại lộ trình (SP_022 → SP_038)

---

## Chạy báo cáo Excel

```bash
npm install
npm run export-report
```

Kết quả: file `Unit_Testing_Report_StudyPlan.xlsx` sẽ được tạo/cập nhật trong thư mục này.
