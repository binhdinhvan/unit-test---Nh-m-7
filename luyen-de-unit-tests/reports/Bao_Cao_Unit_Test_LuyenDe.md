# Báo Cáo Unit Test - Chức năng Luyện đề & Kiểm tra Trực tuyến

**Dự án:** TOEIC Exam Backend (TypeScript)
**Module:** ExamService + AttemptService
**Tổng số Test Case:** 59 (48 Pass, 11 Fail do phát hiện Bug hệ thống)

---

## I. Phạm vi Kiểm thử (Test Scope)

<table border="1" style="border-collapse:collapse; width:100%; font-family:Arial, sans-serif; font-size:13px;">
  <tr>
    <th colspan="3" style="background-color:#1f4e79; color:white; text-align:center; padding:8px; font-size:14px;">
      1.1 Tools and Libraries
    </th>
  </tr>
  <tr>
    <td colspan="3" style="padding:8px;">Jest + ts-jest (TypeScript)</td>
  </tr>
  <tr>
    <th colspan="3" style="background-color:#1f4e79; color:white; text-align:center; padding:8px; font-size:14px;">
      1.2 Scope of Testing
    </th>
  </tr>
  <tr>
    <td rowspan="2" style="padding:8px; vertical-align:middle; font-weight:bold; width:22%;">Các thành phần được kiểm thử</td>
    <td style="padding:8px; width:12%; font-weight:bold;">Tệp</td>
    <td style="padding:8px;">
      exam.service.ts, attempt.service.ts
    </td>
  </tr>
  <tr>
    <td style="padding:8px; vertical-align:top; font-weight:bold;">Hàm</td>
    <td style="padding:8px;">
      <strong>ExamService:</strong>
      getAllExams(), getExamById(), searchExams()
      <br><br>
      <strong>AttemptService:</strong>
      startAttempt(), submitAttempt(), getAttemptResults(), getStudentAttempts(), getBestScore(), getProgressStatistics(), deleteAttempt(), calculateTimeElapsed() (private), identifyWeakAreas() (private)
    </td>
  </tr>
  <tr>
    <td style="padding:8px; vertical-align:top; font-weight:bold;">Các thành phần không được kiểm thử</td>
    <td colspan="2" style="padding:8px;">
      <p>
        - Repository<br>
        =&gt; Lý do: Repositories chịu trách nhiệm giao tiếp trực tiếp với cơ sở dữ liệu vật lý (MySQL/TypeORM).
        Nguyên tắc của Unit Test là phải chạy nhanh và cô lập, không chạm vào database thực để tránh việc thay đổi dữ liệu
        hoặc gây lỗi "Domino" nếu DB rớt mạng. Toàn bộ Repository được thay thế bằng <code>jest.mock()</code>.
      </p>
      <p>
        - Controller<br>
        =&gt; Lý do: Controller chỉ đóng vai trò như "Người đưa thư", nhận HTTP Request (phân giải URL, Header)
        và trả về HTTP status format (200, 400). Hoàn toàn không có phép toán nghiệp vụ nào bên trong nó.
      </p>
      <p>
        - Entity và DTO<br>
        =&gt; Lý do: Đây chỉ là cấu trúc khai báo dữ liệu thuần túy (Plain Old Data Objects) không chứa behavior
        xử lý logic, nên không có giá trị/nguy cơ lỗi để cần thiết chèn Unit Test.
      </p>
    </td>
  </tr>
</table>

---

## II. Danh sách Test Case

**Quy ước kết quả:** Trong phạm vi báo cáo này, các testcase có trạng thái **Fail** là các testcase được viết theo hành vi đúng kỳ vọng của hệ thống nhưng service hiện tại chưa đáp ứng. Vì vậy, **Fail** được dùng để đánh dấu bug/defect đã phát hiện, không phải lỗi do viết sai unit test.

