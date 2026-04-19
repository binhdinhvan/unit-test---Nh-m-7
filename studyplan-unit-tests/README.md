# TOEIC Study Plan - Unit Tests & Reporting

Repository này chứa script xuất báo cáo Unit Test (Test Report) cho module `Study Plan & Placement Test` (Chức năng Kiểm tra đầu vào & Nhận lộ trình).

Lưu ý: Source code Unit Test (.java) được đặt trực tiếp bên trong cấu trúc Spring Boot của dự án tại thư mục `MaiHieu/src/test/java/com/mxhieu/doantotnghiep/StudyPlanFeatureTests.java` để chạy với JUnit & Mockito thực tế. Thư mục này đóng vai trò quản lý báo cáo, giúp sinh ra Excel Report chuẩn hóa đồng nhất với hệ thống.

## Scope of Testing

### ✅ Các thành phần ĐƯỢC kiểm thử (Đã Test)

- **Lớp**: `TestAttemptServiceImpl` và `EnrollmentServeceImpl`
- **Logic Kiểm thử**:
  1. `saveResultFirstTest` (Lưu kết quả kiểm tra xếp lớp, tính điểm từ 0-100%, chặn nộp thiếu bài, xử lý biên NaN, kiểm tra cheat điểm).
  2. `saveEnrollment` (Ghi danh, tự động chọn lộ trình Track dựa trên khoảng điểm <30, 30-60, >=60).
  3. `getAssessmentDetailForFistTest` (Truy vấn đề test đầu vào).

**Lý do**: Layer xử lý lõi nghiệp vụ (Business Logic) phức tạp nhất, tác động đến phân luồng học viên, yêu cầu Test Case dày đặc (38 TCs) bao gồm cả Happy Path và Invalid/Mock Fail (Bug Hunting).

### ❌ Các thành phần KHÔNG thuộc Unit Test

- Lớp: Các lớp Repository (chỉ được Mock để cô lập Database)
- Lớp: Controllers (thuộc phạm vi API Test).

## Thực thi tạo báo cáo

```bash
# 1. Cài đặt dependency
npm install

# 2. Xóa và Cập nhật lại Javadoc trong file Java (nếu cần sync metadata)
npm run inject-comments

# 3. Xuất báo cáo Excel (File output: Unit_Testing_Report_StudyPlan.xlsx)
npm run export-report
```
