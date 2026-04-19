# TOEIC Practice Test - Unit Tests & Reporting

Repository chứa script tự động hóa trích xuất báo cáo Unit Test (Test Report) cho module `Luyện đề & Kiểm tra trực tuyến` (Lấy danh sách đề, chấm điểm theo thời gian thực, lưu trữ lịch sử làm bài).

Code base: `MaiHieu/src/test/java/com/mxhieu/doantotnghiep/PracticeTestFeatureTests.java`.

## Thực thi tạo báo cáo

```bash
# Cài đặt dependency
npm install

# Inject metadata vào file Java
npm run inject-comments

# Xuất file Excel báo cáo test: Unit_Testing_Report_PracticeTest.xlsx
npm run export-report
```
