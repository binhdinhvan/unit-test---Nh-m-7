package com.mxhieu.doantotnghiep;

import com.mxhieu.doantotnghiep.converter.TestAttemptConverter;
import com.mxhieu.doantotnghiep.converter.TestConverter;
import com.mxhieu.doantotnghiep.dto.request.*;
import com.mxhieu.doantotnghiep.dto.response.*;
import com.mxhieu.doantotnghiep.entity.*;
import com.mxhieu.doantotnghiep.exception.AppException;
import com.mxhieu.doantotnghiep.repository.*;
import com.mxhieu.doantotnghiep.service.EnrollmentServece;
import com.mxhieu.doantotnghiep.service.impl.TestAttemptServiceImpl;
import com.mxhieu.doantotnghiep.service.impl.TestServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test cases cho Luyện đề & Đánh giá kết quả (Practice Exam & View Results)")
public class PracticeTestFeatureTests {

    @Nested
    @DisplayName("1. Quản lý Bài Thi (TestService) - Luyện đề Data")
    class TestServiceTests {
        @Mock private TestRepository testRepository;
        @Mock private TestConverter testConverter;
        @Mock private ModuleRepository moduleRepository;
        @Mock private TestAttemptRepository testAttemptRepository;
        @Mock private EnrollmentCourseRepository enrollmentCourseRepository;
        @Mock private TestProgressRepository testProgressRepository;

