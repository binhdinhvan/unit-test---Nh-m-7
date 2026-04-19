const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, '../MaiHieu/src/test/java/com/mxhieu/doantotnghiep/StudyPlanFeatureTests.java');
let content = fs.readFileSync(filePath, 'utf8');

const regex = /\s*@Test\s+@DisplayName\("(SP_\d+_TC):\s*([^"]+)"\)/g;

let count = 0;
const newContent = content.replace(regex, (match, tcId, description) => {
  count++;
  // Rút trích ID (ví dụ 001, 002)
  const num = parseInt(tcId.split('_')[1]);
  
  let objective = `Xác thực logic của ${description}`;
  let input = 'Mock Data hợp lệ hoặc lỗi tùy chỉnh';
  let expected = 'Kết quả theo Description mong đợi';
  let notes = 'Happy path';

  // Customize theo dải ID
  if (num <= 5) {
    objective = 'Kiểm tra thuật toán lưu điểm và tính điểm trung bình cho bài First Test';
    input = 'Dữ liệu test attempt với tỷ lệ câu đúng/sai cụ thể';
    expected = 'Điểm được tính toán chính xác và các trạng thái profile được cập nhật';
  } else if (num >= 6 && num <= 10) {
    objective = 'Kiểm tra ràng buộc ID ngoại lệ (Not Found) trong First Test';
    input = 'Gửi request với các ID thực thể (student, test, assessment) không tồn tại trong CSDL';
    expected = 'Ném ra AppException với ErrorCode tương ứng';
    notes = 'Bảo vệ tính toàn vẹn dư liệu DB';
  } else if (num >= 11 && num <= 18) {
    objective = 'Ngăn chặn Bug hệ thống hoặc gian lận (Cheat) trong First Test';
    input = 'Các tham số null, payload thiếu câu hỏi, ID không thuộc assessment';
    expected = 'Hệ thống báo lỗi hoặc từ chối request gian lận';
    notes = 'Phát hiện lỗi hổng bảo mật điểm thi';
  } else if (num >= 19 && num <= 21) {
    objective = 'Nạp đúng cấu trúc đề First Test cho sinh viên';
    input = 'Gửi request getAssessmentDetailForFistTest';
    expected = 'Lấy đúng danh sách hoặc cắt nhỏ Assessment cho READING/LISTENING';
  } else if (num >= 22 && num <= 29) {
    objective = 'Cập nhật và nhận diện Track Roadmap dựa trên mức điểm';
    input = 'Score của sinh viên: <30, 30-60, >=60';
    expected = 'Mở khóa (Status=1/2/0) đúng dải Track tương ứng';
    notes = 'Core logic phân loại sinh viên';
  } else if (num >= 30 && num <= 38) {
    objective = 'Kiểm thử ngoại lệ, sinh tự động Lesson & Bug phát sinh trong Enrollment';
    input = 'Enrollment parameters và Track Data rỗng/thiếu';
    expected = 'Báo lỗi hoặc vượt qua an toàn mà không crash hệ thống';
  }

  // Tinh chỉnh input dựa vào description nhanh
  if (description.includes('trả lời đúng')) input = 'Tất cả câu trả lời isCorrect=true';
  else if (description.includes('trả lời sai')) input = 'Tất cả câu trả lời isCorrect=false';
  else if (description.includes('NPE') || description.includes('null')) input = 'Payload chứa tham số null';
  else if (description.includes('không tồn tại')) input = 'ID = 999 (không tồn tại)';

  // Build the block
  const javadoc = `    /**\n     * ${tcId}: ${description}\n     * \n     * Test Objective: ${objective}\n     * Input: ${input}\n     * Expected Output: ${expected}\n     * Notes: ${notes}\n     */\n    @Test\n    @DisplayName("${tcId}: ${description}")`;
  
  return javadoc;
});

// Chỉ ghi lại nếu file chưa bị thay đổi trước đó để tránh duplicate
if (content !== newContent && !content.includes('Test Objective: Xác thực logic')) {
  fs.writeFileSync(filePath, newContent, 'utf8');
  console.log(`✅ Đã inject thành công Javadoc metadata cho ${count} test cases.`);
} else {
  console.log('⚠️ File đã chứa Javadoc test hoặc không tìm thấy patterns.');
}
