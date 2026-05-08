import { QuestionService } from '../question.service';
import { QuestionRepository } from '../../../infrastructure/repositories/question.repository';
import { CreateQuestionDto } from '../../dtos/question.dto';

// Mock toàn bộ module repository
jest.mock('../../../infrastructure/repositories/question.repository');

describe('QuestionService', () => {
  let service: QuestionService;
  let mockRepo: jest.Mocked<QuestionRepository>;

  // === TEST DATA BUILDERS ===
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

  describe('A. Tạo câu hỏi (createQuestion)', () => {
    it('[TC_01] Tạo câu hỏi thành công với đầy đủ dữ liệu hợp lệ', async () => {
      const mockDto = createMockQuestionDTO();
      const mockEntity = createMockQuestionEntity();
      mockRepo.create.mockResolvedValue(mockEntity as any);

      const result = await service.createQuestion(mockDto, 1);

      expect(mockRepo.create).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockEntity);
    });

    it('[TC_02] Phải ném ra lỗi Validation khi Media không được gửi lên', async () => {
      const invalidData = createMockQuestionDTO();
      delete (invalidData as any).Media;
      await expect(service.createQuestion(invalidData, 1)).rejects.toThrow(/media/i);
    });

    it('[TC_03] Ràng buộc đáp án không được rỗng', async () => {
      const invalidData = createMockQuestionDTO();
      invalidData.Choices[0].Content = '   ';
      await expect(service.createQuestion(invalidData, 1)).rejects.toThrow('All choices must have content');
    });

    it('[TC_04] Ràng buộc hình ảnh cho Part 1', async () => {
      const invalidData = createMockQuestionDTO();
      invalidData.Media.Section = '1';
      invalidData.Media.ImageUrl = undefined;
      await expect(service.createQuestion(invalidData, 1)).rejects.toThrow('Part 1 questions must have an image');
    });

    it('[TC_05] Định dạng URL không hợp lệ', async () => {
      const invalidData = createMockQuestionDTO();
      invalidData.Media.Skill = 'LISTENING';
      invalidData.Media.AudioUrl = 'abc';
      await expect(service.createQuestion(invalidData, 1)).rejects.toThrow('Invalid audio URL format');
    });

    it('[TC_06] LỖI: Không giới hạn độ dài Nội dung câu hỏi (> 1000 ký tự)', async () => {
      const invalidData = createMockQuestionDTO();
      invalidData.QuestionText = 'A'.repeat(1005);
      await expect(service.createQuestion(invalidData, 1)).rejects.toThrow('Question text exceeds maximum length');
    });
  });

  describe('B. Lấy thông tin chi tiết (getQuestionById)', () => {
    it('[TC_07] Lấy chi tiết câu hỏi thành công', async () => {
      const mockEntity = createMockQuestionEntity();
      mockRepo.findById.mockResolvedValue(mockEntity as any);
      const result = await service.getQuestionById(1);
      expect(result).toEqual(mockEntity);
    });

    it('[TC_08] Ném lỗi "Question not found" khi ID không tồn tại', async () => {
      mockRepo.findById.mockResolvedValue(null);
      await expect(service.getQuestionById(999)).rejects.toThrow('Question not found');
    });
  });

  describe('C. Tìm kiếm & Duyệt danh sách (searchQuestions)', () => {
    it('[TC_09] Lấy danh sách thành công và tính toán phân trang đúng', async () => {
      const mockEntity = createMockQuestionEntity();
      mockRepo.findWithFilters.mockResolvedValue({ questions: [mockEntity as any], total: 25 });
      mockRepo.getUsageStats.mockResolvedValue({ usedInExams: 3, totalAttempts: 10 });

      const result = await service.searchQuestions({ Page: 2, Limit: 10 });

      expect(result.Questions).toHaveLength(1);
      expect(result.Questions[0].UsageCount).toBe(3);
      expect(result.Pagination).toEqual({
        CurrentPage: 2,
        TotalPages: 3,
        TotalQuestions: 25,
        Limit: 10,
      });
    });

    it('[TC_10] Áp dụng Default Pagination (Page 1, Limit 20) khi không truyền params', async () => {
      mockRepo.findWithFilters.mockResolvedValue({ questions: [], total: 0 });
      const result = await service.searchQuestions({});
      expect(result.Pagination.CurrentPage).toBe(1);
      expect(result.Pagination.Limit).toBe(20);
    });

    it('[TC_11] An toàn với Relation null (Xử lý khi không có mediaQuestion)', async () => {
      const mockEntity = createMockQuestionEntity();
      mockEntity.mediaQuestion = null as any;
      mockRepo.findWithFilters.mockResolvedValue({ questions: [mockEntity as any], total: 1 });
      mockRepo.getUsageStats.mockResolvedValue({ usedInExams: 0, totalAttempts: 0 });
      const result = await service.searchQuestions({});
      expect(result.Questions[0].Media.Skill).toBe('');
    });
  });

  describe('D. Cập nhật câu hỏi (updateQuestion)', () => {
    beforeEach(() => {
      const mockEntity = createMockQuestionEntity();
      mockRepo.findById.mockResolvedValue(mockEntity as any);
      mockRepo.getUsageStats.mockResolvedValue({ usedInExams: 0, totalAttempts: 0 });
      mockRepo.update.mockResolvedValue(mockEntity as any);
    });

    it('[TC_12] Cập nhật nội dung thành công (Partial Update)', async () => {
      await service.updateQuestion(1, { QuestionText: 'New text' }, 1);
      expect(mockRepo.update).toHaveBeenCalledWith(1, { QuestionText: 'New text' }, undefined, undefined);
    });

    it('[TC_13] Update Partial: Chỉ update thuộc tính được truyền lên', async () => {
      const updateData = { Media: { AudioUrl: '/new.mp3' } } as any;
      await service.updateQuestion(1, updateData, 1);
      const updateCall = mockRepo.update.mock.calls[0];
      const mediaUpdatePayload = updateCall[2];
      expect(mediaUpdatePayload).toEqual(expect.objectContaining({ AudioUrl: '/new.mp3' }));
      expect(mediaUpdatePayload).not.toHaveProperty('Skill', undefined);
    });

    it('[TC_14] In cảnh báo khi update câu hỏi đã thi nhiều lần (> 5 lần)', async () => {
      const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();
      mockRepo.getUsageStats.mockResolvedValue({ usedInExams: 10, totalAttempts: 0 });
      await service.updateQuestion(1, { QuestionText: 'Fix typo' }, 1);
      expect(consoleSpy).toHaveBeenCalledWith(expect.stringContaining('Warning: Updating question'));
      consoleSpy.mockRestore();
    });

    it('[TC_15] Báo lỗi khi update ID không tồn tại', async () => {
      mockRepo.findById.mockResolvedValue(null);
      await expect(service.updateQuestion(999, {}, 1)).rejects.toThrow('Question not found');
    });

    it('[TC_16] Báo lỗi khi DB Update trả về null', async () => {
      mockRepo.update.mockResolvedValue(null);
      await expect(service.updateQuestion(1, { QuestionText: 'New' }, 1)).rejects.toThrow('Failed to update question');
    });
  });

  describe('E. Xóa câu hỏi (deleteQuestion)', () => {
    beforeEach(() => {
      const mockEntity = createMockQuestionEntity();
      mockRepo.findById.mockResolvedValue(mockEntity as any);
      mockRepo.delete.mockResolvedValue(true);
    });

    it('[TC_17] Xóa thành công khi câu hỏi chưa được sử dụng', async () => {
      mockRepo.getUsageStats.mockResolvedValue({ usedInExams: 0, totalAttempts: 0 });
      const result = await service.deleteQuestion(1, 1);
      expect(result).toBe(true);
      expect(mockRepo.delete).toHaveBeenCalledWith(1);
    });

    it('[TC_18] Chặn xóa khi câu hỏi đã nằm trong đề thi', async () => {
      mockRepo.getUsageStats.mockResolvedValue({ usedInExams: 1, totalAttempts: 0 });
      await expect(service.deleteQuestion(1, 1)).rejects.toThrow('Cannot delete question that is used');
    });

    it('[TC_19] LỖI: Bypass bảo vệ khi không lấy được usageStats (Fail-Closed)', async () => {
      mockRepo.getUsageStats.mockResolvedValue(null);
      // Kỳ vọng: HỆ THỐNG PHẢI TỪ CHỐI XÓA khi không rõ trạng thái sử dụng.
      // Test này sẽ FAIL với code hiện tại.
      await expect(service.deleteQuestion(1, 1)).rejects.toThrow();
    });
  });

  describe('F. Bulk Operations (performBulkOperation)', () => {
    it('[TC_20] Thực hiện xóa nhiều thành công', async () => {
      mockRepo.bulkDelete.mockResolvedValue(3);
      const result = await service.performBulkOperation({ Operation: 'DELETE', QuestionIDs: [1, 2, 3] }, 1);
      expect(result.success).toBe(3);
      expect(result.failed).toBe(0);
    });

    it('[TC_21] Quản lý Exception khi bulk delete thất bại', async () => {
      mockRepo.bulkDelete.mockRejectedValue(new Error('Transaction lock'));
      const result = await service.performBulkOperation({ Operation: 'DELETE', QuestionIDs: [1, 2] }, 1);
      expect(result.success).toBe(0);
      expect(result.failed).toBe(2);
      expect(result.errors[0]).toContain('Transaction lock');
    });

    it('[TC_22] Trả lỗi khi thao tác không được hỗ trợ', async () => {
      const result = await service.performBulkOperation({ Operation: 'ADD_TO_EXAM', QuestionIDs: [1] } as any, 1);
      expect(result.failed).toBe(1);
      expect(result.errors[0]).toContain('handled by ExamService');
    });

    it('[TC_23] Xử lý lỗi an toàn khi truyền danh sách ID null', async () => {
      const promise = service.performBulkOperation({ Operation: 'DELETE', QuestionIDs: undefined as any }, 1);
      await expect(promise).resolves.toEqual(expect.objectContaining({ failed: expect.any(Number) }));
    });
  });

  describe('G. Get Questions By Section (Practice Mode)', () => {
    it('[TC_24] Lấy danh sách luyện tập thành công', async () => {
      const mockEntity = createMockQuestionEntity();
      mockRepo.getQuestionsBySection.mockResolvedValue([mockEntity as any]);
      const result = await service.getQuestionsBySection(['5', '6'], 10);
      expect(result).toHaveLength(1);
    });

    it('[TC_25] Báo lỗi khi mảng section rỗng', async () => {
      await expect(service.getQuestionsBySection([])).rejects.toThrow('At least one section must be specified');
    });
  });

  describe('H. Statistics', () => {
    it('[TC_26] Lấy thống kê thành công', async () => {
      const mockEntity = createMockQuestionEntity();
      mockRepo.findById.mockResolvedValue(mockEntity as any);
      mockRepo.getUsageStats.mockResolvedValue({ total: 100 });
      const result = await service.getQuestionStatistics(1);
      expect(result.total).toBe(100);
    });
  });
});
