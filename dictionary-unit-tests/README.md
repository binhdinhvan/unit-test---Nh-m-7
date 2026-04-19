# TOEIC Dictionary & Flashcard - Unit Tests & Reporting

Repository này chứa script xuất báo cáo Unit Test (Test Report) cho module Tra Từ Điển & Lưu Từ Cá Nhân (Gọi API WebClient tích hợp Merriam-Webster API, Parse Json rác, Xử lý Flashcard sinh viên).

## Scope of Testing

### ✅ Các thành phần ĐƯỢC kiểm thử (Đã Test)
- **Lớp**: `DictionaryServiceImpl` (xử lý WebClient và DB Cache) 
- **Lớp**: `StudentDictionaryServiceImpl` (xử lý Data Cá Nhân của StudentProfile).

**Lý do**: Đây là cầu nối Integration với Web API bên thứ 3 (Merriam Webster), chứa nhiều lỗ hổng JSON Parse cực lớn, dễ gây crash tiến trình Java. Rất cần Test Coverage.

## Thực thi tạo báo cáo

```bash
# 1. Cài đặt dependency
npm install

# 2. Xóa và Cập nhật lại Javadoc trong file Java (nếu cần sync metadata)
npm run inject-comments

# 3. Xuất file Excel báo cáo: Unit_Testing_Report_Dictionary.xlsx
npm run export-report
```