| Testcase | Chức năng | Service | Hàm | Mục tiêu kiểm thử | Input / Mock | Expected | Kết quả | Ghi chú |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **TC_EX_001** | Xem danh sách | ExamService | `getAllExams` | Lấy tất cả đề thi không lọc | `filters = undefined;`<br>`mocks = { findAll() -> [3 items] }` | `expected = { type: 'object', result: { length: 3 } }` | Pass | Kiểm tra luồng chuẩn (Happy path) |
| **TC_EX_002** | Xem danh sách | ExamService | `getAllExams` | Lọc chỉ lấy FULL_TEST | `filters = { Type: 'FULL_TEST' };`<br>`mocks = { findAll() -> [2 items] }` | `expected = { type: 'object', result: { length: 2 } }` | Pass | Kiểm tra luồng chuẩn (Happy path) |
| **TC_EX_003** | Xem danh sách | ExamService | `getAllExams` | Lọc theo ExamTypeID | `filters = { ExamTypeID: 2 };`<br>`mocks = { findAll() -> [...] }` | `expected = { sideEffects: { findAll: calledWith({ExamTypeID: 2}) } }` | Pass | Kiểm tra luồng chuẩn (Happy path) |
| **TC_EX_004** | Xem danh sách | ExamService | `getAllExams` | Không có đề thi nào | `filters = undefined;`<br>`mocks = { findAll() -> [] }` | `expected = { type: 'object', result: [] }` | Pass | Xử lý trường hợp biên (Edge case) khi DB rỗng |
| **TC_EX_005** | Xem chi tiết | ExamService | `getExamById` | Lấy chi tiết thành công | `id = 1;`<br>`mocks = { findById(1) -> ExamDetail }` | `expected = { type: 'object', result: { ID: 1, TimeExam: 120 } }` | Pass | Kiểm tra luồng chuẩn (Happy path) |
| **TC_EX_006** | Xem chi tiết | ExamService | `getExamById` | IsCorrect bị ẩn | `id = 1;`<br>`mocks = { findById(1) -> ExamDetail }` | `expected = { result.Questions[0].Choices: omit(['IsCorrect']) } }` | Pass | Ràng buộc bảo mật dữ liệu (Data Security) |
| **TC_EX_007** | Xem chi tiết | ExamService | `getExamById` | Questions sắp xếp theo OrderIndex | `id = 1;`<br>`mocks = { findById() -> { examQuestions: [...] /* randomized */ } }` | `expected = { result.Questions[0].OrderIndex: 1 }` | Pass | Kiểm tra logic sắp xếp (Sorting logic) |
| **TC_EX_008** | Xem chi tiết | ExamService | `getExamById` | examQuestions=undefined | `id = 1;`<br>`mocks = { findById() -> { examQuestions: undefined } }` | `expected = { type: 'object', result: { Questions: [] } }` | Pass | Xử lý ngoại lệ an toàn khi dữ liệu undefined |
| **TC_EX_009** | Xem chi tiết | ExamService | `getExamById` | Đề không tồn tại | `id = 999;`<br>`mocks = { findById(999) -> null }` | `expected = { type: 'throw', error: 'Exam not found' }` | Pass | Xử lý ngoại lệ (Exception handling) khi ID sai |
| **TC_EX_010** | Xem chi tiết | ExamService | `getExamById` | examQuestions=[] (mảng rỗng) | `id = 1;`<br>`mocks = { findById(1) -> { examQuestions: [] } }` | `expected = { type: 'object', result: { Questions: [] } }` | Pass | Xử lý trường hợp biên (Edge case) khi đề chưa có câu hỏi |
| **TC_EX_011** | Xem chi tiết | ExamService | `getExamById` | Nhiều trường nullable bị null | `id = 1;`<br>`mocks = { findById(1) -> { Type: null, examType: { Description: null }, examQuestions: [{ question: { QuestionText: null, mediaQuestion: { Section: null }, choices: [{ Attribute: null, Content: null }] } }] } }` | `expected = { result.Type: '', result.Questions[0].QuestionText: '' }` | Pass | Kiểm tra nhánh falsy fallback trả về chuỗi rỗng |
| **TC_EX_012** | Xem chi tiết | ExamService | `getExamById` | examQuestions=null (ternary path) | `id = 1;`<br>`mocks = { findById(1) -> { examQuestions: null } }` | `expected = { type: 'object', result: { Questions: [] } }` | Pass | Kiểm tra nhánh false của toán tử ternary `? :` |
| **TC_EX_013** | Xem chi tiết | ExamService | `getExamById` | NPE khi mediaQuestion=null | `id = 1;`<br>`mocks = { findById(1) -> { examQuestions: [{ question: { mediaQuestion: null } }] } }` | `expected = { type: 'throw', error: 'Invalid question data' }` | Fail | **Bug** - Ứng dụng crash (TypeError) do truy cập `mediaQuestion.ID` mà không kiểm tra null |
| **TC_EX_014** | Tìm kiếm | ExamService | `searchExams` | Tìm theo từ khóa "ETS" | `term = 'ETS';`<br>`mocks = { searchByTitle('ETS') -> [2 items] }` | `expected = { type: 'object', result: { length: 2 } }` | Pass | Kiểm tra luồng chuẩn (Happy path) |
| **TC_EX_015** | Tìm kiếm | ExamService | `searchExams` | Từ khóa không tìm thấy | `term = 'XYZ_NOT_EXIST';`<br>`mocks = { searchByTitle('XYZ_NOT_EXIST') -> [] }` | `expected = { type: 'object', result: [] }` | Pass | Xử lý trường hợp biên (Edge case) |
| **TC_EX_016** | Tìm kiếm | ExamService | `searchExams` | Từ khóa rỗng | `term = '';`<br>`mocks = { none (validation failed) }` | `expected = { type: 'throw', error: 'Search term cannot be empty' }` | Pass | Kiểm tra ràng buộc đầu vào (Validation) |
| **TC_EX_017** | Tìm kiếm | ExamService | `searchExams` | Từ khóa chỉ khoảng trắng | `term = '   ';`<br>`mocks = { none (validation failed) }` | `expected = { type: 'throw', error: 'Search term cannot be empty' }` | Pass | Kiểm tra ràng buộc đầu vào (Validation) |
| **TC_EX_018** | Tìm kiếm | ExamService | `searchExams` | Từ khóa có ký tự đặc biệt | `term = 'ETS%2023';`<br>`mocks = { searchByTitle('ETS%2023') -> [] }` | `expected = { sideEffects: { searchByTitle: calledWith('ETS%2023') } }` | Pass | Kiểm tra truyền nguyên ký tự đặc biệt xuống repo (Passthrough) |
| **TC_ATT_001** | Bắt đầu làm bài | AttemptService | `startAttempt` | FULL_TEST thành công | `dto = { ExamID: 1, Type: 'FULL_TEST' };`<br>`mocks = { create() -> { ID: 99 } }` | `expected = { type: 'object', result: { ID: 99, SubmittedAt: null } }` | Pass | Kiểm tra luồng chuẩn (Happy path) |
| **TC_ATT_002** | Bắt đầu làm bài | AttemptService | `startAttempt` | PRACTICE_BY_PART thành công | `dto = { Parts: [5,6,7] };`<br>`mocks = { create() -> { ID: 99 } }` | `expected = { type: 'object', result: { Type: 'PRACTICE_BY_PART' } }` | Pass | Kiểm tra luồng chuẩn (Happy path) |
| **TC_ATT_003** | Bắt đầu làm bài | AttemptService | `startAttempt` | Exam không tồn tại | `dto = { ExamID: 999 };`<br>`mocks = { findById(999) -> null }` | `expected = { type: 'throw', error: 'Exam not found' }` | Pass | Xử lý ngoại lệ (Exception handling) khi ID sai |
| **TC_ATT_004** | Bắt đầu làm bài | AttemptService | `startAttempt` | PRACTICE_BY_PART thiếu Parts | `dto = { Type: 'PRACTICE_BY_PART', Parts: [] };` | `expected = { type: 'throw', error: 'Parts must be specified' }` | Pass | Kiểm tra ràng buộc đầu vào (Validation) |
| **TC_ATT_005** | Bắt đầu làm bài | AttemptService | `startAttempt` | Parts ngoài phạm vi | `dto = { Parts: [0,8] };` | `expected = { type: 'throw', error: 'Invalid part numbers' }` | Pass | Kiểm tra ràng buộc đầu vào (Validation) |
| **TC_ATT_006** | Bắt đầu làm bài | AttemptService | `startAttempt` | Thiếu validate loại hình | `dto = { Type: null };`<br>`mocks = { create() -> success }` | `expected = { type: 'throw', error: 'Invalid exam type' }` | Fail | **Bug** - Cho phép lưu data rác do thiếu validation |
| **TC_ATT_007** | Bắt đầu làm bài | AttemptService | `startAttempt` | Thiếu validate StudentID | `studentId = -1;`<br>`mocks = { create() -> success }` | `expected = { type: 'throw', error: 'Invalid student profile ID' }` | Fail | **Bug** - Hệ thống không bắt lỗi ID âm |
| **TC_ATT_008** | Nộp bài | AttemptService | `submitAttempt` | Nộp bài thành công | `dto = { AttemptID: 1, answers: [] };`<br>`mocks = { findById(1) -> Attempt }` | `expected = { type: 'object', result: { TotalScore: 525 } }` | Pass | Kiểm tra luồng chuẩn (Happy path) |
| **TC_ATT_009** | Nộp bài | AttemptService | `submitAttempt` | Chấm đúng Nghe/Đọc | `dto = { AttemptID: 1 };`<br>`mocks = { attemptAnswers: [...] }` | `expected = { result.Analysis: { ListeningCorrect: 1, ReadingCorrect: 0 } }` | Pass | Kiểm tra thuật toán chấm điểm (Business logic) |
| **TC_ATT_010** | Nộp bài | AttemptService | `submitAttempt` | WeakAreas rỗng khi làm đúng 100% | `mocks = { attemptAnswers: [{ IsCorrect: true }] }` | `expected = { result.Analysis.WeakAreas: [] }` | Pass | Xử lý trường hợp biên (Edge case) |
| **TC_ATT_011** | Nộp bài | AttemptService | `submitAttempt` | Attempt không tồn tại | `dto = { AttemptID: 999 };`<br>`mocks = { findById(999) -> null }` | `expected = { type: 'throw', error: 'Attempt not found' }` | Pass | Xử lý ngoại lệ (Exception handling) khi ID sai |
| **TC_ATT_012** | Nộp bài | AttemptService | `submitAttempt` | Nộp bài người khác | `studentId = 1;`<br>`mocks = { attempt.StudentProfileID: 2 }` | `expected = { type: 'throw', error: 'You can only submit your own attempts' }` | Pass | Ràng buộc bảo mật (Security) |
| **TC_ATT_013** | Nộp bài | AttemptService | `submitAttempt` | Nộp bài đã nộp (double submit) | `dto = { AttemptID: 1 };`<br>`mocks = { attempt.SubmittedAt: new Date() }` | `expected = { type: 'throw', error: 'This attempt has already been submitted' }` | Pass | Ràng buộc nghiệp vụ (Business constraint) |
| **TC_ATT_014** | Nộp bài | AttemptService | `submitAttempt` | Hết thời gian làm bài | `dto = { AttemptID: 1 };`<br>`mocks = { StartedAt: new Date(Date.now() - 5 * 3600000) }` | `expected = { type: 'throw', error: 'Time limit exceeded' }` | Pass | Ràng buộc nghiệp vụ (Business constraint) |
| **TC_ATT_015** | Nộp bài | AttemptService | `submitAttempt` | Bị lỗi khi liên kết Exam mất | `dto = { AttemptID: 1 };`<br>`mocks = { attempt.exam: null }` | `expected = { type: 'throw', error: 'Exam no longer exists' }` | Fail | **Bug** - Ứng dụng crash (TypeError) tại dòng 155 |
| **TC_ATT_016** | Nộp bài | AttemptService | `submitAttempt` | Cho phép nộp đè bài cũ | `dto = { AttemptID: 1 };`<br>`mocks = { findById(1) -> { SubmittedAt: '' } }` | `expected = { type: 'throw', error: 'This attempt has already been submitted' }` | Fail | **Bug** - Kiểm tra chuỗi rỗng (falsy) không ngăn được nộp bài lần 2 |
| **TC_ATT_017** | Nộp bài | AttemptService | `submitAttempt` | Crash khi câu hỏi mất media | `dto = { AttemptID: 1 };`<br>`mocks = { mediaQuestion: null }` | `expected = { type: 'throw', error: 'Invalid question data' }` | Fail | **Bug** - Ứng dụng crash (TypeError) tại dòng 385 |
| **TC_ATT_018** | Nộp bài | AttemptService | `submitAttempt` | Trả về ID 0 giả mạo | `mocks = { attemptAnswers: [{ IsCorrect: false }] }` | `expected = { type: 'throw', error: 'Invalid question configuration' }` | Fail | **Bug** - Lỗi cấu hình rơi vào trạng thái fallback value 0 |
| **TC_ATT_019** | Nộp bài | AttemptService | `identifyWeakAreas` | Crash thống kê điểm yếu | `mocks = { mediaQuestion: null }` | `expected = { resolves: void }` | Fail | **Bug** - Ứng dụng crash (TypeError) tại dòng 477 |
| **TC_ATT_020** | Xem kết quả | AttemptService | `getAttemptResults` | Xem kết quả bài đã nộp | `id = 1;`<br>`mocks = { findById(1) -> Attempt /* submitted */ }` | `expected = { type: 'object', result: { TotalScore: 525 } }` | Pass | Kiểm tra luồng chuẩn (Happy path) |
| **TC_ATT_021** | Xem kết quả | AttemptService | `getAttemptResults` | Đủ thông tin chi tiết cho modal | `id = 1;`<br>`mocks = { findById(1) -> Attempt }` | `expected = { result.DetailedAnswers[0].Choices: [all choices] }` | Fail | **Bug** - Response chỉ trả `StudentChoice` và `CorrectChoice`, thiếu danh sách `Choices` nên modal chi tiết không hiển thị đủ đáp án |
| **TC_ATT_022** | Xem kết quả | AttemptService | `getAttemptResults` | ID không tồn tại | `id = 999;`<br>`mocks = { findById(999) -> null }` | `expected = { type: 'throw', error: 'Attempt not found' }` | Pass | Xử lý ngoại lệ (Exception handling) khi ID sai |
| **TC_ATT_023** | Xem kết quả | AttemptService | `getAttemptResults` | Xem bài người khác | `studentId = 1;`<br>`mocks = { attempt.StudentProfileID: 2 }` | `expected = { type: 'throw', error: 'You can only view your own attempt results' }` | Pass | Ràng buộc bảo mật (Security) |
| **TC_ATT_024** | Xem kết quả | AttemptService | `getAttemptResults` | Bài chưa nộp | `id = 1;`<br>`mocks = { attempt.SubmittedAt: null }` | `expected = { type: 'throw', error: 'This attempt has not been submitted yet' }` | Pass | Ràng buộc nghiệp vụ (Business constraint) |
| **TC_ATT_025** | Xem kết quả | AttemptService | `getAttemptResults` | Điểm số null | `mocks = { ScoreListening: null, ScoreReading: null, ScorePercent: null }` | `expected = { result.Scores.TotalScore: 0 }` | Pass | Kiểm tra nhánh fallback điểm số null về 0, tránh trả về `NaN` |
| **TC_ATT_026** | Xem kết quả | AttemptService | `getAttemptResults` | attemptAnswers không được load | `mocks = { attemptAnswers: undefined }` | `expected = { Analysis.TotalQuestions: 0, DetailedAnswers: [] }` | Pass | Kiểm tra optional chaining khi relation câu trả lời bị thiếu |
| **TC_ATT_027** | Lịch sử | AttemptService | `getStudentAttempts` | Lấy tất cả lịch sử | `studentId = 1;`<br>`mocks = { findByStudentId(1) -> [2 items] }` | `expected = { type: 'object', result: { length: 2 } }` | Pass | Kiểm tra luồng chuẩn (Happy path) |
| **TC_ATT_028** | Lịch sử | AttemptService | `getStudentAttempts` | Lọc theo loại đề | `filters = { Type: 'FULL_TEST', SubmittedOnly: true };`<br>`mocks = { findByStudentId() -> [1 item] }` | `expected = { type: 'object', result: { length: 1 } }` | Pass | Kiểm tra luồng chuẩn (Happy path) |
| **TC_ATT_029** | Lịch sử | AttemptService | `getStudentAttempts` | Học viên chưa có lịch sử | `studentId = 1;`<br>`mocks = { findByStudentId() -> [] }` | `expected = { type: 'object', result: [] }` | Pass | Xử lý trường hợp biên (Edge case) khi DB rỗng |
| **TC_ATT_030** | Điểm cao nhất | AttemptService | `getBestScore` | Trả về kỷ lục | `studentId = 1, examId = 1;`<br>`mocks = { getBestScore() -> { ScorePercent: 95 } }` | `expected = { type: 'object', result: { ScorePercent: 95 } }` | Pass | Kiểm tra luồng chuẩn (Happy path) |
| **TC_ATT_031** | Điểm cao nhất | AttemptService | `getBestScore` | Chưa có kỷ lục | `studentId = 1, examId = 1;`<br>`mocks = { getBestScore() -> null }` | `expected = { type: 'object', result: null }` | Pass | Xử lý trường hợp biên (Edge case) |
| **TC_ATT_032** | Thống kê | AttemptService | `getProgressStatistics` | Trả về thống kê tiến độ | `studentId = 1;`<br>`mocks = { getProgressStats() -> { totalAttempts: 10 } }` | `expected = { type: 'object', result: { totalAttempts: 10 } }` | Pass | Kiểm tra luồng chuẩn (Happy path) |
| **TC_ATT_033** | Thống kê | AttemptService | `getProgressStatistics` | Giá trị rỗng | `studentId = 1;`<br>`mocks = { getProgressStats() -> { totalAttempts: 0 } }` | `expected = { type: 'object', result: { totalAttempts: 0 } }` | Pass | Xử lý trường hợp biên (Edge case) |
| **TC_ATT_034** | Thoát bài | AttemptService | `deleteAttempt` | Xóa bài làm dở | `id = 1;`<br>`mocks = { attempt.SubmittedAt: null }` | `expected = { type: 'boolean', result: true }` | Pass | Kiểm tra luồng chuẩn (Happy path) |
| **TC_ATT_035** | Thoát bài | AttemptService | `deleteAttempt` | Attempt không tồn tại | `id = 999;`<br>`mocks = { findById(999) -> null }` | `expected = { type: 'throw', error: 'Attempt not found' }` | Pass | Xử lý ngoại lệ (Exception handling) khi ID sai |
| **TC_ATT_036** | Thoát bài | AttemptService | `deleteAttempt` | Xóa bài của người khác | `studentId = 1;`<br>`mocks = { attempt.StudentProfileID: 2 }` | `expected = { type: 'throw', error: 'You can only delete your own attempts' }` | Pass | Ràng buộc bảo mật (Security) |
| **TC_ATT_037** | Thoát bài | AttemptService | `deleteAttempt` | Xóa mất lịch sử bài thi | `id = 1;`<br>`mocks = { attempt.SubmittedAt: new Date() }` | `expected = { type: 'throw', error: 'Cannot delete a submitted attempt' }` | Fail | **Bug** - Bypass luồng chặn xóa bài đã nộp |
| **TC_ATT_038** | Tính giờ | AttemptService | `calculateTimeElapsed` | Tính số phút chính xác | <code style="white-space:nowrap;">start = new Date('2024-01-01T08:30:00Z');</code><br><code style="white-space:nowrap;">end = new Date('2024-01-01T10:00:00Z');</code> | `expected = { type: 'number', result: 90 }` | Pass | Kiểm tra hàm tiện ích (Utility) |
| **TC_ATT_039** | Tính giờ | AttemptService | `calculateTimeElapsed` | Khác mốc giờ | <code style="white-space:nowrap;">start = new Date('2024-01-01T08:00:00Z');</code><br><code style="white-space:nowrap;">end = new Date('2024-01-01T10:00:00Z');</code> | `expected = { type: 'number', result: 120 }` | Pass | Kiểm tra hàm tiện ích (Utility) |
| **TC_ATT_040** | Tính giờ | AttemptService | `calculateTimeElapsed` | Nộp ngay lập tức | <code style="white-space:nowrap;">start = new Date();</code><br><code style="white-space:nowrap;">end = start;</code> | `expected = { type: 'number', result: 0 }` | Pass | Xử lý trường hợp biên (Edge case) |
| **TC_ATT_041** | Tính giờ | AttemptService | `calculateTimeElapsed` | Lỗi Clock Skew | <code style="white-space:nowrap;">start = new Date('2099-01-01');</code><br><code style="white-space:nowrap;">end = new Date();</code> | `expected = { type: 'throw', error: 'Invalid time detected' }` | Fail | **Bug** - Không có validation ngăn chặn thời gian nộp bài nhỏ hơn thời gian bắt đầu |

