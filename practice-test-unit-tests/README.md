# Unit Tests – Chức năng Luyện đề TOEIC

## Tổng quan

Module kiểm thử đơn vị cho chức năng **Luyện đề TOEIC** gồm Backend (Node.js/Jest) và Frontend (React/Vitest).

| Chỉ số | Giá trị |
|---|---|
| Tổng Backend Test Case | 52 |
| Pass (Backend) | 43 |
| Fail/Bug (Backend) | 9 |
| Tổng Frontend Test Case | 31 |
| Pass (Frontend) | 31 |
| Framework Backend | Jest (Node.js) |
| Framework Frontend | Vitest (React) |
| Báo cáo Excel | `Unit_Testing_Report_PracticeTest.xlsx` |

> **Lưu ý:** File Excel xuất ra hiển thị **52 test Backend** (AttemptService + ExamService).  
> File Java `PracticeTestFeatureTests.java` trong `tests/` là bộ test Spring Boot riêng của module MaiHieu (33 test cases).

---

## Cấu trúc thư mục

```
practice-test-unit-tests/
├── src/                              # Source code được kiểm thử
│   ├── TestAttemptServiceImpl.java   # Xử lý lượt thi (Java)
│   ├── TestServiceImpl.java          # Quản lý đề thi (Java)
│   └── AttemptSeviceImpl.java        # Dịch vụ attempt (Java)
│
├── tests/                            # File kiểm thử
│   └── PracticeTestFeatureTests.java # 33 test cases Spring Boot (JUnit 5)
│
├── export-report.js                  # Script xuất Excel (đọc từ MD)
├── package.json
└── Unit_Testing_Report_PracticeTest.xlsx  # Báo cáo kết quả
```

---

## Mô tả Test Cases (Backend – 52 TC)

### AttemptService – 39 Test Cases (`TC_ATT_001` → `TC_ATT_039`)
| Chức năng | Số TC |
|---|---|
| Bắt đầu làm bài (`startAttempt`) | 7 |
| Nộp bài (`submitAttempt`) | 7 |
| Xem kết quả (`getAttemptResults`) | 5 |
| Lịch sử (`getStudentAttempts`) | 3 |
| Điểm cao nhất (`getBestScore`) | 2 |
| Thống kê (`getProgressStatistics`) | 2 |
| Thoát bài (`deleteAttempt`) | 4 |
| Tính thời gian (`calculateTimeElapsed`) | 4 |
| Helper (`transformToExamResultResponse`, `identifyWeakAreas`) | 5 |

**Các lỗi phát hiện:** `TC_ATT_006`, `TC_ATT_007`, `TC_ATT_015`, `TC_ATT_016`, `TC_ATT_017`, `TC_ATT_018`, `TC_ATT_019`, `TC_ATT_035`, `TC_ATT_039`

### ExamService – 13 Test Cases (`TC_EX_001` → `TC_EX_013`)
| Chức năng | Số TC |
|---|---|
| Danh sách đề (`getAllExams`) | 4 |
| Chi tiết đề (`getExamById`) | 5 |
| Tìm kiếm (`searchExams`) | 4 |

---

## Chạy báo cáo Excel

```bash
npm install
npm run export-report
```

Kết quả: file `Unit_Testing_Report_PracticeTest.xlsx` sẽ được tạo/cập nhật trong thư mục này.
