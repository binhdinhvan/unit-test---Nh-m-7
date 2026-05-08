import { ExamService } from '../exam.service';
import { ExamRepository } from '../../../infrastructure/repositories/exam.repository';
import { QuestionRepository } from '../../../infrastructure/repositories/question.repository';
import { MediaQuestionRepository } from '../../../infrastructure/repositories/media-question.repository';

jest.mock('../../../infrastructure/repositories/exam.repository');
jest.mock('../../../infrastructure/repositories/question.repository');
jest.mock('../../../infrastructure/repositories/media-question.repository');

// Quy ước chú thích trong file test:
// INPUT: dữ liệu truyền trực tiếp vào hàm service cần test.
// MOCK DATA: dữ liệu giả lập repository trả về, không phải dữ liệu DB thật.
// ACTION: lời gọi hàm service đang được kiểm thử.
// EXPECTED: kết quả trả về, lỗi ném ra, hoặc mock repository call được kỳ vọng.

// =====================================================================
// Factory: Tạo mock data, dùng JSON.parse/stringify để đảm bảo mỗi
// test nhận được bản sao độc lập, tránh shared mutable state.
// =====================================================================
const createMockExamDetail = (overrides: any = {}) => structuredClone({
  ID: 1,
  Title: 'ETS 2023 Test 1',
  TimeExam: 120,
  Type: 'FULL_TEST',
  UserID: 1,
  examType: { ID: 1, Code: 'FULL', Description: 'Full Test' },
  examQuestions: [
    {
      OrderIndex: 1,
      question: {
        ID: 1, QuestionText: 'Q1', QuestionType: null,
        mediaQuestion: {
          ID: 1, Skill: 'LISTENING', Type: 'Part1', Section: '1',
          AudioUrl: null, ImageUrl: null, Scirpt: null,
        },
        choices: [
          { ID: 1, Attribute: 'A', Content: 'Answer A' },
          { ID: 2, Attribute: 'B', Content: 'Answer B' },
        ],
      },
    },
  ],
  attempts: [],
  ...overrides,
});

