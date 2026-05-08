# Báo Cáo Unit Test - Chức năng Quản lý Ngân hàng Câu hỏi

**Dự án:** TOEIC Exam Backend (TypeScript)
**Module:** QuestionService + MediaGroupService
**Tổng số Test Case:** 75 (60 Pass, 15 Fail do phát hiện Bug hệ thống)

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
      question.service.ts, media-group.service.ts
    </td>
  </tr>
  <tr>
    <td style="padding:8px; vertical-align:top; font-weight:bold;">Hàm</td>
    <td style="padding:8px;">
      <strong>QuestionService:</strong>
      createQuestion(), getQuestionById(), searchQuestions(), updateQuestion(), deleteQuestion(),
      getQuestionStatistics(), getQuestionsBySection(), performBulkOperation(),
      validateChoices(), validateMediaRequirements(), isValidUrl()
      <br><br>
      <strong>MediaGroupService:</strong>
      getMediaGroupsForBrowsing(), getMediaGroupDetail(), createMediaGroup(),
      updateMediaGroupMetadata(), deleteMediaGroup(), getMediaGroupStatistics(),
      cloneMediaGroup(), addQuestionToGroup(), removeQuestionFromGroup(),
      validateMediaGroupData(), generateDefaultTitle(), createPreviewText()
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