        @InjectMocks
        private TestServiceImpl testService;        /**
     * TC_PTF_001: getFirstTestsSummery - Trả về danh sách tests
     * 
     * Test Objective: Lấy danh sách đề thi (First Test)
     * Input: Mock Data TestRepository (Type=FIRST_TEST)
     * Expected Output: Trả về List<TestResponse> với size=1
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_001: getFirstTestsSummery - Trả về danh sách tests")
        void t006() {
            TestEntity t1 = new TestEntity();
            when(testRepository.findByType("FIRST_TEST")).thenReturn(List.of(t1));
            when(testConverter.toResponseSummery(t1)).thenReturn(new TestResponse());
            List<TestResponse> res = testService.getFirstTestsSummery();
            assertEquals(1, res.size());
        }        /**
     * TC_PTF_002: commpletedStar - 3 Stars khi điểm = 100
     * 
     * Test Objective: Tính số sao khi điểm thi tuyệt đối
     * Input: TestAttemptTotalScore = 100
     * Expected Output: Trả về 3 Stars
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_002: commpletedStar - 3 Stars khi điểm = 100")
        void t007() {
            TestAttemptEntity attempt = new TestAttemptEntity();
            attempt.setTotalScore(100f);
            when(testAttemptRepository.findByTestIdAndStudentProfileId(1, 1)).thenReturn(List.of(attempt));
            int stars = testService.commpletedStar(1, 1);
            assertEquals(3, stars);
        }        /**
     * TC_PTF_003: commpletedStar - 2 Stars khi điểm = 75 (>=70)
     * 
     * Test Objective: Tính số sao khi điểm khá/giỏi
     * Input: TestAttemptTotalScore = 75 (>= 70)
     * Expected Output: Trả về 2 Stars
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_003: commpletedStar - 2 Stars khi điểm = 75 (>=70)")
        void t008() {
            TestAttemptEntity attempt = new TestAttemptEntity();
            attempt.setTotalScore(75f);
            when(testAttemptRepository.findByTestIdAndStudentProfileId(1, 1)).thenReturn(List.of(attempt));
            int stars = testService.commpletedStar(1, 1);
            assertEquals(2, stars);
        }        /**
     * TC_PTF_004: commpletedStar - 1 Star khi điểm = 50 (<70)
     * 
     * Test Objective: Tính số sao khi điểm thấp
     * Input: TestAttemptTotalScore = 50 (< 70)
     * Expected Output: Trả về 1 Star
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_004: commpletedStar - 1 Star khi điểm = 50 (<70)")
        void t009() {
            TestAttemptEntity attempt = new TestAttemptEntity();
            attempt.setTotalScore(50f);
            when(testAttemptRepository.findByTestIdAndStudentProfileId(1, 1)).thenReturn(List.of(attempt));
            int stars = testService.commpletedStar(1, 1);
            assertEquals(1, stars);
        }        /**
     * TC_PTF_005: commpletedStar - 0 Star khi chưa có attempt
     * 
     * Test Objective: Tính số sao khi chưa có dữ liệu làm bài
     * Input: Empty List từ TestAttemptRepository
     * Expected Output: Trả về 0 Star
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_005: commpletedStar - 0 Star khi chưa có attempt")
        void t010() {
            when(testAttemptRepository.findByTestIdAndStudentProfileId(1, 1)).thenReturn(Collections.emptyList());
            int stars = testService.commpletedStar(1, 1);
            assertEquals(0, stars);
        }        /**
     * TC_PTF_006: getTestResponseDetail - Làm rõ trạng thái LOCK/UNLOCK
     * 
     * Test Objective: Xác thực logic khóa bài học (LOCK/UNLOCK)
     * Input: Mock EnrollmentRepository rỗng
     * Expected Output: TestResponse có Trạng thái = UNLOCK
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_006: getTestResponseDetail - Làm rõ trạng thái LOCK/UNLOCK")
        void t011() {
            TestEntity t1 = new TestEntity();
            t1.setId(1);
            ModuleEntity m1 = new ModuleEntity();
            CourseEntity c1 = new CourseEntity();
            c1.setId(10); m1.setCourse(c1); t1.setModule(m1);

            when(testConverter.toResponseNoQuestion(t1)).thenReturn(new TestResponse());
            when(testAttemptRepository.findByTestIdAndStudentProfileId(1, 1)).thenReturn(Collections.emptyList());
            when(testRepository.findById(1)).thenReturn(Optional.of(t1));
            when(enrollmentCourseRepository.findStatus(1, 10)).thenReturn("DONE");

            List<TestResponse> res = testService.getTestResponseDetail(List.of(t1), 1);
            assertEquals(1, res.size());
            assertEquals("UNLOCK", res.get(0).getStatus());
        }        /**
     * TC_PTF_007: getCompletedTestsOfStudent - Đếm số bài thi đã hoàn thành
     * 
     * Test Objective: Đếm số lượng Test đã hoàn thành
     * Input: TestProgressEntity process = 2
     * Expected Output: count = 1
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_007: getCompletedTestsOfStudent - Đếm số bài thi đã hoàn thành")
        void t012() {
            TestEntity t1 = new TestEntity();
            t1.setId(1);
            TestProgressEntity progress = new TestProgressEntity(); progress.setProcess(2);
            when(testProgressRepository.findByTest_IdAndStudentProfile_Id(1, 1)).thenReturn(List.of(progress));
            int count = testService.getCompletedTestsOfStudent(List.of(t1), 1);
            assertEquals(1, count);
        }        /**
     * TC_PTF_008: isLock - Bug NullPointerException khi statusOfCourse là null
     * 
     * Test Objective: Phơi bày Bug NPE khi trạng thái khóa học Null
     * Input: Dữ liệu Database bẩn: status=null
     * Expected Output: Bắt NullPointerException thay vì AppException
     * Notes: Phát hiện lỗ hổng hệ thống
     */
    @Test
    @DisplayName("TC_PTF_008: isLock - Bug NullPointerException khi statusOfCourse là null")
        void t013() {
            // isLock() line 169 "statusOfCourse.equals("LOCK")" thiếu check null (BUG hunting)
            TestEntity t = new TestEntity(); t.setId(1);
            ModuleEntity m = new ModuleEntity(); CourseEntity c = new CourseEntity(); c.setId(10);
            m.setCourse(c); t.setModule(m);
            when(testRepository.findById(1)).thenReturn(Optional.of(t));
            when(enrollmentCourseRepository.findStatus(1, 10)).thenReturn(null); // NULL STATUS
            
            // Expected to return TRUE (Lock if no status), but throws NPE in current source code
            assertThrows(AppException.class, () -> testService.isLock(1, 1), "Phải bắt AppException chứ ko phải NPE");
        }        /**
     * TC_PTF_009: isLock - Trả về false (Mở) nếu course status là DONE
     * 
     * Test Objective: Test trạng thái Unlock cho Track Done
     * Input: Course status = DONE
     * Expected Output: isLock() trả về False
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_009: isLock - Trả về false (Mở) nếu course status là DONE")
        void t014() {
            TestEntity t = new TestEntity(); t.setId(1);
            ModuleEntity m = new ModuleEntity(); CourseEntity c = new CourseEntity(); c.setId(10);
            m.setCourse(c); t.setModule(m);
            when(testRepository.findById(1)).thenReturn(Optional.of(t));
            when(enrollmentCourseRepository.findStatus(1, 10)).thenReturn("DONE");
            assertFalse(testService.isLock(1, 1));
        }        /**
     * TC_PTF_010: getMiniTestsSummery - Lấy thành công
     * 
     * Test Objective: Hiển thị Tổng quan đề luyện Mini Test
     * Input: TestEntity Type=MINI
     * Expected Output: Trả về đối tượng TestResponse hợp lệ
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_010: getMiniTestsSummery - Lấy thành công")
        void t015() {
            TestEntity t = new TestEntity(); t.setId(1); t.setType("MINI");
            when(testRepository.findById(1)).thenReturn(Optional.of(t));
            TestResponse res = testService.getMiniTestsSummery(1);
            assertEquals("MINI", res.getType());
        }        /**
     * TC_PTF_011: getMaxScore - Trả về điểm cao nhất
     * 
     * Test Objective: Tìm đỉnh điểm (Max Score) của bài làm
     * Input: Attemp1=40f, Attemp2=80f
     * Expected Output: MaxScore trả về 80f
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_011: getMaxScore - Trả về điểm cao nhất")
        void t016() {
            TestAttemptEntity a1 = new TestAttemptEntity(); a1.setTotalScore(40f);
            TestAttemptEntity a2 = new TestAttemptEntity(); a2.setTotalScore(80f);
            when(testAttemptRepository.findByTestIdAndStudentProfileId(1, 1)).thenReturn(List.of(a1, a2));
            float max = testService.getMaxScore(1, 1);
            assertEquals(80f, max);
        }        /**
     * TC_PTF_012: getMaxScore - Trả về 0 nếu chưa có lần thử nào
     * 
     * Test Objective: Tìm Max Score khi chưa thi lần nào
     * Input: Empty Attempts Array
     * Expected Output: MaxScore trả về 0.0f
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_012: getMaxScore - Trả về 0 nếu chưa có lần thử nào")
        void t017() {
            when(testAttemptRepository.findByTestIdAndStudentProfileId(1, 1)).thenReturn(Collections.emptyList());
            float max = testService.getMaxScore(1, 1);
            assertEquals(0f, max);
        }        /**
     * TC_PTF_013: isCompletedTest - False nếu chưa có tiến độ
     * 
     * Test Objective: Biện luận kiểm tra hoàn thành bài rèn luyện
     * Input: Lịch sử Progress rỗng
     * Expected Output: isCompletedTest = False
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_013: isCompletedTest - False nếu chưa có tiến độ")
        void t018() {
            when(testProgressRepository.findByTest_IdAndStudentProfile_Id(1, 1)).thenReturn(Collections.emptyList());
            assertFalse(testService.isCompletedTest(1, 1));
        }        /**
     * TC_PTF_014: isCompletedTest - False nếu progress = 0
     * 
     * Test Objective: Kiểm tra nếu process dậm chân tại chỗ
     * Input: TestProgress process = 0
     * Expected Output: isCompletedTest = False
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_014: isCompletedTest - False nếu progress = 0")
        void t019() {
            TestProgressEntity p = new TestProgressEntity(); p.setProcess(0);
            when(testProgressRepository.findByTest_IdAndStudentProfile_Id(1, 1)).thenReturn(List.of(p));
            assertFalse(testService.isCompletedTest(1, 1));
        }        /**
     * TC_PTF_015: isCompletedTest - True nếu progress process > 1
     * 
     * Test Objective: Đánh dấu hoàn thành khi có tiến độ rõ ràng
     * Input: TestProgress process = 2
     * Expected Output: isCompletedTest = True
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_015: isCompletedTest - True nếu progress process > 1")
        void t020() {
            TestProgressEntity p = new TestProgressEntity(); p.setProcess(2);
            when(testProgressRepository.findByTest_IdAndStudentProfile_Id(1, 1)).thenReturn(List.of(p));
            assertTrue(testService.isCompletedTest(1, 1));
        }        /**
     * TC_PTF_016: getTestAttemptIds - Lấy danh sách IDs success
     * 
     * Test Objective: Query danh sách IDs lịch sử làm bài
     * Input: TestAttemptEntity Array
     * Expected Output: Trả về List IDs (size = 1)
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_016: getTestAttemptIds - Lấy danh sách IDs success")
        void t022() {
            TestAttemptEntity a = new TestAttemptEntity(); a.setId(100);
            when(testAttemptRepository.findByTestIdAndStudentProfileId(1,1)).thenReturn(List.of(a));
            assertEquals(1, testService.getTestAttemptIds(1,1).size());
        }        /**
     * TC_PTF_017: getCompletedTestsOfStudent - Truong hop khong the pass status
     * 
     * Test Objective: Kiểm tra đếm test khi chưa pass status lộ trình
     * Input: Mảng rỗng từ Progress
     * Expected Output: Count Test Completed = 0
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_017: getCompletedTestsOfStudent - Truong hop khong the pass status")
        void t023() {
            TestEntity t1 = new TestEntity(); t1.setId(1);
            when(testProgressRepository.findByTest_IdAndStudentProfile_Id(1, 1)).thenReturn(Collections.emptyList());
            assertEquals(0, testService.getCompletedTestsOfStudent(List.of(t1), 1));
        }        /**
     * TC_PTF_018: getTestResponseDetail - Test entity bug without wrapper
     * 
     * Test Objective: Lỗi NPE khi Request gửi Module Null
     * Input: TestEntity thiếu Wrapper (Course=null)
     * Expected Output: Throw NullPointerException / AppException nếu bắt được
     * Notes: Phát hiện lỗ hổng hệ thống
     */
    @Test
    @DisplayName("TC_PTF_018: getTestResponseDetail - Test entity bug without wrapper")
        void t024() {
             TestEntity t1 = new TestEntity();
             t1.setId(1);
             // Leave Module/Course null -> This will throw NPE in isLockLesson
             when(testRepository.findById(1)).thenReturn(Optional.of(t1));
             when(testConverter.toResponseNoQuestion(t1)).thenReturn(new TestResponse());
             
             assertThrows(AppException.class, () -> {
                 testService.getTestResponseDetail(List.of(t1), 1);
             }, "Missing Course association should be gracefully handled");
        }        /**
     * TC_PTF_019: isLock - Nếu status khác LOCK/DONE và rỗng
     * 
     * Test Objective: Kiểm tra isLock khi Course đang IN_PROGRESS
     * Input: Course status = IN_PROGRESS
     * Expected Output: isLock = True
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_019: isLock - Nếu status khác LOCK/DONE và rỗng")
        void t025() {
            TestEntity t = new TestEntity(); t.setId(1);
            ModuleEntity m = new ModuleEntity(); CourseEntity c = new CourseEntity(); c.setId(10);
            m.setCourse(c); t.setModule(m);
            when(testRepository.findById(1)).thenReturn(Optional.of(t));
            when(enrollmentCourseRepository.findStatus(1, 10)).thenReturn("IN_PROGRESS");
            assertTrue(testService.isLock(1, 1));
        }        /**
     * TC_PTF_020: BUG - commpletedStar dính NullPointerException khi điểm TestAttempt là Null (Dirty Data)
     * 
     * Test Objective: Bug thâm căn cố đế do thiếu Validate DB cũ
     * Input: TotalScore là Null
     * Expected Output: NPE thay vì trả về mặc định
     * Notes: Phát hiện lỗ hổng hệ thống
     */
    @Test
    @DisplayName("TC_PTF_020: BUG - commpletedStar dính NullPointerException khi điểm TestAttempt là Null (Dirty Data)")
        void t037_commpletedStar_NullScore() {
             TestAttemptEntity attempt = new TestAttemptEntity();
             attempt.setTotalScore(null); // Dirty DB record 
             when(testAttemptRepository.findByTestIdAndStudentProfileId(1, 1)).thenReturn(List.of(attempt));
             
             assertDoesNotThrow(() -> testService.commpletedStar(1, 1),
                  "Thuật toán lấy Max Score dính NPE khi dữ liệu cũ trong DB bị null TotalScore");
        }        /**
     * TC_PTF_021: BUG - isCompletedTest dính NPE khi Process của Tiến độ là Null (Dirty Data)
     * 
     * Test Objective: Bug so sánh mộc do unboxing giá trị Null
     * Input: TestProgressEntity process = Null
     * Expected Output: NPE khi getProcess() == 0
     * Notes: Phát hiện lỗ hổng hệ thống
     */
    @Test
    @DisplayName("TC_PTF_021: BUG - isCompletedTest dính NPE khi Process của Tiến độ là Null (Dirty Data)")
        void t038_isCompletedTest_NullProcess() {
             TestProgressEntity p = new TestProgressEntity(); 
             p.setProcess(null); // Buggy or Dirty DB record
             when(testProgressRepository.findByTest_IdAndStudentProfile_Id(1, 1)).thenReturn(List.of(p));
             
             assertDoesNotThrow(() -> testService.isCompletedTest(1, 1),
                  "So sánh mộc `getProcess() == 0` ném NPE khi Object Integer là null");
        }
    }

