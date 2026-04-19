const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, '../MaiHieu/src/test/java/com/mxhieu/doantotnghiep/DictionaryFeatureTests.java');
let content = fs.readFileSync(filePath, 'utf8');

const regex = /\s*@Test\s+@DisplayName\("(DICT_\d+_TC([^"]*))"\)/g;

let count = 0;
const newContent = content.replace(regex, (match, fullStr) => {
  count++;
  
  const hasColon = fullStr.includes(':');
  const tcId = hasColon ? fullStr.split(':')[0].trim() : fullStr;
  const description = hasColon ? fullStr.substring(fullStr.indexOf(':') + 1).trim() : fullStr;
  
  let numStr = tcId.match(/DICT_(\d+)_TC/);
  const num = numStr ? parseInt(numStr[1], 10) : count;

  let objective = `Kiểm thử đánh giá chất lượng hệ thống cho hành vi: ${description}`;
  let input = 'Tham số hoặc payload giả lập (Mocked Request/Data)';
  let expected = 'Thực thi theo luồng chức năng tự điển đã đề ra';
  let notes = 'Dictionary / Flashcard Module';

  if (description.toLowerCase().includes('bug') || description.toLowerCase().includes('throw') || description.toLowerCase().includes('empty')) {
     input = 'Dữ liệu không toàn vẹn từ API WebClient (Node lỗi, thiếu Data)';
     expected = 'Phải được chặn an toàn tránh Crash';
     notes = 'Phát hiện lỗ hổng Crawl Dictionary API';
  } else if (num >= 7) {
     objective = 'Mô đun Từ vựng cá nhân (Student Dictionary)';
  }

  const javadoc = `    /**\n     * ${fullStr}: ${description}\n     * \n     * Test Objective: ${objective}\n     * Input: ${input}\n     * Expected Output: ${expected}\n     * Notes: ${notes}\n     */\n    @Test\n    @DisplayName("${fullStr}")`;
  
  return javadoc;
});

if (content !== newContent && !content.includes('Test Objective:')) {
  fs.writeFileSync(filePath, newContent, 'utf8');
  console.log(`✅ Đã inject thành công Javadoc cho ${count} test cases vào DICT.`);
} else {
  console.log('⚠️ File đã chứa Javadoc test hoặc không tìm thấy patterns.');
}
