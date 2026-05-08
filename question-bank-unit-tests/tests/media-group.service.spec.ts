import { MediaGroupService } from '../media-group.service';
import { MediaQuestionRepository } from '../../../infrastructure/repositories/media-question.repository';
import { QuestionRepository } from '../../../infrastructure/repositories/question.repository';
import { CreateMediaGroupDto } from '../../dtos/media-group.dto';

jest.mock('../../../infrastructure/repositories/media-question.repository');
jest.mock('../../../infrastructure/repositories/question.repository');

// Factory để clone dữ liệu mock, tránh việc chia sẻ trạng thái (shared mutable state) giữa các test case
const createMockMedia = (ov: any = {}) => structuredClone({
  ID: 1, Skill: 'LISTENING', Type: 'CONVERSATION', Section: '3',
  AudioUrl: 'http://ex.com/a.mp3', ImageUrl: null, Scirpt: 'Hello',
  GroupTitle: 'Office Talk', GroupDescription: 'Part 3',
  Difficulty: 'MEDIUM', Tags: ['listening'], OrderIndex: 1,
  questions: [
    { ID: 10, QuestionText: 'Q1?', OrderInGroup: 1, MediaQuestionID: 1,
      choices: [
        { ID: 100, Attribute: 'A', Content: 'Ans1', IsCorrect: true },
        { ID: 101, Attribute: 'B', Content: 'Ans2', IsCorrect: false },
        { ID: 102, Attribute: 'C', Content: 'Ans3', IsCorrect: false },
        { ID: 103, Attribute: 'D', Content: 'Ans4', IsCorrect: false },
      ], attemptAnswers: [] },
    { ID: 11, QuestionText: 'Q2?', OrderInGroup: 2, MediaQuestionID: 1,
      choices: [
        { ID: 104, Attribute: 'A', Content: 'Ans1', IsCorrect: false },
        { ID: 105, Attribute: 'B', Content: 'Ans2', IsCorrect: true },
      ], attemptAnswers: [] },
  ], ...ov,
});

const createDto = (ov: Partial<CreateMediaGroupDto> = {}): CreateMediaGroupDto => structuredClone({
  Title: 'New', Description: 'Desc',
  Media: { Skill: 'READING', Type: 'TEXT_COMPLETION', Section: '6' },
  Questions: [{ QuestionText: 'Q', OrderInGroup: 1,
    Choices: [
      { Content: 'A', Attribute: 'A', IsCorrect: true },
      { Content: 'B', Attribute: 'B', IsCorrect: false },
    ] }],
  Difficulty: 'EASY', Tags: ['r'], ...ov,
});

const USAGE_ZERO = { questionCount: 2, usedInExams: 0, totalAttempts: 0 };
const USAGE_ACTIVE = { questionCount: 2, usedInExams: 3, totalAttempts: 50 };

