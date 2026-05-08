import { AttemptService } from '../attempt.service';
import { AttemptRepository } from '../../../infrastructure/repositories/attempt.repository';
import { ExamRepository } from '../../../infrastructure/repositories/exam.repository';

jest.mock('../../../infrastructure/repositories/attempt.repository');
jest.mock('../../../infrastructure/repositories/exam.repository');

// Quy ước chú thích trong file test:
// INPUT: dữ liệu truyền trực tiếp vào hàm service cần test.
// MOCK DATA: dữ liệu giả lập repository trả về, không phải dữ liệu DB thật.
// ACTION: lời gọi hàm service đang được kiểm thử.
// EXPECTED: kết quả trả về, lỗi ném ra, hoặc mock repository call được kỳ vọng.

// =====================================================================
// Helper: Tạo mock attempt đầy đủ cho submit/result tests
// =====================================================================
const buildMockGradedAttempt = (overrides: any = {}) => ({
  ID: 1,
  StudentProfileID: 1,
  SubmittedAt: new Date(),
  StartedAt: new Date(Date.now() - 90 * 60000),
  ScoreListening: 245,
  ScoreReading: 280,
  ScorePercent: 87,
  exam: { ID: 1, Title: 'ETS 2023 Test 1', TimeExam: 120 },
  attemptAnswers: [
    {
      IsCorrect: true,
      question: {
        ID: 1, QuestionText: 'What is the woman doing?',
        mediaQuestion: { Skill: 'LISTENING', Type: 'Part1', Section: '1', AudioUrl: null, ImageUrl: null, Scirpt: null },
        choices: [
          { ID: 1, IsCorrect: true, Attribute: 'A', Content: 'She is reading.' },
          { ID: 2, IsCorrect: false, Attribute: 'B', Content: 'She is walking.' },
        ],
      },
      choice: { ID: 1, Attribute: 'A', Content: 'She is reading.' },
    },
    {
      IsCorrect: false,
      question: {
        ID: 2, QuestionText: 'Where is the meeting?',
        mediaQuestion: { Skill: 'READING', Type: 'Part5', Section: '5', AudioUrl: null, ImageUrl: null, Scirpt: null },
        choices: [
          { ID: 3, IsCorrect: false, Attribute: 'A', Content: 'Conference room.' },
          { ID: 4, IsCorrect: true, Attribute: 'B', Content: 'Cafeteria.' },
        ],
      },
      choice: { ID: 3, Attribute: 'A', Content: 'Conference room.' },
    },
  ],
  ...overrides,
});