---

## III. Tài liệu tham khảo & Danh sách Prompt đã dùng

<table border="1" style="border-collapse:collapse; width:100%; font-family:Arial, sans-serif; font-size:13px;">
  <tr>
    <th colspan="2" style="background-color:#1f4e79; color:white; text-align:center; padding:8px; font-size:14px;">
      1.7 Tài liệu tham khảo + danh sách các prompt đã dùng
    </th>
  </tr>
  <tr>
    <td style="padding:8px; vertical-align:top; font-weight:bold; width:20%; text-align:center;">Tài liệu</td>
    <td style="padding:8px;">
      - Jest Mock Functions: https://jestjs.io/docs/mock-function-api<br>
      - Jest jest.mock / manual mocks: https://jestjs.io/docs/es6-class-mocks<br>
      - TypeScript + ts-jest: https://kulshekhar.github.io/ts-jest/
    </td>
  </tr>
  <tr>
    <td style="padding:8px; vertical-align:top; font-weight:bold; text-align:center;">Prompt</td>
    <td style="padding:8px;">
      "Mock AttemptRepository và ExamRepository; viết test cho startAttempt với FULL_TEST và PRACTICE_BY_PART."<br>
      "Thiết kế test submitAttempt: kiểm tra timing, security (student chỉ nộp bài mình), double-submit, time limit exceeded."<br>
      "Thêm test [DEFECT] để phát hiện NPE khi exam=null, mediaQuestion=null, SubmittedAt falsy."<br>
      "Sử dụng Strong Assertions cho các Test bắt Bug, định hình expectation là phải bắn ra Error."<br>
      "Viết test cho calculateTimeElapsed (private method) bằng cách gọi qua (service as any)."<br>
      "Cập nhật jest.config.js collectCoverageFrom để include exam.service.ts và attempt.service.ts."
    </td>
  </tr>
</table>