describe('MediaGroupService - Quản lý Ngân hàng Câu hỏi', () => {
  let svc: MediaGroupService;
  let mRepo: jest.Mocked<MediaQuestionRepository>;
  let qRepo: jest.Mocked<QuestionRepository>;

  beforeEach(() => {
    jest.clearAllMocks();
    svc = new MediaGroupService();
    // LƯU Ý CHO DEV: Hãy refactor lại MediaGroupService để nhận repository qua Constructor (Dependency Injection)
    // Ví dụ: constructor(mediaRepo?: MediaQuestionRepository, qRepo?: QuestionRepository)
    mRepo = new MediaQuestionRepository() as any;
    qRepo = new QuestionRepository() as any;
    (svc as any).mediaQuestionRepository = mRepo;
    (svc as any).questionRepository = qRepo;
  });

  // ==============================================
  // A. BROWSE (Duyệt danh sách)
  // ==============================================
  describe('Duyệt danh sách (getMediaGroupsForBrowsing)', () => {
    
    /**
     * Chức năng: Duyệt danh sách nhóm câu hỏi.
     * Trường hợp: DB trả về dữ liệu hợp lệ (Happy Path).
     * Kỳ vọng: Ánh xạ đúng các trường tóm tắt (HasAudio, HasImage, Title).
     */
    it('[TC_01] Lấy danh sách nhóm câu hỏi thành công', async () => {
      mRepo.findWithFilters.mockResolvedValueOnce({ mediaQuestions: [createMockMedia()], total: 1 });
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      const r = await svc.getMediaGroupsForBrowsing();
      expect(r.groups).toHaveLength(1);
      expect(r.groups[0].Title).toBe('Office Talk');
      expect(r.groups[0].HasAudio).toBe(true);
      expect(r.groups[0].HasImage).toBe(false);
    });

    /**
     * Chức năng: Tạo đoạn text xem trước (PreviewText) cho UI.
     * Trường hợp: Câu hỏi đầu tiên có nội dung dài hơn 100 ký tự (Boundary Value).
     * Kỳ vọng: Chuỗi bị cắt bớt còn 100 ký tự và thêm dấu '...' ở cuối.
     */
    it('[TC_02] Cắt ngắn chuỗi PreviewText khi vượt quá giới hạn', async () => {
      const longText = 'A'.repeat(105);
      const media = createMockMedia();
      media.questions[0].QuestionText = longText;
      mRepo.findWithFilters.mockResolvedValueOnce({ mediaQuestions: [media], total: 1 });
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      
      const r = await svc.getMediaGroupsForBrowsing();
      expect(r.groups[0].PreviewText.length).toBe(103); // 100 char + '...'
      expect(r.groups[0].PreviewText.endsWith('...')).toBe(true);
    });

    /**
     * Chức năng: Duyệt danh sách nhóm câu hỏi.
     * Trường hợp: Ngân hàng câu hỏi trống.
     * Kỳ vọng: Trả về mảng rỗng và tổng số lượng bằng 0.
     */
    it('[TC_03] Lấy danh sách khi không có dữ liệu', async () => {
      mRepo.findWithFilters.mockResolvedValueOnce({ mediaQuestions: [], total: 0 });
      const r = await svc.getMediaGroupsForBrowsing();
      expect(r.groups).toEqual([]);
      expect(r.total).toBe(0);
    });

    /**
     * Chức năng: Phân trang danh sách.
     * Trường hợp: Client không truyền params phân trang (Page, Limit).
     * Kỳ vọng: Gán giá trị mặc định là Page=1, Limit=20.
     */
    it('[TC_04] Áp dụng phân trang mặc định', async () => {
      mRepo.findWithFilters.mockResolvedValueOnce({ mediaQuestions: [], total: 100 });
      const r = await svc.getMediaGroupsForBrowsing();
      expect(r.pagination).toEqual({ CurrentPage: 1, TotalPages: 5, Limit: 20 });
    });

    /**
     * Chức năng: Tính toán tổng số trang (TotalPages).
     * Trường hợp: Tổng số record không chia hết cho Limit (33 records, Limit 10).
     * Kỳ vọng: Sử dụng Math.ceil để làm tròn lên thành 4 trang.
     */
    it('[TC_05] Tính toán tổng số trang phân trang', async () => {
      mRepo.findWithFilters.mockResolvedValueOnce({ mediaQuestions: [], total: 33 });
      const r = await svc.getMediaGroupsForBrowsing({ Page: 2, Limit: 10 } as any);
      expect(r.pagination.TotalPages).toBe(4);
    });

    /**
     * Chức năng: Xử lý hiển thị tiêu đề nhóm.
     * Trường hợp: Dữ liệu cũ trong DB không có GroupTitle (null).
     * Kỳ vọng: Tự động tạo tiêu đề dựa trên Type và Section (Fallback behavior).
     */
    it('[TC_06] Fallback tiêu đề nhóm khi GroupTitle bị null', async () => {
      mRepo.findWithFilters.mockResolvedValueOnce({ mediaQuestions: [createMockMedia({ GroupTitle: null })], total: 1 });
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      const r = await svc.getMediaGroupsForBrowsing();
      expect(r.groups[0].Title).toBe('CONVERSATION - Part 3');
    });

    /**
     * Chức năng: Xử lý dữ liệu khuyết thiếu từ DB.
     * Trường hợp: Tags và Difficulty bị null trong DB.
     * Kỳ vọng: Fallback về mảng rỗng cho Tags và 'MEDIUM' cho Difficulty.
     */
    it('[TC_07] Fallback dữ liệu khi Tags hoặc Difficulty bị null', async () => {
      mRepo.findWithFilters.mockResolvedValueOnce({ mediaQuestions: [createMockMedia({ Tags: null, Difficulty: null })], total: 1 });
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      const r = await svc.getMediaGroupsForBrowsing();
      expect(r.groups[0].Tags).toEqual([]);
      expect(r.groups[0].Difficulty).toBe('MEDIUM');
    });

    /**
     * Chức năng: Đếm số lượng câu hỏi con.
     * Trường hợp: Mảng questions trả về từ DB bị null (inconsistency).
     * Kỳ vọng: Gọi DB để đếm lại và không bị crash hệ thống.
     */
    it('[TC_08] Xử lý an toàn khi mảng questions bị null', async () => {
      mRepo.findWithFilters.mockResolvedValueOnce({ mediaQuestions: [createMockMedia({ questions: null })], total: 1 });
      qRepo.countByMediaQuestionId.mockResolvedValueOnce(0);
      qRepo.findFirstByMediaQuestionId.mockResolvedValueOnce(null as any);
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      const r = await svc.getMediaGroupsForBrowsing();
      expect(r.groups[0].QuestionCount).toBe(0);
    });
  });

  // ==============================================
  // B. DETAIL (Xem chi tiết)
  // ==============================================
  describe('Xem chi tiết (getMediaGroupDetail)', () => {
    
    /**
     * Chức năng: Xem chi tiết nhóm câu hỏi.
     * Trường hợp: ID hợp lệ.
     * Kỳ vọng: Sắp xếp các câu hỏi con tăng dần theo OrderInGroup.
     */
    it('[TC_09] Lấy chi tiết nhóm câu hỏi thành công', async () => {
      const m = createMockMedia();
      m.questions = [m.questions[1], m.questions[0]];
      mRepo.findById.mockResolvedValueOnce(m);
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      const r = await svc.getMediaGroupDetail(1);
      expect(r.Questions[0].OrderInGroup).toBe(1);
      expect(r.Questions[1].OrderInGroup).toBe(2);
      expect(r.TotalQuestions).toBe(2);
    });

    /**
     * Chức năng: Xem chi tiết nhóm câu hỏi.
     * Trường hợp: Truyền vào ID không tồn tại.
     * Kỳ vọng: Ném lỗi 'Media group not found'.
     */
    it('[TC_10] Báo lỗi khi không tìm thấy nhóm câu hỏi', async () => {
      mRepo.findById.mockResolvedValueOnce(null);
      await expect(svc.getMediaGroupDetail(999)).rejects.toThrow('Media group not found');
    });

    /**
     * Chức năng: Xem chi tiết nhóm câu hỏi.
     * Trường hợp: Relation questions bị undefined do lỗi ORM hoặc DB.
     * Kỳ vọng: Trả về mảng Questions rỗng thay vì crash ứng dụng.
     */
    it('[TC_11] Xử lý an toàn khi không load được questions từ DB', async () => {
      mRepo.findById.mockResolvedValueOnce(createMockMedia({ questions: undefined }));
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      const r = await svc.getMediaGroupDetail(1);
      expect(r.Questions).toEqual([]);
    });

    /**
     * Chức năng: Xem chi tiết nhóm câu hỏi.
     * Trường hợp: Relation choices bên trong question bị undefined.
     * Kỳ vọng: Trả về mảng Choices rỗng cho câu hỏi đó.
     */
    it('[TC_12] Xử lý an toàn khi không load được choices từ DB', async () => {
      const m = createMockMedia(); m.questions[0].choices = undefined;
      mRepo.findById.mockResolvedValueOnce(m);
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      const r = await svc.getMediaGroupDetail(1);
      expect(r.Questions[0].Choices).toEqual([]);
    });

    /**
     * Chức năng: Xem chi tiết nhóm câu hỏi.
     * Trường hợp: Lỗi logic khi sort array.
     * LỖI THỰC TẾ: Code dev dùng trực tiếp `.sort()` làm thay đổi thứ tự mảng questions gốc từ repository.
     */
    it('[TC_13] LỖI: .sort() làm biến đổi mảng gốc từ repository', async () => {
      const m = createMockMedia();
      m.questions = [m.questions[1], m.questions[0]];
      const orig = m.questions.map((q: any) => q.ID);
      mRepo.findById.mockResolvedValueOnce(m);
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      await svc.getMediaGroupDetail(1);
      // Kỳ vọng mảng gốc không bị đổi thứ tự
      expect(m.questions.map((q: any) => q.ID)).toEqual(orig);
    });
  });

  // ==============================================
  // C. CREATE (Tạo mới)
  // ==============================================
  describe('Tạo mới (createMediaGroup)', () => {
    
    /**
     * Chức năng: Tạo mới nhóm câu hỏi.
     * Trường hợp: Payload DTO hợp lệ toàn bộ (Happy Path).
     * Kỳ vọng: Gọi repository tạo media và questions, trả về ID mới.
     */
    it('[TC_14] Tạo nhóm câu hỏi mới thành công', async () => {
      mRepo.create.mockResolvedValueOnce({ ID: 5 } as any);
      qRepo.createMultipleForMedia.mockResolvedValueOnce([{ ID: 50 }] as any);
      mRepo.findById.mockResolvedValueOnce(createMockMedia({ ID: 5 }));
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      const r = await svc.createMediaGroup(createDto(), 1);
      expect(r.MediaQuestionID).toBe(5);
    });

    /**
     * Chức năng: Ràng buộc số lượng câu hỏi.
     * Trường hợp: Payload gửi lên không có câu hỏi nào (mảng rỗng).
     * Kỳ vọng: Báo lỗi yêu cầu ít nhất 1 câu hỏi.
     */
    it('[TC_15] Báo lỗi khi tạo nhóm không có câu hỏi', async () => {
      await expect(svc.createMediaGroup(createDto({ Questions: [] }), 1))
        .rejects.toThrow('at least one question');
    });

    /**
     * Chức năng: Ràng buộc thứ tự câu hỏi.
     * Trường hợp: Khách hàng gửi lên 2 câu hỏi có cùng OrderInGroup = 1.
     * Kỳ vọng: Báo lỗi yêu cầu OrderInGroup phải unique.
     */
    it('[TC_16] Báo lỗi khi trùng lặp thứ tự OrderInGroup', async () => {
      const d = createDto({ Questions: [
        { QuestionText: 'Q1', OrderInGroup: 1, Choices: createDto().Questions[0].Choices },
        { QuestionText: 'Q2', OrderInGroup: 1, Choices: createDto().Questions[0].Choices },
      ]});
      await expect(svc.createMediaGroup(d, 1)).rejects.toThrow('unique');
    });

    /**
     * Chức năng: Ràng buộc đáp án câu hỏi.
     * Trường hợp: Câu hỏi chỉ có 1 đáp án.
     * Kỳ vọng: Báo lỗi yêu cầu tối thiểu 2 đáp án.
     */
    it('[TC_17] Báo lỗi khi câu hỏi có ít hơn 2 đáp án', async () => {
      const d = createDto({ Questions: [{ QuestionText: 'Q', OrderInGroup: 1,
        Choices: [{ Content: 'A', Attribute: 'A', IsCorrect: true }] }] });
      await expect(svc.createMediaGroup(d, 1)).rejects.toThrow('at least 2 choices');
    });

    /**
     * Chức năng: Ràng buộc đáp án đúng.
     * Trường hợp: Câu hỏi có 2 đáp án nhưng không có cái nào IsCorrect = true.
     * Kỳ vọng: Báo lỗi yêu cầu chính xác 1 đáp án đúng.
     */
    it('[TC_18] Báo lỗi khi câu hỏi không có đáp án đúng', async () => {
      const d = createDto({ Questions: [{ QuestionText: 'Q', OrderInGroup: 1,
        Choices: [
          { Content: 'A', Attribute: 'A', IsCorrect: false },
          { Content: 'B', Attribute: 'B', IsCorrect: false },
        ] }] });
      await expect(svc.createMediaGroup(d, 1)).rejects.toThrow('exactly one correct');
    });

    /**
     * Chức năng: Quản lý giao dịch DB (Transaction).
     * Trường hợp: DB throw lỗi khi insert mảng questions.
     * Kỳ vọng: Service phải bắt và ném lại lỗi DB cho caller xử lý.
     */
    it('[TC_19] Báo lỗi khi quá trình insert câu hỏi vào DB thất bại', async () => {
      mRepo.create.mockResolvedValueOnce({ ID: 5 } as any);
      qRepo.createMultipleForMedia.mockRejectedValueOnce(new Error('DB error'));
      await expect(svc.createMediaGroup(createDto(), 1)).rejects.toThrow('DB error');
    });

    /**
     * Chức năng: Quản lý giao dịch DB (Transaction Rollback).
     * Trường hợp: Đã tạo MediaGroup nhưng bị lỗi khi tạo Questions.
     * LỖI THỰC TẾ: Code dev thiếu logic xóa MediaGroup vừa tạo khi Questions bị lỗi, gây rác DB.
     */
    it('[TC_20] LỖI: Không rollback dữ liệu Media khi tạo Questions thất bại', async () => {
      mRepo.create.mockResolvedValueOnce({ ID: 77 } as any);
      qRepo.createMultipleForMedia.mockRejectedValueOnce(new Error('FK error'));
      try { await svc.createMediaGroup(createDto(), 1); } catch (e) {}
      expect(mRepo.delete).toHaveBeenCalledWith(77); // Fail: không có hàm rollback
    });

    /**
     * Chức năng: Ràng buộc Audio cho kỹ năng Listening.
     * Trường hợp: Tạo nhóm câu hỏi LISTENING nhưng trường AudioUrl bị bỏ trống.
     * LỖI THỰC TẾ: Dev chỉ dùng console.warn thay vì chặn (throw Error), dẫn đến bài nghe không có file âm thanh.
     */
    it('[TC_21] LỖI: Chỉ cảnh báo thay vì chặn lưu bài Listening không có AudioUrl', async () => {
      const d = createDto({ Media: { Skill: 'LISTENING', Type: 'CONVERSATION', Section: '3' } } as any);
      jest.spyOn(console, 'warn').mockImplementation();
      mRepo.create.mockResolvedValueOnce({ ID: 88 } as any);
      qRepo.createMultipleForMedia.mockResolvedValueOnce([{ ID: 880 }] as any);
      mRepo.findById.mockResolvedValueOnce(createMockMedia({ ID: 88 }));
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      await expect(svc.createMediaGroup(d, 1)).rejects.toThrow('AudioUrl is required');
      (console.warn as jest.Mock).mockRestore();
    });

    /**
     * Chức năng: Chuẩn hóa dữ liệu đầu vào.
     * Trường hợp: Tiêu đề nhóm (Title) chỉ chứa khoảng trắng.
     * LỖI THỰC TẾ: Không có logic trim() và chặn tiêu đề rỗng, dẫn đến hiển thị UI bị lỗi.
     */
    it('[TC_22] LỖI: Chấp nhận lưu tiêu đề nhóm (Title) chỉ chứa khoảng trắng', async () => {
      mRepo.create.mockResolvedValueOnce({ ID: 89 } as any);
      qRepo.createMultipleForMedia.mockResolvedValueOnce([{ ID: 890 }] as any);
      mRepo.findById.mockResolvedValueOnce(createMockMedia({ ID: 89, GroupTitle: '   ' }));
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      await expect(svc.createMediaGroup(createDto({ Title: '   ' }), 1)).rejects.toThrow('Title cannot be empty');
    });

    /**
     * Chức năng: Ràng buộc định danh đáp án.
     * Trường hợp: Cùng 1 câu hỏi có 2 đáp án mang cùng Attribute (VD: 2 đáp án A).
     * LỖI THỰC TẾ: Thiếu logic kiểm tra uniqueness của Attribute trong mảng Choices.
     */
    it('[TC_23] LỖI: Chấp nhận các đáp án có ký hiệu (Attribute) trùng nhau', async () => {
      const d = createDto({ Questions: [{ QuestionText: 'Q', OrderInGroup: 1,
        Choices: [
          { Content: 'X', Attribute: 'A', IsCorrect: true },
          { Content: 'Y', Attribute: 'A', IsCorrect: false },
        ] }] } as any);
      mRepo.create.mockResolvedValueOnce({ ID: 90 } as any);
      qRepo.createMultipleForMedia.mockResolvedValueOnce([{ ID: 900 }] as any);
      mRepo.findById.mockResolvedValueOnce(createMockMedia({ ID: 90 }));
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      await expect(svc.createMediaGroup(d, 1)).rejects.toThrow('Choice attributes must be unique');
    });

    /**
     * Chức năng: Ràng buộc độ dài tối đa của Tiêu đề (MaxLength).
     * Trường hợp: Tiêu đề nhập vào quá dài (> 500 ký tự).
     * LỖI THỰC TẾ: Không có logic check độ dài, có thể gây crash DB nếu cột là VARCHAR.
     */
    it('[TC_24] LỖI: Không giới hạn độ dài của tiêu đề nhóm (> 500 ký tự)', async () => {
      const longTitle = 'A'.repeat(505);
      mRepo.create.mockResolvedValueOnce({ ID: 91 } as any);
      qRepo.createMultipleForMedia.mockResolvedValueOnce([{ ID: 910 }] as any);
      mRepo.findById.mockResolvedValueOnce(createMockMedia({ ID: 91, GroupTitle: longTitle }));
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      await expect(svc.createMediaGroup(createDto({ Title: longTitle }), 1)).rejects.toThrow('Title exceeds maximum length of 500 characters');
    });

    /**
     * Chức năng: Ràng buộc độ dài tối đa của Mô tả (MaxLength).
     * Trường hợp: Mô tả nhập vào quá dài (> 1000 ký tự).
     * LỖI THỰC TẾ: Không có logic check độ dài mô tả.
     */
    it('[TC_25] LỖI: Không giới hạn độ dài của mô tả nhóm (> 1000 ký tự)', async () => {
      const longDesc = 'A'.repeat(1005);
      mRepo.create.mockResolvedValueOnce({ ID: 92 } as any);
      qRepo.createMultipleForMedia.mockResolvedValueOnce([{ ID: 920 }] as any);
      mRepo.findById.mockResolvedValueOnce(createMockMedia({ ID: 92, GroupDescription: longDesc }));
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      await expect(svc.createMediaGroup(createDto({ Description: longDesc }), 1)).rejects.toThrow('Description exceeds maximum length of 1000 characters');
    });
  });

  // ==============================================
  // D. UPDATE (Cập nhật)
  // ==============================================
  describe('Cập nhật (updateMediaGroupMetadata)', () => {
    
    /**
     * Chức năng: Cập nhật thông tin nhóm.
     * Trường hợp: Client chỉ truyền lên 1 trường (Title) (Partial Update).
     * Kỳ vọng: Service chỉ cập nhật đúng trường Title, giữ nguyên các trường khác.
     */
    it('[TC_26] Cập nhật thông tin cục bộ (chỉ trường được chọn)', async () => {
      mRepo.findById.mockResolvedValue(createMockMedia());
      mRepo.update.mockResolvedValueOnce(createMockMedia());
      mRepo.getUsageStats.mockResolvedValue(USAGE_ZERO);
      await svc.updateMediaGroupMetadata(1, { Title: 'New' } as any);
      expect(mRepo.update.mock.calls[0][1]).toHaveProperty('GroupTitle', 'New');
    });

    /**
     * Chức năng: Cập nhật thông tin nhóm.
     * Trường hợp: Cập nhật toàn bộ các trường metadata và URL media (Full Update).
     * Kỳ vọng: Gói cập nhật gửi tới repository chứa đầy đủ các trường đã map đúng tên.
     */
    it('[TC_27] Cập nhật toàn bộ các trường thông tin của nhóm', async () => {
      mRepo.findById.mockResolvedValue(createMockMedia());
      mRepo.update.mockResolvedValueOnce(createMockMedia());
      mRepo.getUsageStats.mockResolvedValue(USAGE_ZERO);
      
      await svc.updateMediaGroupMetadata(1, { 
        Title: 'New Title',
        Description: 'New Desc',
        Difficulty: 'HARD',
        Tags: ['tag1', 'tag2'],
        Media: {
          AudioUrl: 'http://new.mp3',
          ImageUrl: 'http://new.jpg',
          Script: 'New Script'
        }
      } as any);

      const updates = mRepo.update.mock.calls[0][1];
      expect(updates).toHaveProperty('GroupTitle', 'New Title');
      expect(updates).toHaveProperty('GroupDescription', 'New Desc');
      expect(updates).toHaveProperty('Difficulty', 'HARD');
      expect(updates).toHaveProperty('Tags', ['tag1', 'tag2']);
      expect(updates).toHaveProperty('AudioUrl', 'http://new.mp3');
      expect(updates).toHaveProperty('ImageUrl', 'http://new.jpg');
      expect(updates).toHaveProperty('Scirpt', 'New Script');
    });

    /**
     * Chức năng: Cập nhật thông tin nhóm.
     * Trường hợp: ID truyền vào không tồn tại trong hệ thống.
     * Kỳ vọng: Từ chối thao tác và ném lỗi Not Found.
     */
    it('[TC_28] Báo lỗi khi cập nhật nhóm không tồn tại', async () => {
      mRepo.findById.mockResolvedValueOnce(null);
      await expect(svc.updateMediaGroupMetadata(999, { Title: 'X' } as any))
        .rejects.toThrow('Media group not found');
    });
  });

  // ==============================================
  // E. DELETE (Xóa)
  // ==============================================
  describe('Xóa (deleteMediaGroup)', () => {
    
    /**
     * Chức năng: Xóa toàn bộ nhóm câu hỏi.
     * Trường hợp: Nhóm thỏa mãn điều kiện xóa.
     * Kỳ vọng: Đảm bảo thứ tự xóa đúng quy trình (xóa Questions trước rồi mới xóa MediaGroup).
     */
    it('[TC_29] Xóa toàn bộ dữ liệu nhóm theo đúng thứ tự Cascade', async () => {
      mRepo.findById.mockResolvedValueOnce(createMockMedia());
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      const order: string[] = [];
      qRepo.deleteByMediaQuestionId.mockImplementationOnce(async () => { order.push('q'); return 2; });
      mRepo.delete.mockImplementationOnce(async () => { order.push('m'); return true; });
      const r = await svc.deleteMediaGroup(1);
      expect(r).toBe(true);
      expect(order).toEqual(['q', 'm']); // Thứ tự xóa phải chuẩn
    });

    /**
     * Chức năng: Xóa toàn bộ nhóm câu hỏi.
     * Trường hợp: Cố tình xóa 1 nhóm không tồn tại.
     * Kỳ vọng: Ném lỗi ngăn chặn hành động xóa dư thừa.
     */
    it('[TC_30] Báo lỗi khi xóa nhóm không tồn tại', async () => {
      mRepo.findById.mockResolvedValueOnce(null);
      await expect(svc.deleteMediaGroup(999)).rejects.toThrow('Media group not found');
    });

    /**
     * Chức năng: Ràng buộc dữ liệu tham chiếu (Data Integrity).
     * Trường hợp: Nhóm câu hỏi này đã được gán vào 1 bài thi thực tế.
     * Kỳ vọng: Bắt buộc chặn thao tác xóa để bảo vệ dữ liệu lịch sử thi của học sinh.
     */
    it('[TC_31] Chặn xóa khi nhóm câu hỏi đang nằm trong kỳ thi', async () => {
      mRepo.findById.mockResolvedValueOnce(createMockMedia());
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ACTIVE); // usage > 0
      await expect(svc.deleteMediaGroup(1)).rejects.toThrow('Cannot delete');
      expect(mRepo.delete).not.toHaveBeenCalled();
    });

    /**
     * Chức năng: Ràng buộc dữ liệu tham chiếu (Data Integrity).
     * Trường hợp: DB gặp sự cố khiến hàm getUsageStats trả về null (không rõ trạng thái).
     * LỖI THỰC TẾ: Lệnh if (usageStats && usageStats.used > 0) bị sai logic null-safety, dẫn đến việc VẪN CHO PHÉP XÓA dù không rõ trạng thái.
     */
    it('[TC_32] LỖI: Bypass bảo vệ và cho phép xóa khi hệ thống không lấy được usageStats', async () => {
      mRepo.findById.mockResolvedValueOnce(createMockMedia());
      mRepo.getUsageStats.mockResolvedValueOnce(null); // DB lỗi trả về null
      qRepo.deleteByMediaQuestionId.mockResolvedValueOnce(2);
      mRepo.delete.mockResolvedValueOnce(true);
      // Kỳ vọng: Phải chặn xóa khi không rõ trạng thái (Null/Undefined)
      await expect(svc.deleteMediaGroup(1)).rejects.toThrow('Cannot verify usage stats');
    });
  });

  // ==============================================
  // F. ADD QUESTION (Thêm câu hỏi)
  // ==============================================
  describe('Thêm câu hỏi (addQuestionToGroup)', () => {
    const vq = { QuestionText: 'Q', OrderInGroup: 3,
      Choices: [
        { Content: 'A', Attribute: 'A', IsCorrect: true },
        { Content: 'B', Attribute: 'B', IsCorrect: false },
      ] };

    /**
     * Chức năng: Thêm 1 câu hỏi con vào nhóm đã tồn tại.
     * Trường hợp: Truyền đầy đủ dữ liệu hợp lệ (OrderInGroup không trùng).
     * Kỳ vọng: Tạo thành công và trả về entity câu hỏi đó.
     */
    it('[TC_33] Thêm câu hỏi thành công', async () => {
      mRepo.findById.mockResolvedValueOnce(createMockMedia());
      qRepo.isOrderInGroupUnique.mockResolvedValueOnce(true);
      qRepo.createMultipleForMedia.mockResolvedValueOnce([{ ID: 99, OrderInGroup: 3 }] as any);
      const r = await svc.addQuestionToGroup(1, vq, 1);
      expect(r.ID).toBe(99);
    });

    /**
     * Chức năng: Tự động đánh số thứ tự (Auto-increment).
     * Trường hợp: Client không truyền OrderInGroup hoặc truyền giá trị 0.
     * Kỳ vọng: Hàm getNextOrderInGroup được gọi để tự gán ID tiếp theo.
     */
    it('[TC_34] Tự động gán số thứ tự nếu client không cung cấp', async () => {
      mRepo.findById.mockResolvedValueOnce(createMockMedia());
      qRepo.getNextOrderInGroup.mockResolvedValueOnce(3);
      qRepo.isOrderInGroupUnique.mockResolvedValueOnce(true);
      qRepo.createMultipleForMedia.mockResolvedValueOnce([{ ID: 99 }] as any);
      await svc.addQuestionToGroup(1, { ...vq, OrderInGroup: 0 }, 1);
      expect(qRepo.getNextOrderInGroup).toHaveBeenCalledWith(1);
    });

    /**
     * Chức năng: Ràng buộc thứ tự câu hỏi (Unique constraint).
     * Trường hợp: Client truyền số thứ tự đã bị chiếm bởi câu hỏi khác trong cùng nhóm.
     * Kỳ vọng: Chặn và báo lỗi "already used".
     */
    it('[TC_35] Báo lỗi khi số thứ tự (OrderInGroup) đã bị chiếm', async () => {
      mRepo.findById.mockResolvedValueOnce(createMockMedia());
      qRepo.isOrderInGroupUnique.mockResolvedValueOnce(false);
      await expect(svc.addQuestionToGroup(1, { ...vq, OrderInGroup: 1 }, 1)).rejects.toThrow('already used');
    });

    /**
     * Chức năng: Ràng buộc quan hệ.
     * Trường hợp: Client thêm câu hỏi vào 1 MediaGroup ID không tồn tại.
     * Kỳ vọng: Từ chối thao tác ngay từ bước kiểm tra đầu tiên.
     */
    it('[TC_36] Báo lỗi khi thêm câu hỏi vào nhóm không tồn tại', async () => {
      mRepo.findById.mockResolvedValueOnce(null);
      await expect(svc.addQuestionToGroup(999, vq, 1)).rejects.toThrow('Media group not found');
    });

    /**
     * Chức năng: Ràng buộc giá trị hợp lệ.
     * Trường hợp: Client cố tình truyền OrderInGroup là số âm (-1).
     * LỖI THỰC TẾ: Do code dev dùng `!orderInGroup` (kiểm tra falsy) nên số âm (-1) vẫn lọt qua kiểm tra và được ghi vào DB.
     */
    it('[TC_37] LỖI: Cho phép lưu thứ tự câu hỏi là số âm', async () => {
      mRepo.findById.mockResolvedValueOnce(createMockMedia());
      qRepo.isOrderInGroupUnique.mockResolvedValueOnce(true);
      qRepo.createMultipleForMedia.mockResolvedValueOnce([{ ID: 99 }] as any);
      // Kỳ vọng: Phải chặn giá trị OrderInGroup <= 0
      await expect(svc.addQuestionToGroup(1, { ...vq, OrderInGroup: -1 }, 1)).rejects.toThrow('OrderInGroup must be positive');
    });

    /**
     * Chức năng: Quản lý biến đầu vào (Immutability).
     * Trường hợp: Service thay đổi trực tiếp thuộc tính của object DTO do client truyền vào.
     * LỖI THỰC TẾ: Biến DTO bị mutate trực tiếp (`questionData.OrderInGroup = ...`), vi phạm nguyên tắc Clean Code và gây lỗi side-effect nếu DTO này được dùng ở nơi khác.
     */
    it('[TC_38] LỖI: Service làm biến đổi (mutate) object DTO truyền vào', async () => {
      mRepo.findById.mockResolvedValueOnce(createMockMedia());
      qRepo.getNextOrderInGroup.mockResolvedValueOnce(5);
      qRepo.isOrderInGroupUnique.mockResolvedValueOnce(true);
      qRepo.createMultipleForMedia.mockResolvedValueOnce([{ ID: 99 }] as any);
      const input = { ...vq, OrderInGroup: 0 };
      await svc.addQuestionToGroup(1, input, 1);
      // Kỳ vọng: Input DTO không bị thay đổi, giữ nguyên giá trị 0
      expect(input.OrderInGroup).toBe(0);
    });
  });

  // ==============================================
  // G. REMOVE QUESTION (Xóa câu hỏi)
  // ==============================================
  describe('Xóa câu hỏi (removeQuestionFromGroup)', () => {
    
    /**
     * Chức năng: Xóa 1 câu hỏi con khỏi nhóm.
     * Trường hợp: Câu hỏi hợp lệ và không nằm trong bài thi nào.
     * Kỳ vọng: Xóa thành công.
     */
    it('[TC_39] Xóa câu hỏi thành công', async () => {
      qRepo.findById.mockResolvedValueOnce({ ID: 10, MediaQuestionID: 1 } as any);
      qRepo.getUsageStats.mockResolvedValueOnce({ usedInExams: 0 });
      qRepo.delete.mockResolvedValueOnce(true);
      expect(await svc.removeQuestionFromGroup(1, 10)).toBe(true);
    });

    /**
     * Chức năng: Xóa 1 câu hỏi con khỏi nhóm.
     * Trường hợp: Câu hỏi không tồn tại.
     * Kỳ vọng: Báo lỗi Not Found.
     */
    it('[TC_40] Báo lỗi khi câu hỏi cần xóa không tồn tại', async () => {
      qRepo.findById.mockResolvedValueOnce(null);
      await expect(svc.removeQuestionFromGroup(1, 999)).rejects.toThrow('Question not found');
    });

    /**
     * Chức năng: Xác thực quan hệ cha-con trước khi xóa.
     * Trường hợp: Xóa 1 câu hỏi có tồn tại nhưng nó thuộc về MediaGroup khác (sai tham chiếu).
     * Kỳ vọng: Báo lỗi chặn hành vi xóa dữ liệu chéo.
     */
    it('[TC_41] Báo lỗi khi cố gắng xóa câu hỏi của nhóm khác', async () => {
      qRepo.findById.mockResolvedValueOnce({ ID: 10, MediaQuestionID: 99 } as any);
      await expect(svc.removeQuestionFromGroup(1, 10)).rejects.toThrow('Question not found');
    });

    /**
     * Chức năng: Ràng buộc dữ liệu tham chiếu.
     * Trường hợp: Câu hỏi này đang nằm trong 1 bài thi của học sinh.
     * Kỳ vọng: Chặn xóa để không làm hỏng bài thi cũ.
     */
    it('[TC_42] Chặn xóa khi câu hỏi đang nằm trong bài thi thực tế', async () => {
      qRepo.findById.mockResolvedValueOnce({ ID: 10, MediaQuestionID: 1 } as any);
      qRepo.getUsageStats.mockResolvedValueOnce({ usedInExams: 2 });
      await expect(svc.removeQuestionFromGroup(1, 10)).rejects.toThrow('Cannot remove');
      expect(qRepo.delete).not.toHaveBeenCalled();
    });

    /**
     * Chức năng: Ràng buộc dữ liệu tham chiếu (Safe-guard).
     * Trường hợp: Hàm kiểm tra usageStats bị lỗi nội bộ hoặc DB lỗi trả về null.
     * LỖI THỰC TẾ: Giống hệt lỗi xóa MediaGroup, check `null` bị sai khiến câu hỏi vẫn bị xóa.
     */
    it('[TC_43] LỖI: Bypass bảo vệ và cho phép xóa khi usageStats của câu hỏi bị null', async () => {
      qRepo.findById.mockResolvedValueOnce({ ID: 10, MediaQuestionID: 1 } as any);
      qRepo.getUsageStats.mockResolvedValueOnce(null); // DB lỗi trả về null
      qRepo.delete.mockResolvedValueOnce(true);
      // Kỳ vọng: Phải chặn xóa khi không rõ trạng thái (Null/Undefined)
      await expect(svc.removeQuestionFromGroup(1, 10)).rejects.toThrow('Cannot verify usage stats');
    });
  });

  // ==============================================
  // H. STATISTICS (Thống kê)
  // ==============================================
  describe('Thống kê (getMediaGroupStatistics)', () => {
    
    /**
     * Chức năng: Lấy thông tin thống kê lượt sử dụng.
     * Trường hợp: Nhóm hợp lệ, có dữ liệu sử dụng.
     * Kỳ vọng: Trả về số lần dùng trong exam và số lượt làm bài (attempts).
     */
    it('[TC_44] Trả về số liệu thống kê sử dụng chính xác', async () => {
      mRepo.findById.mockResolvedValueOnce(createMockMedia());
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ACTIVE);
      const r = await svc.getMediaGroupStatistics(1);
      expect(r.mediaGroupId).toBe(1);
      expect(r.usedInExams).toBe(3);
      expect(r.totalAttempts).toBe(50);
    });

    /**
     * Chức năng: Lấy thông tin thống kê.
     * Trường hợp: ID không tồn tại.
     * Kỳ vọng: Báo lỗi thay vì tính toán sai.
     */
    it('[TC_45] Báo lỗi thống kê nếu nhóm không tồn tại', async () => {
      mRepo.findById.mockResolvedValueOnce(null);
      await expect(svc.getMediaGroupStatistics(999)).rejects.toThrow('Media group not found');
    });
  });

  // ==============================================
  // I. CLONE (Nhân bản)
  // ==============================================
  describe('Nhân bản (cloneMediaGroup)', () => {
    
    /**
     * Chức năng: Copy (Nhân bản) 1 nhóm câu hỏi.
     * Trường hợp: Thực thi đúng quy trình (Happy Path).
     * Kỳ vọng: Clone MediaGroup tạo ra ID mới, sau đó clone toàn bộ Questions trỏ về ID mới này.
     */
    it('[TC_46] Nhân bản thành công cả nhóm và danh sách câu hỏi', async () => {
      mRepo.findById.mockResolvedValueOnce(createMockMedia()).mockResolvedValueOnce(createMockMedia({ ID: 99 }));
      mRepo.clone.mockResolvedValueOnce({ ...createMockMedia(), ID: 99 });
      qRepo.cloneQuestionsToMedia.mockResolvedValueOnce(createMockMedia().questions);
      mRepo.getUsageStats.mockResolvedValueOnce(USAGE_ZERO);
      const r = await svc.cloneMediaGroup(1, 42, 'Clone');
      expect(mRepo.clone).toHaveBeenCalledWith(1, 'Clone');
      expect(qRepo.cloneQuestionsToMedia).toHaveBeenCalledWith(1, 99, 42);
      expect(r.MediaQuestionID).toBe(99);
    });

    /**
     * Chức năng: Nhân bản nhóm câu hỏi.
     * Trường hợp: Cố gắng nhân bản một nhóm không tồn tại.
     * Kỳ vọng: Chặn thao tác ngay từ đầu.
     */
    it('[TC_47] Báo lỗi khi nhóm gốc dùng để nhân bản không tồn tại', async () => {
      mRepo.findById.mockResolvedValueOnce(null);
      await expect(svc.cloneMediaGroup(999, 1)).rejects.toThrow('Media group not found');
    });

    /**
     * Chức năng: Xử lý lỗi trong quá trình nhân bản.
     * Trường hợp: Hàm clone MediaGroup của Repository trả về null (lỗi DB).
     * Kỳ vọng: Bắt lỗi và dừng quá trình clone Questions.
     */
    it('[TC_48] Dừng quy trình và báo lỗi nếu việc nhân bản Media thất bại', async () => {
      mRepo.findById.mockResolvedValueOnce(createMockMedia());
      mRepo.clone.mockResolvedValueOnce(null);
      await expect(svc.cloneMediaGroup(1, 1)).rejects.toThrow('Failed to clone');
    });

    /**
     * Chức năng: Xử lý lỗi trong quá trình nhân bản.
     * Trường hợp: Nhân bản Media thành công nhưng quá trình nhân bản Questions bị lỗi ném ra.
     * Kỳ vọng: Service bắt được lỗi và ném ra cho caller (Cần có Transaction thực thụ ở repo để tự động Rollback Media vừa tạo).
     */
    it('[TC_49] Truyền lỗi lên khi việc nhân bản danh sách câu hỏi thất bại', async () => {
      mRepo.findById.mockResolvedValueOnce(createMockMedia());
      mRepo.clone.mockResolvedValueOnce({ ...createMockMedia(), ID: 99 });
      qRepo.cloneQuestionsToMedia.mockRejectedValueOnce(new Error('Clone fail'));
      await expect(svc.cloneMediaGroup(1, 1)).rejects.toThrow('Clone fail');
    });
  });
});
