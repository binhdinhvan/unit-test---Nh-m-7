import { QuestionService } from '../question.service';
import { QuestionRepository } from '../../../infrastructure/repositories/question.repository';
import { CreateQuestionDto } from '../../dtos/question.dto';

// Mock toàn bộ module repository
jest.mock('../../../infrastructure/repositories/question.repository');

// Quy ước chú thích trong file test:
// INPUT: dữ liệu truyền trực tiếp vào hàm service cần test.
// MOCK DATA: dữ liệu giả lập repository trả về, không phải dữ liệu DB thật.
// ACTION: lời gọi hàm service đang được kiểm thử.
// EXPECTED: kết quả trả về, lỗi ném ra, hoặc mock repository call được kỳ vọng.

describe('QuestionService', () => {
  let service: QuestionService;
  let mockRepo: jest.Mocked<QuestionRepository>;

  // === TEST DATA BUILDERS ===
  // Factory tạo dữ liệu mock để tránh dùng chung reference giữa các test case.
  const createMockQuestionDTO = (overrides?: Partial<CreateQuestionDto>): CreateQuestionDto => ({
    QuestionText: 'Sample question?',
    Media: {
      Skill: 'READING',
      Type: 'GRAMMAR',
      Section: '5',
    },
    Choices: [
      { Content: 'A', Attribute: 'A', IsCorrect: true },
      { Content: 'B', Attribute: 'B', IsCorrect: false },
      { Content: 'C', Attribute: 'C', IsCorrect: false },
      { Content: 'D', Attribute: 'D', IsCorrect: false },
    ],
    ...overrides,
  });

  const createMockQuestionEntity = (overrides?: any) => ({
    ID: 1,
    QuestionText: 'Sample question?',
    UserID: 1,
    mediaQuestion: {
      ID: 10,
      Skill: 'READING',
      Type: 'GRAMMAR',
      Section: '5',
    },
    choices: [
      { ID: 101, Content: 'A', Attribute: 'A', IsCorrect: true },
      { ID: 102, Content: 'B', Attribute: 'B', IsCorrect: false },
    ],
    ...overrides,
  });

  beforeEach(() => {
    jest.clearAllMocks();
    service = new QuestionService();
    mockRepo = (service as any).questionRepository as jest.Mocked<QuestionRepository>;
  });

  // ==============================================
  // A. CREATE (Tạo câu hỏi)
  // ==============================================
  describe('A. Tạo câu hỏi (createQuestion)', () => {
    /**
     * Chức năng: Tạo câu hỏi mới.
     * Trường hợp: Payload hợp lệ đầy đủ (Happy Path).
     * Kỳ vọng: Repository create được gọi và trả về entity.
     */
    it('[TC_01] Tạo câu hỏi thành công với đầy đủ dữ liệu hợp lệ', async () => {
      // INPUT: DTO đầy đủ fields.
      const mockDto = createMockQuestionDTO();
      // MOCK DATA: repository trả entity mới.
      const mockEntity = createMockQuestionEntity();
      mockRepo.create.mockResolvedValue(mockEntity as any);
      // ACTION: gọi createQuestion.
      const result = await service.createQuestion(mockDto, 1);
      // EXPECTED: create được gọi và trả đúng entity.
      expect(mockRepo.create).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockEntity);
    });

    /**
     * Chức năng: Validate payload bắt buộc.
     * Trường hợp: Thiếu Media.
     * Kỳ vọng: Ném lỗi validation liên quan Media.
     */
    it('[TC_02] Phải ném ra lỗi Validation khi Media không được gửi lên', async () => {
      // INPUT: DTO thiếu Media.
      const invalidData = createMockQuestionDTO();
      delete (invalidData as any).Media;
      // EXPECTED: lỗi validation chứa từ khóa media.
      await expect(service.createQuestion(invalidData, 1)).rejects.toThrow(/media/i);
    });

    /**
     * Chức năng: Validate nội dung đáp án.
     * Trường hợp: Choice có Content rỗng/whitespace.
     * Kỳ vọng: Ném lỗi "All choices must have content".
     */
    it('[TC_03] Ràng buộc đáp án không được rỗng', async () => {
      // INPUT: choice content chỉ có khoảng trắng.
      const invalidData = createMockQuestionDTO();
      invalidData.Choices[0].Content = '   ';
      // EXPECTED: reject với thông báo rõ ràng.
      await expect(service.createQuestion(invalidData, 1)).rejects.toThrow('All choices must have content');
    });

    /**
     * Chức năng: Validate điều kiện ảnh cho Part 1.
     * Trường hợp: Section=1 nhưng thiếu ImageUrl.
     * Kỳ vọng: Ném lỗi "Part 1 questions must have an image".
     */
    it('[TC_04] Ràng buộc hình ảnh cho Part 1', async () => {
      // INPUT: media section 1 không có ImageUrl.
      const invalidData = createMockQuestionDTO();
      invalidData.Media.Section = '1';
      invalidData.Media.ImageUrl = undefined;
      // EXPECTED: reject do thiếu ảnh.
      await expect(service.createQuestion(invalidData, 1)).rejects.toThrow('Part 1 questions must have an image');
    });

    /**
     * Chức năng: Validate định dạng URL Audio.
     * Trường hợp: Skill LISTENING nhưng AudioUrl sai định dạng.
     * Kỳ vọng: Ném lỗi "Invalid audio URL format".
     */
    it('[TC_05] Định dạng URL không hợp lệ', async () => {
      // INPUT: AudioUrl không hợp lệ.
      const invalidData = createMockQuestionDTO();
      invalidData.Media.Skill = 'LISTENING';
      invalidData.Media.AudioUrl = 'abc';
      // EXPECTED: reject do URL sai format.
      await expect(service.createQuestion(invalidData, 1)).rejects.toThrow('Invalid audio URL format');
    });

    /**
     * Chức năng: Giới hạn độ dài nội dung câu hỏi.
     * Trường hợp: QuestionText > 1000 ký tự.
     * Kỳ vọng: Ném lỗi vượt giới hạn độ dài.
     */
    it('[TC_06] LỖI: Không giới hạn độ dài Nội dung câu hỏi (> 1000 ký tự)', async () => {
      // INPUT: QuestionText dài 1005 ký tự.
      const invalidData = createMockQuestionDTO();
      invalidData.QuestionText = 'A'.repeat(1005);
      // EXPECTED: reject do vượt max length.
      await expect(service.createQuestion(invalidData, 1)).rejects.toThrow('Question text exceeds maximum length');
    });
  });

  // ==============================================
  // B. DETAIL (Lấy chi tiết)
  // ==============================================
  describe('B. Lấy thông tin chi tiết (getQuestionById)', () => {
    /**
     * Chức năng: Lấy chi tiết câu hỏi.
     * Trường hợp: ID hợp lệ.
     * Kỳ vọng: Trả về entity từ repository.
     */
    it('[TC_07] Lấy chi tiết câu hỏi thành công', async () => {
      // MOCK DATA: repository trả entity.
      const mockEntity = createMockQuestionEntity();
      mockRepo.findById.mockResolvedValue(mockEntity as any);
      // ACTION: gọi getQuestionById.
      const result = await service.getQuestionById(1);
      // EXPECTED: trả đúng entity.
      expect(result).toEqual(mockEntity);
    });

    /**
     * Chức năng: Lấy chi tiết câu hỏi.
     * Trường hợp: ID không tồn tại.
     * Kỳ vọng: Ném lỗi "Question not found".
     */
    it('[TC_08] Ném lỗi "Question not found" khi ID không tồn tại', async () => {
      // MOCK DATA: repository trả null.
      mockRepo.findById.mockResolvedValue(null);
      // EXPECTED: throw not found.
      await expect(service.getQuestionById(999)).rejects.toThrow('Question not found');
    });
  });

  // ==============================================
  // C. SEARCH (Tìm kiếm & duyệt danh sách)
  // ==============================================
  describe('C. Tìm kiếm & Duyệt danh sách (searchQuestions)', () => {
    /**
     * Chức năng: Tìm kiếm câu hỏi và phân trang.
     * Trường hợp: Có dữ liệu và usage stats.
     * Kỳ vọng: Trả đúng list, UsageCount, và Pagination.
     */
    it('[TC_09] Lấy danh sách thành công và tính toán phân trang đúng', async () => {
      // MOCK DATA: 1 question, total=25, usageStats có usedInExams=3.
      const mockEntity = createMockQuestionEntity();
      mockRepo.findWithFilters.mockResolvedValue({ questions: [mockEntity as any], total: 25 });
      mockRepo.getUsageStats.mockResolvedValue({ usedInExams: 3, totalAttempts: 10 });
      // ACTION: search với Page=2, Limit=10.
      const result = await service.searchQuestions({ Page: 2, Limit: 10 });
      // EXPECTED: UsageCount và Pagination đúng.
      expect(result.Questions).toHaveLength(1);
      expect(result.Questions[0].UsageCount).toBe(3);
      expect(result.Pagination).toEqual({
        CurrentPage: 2,
        TotalPages: 3,
        TotalQuestions: 25,
        Limit: 10,
      });
    });

    /**
     * Chức năng: Tìm kiếm câu hỏi.
     * Trường hợp: Không truyền params phân trang.
     * Kỳ vọng: Dùng mặc định Page=1, Limit=20.
     */
    it('[TC_10] Áp dụng Default Pagination (Page 1, Limit 20) khi không truyền params', async () => {
      // MOCK DATA: danh sách rỗng, total=0.
      mockRepo.findWithFilters.mockResolvedValue({ questions: [], total: 0 });
      // ACTION: search không truyền params.
      const result = await service.searchQuestions({});
      // EXPECTED: pagination default.
      expect(result.Pagination.CurrentPage).toBe(1);
      expect(result.Pagination.Limit).toBe(20);
    });

    /**
     * Chức năng: Map dữ liệu media trong kết quả search.
     * Trường hợp: mediaQuestion null.
     * Kỳ vọng: Fallback an toàn, không crash.
     */
    it('[TC_11] An toàn với Relation null (Xử lý khi không có mediaQuestion)', async () => {
      // MOCK DATA: mediaQuestion=null.
      const mockEntity = createMockQuestionEntity();
      mockEntity.mediaQuestion = null as any;
      mockRepo.findWithFilters.mockResolvedValue({ questions: [mockEntity as any], total: 1 });
      mockRepo.getUsageStats.mockResolvedValue({ usedInExams: 0, totalAttempts: 0 });
      // ACTION: search.
      const result = await service.searchQuestions({});
      // EXPECTED: Media.Skill fallback '' khi mediaQuestion null.
      expect(result.Questions[0].Media.Skill).toBe('');
    });
  });

  // ==============================================
  // D. UPDATE (Cập nhật)
  // ==============================================
  describe('D. Cập nhật câu hỏi (updateQuestion)', () => {
    beforeEach(() => {
      const mockEntity = createMockQuestionEntity();
      mockRepo.findById.mockResolvedValue(mockEntity as any);
      mockRepo.getUsageStats.mockResolvedValue({ usedInExams: 0, totalAttempts: 0 });
      mockRepo.update.mockResolvedValue(mockEntity as any);
    });

    /**
     * Chức năng: Cập nhật câu hỏi.
     * Trường hợp: Partial update chỉ thay đổi QuestionText.
     * Kỳ vọng: Gọi repository update với payload đúng.
     */
    it('[TC_12] Cập nhật nội dung thành công (Partial Update)', async () => {
      // INPUT: chỉ đổi QuestionText.
      await service.updateQuestion(1, { QuestionText: 'New text' }, 1);
      // EXPECTED: update chỉ nhận payload cần thiết.
      expect(mockRepo.update).toHaveBeenCalledWith(1, { QuestionText: 'New text' }, undefined, undefined);
    });

    /**
     * Chức năng: Partial update với Media.
     * Trường hợp: Chỉ cập nhật AudioUrl.
     * Kỳ vọng: Payload media chỉ chứa AudioUrl, không bao gồm field khác.
     */
    it('[TC_13] Update Partial: Chỉ update thuộc tính được truyền lên', async () => {
      // INPUT: update chỉ AudioUrl.
      const updateData = { Media: { AudioUrl: '/new.mp3' } } as any;
      await service.updateQuestion(1, updateData, 1);
      // EXPECTED: payload chỉ có AudioUrl.
      const updateCall = mockRepo.update.mock.calls[0];
      const mediaUpdatePayload = updateCall[2];
      expect(mediaUpdatePayload).toEqual(expect.objectContaining({ AudioUrl: '/new.mp3' }));
      expect(mediaUpdatePayload).not.toHaveProperty('Skill');
      expect(mediaUpdatePayload).not.toHaveProperty('Type');
      expect(mediaUpdatePayload).not.toHaveProperty('Section');
    });

    /**
     * Chức năng: Cảnh báo khi update câu hỏi đã dùng nhiều lần.
     * Trường hợp: usedInExams > 5.
     * Kỳ vọng: console.warn được gọi.
     */
    it('[TC_14] In cảnh báo khi update câu hỏi đã thi nhiều lần (> 5 lần)', async () => {
      // MOCK DATA: usageStats usedInExams=10.
      const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();
      mockRepo.getUsageStats.mockResolvedValue({ usedInExams: 10, totalAttempts: 0 });
      // ACTION: update question.
      await service.updateQuestion(1, { QuestionText: 'Fix typo' }, 1);
      // EXPECTED: có cảnh báo.
      expect(consoleSpy).toHaveBeenCalledWith(expect.stringContaining('Warning: Updating question'));
      consoleSpy.mockRestore();
    });

    /**
     * Chức năng: Cập nhật câu hỏi.
     * Trường hợp: ID không tồn tại.
     * Kỳ vọng: Ném lỗi "Question not found".
     */
    it('[TC_15] Báo lỗi khi update ID không tồn tại', async () => {
      // MOCK DATA: findById trả null.
      mockRepo.findById.mockResolvedValue(null);
      // EXPECTED: throw not found.
      await expect(service.updateQuestion(999, {}, 1)).rejects.toThrow('Question not found');
    });

    /**
     * Chức năng: Cập nhật câu hỏi.
     * Trường hợp: Repository update trả null.
     * Kỳ vọng: Ném lỗi "Failed to update question".
     */
    it('[TC_16] Báo lỗi khi DB Update trả về null', async () => {
      // MOCK DATA: update trả null.
      mockRepo.update.mockResolvedValue(null);
      // EXPECTED: throw failed update.
      await expect(service.updateQuestion(1, { QuestionText: 'New' }, 1)).rejects.toThrow('Failed to update question');
    });
  });

  // ==============================================
  // E. DELETE (Xóa)
  // ==============================================
  describe('E. Xóa câu hỏi (deleteQuestion)', () => {
    beforeEach(() => {
      const mockEntity = createMockQuestionEntity();
      mockRepo.findById.mockResolvedValue(mockEntity as any);
      mockRepo.delete.mockResolvedValue(true);
    });

    /**
     * Chức năng: Xóa câu hỏi.
     * Trường hợp: Câu hỏi chưa được dùng.
     * Kỳ vọng: Xóa thành công và gọi repository delete.
     */
    it('[TC_17] Xóa thành công khi câu hỏi chưa được sử dụng', async () => {
      // MOCK DATA: usageStats usedInExams=0.
      mockRepo.getUsageStats.mockResolvedValue({ usedInExams: 0, totalAttempts: 0 });
      // ACTION: delete question.
      const result = await service.deleteQuestion(1, 1);
      // EXPECTED: trả true và gọi delete.
      expect(result).toBe(true);
      expect(mockRepo.delete).toHaveBeenCalledWith(1);
    });

    /**
     * Chức năng: Xóa câu hỏi.
     * Trường hợp: Câu hỏi đã nằm trong đề thi.
     * Kỳ vọng: Chặn xóa và ném lỗi.
     */
    it('[TC_18] Chặn xóa khi câu hỏi đã nằm trong đề thi', async () => {
      // MOCK DATA: usedInExams > 0.
      mockRepo.getUsageStats.mockResolvedValue({ usedInExams: 1, totalAttempts: 0 });
      // EXPECTED: reject xóa.
      await expect(service.deleteQuestion(1, 1)).rejects.toThrow('Cannot delete question that is used');
    });

    /**
     * Chức năng: Xóa câu hỏi.
     * Trường hợp: Không lấy được usageStats (null).
     * Kỳ vọng: Fail-closed, từ chối xóa.
     */
    it('[TC_19] LỖI: Bypass bảo vệ khi không lấy được usageStats (Fail-Closed)', async () => {
      // MOCK DATA: usageStats=null.
      mockRepo.getUsageStats.mockResolvedValue(null);
      // Kỳ vọng: HỆ THỐNG PHẢI TỪ CHỐI XÓA khi không rõ trạng thái sử dụng.
      // Test này sẽ FAIL với code hiện tại.
      await expect(service.deleteQuestion(1, 1)).rejects.toThrow('Cannot verify usage stats');
    });
  });

  // ==============================================
  // F. BULK (Bulk Operations)
  // ==============================================
  describe('F. Bulk Operations (performBulkOperation)', () => {
    /**
     * Chức năng: Bulk delete.
     * Trường hợp: Repository xóa thành công 3 records.
     * Kỳ vọng: success=3, failed=0.
     */
    it('[TC_20] Thực hiện xóa nhiều thành công', async () => {
      // MOCK DATA: bulkDelete trả 3.
      mockRepo.bulkDelete.mockResolvedValue(3);
      // ACTION: bulk delete 3 ID.
      const result = await service.performBulkOperation({ Operation: 'DELETE', QuestionIDs: [1, 2, 3] }, 1);
      // EXPECTED: kết quả đúng.
      expect(result.success).toBe(3);
      expect(result.failed).toBe(0);
    });

    /**
     * Chức năng: Bulk delete.
     * Trường hợp: Repository throw lỗi.
     * Kỳ vọng: success=0, failed=2 và errors có message.
     */
    it('[TC_21] Quản lý Exception khi bulk delete thất bại', async () => {
      // MOCK DATA: bulkDelete throw error.
      mockRepo.bulkDelete.mockRejectedValue(new Error('Transaction lock'));
      // ACTION: bulk delete.
      const result = await service.performBulkOperation({ Operation: 'DELETE', QuestionIDs: [1, 2] }, 1);
      // EXPECTED: failed=2 và error message được map.
      expect(result.success).toBe(0);
      expect(result.failed).toBe(2);
      expect(result.errors[0]).toContain('Transaction lock');
    });

    /**
     * Chức năng: Bulk operation.
     * Trường hợp: Operation không được hỗ trợ.
     * Kỳ vọng: failed=1 và có thông báo hướng dẫn.
     */
    it('[TC_22] Trả lỗi khi thao tác không được hỗ trợ', async () => {
      // INPUT: Operation=ADD_TO_EXAM.
      const result = await service.performBulkOperation({ Operation: 'ADD_TO_EXAM', QuestionIDs: [1] } as any, 1);
      // EXPECTED: báo lỗi handled by ExamService.
      expect(result.failed).toBe(1);
      expect(result.errors[0]).toContain('handled by ExamService');
    });

    /**
     * Chức năng: Bulk delete.
     * Trường hợp: QuestionIDs null/undefined.
     * Kỳ vọng: Xử lý an toàn và trả kết quả có failed.
     */
    it('[TC_23] LỖI: Crash ứng dụng khi truyền danh sách ID null', async () => {
      // MOCK DATA: Giả lập Repository ném lỗi khi nhận mảng null/undefined
      mockRepo.bulkDelete.mockRejectedValue(new Error('Invalid parameters'));
      
      // INPUT: QuestionIDs undefined.
      const promise = service.performBulkOperation({ Operation: 'DELETE', QuestionIDs: undefined as any }, 1);
      
      // EXPECTED: Hệ thống phải bắt lỗi an toàn (Fail-safe) và trả về object lỗi, chứ không được văng Exception làm crash server
      await expect(promise).resolves.toEqual(
        expect.objectContaining({ failed: expect.any(Number) })
      );
    });
  });

  // ==============================================
  // G. PRACTICE (Get Questions By Section)
  // ==============================================
  describe('G. Get Questions By Section (Practice Mode)', () => {
    /**
     * Chức năng: Lấy câu hỏi theo section.
     * Trường hợp: Sections hợp lệ.
     * Kỳ vọng: Trả danh sách câu hỏi.
     */
    it('[TC_24] Lấy danh sách luyện tập thành công', async () => {
      // MOCK DATA: repository trả 1 entity.
      const mockEntity = createMockQuestionEntity();
      mockRepo.getQuestionsBySection.mockResolvedValue([mockEntity as any]);
      // ACTION: getQuestionsBySection.
      const result = await service.getQuestionsBySection(['5', '6'], 10);
      // EXPECTED: có 1 phần tử.
      expect(result).toHaveLength(1);
    });

    /**
     * Chức năng: Lấy câu hỏi theo section.
     * Trường hợp: Mảng section rỗng.
     * Kỳ vọng: Ném lỗi yêu cầu ít nhất 1 section.
     */
    it('[TC_25] Báo lỗi khi mảng section rỗng', async () => {
      // INPUT: sections=[]
      await expect(service.getQuestionsBySection([])).rejects.toThrow('At least one section must be specified');
    });
  });

  // ==============================================
  // H. STATISTICS (Thống kê)
  // ==============================================
  describe('H. Statistics', () => {
    /**
     * Chức năng: Lấy thống kê câu hỏi.
     * Trường hợp: Question tồn tại và có usage stats.
     * Kỳ vọng: Trả về số liệu total.
     */
    it('[TC_26] Lấy thống kê thành công', async () => {
      // MOCK DATA: findById trả entity, usageStats trả total=100.
      const mockEntity = createMockQuestionEntity();
      mockRepo.findById.mockResolvedValue(mockEntity as any);
      mockRepo.getUsageStats.mockResolvedValue({ total: 100 });
      // ACTION: getQuestionStatistics.
      const result = await service.getQuestionStatistics(1);
      // EXPECTED: total=100.
      expect(result.total).toBe(100);
    });
  });
});
