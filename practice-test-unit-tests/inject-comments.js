const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, '../MaiHieu/src/test/java/com/mxhieu/doantotnghiep/PracticeTestFeatureTests.java');
let content = fs.readFileSync(filePath, 'utf8');

const regex = /\s*@Test\s+@DisplayName\("(TC_PTF_\d+):\s*([^"]+)"\)/g;

let count = 0;
const newContent = content.replace(regex, (match, tcId, description) => {
  count++;
  const num = parseInt(tcId.split('_')[2], 10);
  
  let objective = `Kiểm thử đánh giá chất lượng code cho chức năng: ${description}`;
  let input = 'Mock test request payload / mock database records';
  let expected = 'Thực thi đúng luồng Happy Path / Chặn lỗi đúng yêu cầu';
  let notes = 'Luyện đề & Đánh giá';

  if (num <= 21) {
    objective = 'Kiểm tra chức năng Quản lý Bài Thi (Xem danh sách, kiểm tra sao, check lock)';
  } else if (num >= 22) {
    objective = 'Kiểm tra chức năng Nộp bài, Đánh giá kết quả & Xem lịch sử (Test Attempt Service)';
  }

  if (description.toLowerCase().includes('bug') || description.toLowerCase().includes('lỗi')) {
     input = 'Dữ liệu bẩn (Dirty data) hoặc Null Payload (Hacker attack)';
     expected = 'Hệ thống cần throw Exception an toàn (AppException) thay vì đâm crash DB hay RuntimeException';
     notes = 'Phát hiện mã nguồn đang bảo mật lỏng lẻo';
  }

  const javadoc = `    /**\n     * ${tcId}: ${description}\n     * \n     * Test Objective: ${objective}\n     * Input: ${input}\n     * Expected Output: ${expected}\n     * Notes: ${notes}\n     */\n    @Test\n    @DisplayName("${tcId}: ${description}")`;
  
  return javadoc;
});

if (content !== newContent && !content.includes('Test Objective:')) {
  fs.writeFileSync(filePath, newContent, 'utf8');
  console.log(`✅ Đã inject thành công Javadoc metadata cho ${count} test cases vào PTF.`);
} else {
  console.log('⚠️ File đã chứa Javadoc test hoặc không tìm thấy patterns.');
}
