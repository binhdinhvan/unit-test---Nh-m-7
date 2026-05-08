# Báo Cáo Unit Test - Chức năng Quản lý Ngân hàng Câu hỏi

**Dự án:** TOEIC Exam Backend (TypeScript)
**Module:** QuestionService + MediaGroupService
**Độ phủ mã nguồn (Coverage):** ~99% Statements & Lines (media-group.service.ts)
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
| **TC_01** | Duyệt danh sách | MediaGroupService | `getMediaGroupsForBrowsing` | Lấy danh sách nhóm câu hỏi thành công | `params = undefined;`<br>`mocks = { findWithFilters() -> { mediaQuestions: [ { ID: 1, GroupTitle: 'Office Talk', AudioUrl: '...', ImageUrl: null } ], total: 1 }, getUsageStats() -> { questionCount: 2, usedInExams: 0, totalAttempts: 0 } }` | `expected = { type: 'object', result: { groups: [ { Title: 'Office Talk', HasAudio: true, HasImage: false } ], total: 1, pagination: { CurrentPage: 1, TotalPages: 1, Limit: 20 } } }` | Pass | Happy path - Lấy danh sách mặc định với mapping đúng định dạng UI |
| **TC_02** | Duyệt danh sách | MediaGroupService | `getMediaGroupsForBrowsing` | Cắt ngắn chuỗi PreviewText khi vượt giới hạn | `params = undefined;`<br>`mocks = { findWithFilters() -> { mediaQuestions: [ { ID: 1, questions: [ { QuestionText: 'A'.repeat(105) } ] } ] } }` | `expected = { result.groups[0].PreviewText: 'A'.repeat(100) + '...' }` | Pass | Boundary value - Kiểm tra độ dài PreviewText tại mốc 100 ký tự |
| **TC_03** | Duyệt danh sách | MediaGroupService | `getMediaGroupsForBrowsing` | Xử lý khi danh sách trống | `params = undefined;`<br>`mocks = { findWithFilters() -> { mediaQuestions: [], total: 0 } }` | `expected = { type: 'object', result: { groups: [], total: 0 } }` | Pass | Edge case - DB không có dữ liệu nào trả về |
| **TC_04** | Duyệt danh sách | MediaGroupService | `getMediaGroupsForBrowsing` | Áp dụng phân trang mặc định | `params = undefined;`<br>`mocks = { findWithFilters() -> { mediaQuestions: [], total: 100 } }` | `expected = { result.pagination: { CurrentPage: 1, Limit: 20, TotalPages: 5 } }` | Pass | Default values - Khách hàng không cung cấp tham số phân trang |
| **TC_05** | Duyệt danh sách | MediaGroupService | `getMediaGroupsForBrowsing` | Tính toán số trang (TotalPages) lẻ | `params = { Page: 2, Limit: 10 };`<br>`mocks = { findWithFilters() -> { mediaQuestions: [], total: 33 } }` | `expected = { result.pagination.TotalPages: 4 }` | Pass | Logic check - Phép tính làm tròn lên (Math.ceil) cho trang lẻ |
| **TC_06** | Duyệt danh sách | MediaGroupService | `getMediaGroupsForBrowsing` | Fallback Title khi DB trả null | `params = undefined;`<br>`mocks = { findWithFilters() -> { mediaQuestions: [ { GroupTitle: null, Type: 'CONVERSATION', Section: '3' } ] } }` | `expected = { result.groups[0].Title: 'CONVERSATION - Part 3' }` | Pass | Fallback data - Xử lý dữ liệu rác/cũ từ DB |
| **TC_07** | Duyệt danh sách | MediaGroupService | `getMediaGroupsForBrowsing` | Fallback Tags/Difficulty khi null | `params = undefined;`<br>`mocks = { findWithFilters() -> { mediaQuestions: [ { Tags: null, Difficulty: null } ] } }` | `expected = { result.groups[0].Tags: [], result.groups[0].Difficulty: 'MEDIUM' }` | Pass | Fallback data - Chống lỗi undefined khi map dữ liệu mảng |
| **TC_08** | Duyệt danh sách | MediaGroupService | `getMediaGroupsForBrowsing` | Xử lý an toàn khi mảng questions bị null | `params = undefined;`<br>`mocks = { findWithFilters() -> { mediaQuestions: [ { questions: null } ] }, qRepo.count() -> 0 }` | `expected = { result.groups[0].QuestionCount: 0 }` | Pass | Exception handling - Relation query bị hỏng, questions không được include |
| **TC_09** | Xem chi tiết | MediaGroupService | `getMediaGroupDetail` | Lấy chi tiết và sắp xếp câu hỏi | `id = 1;`<br>`mocks = { findById(1) -> { ID: 1, questions: [ { ID: 11, OrderInGroup: 2 }, { ID: 10, OrderInGroup: 1 } ] } }` | `expected = { result.Questions[0].OrderInGroup: 1, result.Questions[1].OrderInGroup: 2 }` | Pass | Happy path - Sắp xếp câu hỏi tăng dần trước khi trả về |
| **TC_10** | Xem chi tiết | MediaGroupService | `getMediaGroupDetail` | Xử lý ID không tồn tại | `id = 999;`<br>`mocks = { findById(999) -> null }` | `expected = { type: 'throw', error: 'Media group not found' }` | Pass | Exception handling - ID ảo hoặc đã bị xóa |
| **TC_11** | Xem chi tiết | MediaGroupService | `getMediaGroupDetail` | Xử lý questions relation bị undefined | `id = 1;`<br>`mocks = { findById(1) -> { ID: 1, questions: undefined } }` | `expected = { result.Questions: [] }` | Pass | Edge case - DB query bị thiếu left join |
| **TC_12** | Xem chi tiết | MediaGroupService | `getMediaGroupDetail` | Xử lý choices relation bị undefined | `id = 1;`<br>`mocks = { findById(1) -> { ID: 1, questions: [ { choices: undefined } ] } }` | `expected = { result.Questions[0].Choices: [] }` | Pass | Edge case - Lỗi relation nested query cho choices |
| **TC_13** | Xem chi tiết | MediaGroupService | `getMediaGroupDetail` | Đảm bảo Immutability của mảng DB | `id = 1;`<br>`mocks = { findById(1) -> { ID: 1, questions: [ { ID: 11 }, { ID: 10 } ] } }` | `expected = { sideEffects: { originArray.mutated: false } }` | Fail | **Bug** - Sử dụng `.sort()` trực tiếp làm thay đổi vĩnh viễn reference array gốc |
| **TC_14** | Tạo mới nhóm | MediaGroupService | `createMediaGroup` | Tạo nhóm và câu hỏi thành công | `data = { Title: 'New', Media: {...}, Questions: [...] }, userId = 1;`<br>`mocks = { mRepo.create() -> { ID: 5 }, qRepo.createMultiple() -> [...] }` | `expected = { type: 'object', result: { MediaQuestionID: 5 } }`<br>`sideEffects: { mRepo.create: called, qRepo.createMultipleForMedia: called }` | Pass | Happy path - Luồng tạo mới 2 bảng hoàn chỉnh |
| **TC_15** | Tạo mới nhóm | MediaGroupService | `createMediaGroup` | Ràng buộc số lượng câu hỏi | `data = { Title: 'New', Questions: [] }, userId = 1;` | `expected = { type: 'throw', error: 'Must provide at least one question' }` | Pass | Validation - Bắt buộc mảng câu hỏi không rỗng |
| **TC_16** | Tạo mới nhóm | MediaGroupService | `createMediaGroup` | Ràng buộc OrderInGroup unique | `data = { Questions: [ { OrderInGroup: 1 }, { OrderInGroup: 1 } ] }` | `expected = { type: 'throw', error: 'OrderInGroup must be unique' }` | Pass | Validation - Thứ tự câu hỏi bị trùng lặp |
| **TC_17** | Tạo mới nhóm | MediaGroupService | `createMediaGroup` | Ràng buộc số lượng đáp án | `data = { Questions: [ { Choices: [ { Content: 'A' } ] } ] }` | `expected = { type: 'throw', error: 'must have at least 2 choices' }` | Pass | Validation - Ít nhất 2 đáp án (trắc nghiệm) |
| **TC_18** | Tạo mới nhóm | MediaGroupService | `createMediaGroup` | Ràng buộc phải có đáp án đúng | `data = { Questions: [ { Choices: [ { IsCorrect: false }, { IsCorrect: false } ] } ] }` | `expected = { type: 'throw', error: 'exactly one correct choice' }` | Pass | Validation - Không có đáp án đúng nào được đánh dấu |
| **TC_19** | Tạo mới nhóm | MediaGroupService | `createMediaGroup` | Xử lý lỗi từ DB khi tạo câu hỏi | `data = {...};`<br>`mocks = { mRepo.create() -> { ID: 5 }, qRepo.createMultiple() -> throw Error('DB Crash') }` | `expected = { type: 'throw', error: 'DB Crash' }` | Pass | Exception handling - Truyền lỗi SQL Exception |
| **TC_20** | Tạo mới nhóm | MediaGroupService | `createMediaGroup` | Rollback khi quy trình tạo lỗi | `data = {...};`<br>`mocks = { mRepo.create() -> { ID: 77 }, qRepo.createMultiple() -> throw Error('FK') }` | `expected = { sideEffects: { mRepo.delete(77): called } }` | Fail | **Bug** - Thiếu Rollback/Transaction khi tạo câu hỏi thất bại, làm dư thừa data |
| **TC_21** | Tạo mới nhóm | MediaGroupService | `createMediaGroup` | Ràng buộc Audio cho LISTENING | `data = { Media: { Skill: 'LISTENING', AudioUrl: null } }` | `expected = { type: 'throw', error: 'AudioUrl is required' }` | Fail | **Bug** - Chỉ `console.warn` thay vì chặn luồng (throw), làm hỏng bài thi |
| **TC_22** | Tạo mới nhóm | MediaGroupService | `createMediaGroup` | Validate chuỗi ký tự rỗng | `data = { Title: '   ', ... }` | `expected = { type: 'throw', error: 'Title cannot be empty' }` | Fail | **Bug** - Thiếu logic `.trim()` khiến dữ liệu rác qua mặt validation |
| **TC_23** | Tạo mới nhóm | MediaGroupService | `createMediaGroup` | Validate trùng ký hiệu đáp án | `data = { Questions: [ { Choices: [ { Attribute: 'A' }, { Attribute: 'A' } ] } ] }` | `expected = { type: 'throw', error: 'Choice attributes must be unique' }` | Fail | **Bug** - Cùng 1 câu hỏi có 2 lựa chọn A gây sập UI trắc nghiệm |
| **TC_24** | Tạo mới nhóm | MediaGroupService | `createMediaGroup` | Giới hạn ký tự Tiêu đề (> 500) | `data = { Title: 'A'.repeat(505) }` | `expected = { type: 'throw', error: 'Title exceeds maximum length' }` | Fail | **Bug** - Thiếu MaxLength Validation gây vỡ Layout hoặc Crash DB |
| **TC_25** | Tạo mới nhóm | MediaGroupService | `createMediaGroup` | Giới hạn ký tự Mô tả (> 1000) | `data = { Description: 'A'.repeat(1005) }` | `expected = { type: 'throw', error: 'Description exceeds maximum length' }` | Fail | **Bug** - Thiếu MaxLength Validation cho Description |
| **TC_26** | Cập nhật nhóm | MediaGroupService | `updateMediaGroupMetadata` | Cập nhật cục bộ (Partial) | `id = 1, updateDto = { Title: 'New' };`<br>`mocks = { findById(1) -> {...}, mRepo.update() -> {...} }` | `expected = { sideEffects: { mRepo.update: calledWith({ GroupTitle: 'New' }) } }` | Pass | Happy path - Update duy nhất 1 trường DTO cung cấp |
| **TC_27** | Cập nhật nhóm | MediaGroupService | `updateMediaGroupMetadata` | Cập nhật toàn phần (Full) | `id = 1, updateDto = { Title: 'New', Difficulty: 'HARD', Media: { AudioUrl: 'url' } };` | `expected = { sideEffects: { mRepo.update: calledWith({ GroupTitle: 'New', Difficulty: 'HARD', AudioUrl: 'url' }) } }` | Pass | Happy path - Update toàn bộ metadata và media URLs |
| **TC_28** | Cập nhật nhóm | MediaGroupService | `updateMediaGroupMetadata` | ID cập nhật không tồn tại | `id = 999, updateDto = {...};`<br>`mocks = { findById(999) -> null }` | `expected = { type: 'throw', error: 'Media group not found' }` | Pass | Exception handling - Tham chiếu ID rỗng |
| **TC_29** | Xóa nhóm | MediaGroupService | `deleteMediaGroup` | Xóa thành công (Cascade) | `id = 1;`<br>`mocks = { findById(1) -> {...}, getUsageStats() -> { usedInExams: 0 } }` | `expected = { type: 'boolean', result: true, sideEffects: { qRepo.deleteByMediaQuestionId: calledFirst, mRepo.delete: calledSecond } }` | Pass | Happy path - Xóa câu hỏi trước (child), xóa nhóm sau (parent) |
| **TC_30** | Xóa nhóm | MediaGroupService | `deleteMediaGroup` | Báo lỗi khi ID không tồn tại | `id = 999;`<br>`mocks = { findById(999) -> null }` | `expected = { type: 'throw', error: 'Media group not found' }` | Pass | Exception handling - Gọi xóa entity ảo |
| **TC_31** | Xóa nhóm | MediaGroupService | `deleteMediaGroup` | Chặn xóa khi đang sử dụng | `id = 1;`<br>`mocks = { getUsageStats() -> { usedInExams: 3 } }` | `expected = { type: 'throw', error: 'Cannot delete' }` | Pass | Business constraint - Ngăn xóa dữ liệu lịch sử thi |
| **TC_32** | Xóa nhóm | MediaGroupService | `deleteMediaGroup` | Xử lý an toàn khi usageStats null | `id = 1;`<br>`mocks = { getUsageStats() -> null }` | `expected = { type: 'throw', error: 'Cannot verify usage stats' }` | Fail | **Bug** - Null-check bị bypass, cho phép xóa dữ liệu không rõ trạng thái |
| **TC_33** | Thêm câu hỏi | MediaGroupService | `addQuestionToGroup` | Thêm thành công vào nhóm | `id = 1, questionData = { OrderInGroup: 3, ... };`<br>`mocks = { isOrderInGroupUnique() -> true, qRepo.createMultiple() -> [{ID: 99}] }` | `expected = { type: 'object', result: { ID: 99 } }` | Pass | Happy path - Thêm 1 câu hỏi vào existing array |
| **TC_34** | Thêm câu hỏi | MediaGroupService | `addQuestionToGroup` | Tự động sinh OrderInGroup | `id = 1, questionData = { OrderInGroup: 0 };`<br>`mocks = { getNextOrderInGroup() -> 5 }` | `expected = { sideEffects: { getNextOrderInGroup: calledWith(1) } }` | Pass | Auto-increment logic - Tạo thứ tự tiếp theo nếu rỗng |
| **TC_35** | Thêm câu hỏi | MediaGroupService | `addQuestionToGroup` | Ràng buộc OrderInGroup trùng lặp | `id = 1, questionData = { OrderInGroup: 1 };`<br>`mocks = { isOrderInGroupUnique() -> false }` | `expected = { type: 'throw', error: 'already used' }` | Pass | Validation - Check conflict OrderInGroup |
| **TC_36** | Thêm câu hỏi | MediaGroupService | `addQuestionToGroup` | Ràng buộc ID nhóm tồn tại | `id = 999;`<br>`mocks = { findById(999) -> null }` | `expected = { type: 'throw', error: 'Media group not found' }` | Pass | Exception handling |
| **TC_37** | Thêm câu hỏi | MediaGroupService | `addQuestionToGroup` | Ràng buộc số âm cho thứ tự | `id = 1, questionData = { OrderInGroup: -1 };` | `expected = { type: 'throw', error: 'OrderInGroup must be positive' }` | Fail | **Bug** - Thiếu validate âm dương, lọt giá trị rác |
| **TC_38** | Thêm câu hỏi | MediaGroupService | `addQuestionToGroup` | Immutability tham số DTO | `inputDto = { OrderInGroup: 0 };` gọi function | `expected = { inputDto.OrderInGroup: 0 }` | Fail | **Bug** - Service thay đổi (mutate) giá trị input DTO thành số mới |
| **TC_39** | Xóa câu hỏi | MediaGroupService | `removeQuestionFromGroup` | Xóa thành công 1 câu hỏi | `groupId = 1, questionId = 10;`<br>`mocks = { findById(10) -> { MediaQuestionID: 1 }, getUsageStats() -> { usedInExams: 0 } }` | `expected = { type: 'boolean', result: true, sideEffects: { qRepo.delete: called } }` | Pass | Happy path - Xóa single record độc lập |
| **TC_40** | Xóa câu hỏi | MediaGroupService | `removeQuestionFromGroup` | Câu hỏi không tồn tại | `groupId = 1, questionId = 999;`<br>`mocks = { findById(999) -> null }` | `expected = { type: 'throw', error: 'Question not found' }` | Pass | Exception handling |
| **TC_41** | Xóa câu hỏi | MediaGroupService | `removeQuestionFromGroup` | Xóa câu hỏi của nhóm khác | `groupId = 1, questionId = 10;`<br>`mocks = { findById(10) -> { MediaQuestionID: 99 } }` | `expected = { type: 'throw', error: 'Question not found' }` | Pass | Validation - Chặn xóa chéo resource (Cross-tenant leak) |
| **TC_42** | Xóa câu hỏi | MediaGroupService | `removeQuestionFromGroup` | Chặn xóa câu hỏi đã thi | `groupId = 1, questionId = 10;`<br>`mocks = { getUsageStats() -> { usedInExams: 2 } }` | `expected = { type: 'throw', error: 'Cannot remove' }` | Pass | Business constraint - Bảo vệ lịch sử thi |
| **TC_43** | Xóa câu hỏi | MediaGroupService | `removeQuestionFromGroup` | Xử lý an toàn khi usageStats null | `groupId = 1, questionId = 10;`<br>`mocks = { getUsageStats() -> null }` | `expected = { type: 'throw', error: 'Cannot verify usage stats' }` | Fail | **Bug** - Null-check bị bypass, cho phép xóa dữ liệu không rõ trạng thái |
| **TC_44** | Thống kê nhóm | MediaGroupService | `getMediaGroupStatistics` | Thống kê số lượng sử dụng | `id = 1;`<br>`mocks = { getUsageStats() -> { usedInExams: 3, totalAttempts: 50 } }` | `expected = { type: 'object', result: { usedInExams: 3, totalAttempts: 50 } }` | Pass | Happy path - Map dữ liệu usage ra response UI |
| **TC_45** | Thống kê nhóm | MediaGroupService | `getMediaGroupStatistics` | ID không tồn tại | `id = 999;`<br>`mocks = { findById(999) -> null }` | `expected = { type: 'throw', error: 'Media group not found' }` | Pass | Exception handling |
| **TC_46** | Nhân bản (Clone) | MediaGroupService | `cloneMediaGroup` | Clone nhóm và câu hỏi thành công | `id = 1, authorId = 42, title = 'Clone';`<br>`mocks = { mRepo.clone(1) -> { ID: 99 }, qRepo.cloneQuestionsToMedia(1, 99, 42) -> [...] }` | `expected = { type: 'object', result: { MediaQuestionID: 99 }, sideEffects: { mRepo.clone: called, qRepo.cloneQuestionsToMedia: called } }` | Pass | Happy path - Luồng copy 2 bước database |
| **TC_47** | Nhân bản (Clone) | MediaGroupService | `cloneMediaGroup` | ID gốc không tồn tại | `id = 999;`<br>`mocks = { findById(999) -> null }` | `expected = { type: 'throw', error: 'Media group not found' }` | Pass | Exception handling |
| **TC_48** | Nhân bản (Clone) | MediaGroupService | `cloneMediaGroup` | Dừng quy trình khi clone Media lỗi | `id = 1;`<br>`mocks = { mRepo.clone() -> null }` | `expected = { type: 'throw', error: 'Failed to clone' }` | Pass | Error handling - Lỗi SQL / Stored Procedure từ DB |
| **TC_49** | Nhân bản (Clone) | MediaGroupService | `cloneMediaGroup` | Truyền lỗi khi clone Questions lỗi | `id = 1;`<br>`mocks = { mRepo.clone() -> {ID: 99}, qRepo.cloneQuestionsToMedia() -> throw Error('Copy fail') }` | `expected = { type: 'throw', error: 'Copy fail' }` | Pass | Error handling - Throw exception ngược lên controller |

