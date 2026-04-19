const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, '../MaiHieu/src/test/java/com/mxhieu/doantotnghiep/StudyPlanLearningFeatureTests.java');
let content = fs.readFileSync(filePath, 'utf8');

const regex = /\s*@Test\s+@DisplayName\("(TC_SPL_\d+):\s*([^"]+)"\)/g;

let count = 0;
const newContent = content.replace(regex, (match, tcId, description) => {
  count++;
  const num = parseInt(tcId.split('_')[2], 10);
  
  let objective = `Xác thực logic chức năng ${description}`;
  let input = 'Mock Data hợp lệ hoặc lỗi tùy chỉnh';
  let expected = 'Kết quả theo Description mong đợi';
  let notes = 'Happy path';

  if (num <= 24) {
    objective = 'Kiểm tra chức năng hiển thị và thuật toán thiết lập lịch học';
  } else if (num >= 25 && num <= 31) {
    objective = 'Kiểm tra thuật toán lưu tiến độ khóa học và mở khóa tiến trình (Domino)';
  } else if (num >= 32 && num <= 38) {
    objective = 'Kiểm tra điều hướng bài giảng (Next/Previous) và chấm sao';
  } else if (num >= 39) {
    objective = 'Kiểm tra chấm bài thi TOEIC liên kết lộ trình học';
  }

  // Customize theo fail cases / bug name
  if (description.toLowerCase().includes('lỗi') || description.toLowerCase().includes('bug') || description.toLowerCase().includes('ném')) {
     input = 'Data gây crash hoặc vi phạm validation logic';
     expected = 'Hệ thống khống chế an toàn và ném Exception thay vì Crash DB';
     notes = 'Kiểm thử hộp đen chặn Bug';
  }

  const javadoc = `    /**\n     * ${tcId}: ${description}\n     * \n     * Test Objective: ${objective}\n     * Input: ${input}\n     * Expected Output: ${expected}\n     * Notes: ${notes}\n     */\n    @Test @DisplayName("${tcId}: ${description}")`;
  
  return javadoc;
});

if (content !== newContent && !content.includes('Test Objective:')) {
  fs.writeFileSync(filePath, newContent, 'utf8');
  console.log(`✅ Đã inject thành công Javadoc metadata cho ${count} test cases vào SPL.`);
} else {
  console.log('⚠️ File đã chứa Javadoc test hoặc không tìm thấy patterns.');
}