    @Nested
    @DisplayName("2. Đánh giá & Xem Kết Quả (TestAttemptService) - 10 Tests")
    class TestAttemptServiceTests {
        @Mock private TestRepository testRepository;
        @Mock private StudentProfileRepository studentProfileRepository;
        @Mock private TestAttemptRepository testAttemptRepository;
        @Mock private AssessmentRepository assessmentRepository;
        @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
        @Mock private EnrollmentServece enrollmentServece;
        @Mock private TestAttemptConverter testAttemptConverter;
        @Spy  private ModelMapper modelMapper = new ModelMapper(); // REAL MODEL MAPPER FOR BUG HUNTING

        @InjectMocks
        private TestAttemptServiceImpl testAttemptService;        /**
     * TC_PTF_022: saveResultFirstTest - Thành công
     * 
     * Test Objective: Nộp bài kiểm tra chuẩn hóa thành công
     * Input: Payload Request Đầy Đủ AssessmentAttemptRequests
     * Expected Output: Lưu Record xuống Database thành công
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_022: saveResultFirstTest - Thành công")
        void t026() {
            TestAttemptRequest req = new TestAttemptRequest();
            req.setStudentProfileId(1); req.setTestId(1);
            req.setAssessmentAttemptRequests(Collections.emptyList());

            StudentProfileEntity student = new StudentProfileEntity();
            when(studentProfileRepository.findById(1)).thenReturn(Optional.of(student));
            when(testRepository.findById(1)).thenReturn(Optional.of(new TestEntity()));

            testAttemptService.saveResultFirstTest(req);
            verify(testAttemptRepository, times(1)).save(any(TestAttemptEntity.class));
        }        /**
     * TC_PTF_023: saveResultFirstTest - Báo lỗi nếu StudentId không tồn tại
     * 
     * Test Objective: Chặn Hacker nộp bài với Fake User ID
     * Input: studentProfileId = 99
     * Expected Output: Throw AppException: STUDENT_NOT_FOUND
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_023: saveResultFirstTest - Báo lỗi nếu StudentId không tồn tại")
        void t027() {
            TestAttemptRequest req = new TestAttemptRequest();
            req.setStudentProfileId(99); 
            when(studentProfileRepository.findById(99)).thenReturn(Optional.empty());
            assertThrows(AppException.class, () -> testAttemptService.saveResultFirstTest(req));
        }        /**
     * TC_PTF_024: saveResultMiniTest - Thành công
     * 
     * Test Objective: Lưu điểm chấm theo thời gian thực (MiniTest)
     * Input: Payload hợp lệ loại MINI
     * Expected Output: Gọi Save() cho TestAttempt thành công
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_024: saveResultMiniTest - Thành công")
        void t028() {
            TestAttemptRequest req = new TestAttemptRequest();
            req.setStudentProfileId(1); req.setTestId(1);
            req.setAssessmentAttemptRequests(Collections.emptyList());
            when(studentProfileRepository.findById(1)).thenReturn(Optional.of(new StudentProfileEntity()));
            when(testRepository.findById(1)).thenReturn(Optional.of(new TestEntity()));
            testAttemptService.saveResultMiniTest(req);
            verify(testAttemptRepository, times(1)).save(any(TestAttemptEntity.class));
        }        /**
     * TC_PTF_025: saveResultMiniTest - BUG: Không ném AppException khi Option sai mà ném RuntimeException
     * 
     * Test Objective: Bug bảo mật: Ném sai Exception Options
     * Input: assessmentOptionId = 999
     * Expected Output: Throw RuntimeException thay vì AppException domain
     * Notes: Phát hiện lỗ hổng hệ thống
     */
    @Test
    @DisplayName("TC_PTF_025: saveResultMiniTest - BUG: Không ném AppException khi Option sai mà ném RuntimeException")
        void t029() {
            // Phân tích mã nguồn: khi AssessmentOption không tìm thấy, hệ thống ném RuntimeException thay vì AppException
            TestAttemptRequest req = new TestAttemptRequest();
            req.setStudentProfileId(1); req.setTestId(1);
            
            AssessmentAnswerRequest ansReq = new AssessmentAnswerRequest();
            ansReq.setAssessmentQuestionId(10); ansReq.setAssessmentOptionId(999); ansReq.setIsCorrect(true);

            AssessmentAttemptRequest attReq = new AssessmentAttemptRequest();
            attReq.setAssessmentId(100); attReq.setAssessmentAnswerRequests(List.of(ansReq));
            req.setAssessmentAttemptRequests(List.of(attReq));

            when(studentProfileRepository.findById(1)).thenReturn(Optional.of(new StudentProfileEntity()));
            when(testRepository.findById(1)).thenReturn(Optional.of(new TestEntity()));
            when(assessmentRepository.findById(100)).thenReturn(Optional.of(new AssessmentEntity()));
            
            AssessmentQuestionEntity qMock = new AssessmentQuestionEntity();
            qMock.setAssessmentOptions(Collections.emptyList());
            when(assessmentQuestionRepository.findById(10)).thenReturn(Optional.of(qMock));

            AppException ex = assertThrows(AppException.class, () -> testAttemptService.saveResultMiniTest(req),
                "Backend ném sai loại Exception! Lẽ ra phải là AppException(OPTION_NOT_FOUND) nhưng hiện tại ném RuntimeException.");
        }        /**
     * TC_PTF_026: saveResultMiniTest - BUG: Nếu requests là null thì gây ra NullPointerException do .stream()
     * 
     * Test Objective: Bug bảo mật: DDoS qua Null Payload
     * Input: AssessmentAttemptRequests Array = null
     * Expected Output: Throws NPE khi stream()
     * Notes: Phát hiện lỗ hổng hệ thống
     */
    @Test
    @DisplayName("TC_PTF_026: saveResultMiniTest - BUG: Nếu requests là null thì gây ra NullPointerException do .stream()")
        void t030() {
            TestAttemptRequest req = new TestAttemptRequest();
            req.setStudentProfileId(1); req.setTestId(1);
            req.setAssessmentAttemptRequests(null); // Gửi List null
            
            when(studentProfileRepository.findById(1)).thenReturn(Optional.of(new StudentProfileEntity()));
            when(testRepository.findById(1)).thenReturn(Optional.of(new TestEntity()));

            AppException ex = assertThrows(AppException.class, () -> testAttemptService.saveResultMiniTest(req), 
                  "Tính điểm không check Null list, sinh ra NPE ! Cần AppException hợp lệ.");
        }        /**
     * TC_PTF_027: getTestAttemptDetailById - Thành công Type=LISTENING_1
     * 
     * Test Objective: Lấy chi tiết lượt thi Nghe TOEIC Phẩn 1
     * Input: ExerciseType code = LISTENING_1
     * Expected Output: Trả về Response Summery đúng định dạng
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_027: getTestAttemptDetailById - Thành công Type=LISTENING_1")
        void t031() {
            TestAttemptEntity attempt = new TestAttemptEntity(); attempt.setId(1);
            AssessmentAttemptEntity asmAtt = new AssessmentAttemptEntity();
            AssessmentEntity asm = new AssessmentEntity();
            ExerciseTypeEntity type = new ExerciseTypeEntity(); type.setCode("LISTENING_1"); type.setDescription("Nghe 1");
            asm.setExercisetype(type); asm.setAssessmentQuestions(Collections.emptyList());
            asmAtt.setAssessment(asm); asmAtt.setAssessmentAnswers(Collections.emptyList());
            attempt.setAssessmentAttempts(List.of(asmAtt));

            when(testAttemptRepository.findById(1)).thenReturn(Optional.of(attempt));
            when(testAttemptConverter.toResponseSummery(attempt)).thenReturn(new TestAttemptResponse());

            TestAttemptResponse res = testAttemptService.getTestAttemptDetailById(1);
            assertNotNull(res);
        }        /**
     * TC_PTF_028: getTestAttemptDetailById - BUG MODEL MAPPER - NullPointerException khi clone
     * 
     * Test Objective: Lỗi thư viện Model Mapper Mapping Ẩn
     * Input: ExerciseTypeEntity = null
     * Expected Output: Throws NullPointerException từ thư viện clone
     * Notes: Phát hiện lỗ hổng hệ thống
     */
    @Test
    @DisplayName("TC_PTF_028: getTestAttemptDetailById - BUG MODEL MAPPER - NullPointerException khi clone")
        void t032() {
            // ModelMapper mặc định sẽ map sai và trả về entity thiếu metadata => NPE System.
            TestAttemptEntity attempt = new TestAttemptEntity(); attempt.setId(1);
            AssessmentAttemptEntity asmAtt = new AssessmentAttemptEntity();
            AssessmentEntity asm = new AssessmentEntity();
            ExerciseTypeEntity type = null; // Fake bug missing map
            asm.setExercisetype(type);
            
            AssessmentQuestionEntity q1 = new AssessmentQuestionEntity(); q1.setId(10);
            q1.setAssessmentOptions(Collections.emptyList());
            asm.setAssessmentQuestions(List.of(q1));
            asmAtt.setAssessment(asm); asmAtt.setAssessmentAnswers(Collections.emptyList());
            attempt.setAssessmentAttempts(List.of(asmAtt));

            when(testAttemptRepository.findById(1)).thenReturn(Optional.of(attempt));
            when(testAttemptConverter.toResponseSummery(attempt)).thenReturn(new TestAttemptResponse());

            // Mong chờ nó xử lý thành công hoặc ra AppException, nhưng chạy sẽ văng NPE vì BUG ModelMapper
            assertDoesNotThrow(() -> {
                testAttemptService.getTestAttemptDetailById(1);
            }, "Lỗi mapping của thư viện khiến mất ExerciseType => NPE");
        }        /**
     * TC_PTF_029: Tính điểm an toàn khi sum=0
     * 
     * Test Objective: Logic an toàn mỏ neo (Anchor) khi Submit Giấy Trắng
     * Input: Answers Array Empty
     * Expected Output: testAttempt.totalScore = 0.0f
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_029: Tính điểm an toàn khi sum=0")
        void t033() {
            TestAttemptRequest req = new TestAttemptRequest();
            req.setStudentProfileId(1); req.setTestId(1);
            AssessmentAttemptRequest attReq = new AssessmentAttemptRequest();
            attReq.setAssessmentId(100); attReq.setAssessmentAnswerRequests(Collections.emptyList()); 
            req.setAssessmentAttemptRequests(List.of(attReq));
            when(studentProfileRepository.findById(1)).thenReturn(Optional.of(new StudentProfileEntity()));
            when(testRepository.findById(1)).thenReturn(Optional.of(new TestEntity()));
            when(assessmentRepository.findById(100)).thenReturn(Optional.of(new AssessmentEntity()));

            testAttemptService.saveResultMiniTest(req);
            verify(testAttemptRepository, times(1)).save(argThat(a -> a.getTotalScore() == 0.0f));
        }        /**
     * TC_PTF_030: getTestAttemptDetailById - Map options đang được chọn
     * 
     * Test Objective: Rọi Đáp án đã được Server Check
     * Input: AnswerEntity có option true/false
     * Expected Output: AssessmentOptionResponse selected = true
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_030: getTestAttemptDetailById - Map options đang được chọn")
        void t034() {
            TestAttemptEntity attempt = new TestAttemptEntity(); attempt.setId(1);
            AssessmentAttemptEntity asmAtt = new AssessmentAttemptEntity();
            AssessmentEntity asm = new AssessmentEntity();
            ExerciseTypeEntity type = new ExerciseTypeEntity(); type.setCode("LISTENING_1"); type.setDescription("Nghe 1");
            asm.setExercisetype(type);
            AssessmentQuestionEntity q1 = new AssessmentQuestionEntity(); q1.setId(100);
            AssessmentOptionEntity op1 = new AssessmentOptionEntity(); op1.setId(101);
            AssessmentOptionEntity op2 = new AssessmentOptionEntity(); op2.setId(102);
            q1.setAssessmentOptions(List.of(op1, op2)); asm.setAssessmentQuestions(List.of(q1));
            
            AssessmentAnswerEntity ans = new AssessmentAnswerEntity();
            ans.setAssessmentQuestion(q1); ans.setAssessmentOption(op1);
            asmAtt.setAssessment(asm); asmAtt.setAssessmentAnswers(List.of(ans));
            attempt.setAssessmentAttempts(List.of(asmAtt));

            when(testAttemptRepository.findById(1)).thenReturn(Optional.of(attempt));
            when(testAttemptConverter.toResponseSummery(attempt)).thenReturn(new TestAttemptResponse());

            TestAttemptResponse res = testAttemptService.getTestAttemptDetailById(1);
            AssessmentOptionResponse rx1 = res.getAssessmentResponses().get(0).getAssessmentQuestions().get(0).getChoices().get(0);
            assertTrue(rx1.getSelected());
        }        /**
     * TC_PTF_031: getTestAttemptDetailById - Không tìm thấy
     * 
     * Test Objective: Không tìm thấy lượt thi
     * Input: TestAttemptID = 99
     * Expected Output: AppException NotFound
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_031: getTestAttemptDetailById - Không tìm thấy")
        void t035() {
            when(testAttemptRepository.findById(99)).thenReturn(Optional.empty());
            assertThrows(AppException.class, () -> testAttemptService.getTestAttemptDetailById(99));
        }        /**
     * TC_PTF_032: BUG - Nộp bài thi lọt AssessmentId=null gây lỗi IllegalArgumentException JPA
     * 
     * Test Objective: Bug lỗ hổng Injection qua Null Assessment ID
     * Input: assessmentId = null
     * Expected Output: IllegalArgumentException từ lõi JPA
     * Notes: Phát hiện lỗ hổng hệ thống
     */
    @Test
    @DisplayName("TC_PTF_032: BUG - Nộp bài thi lọt AssessmentId=null gây lỗi IllegalArgumentException JPA")
        void t039_saveResultMiniTest_NullAssessmentId() {
            TestAttemptRequest req = new TestAttemptRequest();
            req.setStudentProfileId(1); req.setTestId(1);
            
            AssessmentAttemptRequest attReq = new AssessmentAttemptRequest();
            attReq.setAssessmentId(null);
            attReq.setAssessmentAnswerRequests(Collections.emptyList());
            req.setAssessmentAttemptRequests(List.of(attReq));

            when(studentProfileRepository.findById(1)).thenReturn(Optional.of(new StudentProfileEntity()));
            when(testRepository.findById(1)).thenReturn(Optional.of(new TestEntity()));
            
            when(assessmentRepository.findById(null)).thenThrow(new IllegalArgumentException("The given id must not be null!"));

            assertDoesNotThrow(() -> testAttemptService.saveResultMiniTest(req),
                 "Thiếu field Validation khiến hệ thống gọi JPA bằng giá trị ID null");
        }        /**
     * TC_PTF_033: LUỒNG ẨN - Test nhánh gộp câu hỏi (else branch) trong getAssessmentAttemp
     * 
     * Test Objective: Logic ẩn: Gộp câu hỏi chia dạng
     * Input: Submit 2 Request có cùng AssessmentId (Reading Split)
     * Expected Output: Thành công save DB qua Else Branch
     * Notes: Kiểm thử hộp đen Business Logic
     */
    @Test
    @DisplayName("TC_PTF_033: LUỒNG ẨN - Test nhánh gộp câu hỏi (else branch) trong getAssessmentAttemp")
        void t040_saveResultMiniTest_MergeAssessments() {
             // Mô phỏng 1 TestRequest gửi 2 attempts cho CÙNG MỘT AssessmentId (tức là nộp 2 mảng đáp án cho 1 bài gộp)
             TestAttemptRequest req = new TestAttemptRequest();
             req.setStudentProfileId(1); req.setTestId(1);
             
             AssessmentAttemptRequest att1 = new AssessmentAttemptRequest();
             att1.setAssessmentId(100); att1.setAssessmentAnswerRequests(Collections.emptyList());
             
             AssessmentAttemptRequest att2 = new AssessmentAttemptRequest();
             att2.setAssessmentId(100); att2.setAssessmentAnswerRequests(Collections.emptyList());
             
             req.setAssessmentAttemptRequests(List.of(att1, att2));
             
             when(studentProfileRepository.findById(1)).thenReturn(Optional.of(new StudentProfileEntity()));
             when(testRepository.findById(1)).thenReturn(Optional.of(new TestEntity()));
             AssessmentEntity mockedAsm = new AssessmentEntity();
             mockedAsm.setId(100);
             when(assessmentRepository.findById(100)).thenReturn(Optional.of(mockedAsm));

             // Lần 1 qua If, Lần 2 nhảy vào Else
             assertDoesNotThrow(() -> testAttemptService.saveResultMiniTest(req), "Thử nghiệm luồng ẩn gộp mảng mồ côi thành công");
             verify(testAttemptRepository, times(1)).save(any());
        }
    }
}