| TestcaseID | Chức năng/use case | Lớp | Phương thức | Mục tiêu kiểm thử | Input (Dữ liệu mock / JSON) | Expected output (JSON / Side Effects) | Kết quả | Ghi chú |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC_01** | Tạo câu hỏi | QuestionService | `createQuestion` | Tạo mới thành công | `data = { QuestionText: 'Q', Media: {...}, Choices: [...] };`<br>`mocks = { qRepo.create() -> { ID: 1 } }` | `expected = { type: 'object', result: { ID: 1 }, sideEffects: { qRepo.create: called } }` | Pass | Happy path |
| **TC_02** | Tạo câu hỏi | QuestionService | `createQuestion` | Xử lý khi thiếu object Media | `data = { QuestionText: 'Q', Choices: [...] }` (Không gửi Media) | `expected = { type: 'throw', error: '/media/i' }` | Fail | **Bug** - Crash (TypeError) do thiếu null-check `media.Skill` |
| **TC_03** | Tạo câu hỏi | QuestionService | `createQuestion` | Ràng buộc đáp án trống | `data = { Choices: [ { Content: '   ' } ] }` | `expected = { type: 'throw', error: 'All choices must have content' }` | Pass | Validation - Đáp án chứa khoảng trắng |
| **TC_04** | Tạo câu hỏi | QuestionService | `createQuestion` | Ràng buộc hình ảnh cho Part 1 | `data = { Media: { Section: '1', ImageUrl: null } }` | `expected = { type: 'throw', error: 'Part 1 questions must have an image' }` | Pass | Validation - Part 1 bắt buộc có hình |
| **TC_05** | Tạo câu hỏi | QuestionService | `createQuestion` | Định dạng URL | `data = { Media: { Skill: 'LISTENING', AudioUrl: 'abc' } }` | `expected = { type: 'throw', error: 'Invalid audio URL format' }` | Pass | Validation - Sai format đường dẫn |
| **TC_06** | Tạo câu hỏi | QuestionService | `createQuestion` | Giới hạn ký tự Câu hỏi (> 1000) | `data = { QuestionText: 'A'.repeat(1005) }` | `expected = { type: 'throw', error: 'Question text exceeds maximum length' }` | Fail | **Bug** - Thiếu MaxLength Validation |
| **TC_07** | Xem chi tiết | QuestionService | `getQuestionById` | Lấy thông tin chi tiết | `id = 1;`<br>`mocks = { findById(1) -> { ID: 1 } }` | `expected = { type: 'object', result: { ID: 1 } }` | Pass | Happy path |
| **TC_08** | Xem chi tiết | QuestionService | `getQuestionById` | ID không tồn tại | `id = 999;`<br>`mocks = { findById(999) -> null }` | `expected = { type: 'throw', error: 'Question not found' }` | Pass | Exception handling |
| **TC_09** | Tìm kiếm | QuestionService | `searchQuestions` | Trả danh sách có phân trang | `params = { Page: 2, Limit: 10 };`<br>`mocks = { findWithFilters() -> { questions: [...], total: 25 } }` | `expected = { result.Pagination: { CurrentPage: 2, TotalPages: 3 } }` | Pass | Happy path |
| **TC_10** | Tìm kiếm | QuestionService | `searchQuestions` | Default pagination | `params = {};`<br>`mocks = { findWithFilters() -> { questions: [], total: 0 } }` | `expected = { result.Pagination: { CurrentPage: 1, Limit: 20 } }` | Pass | Default values |
| **TC_11** | Tìm kiếm | QuestionService | `searchQuestions` | An toàn với Relation null | `mocks = { findWithFilters() -> { questions: [{ mediaQuestion: null }] } }` | `expected = { result.Questions[0].Media.Skill: '' }` | Pass | Exception handling - Safe Map |
| **TC_12** | Cập nhật | QuestionService | `updateQuestion` | Cập nhật nội dung (Partial) | `id = 1, dto = { QuestionText: 'New' };` | `expected = { sideEffects: { update: calledWith(1, { QuestionText: 'New' }, ...) } }` | Pass | Happy path |
| **TC_13** | Cập nhật | QuestionService | `updateQuestion` | Partial Update Media Entity | `id = 1, dto = { Media: { AudioUrl: '/a.mp3' } }` | `expected = { sideEffects: { update: payload.AudioUrl: '/a.mp3', payload.Skill: không phải undefined } }` | Fail | **Bug** - Hardcode thuộc tính gán `undefined` lên các trường không cần cập nhật |
| **TC_14** | Cập nhật | QuestionService | `updateQuestion` | Báo động khi cập nhật câu hỏi cũ | `id = 1;`<br>`mocks = { getUsageStats() -> { usedInExams: 10 } }` | `expected = { sideEffects: { console.warn: called } }` | Pass | Business constraint - In Warning |
| **TC_15** | Cập nhật | QuestionService | `updateQuestion` | ID không tồn tại | `id = 999;`<br>`mocks = { findById(999) -> null }` | `expected = { type: 'throw', error: 'Question not found' }` | Pass | Exception handling |
| **TC_16** | Cập nhật | QuestionService | `updateQuestion` | Lỗi truy vấn Update DB | `id = 1;`<br>`mocks = { update() -> null }` | `expected = { type: 'throw', error: 'Failed to update question' }` | Pass | Exception handling |
| **TC_17** | Xóa | QuestionService | `deleteQuestion` | Xóa thành công | `id = 1;`<br>`mocks = { getUsageStats() -> { usedInExams: 0 } }` | `expected = { type: 'boolean', result: true }` | Pass | Happy path |
| **TC_18** | Xóa | QuestionService | `deleteQuestion` | Chặn xóa câu hỏi đã dùng | `id = 1;`<br>`mocks = { getUsageStats() -> { usedInExams: 1 } }` | `expected = { type: 'throw', error: 'Cannot delete question that is used' }` | Pass | Business constraint |
| **TC_19** | Xóa | QuestionService | `deleteQuestion` | Xử lý an toàn khi usageStats null (Fail-Closed) | `id = 1;`<br>`mocks = { getUsageStats() -> null }` | `expected = { type: 'throw', error: 'Cannot verify usage stats' }` | Fail | **Bug** - Null-check bị bypass, vẫn thực hiện xóa câu hỏi đang được dùng |
| **TC_20** | Bulk Action | QuestionService | `performBulkOperation` | Thực hiện xóa nhiều (Bulk Delete) | `operation = { Operation: 'DELETE', QuestionIDs: [1,2,3] };`<br>`mocks = { bulkDelete() -> 3 }` | `expected = { result: { success: 3, failed: 0 } }` | Pass | Happy path |
| **TC_21** | Bulk Action | QuestionService | `performBulkOperation` | Quản lý Exception khi Bulk Delete thất bại | `operation = { Operation: 'DELETE', QuestionIDs: [1] };`<br>`mocks = { bulkDelete() -> throw Error('Transaction lock') }` | `expected = { result: { success: 0, failed: 1, errors: [...] } }` | Pass | Error handling |
| **TC_22** | Bulk Action | QuestionService | `performBulkOperation` | Trả lỗi thao tác chưa hỗ trợ | `operation = { Operation: 'ADD_TO_EXAM' }` | `expected = { result: { errors: ['handled by ExamService'] } }` | Pass | Not implemented |
| **TC_23** | Bulk Action | QuestionService | `performBulkOperation` | Exception khi danh sách IDs rỗng | `operation = { Operation: 'DELETE', QuestionIDs: undefined }` | `expected = { resolves: { failed: > 0 } }` | Pass | Exception handling |
| **TC_24** | Practice | QuestionService | `getQuestionsBySection` | Lấy danh sách luyện tập | `sections = ['5', '6'], limit = 10;` | `expected = { type: 'object', result: [...] }` | Pass | Happy path |
| **TC_25** | Practice | QuestionService | `getQuestionsBySection` | Báo lỗi rỗng | `sections = []` | `expected = { type: 'throw', error: 'At least one section' }` | Pass | Validation |
| **TC_26** | Thống kê | QuestionService | `getQuestionStatistics` | Lấy thống kê | `id = 1;`<br>`mocks = { getUsageStats() -> { total: 100 } }` | `expected = { type: 'object', result: { total: 100 } }` | Pass | Happy path |

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
      - TypeScript + ts-jest
    </td>
  </tr>
  <tr>
    <td style="padding:8px; vertical-align:top; font-weight:bold; text-align:center;">Prompt</td>
    <td style="padding:8px;">
      "Mock QuestionRepository, MediaQuestionRepository; verify số lần gọi create/update/delete."<br>
      "Thiết kế test cho createQuestion / deleteQuestion: case Media null, usageStats null, câu hỏi đang dùng trong đề thi."<br>
      "Bổ sung test case Fail-Closed: nếu getUsageStats() trả về null thì service phải từ chối xóa thay vì cho phép."<br>
      "Kiểm tra test nào đang trùng lặp giữa question.service.spec.ts và media-group.service.spec.ts, đề xuất xóa."<br>
      "Sinh báo cáo coverage / export Markdown — kiểm tra collectCoverageFrom trong jest.config.js."
    </td>
  </tr>
</table>