describe('ExamService - Luyện đề (Xem và tìm đề thi)', () => {
  let service: ExamService;
  let mockExamRepo: jest.Mocked<ExamRepository>;
  let mockQuestionRepo: jest.Mocked<QuestionRepository>;
  let mockMediaRepo: jest.Mocked<MediaQuestionRepository>;

  beforeEach(() => {
    jest.clearAllMocks();
    service = new ExamService();
    mockExamRepo = new ExamRepository() as jest.Mocked<ExamRepository>;
    mockQuestionRepo = new QuestionRepository() as jest.Mocked<QuestionRepository>;
    mockMediaRepo = new MediaQuestionRepository() as jest.Mocked<MediaQuestionRepository>;
    (service as any).examRepository = mockExamRepo;
    (service as any).questionRepository = mockQuestionRepo;
    (service as any).mediaQuestionRepository = mockMediaRepo;
  });

  // ==================================================================
  // LUỒNG A: getAllExams - Xem danh sách đề thi
  // ==================================================================
  describe('Luồng A - getAllExams (Xem danh sách đề thi)', () => {

    /**
     * Chức năng: Lấy toàn bộ danh sách đề thi.
     * Trường hợp: Không truyền filter, DB có 3 đề (Happy Path).
     * Kỳ vọng: Trả về đúng 3 đề, gọi repo với undefined (không filter).
     */
    it('[TC_EX_001] Lấy tất cả đề thi không lọc', async () => {
      // MOCK DATA: repository trả về 3 đề thi giả lập.
      const mockList = [
        { ID: 1, Title: 'ETS 2023 Test 1', Type: 'FULL_TEST' },
        { ID: 2, Title: 'ETS 2023 Test 2', Type: 'FULL_TEST' },
        { ID: 3, Title: 'Part 5 Practice', Type: 'PRACTICE' },
      ];
      mockExamRepo.findAll.mockResolvedValueOnce(mockList as any);

      // ACTION: gọi service không truyền filter.
      const result = await service.getAllExams();

      // EXPECTED: trả đúng 3 item và repository được gọi với undefined filter.
      expect(result).toHaveLength(3);
      expect(mockExamRepo.findAll).toHaveBeenCalledWith(undefined);
    });

    /**
     * Chức năng: Lọc danh sách đề thi theo loại.
     * Trường hợp: Client lọc theo Type='FULL_TEST'.
     * Kỳ vọng: Trả về 2 đề FULL_TEST, gọi repo với đúng filter.
     */
    it('[TC_EX_002] Lọc chỉ lấy đề FULL_TEST', async () => {
      // MOCK DATA: repository trả về 2 đề FULL_TEST.
      mockExamRepo.findAll.mockResolvedValueOnce([
        { ID: 1, Title: 'ETS 2023 Test 1', Type: 'FULL_TEST' },
        { ID: 2, Title: 'ETS 2023 Test 2', Type: 'FULL_TEST' },
      ] as any);

      // INPUT + ACTION: truyền filter Type='FULL_TEST'.
      const result = await service.getAllExams({ Type: 'FULL_TEST' });

      // EXPECTED: service trả 2 item và truyền nguyên filter xuống repository mock.
      expect(result).toHaveLength(2);
      expect(mockExamRepo.findAll).toHaveBeenCalledWith({ Type: 'FULL_TEST' });
    });

    /**
     * Chức năng: Lọc danh sách đề thi theo loại đề.
     * Trường hợp: Client lọc theo ExamTypeID=2.
     * Kỳ vọng: Gọi repo với đúng tham số ExamTypeID.
     */
    it('[TC_EX_003] Lọc theo ExamTypeID', async () => {
      // MOCK DATA: repository trả về danh sách giả lập khi lọc ExamTypeID.
      mockExamRepo.findAll.mockResolvedValueOnce([{ ID: 1 }] as any);

      // INPUT + ACTION: truyền filter ExamTypeID=2.
      await service.getAllExams({ ExamTypeID: 2 });

      // EXPECTED: repository mock nhận đúng filter.
      expect(mockExamRepo.findAll).toHaveBeenCalledWith({ ExamTypeID: 2 });
    });

    /**
     * Chức năng: Lấy toàn bộ danh sách đề thi.
     * Trường hợp: Chưa có đề thi nào trong hệ thống.
     * Kỳ vọng: Trả về mảng rỗng, không ném lỗi.
     */
    it('[TC_EX_004] Không có đề thi nào → trả về mảng rỗng', async () => {
      // MOCK DATA: repository trả về mảng rỗng.
      mockExamRepo.findAll.mockResolvedValueOnce([] as any);

      // ACTION + EXPECTED: service trả [] thay vì throw lỗi.
      const result = await service.getAllExams();

      expect(result).toEqual([]);
    });
  });

  // ==================================================================
  // LUỒNG B: getExamById - Xem chi tiết đề thi trước khi làm
  // ==================================================================
  describe('Luồng B - getExamById (Xem chi tiết đề thi)', () => {

    /**
     * Chức năng: Xem chi tiết một đề thi.
     * Trường hợp: ID hợp lệ, đề có đầy đủ thông tin (Happy Path).
     * Kỳ vọng: Trả về đúng tiêu đề, thời gian và danh sách câu hỏi.
     */
    it('[TC_EX_005] Lấy chi tiết đề thi thành công', async () => {
      // MOCK DATA: đề thi đầy đủ examType, examQuestions, media và choices.
      mockExamRepo.findById.mockResolvedValueOnce(createMockExamDetail() as any);

      // INPUT + ACTION: lấy chi tiết ExamID=1.
      const result = await service.getExamById(1);

      // EXPECTED: response map đúng metadata và danh sách câu hỏi.
      expect(result.ID).toBe(1);
      expect(result.Title).toBe('ETS 2023 Test 1');
      expect(result.TimeExam).toBe(120);
      expect(result.Questions).toHaveLength(1);
    });

    /**
     * Chức năng: Bảo mật đáp án trước khi gửi về client.
     * Trường hợp: Đề thi có đáp án (IsCorrect=true/false) trong DB.
     * Kỳ vọng: Trường IsCorrect phải bị loại bỏ khỏi response để tránh lộ đáp án.
     */
    it('[TC_EX_006] IsCorrect của choices bị ẩn (bảo mật)', async () => {
      // MOCK DATA: đề thi có choices; service response cho học sinh không được lộ IsCorrect.
      mockExamRepo.findById.mockResolvedValueOnce(createMockExamDetail() as any);

      // ACTION: lấy chi tiết đề thi.
      const result = await service.getExamById(1);
      const choices = result.Questions[0].Choices;

      // EXPECTED: mọi choice trong response đều không có field IsCorrect.
      choices.forEach(choice => {
        expect(choice).not.toHaveProperty('IsCorrect');
      });
    });

    /**
     * Chức năng: Sắp xếp câu hỏi theo thứ tự.
     * Trường hợp: DB trả về câu hỏi không theo thứ tự OrderIndex.
     * Kỳ vọng: Service phải sắp xếp lại tăng dần theo OrderIndex trước khi trả về.
     */
    it('[TC_EX_007] Questions được sắp xếp đúng thứ tự OrderIndex', async () => {
      // MOCK DATA: examQuestions trả về không theo thứ tự OrderIndex.
      const shuffledExam = createMockExamDetail({
        examQuestions: [
          { OrderIndex: 3, question: { ID: 3, QuestionText: 'Q3', mediaQuestion: { ID: 3, Skill: 'READING', Type: 'Part5', Section: '5', AudioUrl: null, ImageUrl: null, Scirpt: null }, choices: [] } },
          { OrderIndex: 1, question: { ID: 1, QuestionText: 'Q1', mediaQuestion: { ID: 1, Skill: 'LISTENING', Type: 'Part1', Section: '1', AudioUrl: null, ImageUrl: null, Scirpt: null }, choices: [] } },
          { OrderIndex: 2, question: { ID: 2, QuestionText: 'Q2', mediaQuestion: { ID: 2, Skill: 'LISTENING', Type: 'Part2', Section: '2', AudioUrl: null, ImageUrl: null, Scirpt: null }, choices: [] } },
        ],
      });
      mockExamRepo.findById.mockResolvedValueOnce(shuffledExam as any);

      // ACTION: service transform chi tiết đề thi.
      const result = await service.getExamById(1);

      // EXPECTED: Questions trong response được sort tăng dần theo OrderIndex.
      expect(result.Questions[0].OrderIndex).toBe(1);
      expect(result.Questions[1].OrderIndex).toBe(2);
      expect(result.Questions[2].OrderIndex).toBe(3);
    });

    /**
     * Chức năng: Xử lý dữ liệu khuyết thiếu từ DB.
     * Trường hợp: Relation examQuestions bị undefined do lỗi ORM.
     * Kỳ vọng: Trả về mảng Questions rỗng thay vì crash.
     */
    it('[TC_EX_008] Đề thi không có câu hỏi (examQuestions=undefined) → Questions = []', async () => {
      // MOCK DATA: relation examQuestions không được load, giá trị undefined.
      mockExamRepo.findById.mockResolvedValueOnce(
        createMockExamDetail({ examQuestions: undefined }) as any,
      );

      // EXPECTED: service fallback Questions=[] an toàn.
      const result = await service.getExamById(1);

      expect(result.Questions).toEqual([]);
    });

    /**
     * Chức năng: Xem chi tiết đề thi.
     * Trường hợp: ID không tồn tại trong DB.
     * Kỳ vọng: Ném lỗi 'Exam not found'.
     */
    it('[TC_EX_009] Đề thi không tồn tại → ném lỗi', async () => {
      // MOCK DATA: repository không tìm thấy exam.
      mockExamRepo.findById.mockResolvedValueOnce(undefined as any);

      // INPUT + EXPECTED: ExamID không tồn tại phải throw.
      await expect(service.getExamById(999))
        .rejects.toThrow('Exam not found');
    });

    /**
     * Chức năng: Xử lý dữ liệu khuyết thiếu từ DB.
     * Trường hợp: examQuestions là mảng rỗng (đề mới chưa có câu hỏi).
     * Kỳ vọng: Trả về mảng Questions rỗng, không ném lỗi.
     */
    it('[TC_EX_010] examQuestions là mảng rỗng → Questions = []', async () => {
      // MOCK DATA: đề thi tồn tại nhưng examQuestions là mảng rỗng.
      mockExamRepo.findById.mockResolvedValueOnce(
        createMockExamDetail({ examQuestions: [] }) as any,
      );

      // EXPECTED: service trả Questions=[].
      const result = await service.getExamById(1);

      expect(result.Questions).toEqual([]);
    });

    /**
     * Chức năng: Xử lý trường null trong dữ liệu DB (nhánh falsy ||).
     * Trường hợp: Nhiều trường nullable bị null (Type, Description, Section, Content).
     * Kỳ vọng: Các trường null phải được chuyển thành chuỗi rỗng '', không bị undefined.
     */
    it('[TC_EX_011] exam.Type null → trả về chuỗi rỗng (nhánh falsy ||)', async () => {
      // MOCK DATA: nhiều field nullable trả về null để test fallback chuỗi rỗng.
      mockExamRepo.findById.mockResolvedValueOnce(createMockExamDetail({
        Type: null,
        examType: { ID: 1, Code: 'FULL', Description: null },
        examQuestions: [{
          OrderIndex: 1,
          question: {
            ID: 1, QuestionText: null,
            mediaQuestion: { ID: 1, Skill: 'LISTENING', Type: 'Part1', Section: null, AudioUrl: null, ImageUrl: null, Scirpt: null },
            choices: [{ ID: 1, Attribute: null, Content: null }],
          },
        }],
      }) as any);

      // ACTION: transform exam detail sang response DTO.
      const result = await service.getExamById(1);

      // EXPECTED: các field text null được fallback thành ''.
      expect(result.Type).toBe('');
      expect(result.ExamType.Description).toBe('');
      expect(result.Questions[0].QuestionText).toBe('');
      expect(result.Questions[0].Media.Section).toBe('');
      expect(result.Questions[0].Choices[0].Attribute).toBe('');
      expect(result.Questions[0].Choices[0].Content).toBe('');
    });

    /**
     * Chức năng: Xử lý dữ liệu khuyết thiếu từ DB (nhánh false của ternary).
     * Trường hợp: examQuestions là null (khác với undefined).
     * Kỳ vọng: Kích hoạt nhánh false của ternary, trả về mảng Questions rỗng.
     */
    it('[TC_EX_012] examQuestions null (falsy path của ternary) → Questions = []', async () => {
      // MOCK DATA: examQuestions=null, khác với undefined và [].
      mockExamRepo.findById.mockResolvedValueOnce(
        createMockExamDetail({ examQuestions: null }) as any,
      );

      // EXPECTED: service vẫn fallback Questions=[].
      const result = await service.getExamById(1);

      expect(result.Questions).toEqual([]);
    });

    /**
     * Chức năng: Xử lý dữ liệu hỏng từ DB.
     * Trường hợp: Một câu hỏi trong examQuestions có mediaQuestion=null (dữ liệu bị corrupt).
     * LỖI THỰC TẾ: Service truy cập trực tiếp eq.question.mediaQuestion.ID mà không kiểm tra null,
     *              gây NPE (NullPointerException) và crash toàn bộ request.
     * Kỳ vọng: Phải ném lỗi 'Invalid question data' thay vì crash không kiểm soát.
     */
    it('[TC_EX_013] LỖI: NPE khi question.mediaQuestion=null trong examQuestions', async () => {
      // MOCK DATA: dữ liệu câu hỏi bị hỏng, mediaQuestion=null.
      mockExamRepo.findById.mockResolvedValueOnce(createMockExamDetail({
        examQuestions: [{
          OrderIndex: 1,
          question: {
            ID: 99, QuestionText: 'Corrupt Q',
            mediaQuestion: null,
            choices: [],
          },
        }],
      }) as any);

      // EXPECTED: hệ thống đúng phải throw lỗi nghiệp vụ có kiểm soát, không crash TypeError.
      await expect(service.getExamById(1))
        .rejects.toThrow('Invalid question data');
    });
  });

  // ==================================================================
  // LUỒNG C: searchExams - Tìm kiếm đề thi
  // ==================================================================
  describe('Luồng C - searchExams (Tìm kiếm đề thi)', () => {

    /**
     * Chức năng: Tìm kiếm đề thi theo từ khóa.
     * Trường hợp: Từ khóa hợp lệ, DB tìm thấy 2 kết quả (Happy Path).
     * Kỳ vọng: Trả về 2 đề, gọi repo với đúng từ khóa.
     */
    it('[TC_EX_014] Tìm kiếm theo từ khóa "ETS" thành công', async () => {
      // MOCK DATA: repository tìm thấy 2 đề theo keyword ETS.
      mockExamRepo.searchByTitle.mockResolvedValueOnce([
        { ID: 1, Title: 'ETS 2023 Test 1' },
        { ID: 2, Title: 'ETS 2022 Test 1' },
      ] as any);

      // INPUT + ACTION: tìm kiếm với term='ETS'.
      const result = await service.searchExams('ETS');

      // EXPECTED: trả 2 item và truyền đúng keyword xuống repository mock.
      expect(result).toHaveLength(2);
      expect(mockExamRepo.searchByTitle).toHaveBeenCalledWith('ETS');
    });

    /**
     * Chức năng: Tìm kiếm đề thi theo từ khóa.
     * Trường hợp: Từ khóa không khớp với bất kỳ đề nào.
     * Kỳ vọng: Trả về mảng rỗng, không ném lỗi.
     */
    it('[TC_EX_015] Từ khóa không tìm thấy đề nào → trả về mảng rỗng', async () => {
      // MOCK DATA: repository không tìm thấy kết quả.
      mockExamRepo.searchByTitle.mockResolvedValueOnce([] as any);

      // EXPECTED: empty search trả [].
      const result = await service.searchExams('XYZ_NOT_EXIST');

      expect(result).toEqual([]);
    });

    /**
     * Chức năng: Validate đầu vào tìm kiếm.
     * Trường hợp: Client gửi chuỗi rỗng làm từ khóa.
     * Kỳ vọng: Ném lỗi 'Search term cannot be empty' trước khi gọi DB.
     */
    it('[TC_EX_016] Từ khóa tìm kiếm rỗng → ném lỗi', async () => {
      // INPUT + EXPECTED: term rỗng bị validation chặn trước khi gọi repository.
      await expect(service.searchExams(''))
        .rejects.toThrow('Search term cannot be empty');
    });

    /**
     * Chức năng: Validate đầu vào tìm kiếm (Boundary Value).
     * Trường hợp: Từ khóa chỉ chứa khoảng trắng (sau khi trim() sẽ rỗng).
     * Kỳ vọng: Ném lỗi 'Search term cannot be empty', không gọi DB.
     */
    it('[TC_EX_017] Từ khóa chỉ có khoảng trắng → ném lỗi', async () => {
      // INPUT + EXPECTED: term chỉ có whitespace sau trim() là rỗng nên phải throw.
      await expect(service.searchExams('   '))
        .rejects.toThrow('Search term cannot be empty');
    });

    /**
     * Chức năng: Tìm kiếm đề thi với ký tự đặc biệt.
     * Trường hợp: Từ khóa chứa ký tự encode URL (%).
     * Kỳ vọng: Service không xử lý thêm, truyền nguyên văn xuống repo (sanitize là việc của DB layer).
     */
    it('[TC_EX_018] Từ khóa có ký tự đặc biệt → gọi repo bình thường', async () => {
      // MOCK DATA: repository trả [] nhưng vẫn được gọi với nguyên keyword có ký tự đặc biệt.
      mockExamRepo.searchByTitle.mockResolvedValueOnce([] as any);

      // ACTION: tìm kiếm với chuỗi chứa %.
      await service.searchExams('ETS%2023');

      // EXPECTED: service không sanitize ở layer này, chỉ passthrough xuống repository mock.
      expect(mockExamRepo.searchByTitle).toHaveBeenCalledWith('ETS%2023');
    });
  });
});