describe('AttemptService - Luyện đề (Student Flow)', () => {
  let service: AttemptService;
  let mockAttemptRepo: jest.Mocked<AttemptRepository>;
  let mockExamRepo: jest.Mocked<ExamRepository>;

  beforeEach(() => {
    jest.clearAllMocks();
    service = new AttemptService();
    mockAttemptRepo = new AttemptRepository() as jest.Mocked<AttemptRepository>;
    mockExamRepo = new ExamRepository() as jest.Mocked<ExamRepository>;
    (service as any).attemptRepository = mockAttemptRepo;
    (service as any).examRepository = mockExamRepo;
  });

  // ==================================================================
  // LUỒNG 1: startAttempt - Bắt đầu làm bài
  // ==================================================================
  describe('Luồng 1 - startAttempt (Bắt đầu làm bài)', () => {

    /**
     * Chức năng: Bắt đầu làm bài thi FULL_TEST.
     * Trường hợp: Tham số hợp lệ, đề thi tồn tại (Happy Path).
     * Kỳ vọng: Tạo attempt mới với trạng thái chưa nộp (SubmittedAt = null).
     */
    it('[TC_ATT_001] Bắt đầu làm bài FULL_TEST thành công', async () => {
      // MOCK DATA: đề thi tồn tại và repository tạo attempt mới chưa nộp bài.
      mockExamRepo.findById.mockResolvedValueOnce({ ID: 1, Title: 'ETS 2023' } as any);
      mockAttemptRepo.create.mockResolvedValueOnce({
        ID: 99, StudentProfileID: 1, ExamID: 1, Type: 'FULL_TEST',
        ScorePercent: null, ScoreListening: null, ScoreReading: null,
        SubmittedAt: null,
      } as any);

      // INPUT + ACTION: student 1 bắt đầu làm ExamID=1 với loại FULL_TEST.
      const result = await service.startAttempt({ ExamID: 1, Type: 'FULL_TEST' }, 1);

      // EXPECTED: service trả về attempt vừa tạo và gọi create đúng 1 lần.
      expect(result.ID).toBe(99);
      expect(result.Type).toBe('FULL_TEST');
      expect(result.SubmittedAt).toBeNull();
      expect(mockAttemptRepo.create).toHaveBeenCalledTimes(1);
    });

    /**
     * Chức năng: Bắt đầu luyện tập theo PART.
     * Trường hợp: Tham số hợp lệ, truyền Parts=[5, 6, 7].
     * Kỳ vọng: Tạo attempt PRACTICE_BY_PART thành công.
     */
    it('[TC_ATT_002] Bắt đầu làm bài PRACTICE_BY_PART (Part 5, 6, 7) thành công', async () => {
      // MOCK DATA: đề thi tồn tại, repository tạo attempt luyện theo part.
      mockExamRepo.findById.mockResolvedValueOnce({ ID: 2 } as any);
      mockAttemptRepo.create.mockResolvedValueOnce({
        ID: 100, StudentProfileID: 1, ExamID: 2, Type: 'PRACTICE_BY_PART',
        SubmittedAt: null,
      } as any);

      // INPUT + ACTION: student chọn luyện Part 5, 6, 7.
      const result = await service.startAttempt(
        { ExamID: 2, Type: 'PRACTICE_BY_PART', Parts: [5, 6, 7] }, 1
      );

      // EXPECTED: attempt được tạo với đúng loại PRACTICE_BY_PART.
      expect(result.Type).toBe('PRACTICE_BY_PART');
    });

    /**
     * Chức năng: Bắt đầu làm bài.
     * Trường hợp: Truyền ExamID không tồn tại.
     * Kỳ vọng: Chặn thao tác, ném lỗi 'Exam not found'.
     */
    it('[TC_ATT_003] Exam không tồn tại → ném lỗi "Exam not found"', async () => {
      // MOCK DATA: repository không tìm thấy exam.
      mockExamRepo.findById.mockResolvedValueOnce(undefined as any);

      // INPUT + EXPECTED: ExamID không tồn tại phải bị chặn trước khi tạo attempt.
      await expect(service.startAttempt({ ExamID: 999, Type: 'FULL_TEST' }, 1))
        .rejects.toThrow('Exam not found');
    });

    /**
     * Chức năng: Validate dữ liệu luyện tập theo Part.
     * Trường hợp: Type='PRACTICE_BY_PART' nhưng mảng Parts bị rỗng.
     * Kỳ vọng: Báo lỗi yêu cầu phải chỉ định ít nhất 1 Part.
     */
    it('[TC_ATT_004] PRACTICE_BY_PART không truyền Parts → ném lỗi', async () => {
      // MOCK DATA: exam tồn tại, để test tập trung vào validation Parts.
      mockExamRepo.findById.mockResolvedValueOnce({ ID: 1 } as any);

      // INPUT + EXPECTED: Type yêu cầu Parts nhưng mảng Parts rỗng nên phải throw.
      await expect(service.startAttempt({ ExamID: 1, Type: 'PRACTICE_BY_PART', Parts: [] }, 1))
        .rejects.toThrow(/Parts must be specified/);
    });

    /**
     * Chức năng: Validate dữ liệu luyện tập theo Part.
     * Trường hợp: Mảng Parts chứa giá trị ngoài phạm vi [1-7].
     * Kỳ vọng: Báo lỗi 'Invalid part numbers'.
     */
    it('[TC_ATT_005] Parts chứa số ngoài range 1-7 → ném lỗi', async () => {
      // MOCK DATA: exam tồn tại, để test tập trung vào validation range của Parts.
      mockExamRepo.findById.mockResolvedValueOnce({ ID: 1 } as any);

      // INPUT + EXPECTED: Part 0 và 8 không hợp lệ vì TOEIC chỉ có Part 1-7.
      await expect(service.startAttempt({ ExamID: 1, Type: 'PRACTICE_BY_PART', Parts: [0, 8] }, 1))
        .rejects.toThrow(/Invalid part numbers/);
    });

    /**
     * Chức năng: Validate loại hình bài làm (Type).
     * Trường hợp: Type bị truyền là null.
     * LỖI THỰC TẾ: Không validate giá trị null, dẫn đến insert dirty data vào DB. Test FAIL để báo hiệu bug.
     */
    it('[TC_ATT_006] LỖI: Type=null không bị validate → attempt với dirty data được tạo', async () => {
      // MOCK DATA: nếu service không chặn Type=null, repository vẫn trả về attempt bẩn.
      mockExamRepo.findById.mockResolvedValueOnce({ ID: 1 } as any);
      mockAttemptRepo.create.mockResolvedValueOnce({ ID: 99, Type: null } as any);

      // EXPECTED: hệ thống đúng phải throw thay vì tạo attempt Type=null.
      await expect(service.startAttempt({ ExamID: 1, Type: null as any }, 1))
        .rejects.toThrow('Invalid exam type');
    });

    /**
     * Chức năng: Validate người dùng.
     * Trường hợp: StudentProfileID là số âm (-1).
     * LỖI THỰC TẾ: Không validate ID hợp lệ, gây lỗi toàn vẹn tham chiếu. Test FAIL để báo hiệu bug.
     */
    it('[TC_ATT_007] LỖI: StudentProfileId âm (-1) không bị validate', async () => {
      // MOCK DATA: nếu service không chặn studentId âm, repository vẫn trả attempt không hợp lệ.
      mockExamRepo.findById.mockResolvedValueOnce({ ID: 1 } as any);
      mockAttemptRepo.create.mockResolvedValueOnce({ ID: 99, StudentProfileID: -1 } as any);

      // INPUT + EXPECTED: student profile ID âm phải bị reject.
      await expect(service.startAttempt({ ExamID: 1, Type: 'FULL_TEST' }, -1))
        .rejects.toThrow('Invalid student profile ID');
    });
  });

  // ==================================================================
  // LUỒNG 2: submitAttempt - Nộp bài
  // ==================================================================
  describe('Luồng 2 - submitAttempt (Nộp bài)', () => {

    /**
     * Chức năng: Nộp bài thi.
     * Trường hợp: Dữ liệu hợp lệ, attempt chưa được nộp.
     * Kỳ vọng: Trả về kết quả phân tích đầy đủ (tổng điểm, số câu đúng/sai).
     */
    it('[TC_ATT_008] Nộp bài thành công, nhận kết quả đầy đủ', async () => {
      // MOCK DATA: ongoing là attempt chưa nộp, graded là attempt sau khi chấm điểm.
      const graded = buildMockGradedAttempt();
      const ongoing = buildMockGradedAttempt({ SubmittedAt: null });

      mockAttemptRepo.findById.mockResolvedValueOnce(ongoing as any);
      mockAttemptRepo.submitAnswers.mockResolvedValueOnce(graded as any);

      // INPUT + ACTION: student 1 nộp AttemptID=1 với danh sách câu trả lời rỗng.
      const result = await service.submitAttempt({ AttemptID: 1, answers: [] }, 1);

      // EXPECTED: response có điểm tổng, danh sách đáp án chi tiết và phân tích số câu.
      expect(result.AttemptID).toBe(1);
      expect(result.Scores.TotalScore).toBe(245 + 280);
      expect(result.DetailedAnswers).toHaveLength(2);
      expect(result.Analysis.TotalQuestions).toBe(2);
      expect(result.Analysis.CorrectAnswers).toBe(1);
    });

    /**
     * Chức năng: Phân tích số câu đúng theo kỹ năng (Listening/Reading).
     * Trường hợp: Có 1 câu nghe đúng, 1 câu đọc sai.
     * Kỳ vọng: Thống kê chính xác số câu ListeningCorrect=1 và ReadingCorrect=0.
     */
    it('[TC_ATT_009] Nộp bài xác định đúng câu Nghe và Đọc', async () => {
      // MOCK DATA: attempt đã chấm có 1 câu LISTENING đúng và 1 câu READING sai.
      const graded = buildMockGradedAttempt();
      const ongoing = buildMockGradedAttempt({ SubmittedAt: null });

      mockAttemptRepo.findById.mockResolvedValueOnce(ongoing as any);
      mockAttemptRepo.submitAnswers.mockResolvedValueOnce(graded as any);

      // ACTION: nộp bài để service transform dữ liệu chấm điểm.
      const result = await service.submitAttempt({ AttemptID: 1, answers: [] }, 1);

      // EXPECTED: thống kê đúng theo Skill của mediaQuestion.
      expect(result.Analysis.ListeningCorrect).toBe(1);
      expect(result.Analysis.ReadingCorrect).toBe(0);
    });

    /**
     * Chức năng: Xác định điểm yếu (WeakAreas).
     * Trường hợp: Người dùng làm đúng toàn bộ bài thi.
     * Kỳ vọng: Mảng WeakAreas phải trống.
     */
    it('[TC_ATT_010] Nộp bài với tất cả câu trả lời đúng, WeakAreas rỗng', async () => {
      // MOCK DATA: toàn bộ attemptAnswers đều đúng nên không có điểm yếu.
      const allCorrect = buildMockGradedAttempt({
        SubmittedAt: null,
        attemptAnswers: [
          {
            IsCorrect: true,
            question: {
              ID: 1, QuestionText: 'Q1',
              mediaQuestion: { Skill: 'LISTENING', Type: 'Part1', Section: '1', AudioUrl: null, ImageUrl: null, Scirpt: null },
              choices: [{ ID: 1, IsCorrect: true, Attribute: 'A', Content: 'Yes.' }],
            },
            choice: { ID: 1, Attribute: 'A', Content: 'Yes.' },
          },
        ],
      });
      const graded = { ...allCorrect, SubmittedAt: new Date() };

      mockAttemptRepo.findById.mockResolvedValueOnce(allCorrect as any);
      mockAttemptRepo.submitAnswers.mockResolvedValueOnce(graded as any);

      // ACTION + EXPECTED: sau khi nộp bài, WeakAreas phải rỗng.
      const result = await service.submitAttempt({ AttemptID: 1, answers: [] }, 1);
      expect(result.Analysis.WeakAreas).toHaveLength(0);
    });

    /**
     * Chức năng: Validation attempt tồn tại.
     * Trường hợp: Nộp 1 attempt ID không có trong DB.
     * Kỳ vọng: Báo lỗi 'Attempt not found'.
     */
    it('[TC_ATT_011] Attempt không tồn tại → ném lỗi', async () => {
      // MOCK DATA: repository không tìm thấy attempt.
      mockAttemptRepo.findById.mockResolvedValueOnce(undefined as any);

      // INPUT + EXPECTED: AttemptID không tồn tại phải throw.
      await expect(service.submitAttempt({ AttemptID: 999, answers: [] }, 1))
        .rejects.toThrow('Attempt not found');
    });

    /**
     * Chức năng: Bảo mật dữ liệu cá nhân.
     * Trường hợp: Sinh viên A cố nộp bài của sinh viên B.
     * Kỳ vọng: Chặn và báo lỗi 'You can only submit your own attempts'.
     */
    it('[TC_ATT_012] Student nộp bài của người khác → ném lỗi bảo mật', async () => {
      // MOCK DATA: attempt thuộc StudentProfileID=2 nhưng caller là student 1.
      mockAttemptRepo.findById.mockResolvedValueOnce({
        ID: 1, StudentProfileID: 2, SubmittedAt: null, StartedAt: new Date(), exam: { TimeExam: 120 },
      } as any);

      // EXPECTED: service phải chặn submit attempt không thuộc về mình.
      await expect(service.submitAttempt({ AttemptID: 1, answers: [] }, 1))
        .rejects.toThrow('You can only submit your own attempts');
    });

    /**
     * Chức năng: Ngăn chặn nộp bài nhiều lần (Double submission).
     * Trường hợp: Bài làm đã có SubmittedAt.
     * Kỳ vọng: Báo lỗi 'This attempt has already been submitted'.
     */
    it('[TC_ATT_013] Nộp bài đã được nộp (SubmittedAt có giá trị) → ném lỗi', async () => {
      // MOCK DATA: attempt đã có SubmittedAt, tức đã nộp trước đó.
      mockAttemptRepo.findById.mockResolvedValueOnce({
        ID: 1, StudentProfileID: 1, SubmittedAt: new Date(), StartedAt: new Date(), exam: { TimeExam: 120 },
      } as any);

      // EXPECTED: không được phép nộp lại lần hai.
      await expect(service.submitAttempt({ AttemptID: 1, answers: [] }, 1))
        .rejects.toThrow('This attempt has already been submitted');
    });

    /**
     * Chức năng: Kiểm tra giới hạn thời gian.
     * Trường hợp: Thời gian làm bài vượt quá TimeExam cho phép.
     * Kỳ vọng: Từ chối nộp bài, báo 'Time limit exceeded'.
     */
    it('[TC_ATT_014] Nộp bài vượt quá giới hạn thời gian → ném lỗi', async () => {
      // MOCK DATA: StartedAt lùi 300 phút, vượt xa TimeExam=120.
      const veryOldStart = new Date(Date.now() - 300 * 60000);
      mockAttemptRepo.findById.mockResolvedValueOnce({
        ID: 1, StudentProfileID: 1, SubmittedAt: null,
        StartedAt: veryOldStart,
        exam: { TimeExam: 120 },
      } as any);

      // EXPECTED: service phải reject vì quá thời gian làm bài.
      await expect(service.submitAttempt({ AttemptID: 1, answers: [] }, 1))
        .rejects.toThrow(/Time limit exceeded/);
    });

    /**
     * Chức năng: Xử lý quan hệ DB bị mất.
     * Trường hợp: Exam liên kết với attempt đã bị xóa mềm (null).
     * LỖI THỰC TẾ: Không kiểm tra null dẫn đến NPE. Test báo FAIL do ứng dụng sập thay vì ném lỗi có kiểm soát.
     */
    it('[TC_ATT_015] LỖI: NPE khi attempt.exam là null (exam bị xóa)', async () => {
      // MOCK DATA: attempt hợp lệ nhưng relation exam bị null.
      mockAttemptRepo.findById.mockResolvedValueOnce({
        ID: 1, StudentProfileID: 1, SubmittedAt: null,
        StartedAt: new Date(), exam: null,
      } as any);

      // EXPECTED: hệ thống đúng phải throw lỗi nghiệp vụ có kiểm soát, không crash TypeError.
      await expect(service.submitAttempt({ AttemptID: 1, answers: [] }, 1))
        .rejects.toThrow('Exam no longer exists');
    });

    /**
     * Chức năng: Validate double submission an toàn.
     * Trường hợp: SubmittedAt lưu nhầm dưới dạng chuỗi rỗng (falsy value) trong DB.
     * LỖI THỰC TẾ: Điều kiện `!attempt.SubmittedAt` để cho lọt chuỗi rỗng.
     */
    it('[TC_ATT_016] LỖI: SubmittedAt="" (falsy) cho phép double submission', async () => {
      // MOCK DATA: SubmittedAt là chuỗi rỗng, vẫn biểu thị dữ liệu đã có trạng thái bất thường.
      mockAttemptRepo.findById.mockResolvedValueOnce({
        ID: 1, StudentProfileID: 1, SubmittedAt: '' as any,
        StartedAt: new Date(), exam: { TimeExam: 120 },
      } as any);
      mockAttemptRepo.submitAnswers.mockResolvedValueOnce(buildMockGradedAttempt());

      // EXPECTED: service phải fail-closed và không cho nộp lại.
      await expect(service.submitAttempt({ AttemptID: 1, answers: [] }, 1))
        .rejects.toThrow('This attempt has already been submitted');
    });

    /**
     * Chức năng: Map kết quả trả về an toàn.
     * Trường hợp: Câu hỏi mất mediaQuestion (bị null).
     * LỖI THỰC TẾ: Cố đọc `mediaQuestion.Skill` gây crash (NPE) toàn bộ response.
     */
    it('[TC_ATT_017] LỖI: NPE khi mediaQuestion=null trong attemptAnswers', async () => {
      // MOCK DATA: câu hỏi trong attemptAnswers bị mất relation mediaQuestion.
      const badAttempt = buildMockGradedAttempt({
        SubmittedAt: null,
        attemptAnswers: [{
          IsCorrect: true,
          question: { ID: 1, QuestionText: 'Q?', mediaQuestion: null, choices: [] },
          choice: { ID: 1 },
        }],
      });
      mockAttemptRepo.findById.mockResolvedValueOnce(badAttempt as any);
      mockAttemptRepo.submitAnswers.mockResolvedValueOnce(badAttempt as any);

      // EXPECTED: hệ thống đúng phải báo dữ liệu câu hỏi không hợp lệ.
      await expect(service.submitAttempt({ AttemptID: 1, answers: [] }, 1))
        .rejects.toThrow('Invalid question data');
    });

    /**
     * Chức năng: Trả về đáp án đúng cho học sinh xem lại.
     * Trường hợp: Câu hỏi trong DB không có choice nào IsCorrect=true.
     * DEFECT REPORT: Nếu không tìm thấy, code fallback về object rỗng `{ ID: 0 }` gây nhầm lẫn trên UI.
     * KỲ VỌNG: Ném lỗi 'Invalid question configuration' vì dữ liệu câu hỏi bị sai từ DB.
     */
    it('[TC_ATT_018] DEFECT: Fallback CorrectChoice.ID=0 khi không có đáp án đúng trong DB', async () => {
      // MOCK DATA: choices không có đáp án nào IsCorrect=true.
      const noCorrect = buildMockGradedAttempt({
        SubmittedAt: null,
        attemptAnswers: [{
          IsCorrect: false,
          question: {
            ID: 1, QuestionText: 'Q?',
            mediaQuestion: { Skill: 'READING', Type: 'Part5', Section: '5', AudioUrl: null, ImageUrl: null, Scirpt: null },
            choices: [
              { ID: 1, IsCorrect: false, Attribute: 'A', Content: 'Wrong' },
              { ID: 2, IsCorrect: false, Attribute: 'B', Content: 'Wrong' },
            ],
          },
          choice: { ID: 1, Attribute: 'A', Content: 'Wrong' },
        }],
      });
      mockAttemptRepo.findById.mockResolvedValueOnce(noCorrect as any);
      mockAttemptRepo.submitAnswers.mockResolvedValueOnce(noCorrect as any);

      // EXPECTED: hệ thống đúng phải ném lỗi cấu hình sai, không được trả về ID = 0.
      await expect(service.submitAttempt({ AttemptID: 1, answers: [] }, 1))
        .rejects.toThrow('Invalid question configuration: No correct choice found');
    });

    /**
     * Chức năng: Xác định điểm yếu an toàn.
     * Trường hợp: Câu sai nhưng bị mất thông tin mediaQuestion (null).
      * Kỳ vọng: Không crash; fallback trả về WeakAreas với type 'Unknown'.
     */
    it('[TC_ATT_019] LỖI: identifyWeakAreas should handle null mediaQuestion', () => {
      // INPUT: truyền trực tiếp danh sách answer sai có mediaQuestion=null vào private helper.
      const weakAreas = (service as any).identifyWeakAreas([
        { IsCorrect: false, question: { ID: 1, mediaQuestion: null } },
      ]);

      // EXPECTED: helper nên fallback 'Unknown' thay vì crash.
      expect(weakAreas).toHaveLength(1);
      expect(weakAreas[0]).toContain('Unknown');
    });
  });

  // ==================================================================
  // LUỒNG 3: getAttemptResults - Xem kết quả bài thi
  // ==================================================================
  describe('Luồng 3 - getAttemptResults (Xem kết quả)', () => {

    /**
     * Chức năng: Xem kết quả chi tiết của attempt.
     * Trường hợp: Attempt hợp lệ và đã nộp.
     * Kỳ vọng: Lấy ra tổng điểm, điểm từng phần chuẩn xác.
     */
    it('[TC_ATT_020] Xem kết quả bài thi đã nộp thành công', async () => {
      // MOCK DATA: attempt đã nộp và đã có điểm.
      const submitted = buildMockGradedAttempt();
      mockAttemptRepo.findById.mockResolvedValueOnce(submitted as any);

      // ACTION: student 1 xem kết quả AttemptID=1.
      const result = await service.getAttemptResults(1, 1);

      // EXPECTED: response chứa đúng điểm tổng và điểm từng kỹ năng.
      expect(result.AttemptID).toBe(1);
      expect(result.Scores.TotalScore).toBe(245 + 280);
      expect(result.Scores.ScoreListening).toBe(245);
      expect(result.Scores.ScoreReading).toBe(280);
    });

    /**
     * Chức năng: Map dữ liệu câu trả lời chi tiết.
     * Trường hợp: Trả về danh sách DetailedAnswers để hiển thị modal chi tiết.
     * Kỳ vọng: Đảm bảo có đầy đủ các key và toàn bộ Choices của câu hỏi.
     * LỖI THỰC TẾ: Response chỉ có StudentChoice/CorrectChoice, thiếu danh sách Choices nên modal không hiển thị đủ đáp án.
     */
    it('[TC_ATT_021] Kết quả bao gồm đầy đủ thông tin từng câu', async () => {
      // MOCK DATA: attempt có question.choices đầy đủ để modal có thể hiển thị toàn bộ đáp án.
      mockAttemptRepo.findById.mockResolvedValueOnce(buildMockGradedAttempt() as any);

      // ACTION: lấy kết quả chi tiết sau khi bài đã nộp.
      const result = await service.getAttemptResults(1, 1);

      // EXPECTED: từng DetailedAnswer phải có đủ StudentChoice, CorrectChoice, Media và Choices.
      expect(result.DetailedAnswers[0]).toHaveProperty('QuestionID');
      expect(result.DetailedAnswers[0]).toHaveProperty('StudentChoice');
      expect(result.DetailedAnswers[0]).toHaveProperty('CorrectChoice');
      expect(result.DetailedAnswers[0]).toHaveProperty('IsCorrect');
      expect(result.DetailedAnswers[0]).toHaveProperty('Media');
      expect(result.DetailedAnswers[0]).toHaveProperty('Choices');
      expect((result.DetailedAnswers[0] as any).Choices).toHaveLength(2);
    });

    /**
     * Chức năng: Validate xem kết quả.
     * Trường hợp: Xem kết quả của ID không tồn tại.
     * Kỳ vọng: Ném lỗi 'Attempt not found'.
     */
    it('[TC_ATT_022] Attempt không tồn tại → ném lỗi', async () => {
      // MOCK DATA: repository không tìm thấy attempt.
      mockAttemptRepo.findById.mockResolvedValueOnce(undefined as any);

      // EXPECTED: không có attempt thì không được trả kết quả.
      await expect(service.getAttemptResults(999, 1))
        .rejects.toThrow('Attempt not found');
    });

    /**
     * Chức năng: Phân quyền xem kết quả cá nhân.
     * Trường hợp: Học sinh A gọi API xem kết quả bài của Học sinh B.
     * Kỳ vọng: Chặn thao tác để bảo mật.
     */
    it('[TC_ATT_023] Student A xem kết quả của Student B → ném lỗi bảo mật', async () => {
      // MOCK DATA: attempt thuộc student 2, nhưng caller là student 1.
      mockAttemptRepo.findById.mockResolvedValueOnce({
        ID: 1, StudentProfileID: 2, SubmittedAt: new Date(),
      } as any);

      // EXPECTED: service phải chặn truy cập kết quả của người khác.
      await expect(service.getAttemptResults(1, 1))
        .rejects.toThrow('You can only view your own attempt results');
    });

    /**
     * Chức năng: Xem kết quả khi bài chưa hoàn thành.
     * Trường hợp: Cố gắng lấy báo cáo kết quả của một attempt đang làm (chưa nộp).
     * Kỳ vọng: Từ chối vì chưa tính điểm.
     */
    it('[TC_ATT_024] Xem kết quả attempt chưa nộp → ném lỗi', async () => {
      // MOCK DATA: attempt thuộc đúng student nhưng SubmittedAt=null, tức chưa nộp.
      mockAttemptRepo.findById.mockResolvedValueOnce({
        ID: 1, StudentProfileID: 1, SubmittedAt: null,
      } as any);

      // EXPECTED: attempt chưa nộp chưa có kết quả để xem.
      await expect(service.getAttemptResults(1, 1))
        .rejects.toThrow('This attempt has not been submitted yet');
    });

    /**
     * Chức năng: Map kết quả fallback (nhánh falsy).
     * Trường hợp: Database lưu null cho các cột điểm số.
     * Kỳ vọng: Trả về TotalScore = 0 thay vì NaN.
     */
    it('[TC_ATT_025] Score null → TotalScore = 0 (nhánh falsy của ||)', async () => {
      // MOCK DATA: điểm Listening/Reading/Percent là null để kích hoạt fallback về 0.
      const noScore = buildMockGradedAttempt({
        ScoreListening: null,
        ScoreReading: null,
        ScorePercent: null,
        attemptAnswers: [],
      });
      mockAttemptRepo.findById.mockResolvedValueOnce(noScore as any);

      // ACTION + EXPECTED: service không được trả NaN khi điểm bị null.
      const result = await service.getAttemptResults(1, 1);

      expect(result.Scores.TotalScore).toBe(0);
      expect(result.Scores.ScoreListening).toBe(0);
      expect(result.Scores.ScoreReading).toBe(0);
    });

    /**
     * Chức năng: Xử lý an toàn optional chaining.
     * Trường hợp: attemptAnswers không được load từ DB (undefined).
     * Kỳ vọng: Các trường đếm số câu không bị sập, trả về mảng rỗng.
     */
    it('[TC_ATT_026] attemptAnswers null → DetailedAnswers rỗng (nhánh ?. của optional chain)', async () => {
      // MOCK DATA: relation attemptAnswers không được load, giá trị là undefined.
      const noAnswers = buildMockGradedAttempt({ attemptAnswers: undefined });
      mockAttemptRepo.findById.mockResolvedValueOnce(noAnswers as any);

      // EXPECTED: optional chaining giúp response fallback về số 0 và mảng rỗng.
      const result = await service.getAttemptResults(1, 1);

      expect(result.Analysis.TotalQuestions).toBe(0);
      expect(result.Analysis.CorrectAnswers).toBe(0);
      expect(result.DetailedAnswers).toEqual([]);
    });
  });

  // ==================================================================
  // LUỒNG 4: getStudentAttempts - Xem lịch sử làm bài
  // ==================================================================
  describe('Luồng 4 - getStudentAttempts (Lịch sử làm bài)', () => {

    /**
     * Chức năng: Lấy lịch sử làm bài.
     * Trường hợp: Student có lịch sử làm bài, gọi API không có filter.
     * Kỳ vọng: Trả về toàn bộ danh sách attempt của student đó.
     */
    it('[TC_ATT_027] Lấy tất cả lịch sử làm bài của student', async () => {
      // MOCK DATA: student 1 có 2 attempt trong lịch sử.
      const mockAttempts = [
        { ID: 1, ExamID: 1, Type: 'FULL_TEST', SubmittedAt: new Date() },
        { ID: 2, ExamID: 1, Type: 'FULL_TEST', SubmittedAt: new Date() },
      ];
      mockAttemptRepo.findByStudentId.mockResolvedValueOnce(mockAttempts as any);

      // ACTION: lấy lịch sử không truyền filter.
      const result = await service.getStudentAttempts(1);

      // EXPECTED: trả về 2 attempt và gọi repository với filter undefined.
      expect(result).toHaveLength(2);
      expect(mockAttemptRepo.findByStudentId).toHaveBeenCalledWith(1, undefined);
    });

    /**
     * Chức năng: Lọc danh sách lịch sử làm bài.
     * Trường hợp: Truyền filter theo Type='FULL_TEST' và SubmittedOnly=true.
     * Kỳ vọng: Chuyển filter chính xác xuống repository.
     */
    it('[TC_ATT_028] Lọc chỉ lấy FULL_TEST đã nộp', async () => {
      // MOCK DATA: repository trả về 1 attempt khớp filter.
      mockAttemptRepo.findByStudentId.mockResolvedValueOnce([
        { ID: 1, Type: 'FULL_TEST', SubmittedAt: new Date() },
      ] as any);

      // INPUT + ACTION: lọc lịch sử theo Type và SubmittedOnly.
      const result = await service.getStudentAttempts(1, {
        Type: 'FULL_TEST', SubmittedOnly: true,
      });

      // EXPECTED: filter được truyền nguyên vẹn xuống repository mock.
      expect(result).toHaveLength(1);
      expect(mockAttemptRepo.findByStudentId).toHaveBeenCalledWith(1, {
        Type: 'FULL_TEST', SubmittedOnly: true,
      });
    });

    /**
     * Chức năng: Lấy lịch sử làm bài (Empty state).
     * Trường hợp: Student mới chưa từng làm bài thi.
     * Kỳ vọng: Trả về mảng rỗng.
     */
    it('[TC_ATT_029] Student không có lịch sử → trả về mảng rỗng', async () => {
      // MOCK DATA: repository trả về mảng rỗng.
      mockAttemptRepo.findByStudentId.mockResolvedValueOnce([] as any);

      // EXPECTED: service trả [] thay vì throw lỗi.
      const result = await service.getStudentAttempts(99);
      expect(result).toEqual([]);
    });
  });

  // ==================================================================
  // LUỒNG 5: getBestScore - Xem điểm cao nhất
  // ==================================================================
  describe('Luồng 5 - getBestScore (Điểm cao nhất)', () => {

    /**
     * Chức năng: Lấy kỷ lục điểm thi.
     * Trường hợp: Có attempt.
     * Kỳ vọng: Trả về attempt có điểm cao nhất.
     */
    it('[TC_ATT_030] Trả về attempt có điểm cao nhất', async () => {
      // MOCK DATA: repository trả attempt có ScorePercent cao nhất.
      const bestAttempt = { ID: 5, ScorePercent: 95, ScoreListening: 450, ScoreReading: 445 };
      mockAttemptRepo.getBestScore.mockResolvedValueOnce(bestAttempt as any);

      // ACTION + EXPECTED: service trả đúng object và gọi repo với studentId/examId.
      const result = await service.getBestScore(1, 1);
      expect(result).toEqual(bestAttempt);
      expect(mockAttemptRepo.getBestScore).toHaveBeenCalledWith(1, 1);
    });

    /**
     * Chức năng: Lấy kỷ lục điểm thi (Empty state).
     * Trường hợp: Chưa có lần làm bài nào hoàn thành.
     * Kỳ vọng: Trả về null thay vì object rỗng hay ném lỗi.
     */
    it('[TC_ATT_031] Chưa làm bài lần nào → trả về null', async () => {
      // MOCK DATA: không có attempt hoàn thành nên repository trả null.
      mockAttemptRepo.getBestScore.mockResolvedValueOnce(null);

      // EXPECTED: empty state được biểu diễn bằng null.
      const result = await service.getBestScore(1, 99);
      expect(result).toBeNull();
    });
  });

  // ==================================================================
  // LUỒNG 6: getProgressStatistics - Xem thống kê tiến độ
  // ==================================================================
  describe('Luồng 6 - getProgressStatistics (Thống kê tiến độ)', () => {

    /**
     * Chức năng: Xem biểu đồ/số liệu tiến độ cá nhân.
     * Trường hợp: Đã có đủ dữ liệu tính toán từ repo.
     * Kỳ vọng: Trả về đúng các chỉ số tổng kết (totalAttempts, averageScore...).
     */
    it('[TC_ATT_032] Trả về thống kê tiến độ học tập', async () => {
      // MOCK DATA: repository trả thống kê tiến độ đã tính sẵn.
      const mockStats = {
        totalAttempts: 10,
        averageScore: 75,
        improvement: 15,
        weakAreas: ['Part 3', 'Part 6'],
      };
      mockAttemptRepo.getProgressStats.mockResolvedValueOnce(mockStats as any);

      // EXPECTED: service forward đúng các chỉ số thống kê.
      const result = await service.getProgressStatistics(1);
      expect(result.totalAttempts).toBe(10);
      expect(result.averageScore).toBe(75);
    });

    /**
     * Chức năng: Xem biểu đồ/số liệu tiến độ cá nhân (Empty state).
     * Trường hợp: Người mới hoàn toàn.
     * Kỳ vọng: Trả về thống kê các giá trị 0 an toàn.
     */
    it('[TC_ATT_033] Student mới chưa có attempt nào', async () => {
      // MOCK DATA: student chưa có attempt, các chỉ số đều bằng 0.
      mockAttemptRepo.getProgressStats.mockResolvedValueOnce({
        totalAttempts: 0, averageScore: 0, improvement: 0, weakAreas: [],
      } as any);

      // EXPECTED: service trả thống kê empty state an toàn.
      const result = await service.getProgressStatistics(99);
      expect(result.totalAttempts).toBe(0);
    });
  });

  // ==================================================================
  // LUỒNG 7: deleteAttempt - Thoát bài (xóa attempt chưa nộp)
  // ==================================================================
  describe('Luồng 7 - deleteAttempt (Thoát bài)', () => {

    /**
     * Chức năng: Rút lui không làm bài tiếp.
     * Trường hợp: Xóa một bài đang làm dở (SubmittedAt = null).
     * Kỳ vọng: Xóa an toàn.
     */
    it('[TC_ATT_034] Thoát bài (xóa attempt chưa nộp) thành công', async () => {
      // MOCK DATA: attempt thuộc đúng student và chưa nộp.
      mockAttemptRepo.findById.mockResolvedValueOnce({
        ID: 1, StudentProfileID: 1, SubmittedAt: null,
      } as any);
      mockAttemptRepo.delete.mockResolvedValueOnce(true);

      // ACTION + EXPECTED: xóa thành công và gọi delete với AttemptID=1.
      const result = await service.deleteAttempt(1, 1);
      expect(result).toBe(true);
      expect(mockAttemptRepo.delete).toHaveBeenCalledWith(1);
    });

    /**
     * Chức năng: Validate thao tác xóa.
     * Trường hợp: ID attempt không có thực.
     * Kỳ vọng: Báo lỗi 'Attempt not found'.
     */
    it('[TC_ATT_035] Attempt không tồn tại → ném lỗi', async () => {
      // MOCK DATA: repository không tìm thấy attempt.
      mockAttemptRepo.findById.mockResolvedValueOnce(undefined as any);

      // EXPECTED: service throw trước khi gọi delete.
      await expect(service.deleteAttempt(999, 1))
        .rejects.toThrow('Attempt not found');
    });

    /**
     * Chức năng: Bảo mật dữ liệu.
     * Trường hợp: User gửi lệnh xóa bài thi không thuộc quyền sở hữu.
     * Kỳ vọng: Báo lỗi cấm can thiệp.
     */
    it('[TC_ATT_036] Xóa attempt của người khác → ném lỗi bảo mật', async () => {
      // MOCK DATA: attempt thuộc student 2 nhưng caller là student 1.
      mockAttemptRepo.findById.mockResolvedValueOnce({
        ID: 1, StudentProfileID: 2, SubmittedAt: null,
      } as any);

      // EXPECTED: không được xóa attempt của người khác.
      await expect(service.deleteAttempt(1, 1))
        .rejects.toThrow('You can only delete your own attempts');
    });

    /**
     * Chức năng: Ràng buộc tính toàn vẹn (Data Integrity).
     * Trường hợp: Cố gắng xóa một attempt đã nộp điểm lưu lịch sử.
     * LỖI THỰC TẾ: Backend thiếu kiểm tra trạng thái Submitted, cho phép học sinh xóa bài điểm thấp. Test FAIL báo lỗi này.
     */
    it('[TC_ATT_037] LỖI: Cho phép xóa attempt ĐÃ NỘP → mất dữ liệu lịch sử', async () => {
      // MOCK DATA: attempt đã nộp nhưng repository delete vẫn trả true nếu service gọi xuống.
      mockAttemptRepo.findById.mockResolvedValueOnce({
        ID: 1, StudentProfileID: 1, SubmittedAt: new Date(),
      } as any);
      mockAttemptRepo.delete.mockResolvedValueOnce(true);

      // EXPECTED: service đúng phải chặn xóa attempt đã nộp.
      await expect(service.deleteAttempt(1, 1))
        .rejects.toThrow('Cannot delete a submitted attempt');
    });
  });

  // ==================================================================
  // LUỒNG 8: calculateTimeElapsed - Tính giờ (internal logic)
  // ==================================================================
  describe('Luồng 8 - calculateTimeElapsed (Tính thời gian làm bài)', () => {

    /**
     * Chức năng: Tính thời gian làm bài (phút).
     * Trường hợp: Bắt đầu 08:30, nộp 10:00.
     * Kỳ vọng: Tính ra đúng 90 phút.
     */
    it('[TC_ATT_038] Tính đúng thời gian 90 phút', () => {
      // INPUT: start/end cố định cách nhau 90 phút.
      const start = new Date('2024-01-01T08:30:00Z');
      const end = new Date('2024-01-01T10:00:00Z');

      // ACTION + EXPECTED: helper trả số phút chênh lệch.
      const elapsed = (service as any).calculateTimeElapsed(start, end);
      expect(elapsed).toBe(90);
    });

    /**
     * Chức năng: Tính thời gian làm bài.
     * Trường hợp: Làm đúng 120 phút.
     * Kỳ vọng: Trả về 120.
     */
    it('[TC_ATT_039] Tính đúng thời gian giữa 2 mốc cụ thể', () => {
      // INPUT: start/end cố định cách nhau 120 phút.
      const start = new Date('2024-01-01T08:00:00Z');
      const end = new Date('2024-01-01T10:00:00Z');

      // EXPECTED: elapsed = 120 phút.
      const elapsed = (service as any).calculateTimeElapsed(start, end);
      expect(elapsed).toBe(120);
    });

    /**
     * Chức năng: Thời gian bằng 0.
     * Trường hợp: Mốc bắt đầu và kết thúc y hệt nhau.
     * Kỳ vọng: Thời gian = 0.
     */
    it('[TC_ATT_040] Thời gian = 0 phút (nộp ngay lập tức)', () => {
      // INPUT: start và end cùng một thời điểm.
      const now = new Date();

      // EXPECTED: elapsed = 0.
      const elapsed = (service as any).calculateTimeElapsed(now, now);
      expect(elapsed).toBe(0);
    });

    /**
     * Chức năng: Tính toán an toàn với thời gian.
     * Trường hợp: Máy client bị sai giờ (clock skew) gửi thời gian tương lai.
     * DEFECT REPORT: Hàm không xử lý, trả về số phút làm bài bị ÂM, cho phép bypass validation thời gian.
     * KỲ VỌNG: Ném lỗi từ chối việc tính toán thời gian âm.
     */
    it('[TC_ATT_041] DEFECT: StartedAt ở tương lai -> trả về số âm thay vì ném lỗi', () => {
      // INPUT: StartedAt nằm trong tương lai so với thời điểm nộp.
      const future = new Date(Date.now() + 60 * 60000);
      const now = new Date();

      // EXPECTED: hệ thống đúng phải chặn bằng exception khi phát hiện clock skew.
      expect(() => {
        (service as any).calculateTimeElapsed(future, now);
      }).toThrow('Invalid time detected: StartedAt is in the future');
    });
  });
});
