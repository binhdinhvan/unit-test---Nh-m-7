# TOEIC Study Plan Learning - Unit Tests & Reporting

Repository chứa script tự động hóa trích xuất báo cáo Unit Test (Test Report) cho module `Lập lịch học & Học tập khóa học` (Lập lộ trình theo ngày, chấm điểm, quản lý mở khóa bài giảng).

Code base: `MaiHieu/src/test/java/com/mxhieu/doantotnghiep/StudyPlanLearningFeatureTests.java`.

## Thực thi tạo báo cáo

```bash
# Cài đặt dependency
npm install

# Inject metadata vào file Java
npm run inject-comments

# Xuất file Excel báo cáo test: Unit_Testing_Report_StudyPlanLearning.xlsx
npm run export-report
```
