package com.mxhieu.doantotnghiep;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.mxhieu.doantotnghiep.converter.AssessmentConverter;
import com.mxhieu.doantotnghiep.converter.EnrollmentConverter;
import com.mxhieu.doantotnghiep.converter.StudyPlanConverter;
import com.mxhieu.doantotnghiep.converter.TestAttemptConverter;
import com.mxhieu.doantotnghiep.converter.TrackConverter;
import com.mxhieu.doantotnghiep.dto.request.AssessmentAnswerRequest;
import com.mxhieu.doantotnghiep.dto.request.AssessmentAttemptRequest;
import com.mxhieu.doantotnghiep.dto.request.EnrollmentRequest;
import com.mxhieu.doantotnghiep.dto.request.TestAttemptRequest;
import com.mxhieu.doantotnghiep.dto.response.EnrollmentResponst;
import com.mxhieu.doantotnghiep.entity.AssessmentEntity;
import com.mxhieu.doantotnghiep.entity.AssessmentOptionEntity;
import com.mxhieu.doantotnghiep.entity.AssessmentQuestionEntity;
import com.mxhieu.doantotnghiep.entity.CourseEntity;
import com.mxhieu.doantotnghiep.entity.EnrollmentEntity;
import com.mxhieu.doantotnghiep.entity.LessonEntity;
import com.mxhieu.doantotnghiep.entity.ModuleEntity;
import com.mxhieu.doantotnghiep.entity.StudentProfileEntity;
import com.mxhieu.doantotnghiep.entity.TestAttemptEntity;
import com.mxhieu.doantotnghiep.entity.TestEntity;
import com.mxhieu.doantotnghiep.entity.TrackEntity;
import com.mxhieu.doantotnghiep.exception.AppException;
import com.mxhieu.doantotnghiep.exception.ErrorCode;
import com.mxhieu.doantotnghiep.repository.AssessmentAnswerRepository;
import com.mxhieu.doantotnghiep.repository.AssessmentOptionRepository;
import com.mxhieu.doantotnghiep.repository.AssessmentQuestionRepository;
import com.mxhieu.doantotnghiep.repository.AssessmentRepository;
import com.mxhieu.doantotnghiep.repository.CourseRepository;
import com.mxhieu.doantotnghiep.repository.EnrollmentCourseRepository;
import com.mxhieu.doantotnghiep.repository.EnrollmentRepository;
import com.mxhieu.doantotnghiep.repository.ExerciseTypeRepository;
import com.mxhieu.doantotnghiep.repository.LessonProgressRepository;
import com.mxhieu.doantotnghiep.repository.LessonRepository;
import com.mxhieu.doantotnghiep.repository.ModuleRepository;
import com.mxhieu.doantotnghiep.repository.StudentProfileRepository;
import com.mxhieu.doantotnghiep.repository.StudyPlanRepository;
import com.mxhieu.doantotnghiep.repository.TestAttemptRepository;
import com.mxhieu.doantotnghiep.repository.TestProgressRepository;
import com.mxhieu.doantotnghiep.repository.TestRepository;
import com.mxhieu.doantotnghiep.repository.TrackRepository;
import com.mxhieu.doantotnghiep.service.EnrollmentServece;
import com.mxhieu.doantotnghiep.service.LessonService;
import com.mxhieu.doantotnghiep.service.TestService;
import com.mxhieu.doantotnghiep.service.TrackService;
import com.mxhieu.doantotnghiep.service.impl.AssessmentServiceImpl;
import com.mxhieu.doantotnghiep.service.impl.EnrollmentServeceImpl;
import com.mxhieu.doantotnghiep.service.impl.StudyPlanServiceImpl;
import com.mxhieu.doantotnghiep.service.impl.TestAttemptServiceImpl;
import com.mxhieu.doantotnghiep.utils.ModuleType;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@DisplayName("Quản lý Lộ trình Học tập (Study Plan)")
@Epic("Study Plan")
class StudyPlanFeatureTests {

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("Kiểm tra đầu vào (saveResultFirstTest)")
        @Feature("Placement Test")
    class TestAttemptServiceImplTest {

        @Mock
        private TestRepository testRepository;
        @Mock
        private StudentProfileRepository studentProfileRepository;
        @Mock
        private TestAttemptRepository testAttemptRepository;
        @Mock
        private AssessmentRepository assessmentRepository;
        @Mock
        private AssessmentQuestionRepository assessmentQuestionRepository;
        @Mock
        private EnrollmentServece enrollmentServece;
        @Mock
        private TestAttemptConverter testAttemptConverter;
        @Mock
        private AssessmentOptionRepository assessmentOptionRepository;
        @Mock
        private AssessmentAnswerRepository assessmentAnswerRepository;
        @Mock
        private ModelMapper modelMapper;

        @InjectMocks
        private TestAttemptServiceImpl service;

        @Nested
        @DisplayName("Lưu kết quả kiểm tra thành công")
        @Story("Happy Paths")
        class HappyPathCluster {        /**
     * SP_001_TC: Thành công khi trả lời đúng tất cả (Score 100%)
     * 
     * Test Objective: Thành công khi trả lời đúng tất cả (Score 100%)
     * Input: `input = { studentProfileId: 10, testId: 20, assessmentAttemptRequests: [{ assessmentId: 30, answers: [{ questionId: 40, optionId: 50, isCorrect: true }] }] }; mocks = { findById(10)->student{firstLogin:true}, findById(20)->test, findById(30)->assessment, findById(40)->question{options:[{id:50}]} }`
     * Expected Output: `expected = { type: 'void', sideEffects: { student.firstLogin: false, testAttempt.totalScore: 100.0, studentRepo.save: called, testAttemptRepo.save: called } }`
     * Notes: Happy path - tính điểm hoàn hảo
     */
    @Test
    @DisplayName("SP_001_TC: Thành công khi trả lời đúng tất cả (Score 100%)")
            void shouldSaveAttempt_scoreIs100_whenAllAnswersCorrect() {
                StudentProfileEntity student = StudentProfileEntity.builder().id(10).firstLogin(true).build();
                TestEntity test = TestEntity.builder().id(20).build();
                AssessmentEntity assessment = AssessmentEntity.builder().id(30).build();
                AssessmentOptionEntity option = AssessmentOptionEntity.builder().id(50).build();
                AssessmentQuestionEntity question = AssessmentQuestionEntity.builder()
                        .id(40).assessmentOptions(List.of(option)).build();

                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));
                when(testRepository.findById(20)).thenReturn(Optional.of(test));
                when(assessmentRepository.findById(30)).thenReturn(Optional.of(assessment));
                when(assessmentQuestionRepository.findById(40)).thenReturn(Optional.of(question));

                TestAttemptRequest request = TestAttemptRequest.builder()
                        .studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(List.of(
                                AssessmentAttemptRequest.builder().assessmentId(30)
                                        .assessmentAnswerRequests(List.of(
                                                AssessmentAnswerRequest.builder()
                                                        .assessmentQuestionId(40).assessmentOptionId(50).isCorrect(true)
                                                        .build()))
                                        .build()))
                        .build();

                service.saveResultFirstTest(request);
                assertFalse(student.getFirstLogin());
                verify(studentProfileRepository).save(student);
                ArgumentCaptor<TestAttemptEntity> captor = ArgumentCaptor.forClass(TestAttemptEntity.class);
                verify(testAttemptRepository).save(captor.capture());
                assertEquals(100.0f, captor.getValue().getTotalScore(), 0.001f);
            }        /**
     * SP_002_TC: Thành công khi trả lời sai tất cả (Score 0%)
     * 
     * Test Objective: Thành công khi trả lời sai tất cả (Score 0%)
     * Input: `input = { studentProfileId: 10, testId: 20, assessmentAttemptRequests: [{ assessmentId: 30, answers: [{ questionId: 40, optionId: 50, isCorrect: false }] }] }; mocks = { findById(10)->student{firstLogin:true}, findById(20)->test, findById(30)->assessment, findById(40)->question }`
     * Expected Output: `expected = { sideEffects: { student.firstLogin: false, testAttempt.totalScore: 0.0 } }`
     * Notes: Happy path - điểm 0
     */
    @Test
    @DisplayName("SP_002_TC: Thành công khi trả lời sai tất cả (Score 0%)")
            void shouldSaveAttempt_scoreIsZero_whenAllAnswersWrong() {
                StudentProfileEntity student = StudentProfileEntity.builder().id(10).firstLogin(true).build();
                TestEntity test = TestEntity.builder().id(20).build();
                AssessmentEntity assessment = AssessmentEntity.builder().id(30).build();
                AssessmentOptionEntity option = AssessmentOptionEntity.builder().id(50).build();
                AssessmentQuestionEntity question = AssessmentQuestionEntity.builder()
                        .id(40).assessmentOptions(List.of(option)).build();

                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));
                when(testRepository.findById(20)).thenReturn(Optional.of(test));
                when(assessmentRepository.findById(30)).thenReturn(Optional.of(assessment));
                when(assessmentQuestionRepository.findById(40)).thenReturn(Optional.of(question));

                TestAttemptRequest request = TestAttemptRequest.builder()
                        .studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(List.of(
                                AssessmentAttemptRequest.builder().assessmentId(30)
                                        .assessmentAnswerRequests(List.of(
                                                AssessmentAnswerRequest.builder()
                                                        .assessmentQuestionId(40).assessmentOptionId(50)
                                                        .isCorrect(false).build()))
                                        .build()))
                        .build();

                service.saveResultFirstTest(request);
                // [FIX] Bổ sung kiểm tra firstLogin và lưu student
                assertFalse(student.getFirstLogin());
                verify(studentProfileRepository).save(student);
                ArgumentCaptor<TestAttemptEntity> captor = ArgumentCaptor.forClass(TestAttemptEntity.class);
                verify(testAttemptRepository).save(captor.capture());
                assertEquals(0.0f, captor.getValue().getTotalScore(), 0.001f);
            }        /**
     * SP_003_TC: Thành công khi submit không có câu trả lời nào
     * 
     * Test Objective: Thành công khi submit không có câu trả lời nào
     * Input: `input = { studentProfileId: 10, testId: 20, assessmentAttemptRequests: [] }; mocks = { findById(10)->student{firstLogin:true}, findById(20)->test }`
     * Expected Output: `expected = { sideEffects: { student.firstLogin: false, testAttempt.totalScore: 0.0, testAttemptRepo.save: called } }`
     * Notes: Edge case - submit trống
     */
    @Test
    @DisplayName("SP_003_TC: Thành công khi submit không có câu trả lời nào")
            void shouldSaveAttempt_scoreIsZero_whenNoAnswersProvided() {
                StudentProfileEntity student = StudentProfileEntity.builder().id(10).firstLogin(true).build();
                TestEntity test = TestEntity.builder().id(20).build();
                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));
                when(testRepository.findById(20)).thenReturn(Optional.of(test));

                TestAttemptRequest request = TestAttemptRequest.builder()
                        .studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(List.of()).build();

                service.saveResultFirstTest(request);
                // [FIX] Bổ sung kiểm tra firstLogin=false và score=0
                assertFalse(student.getFirstLogin());
                verify(studentProfileRepository).save(student);
                ArgumentCaptor<TestAttemptEntity> scoreCaptor = ArgumentCaptor.forClass(TestAttemptEntity.class);
                verify(testAttemptRepository).save(scoreCaptor.capture());
                assertEquals(0.0f, scoreCaptor.getValue().getTotalScore(), 0.001f);
            }        /**
     * SP_004_TC: Tính toán điểm hỗn hợp (Ví dụ 2/4 đúng -> 50%)
     * 
     * Test Objective: Tính toán điểm hỗn hợp (2/4 đúng → 50%)
     * Input: `input = { assessmentAttemptRequests: [{ assessmentId: 30, answers: [{q:41,isCorrect:true},{q:42,isCorrect:false},{q:43,isCorrect:true},{q:44,isCorrect:false}] }] }; mocks = { findById(30)->assessment, findById(any)->question{options:[{id:1}]} }`
     * Expected Output: `expected = { sideEffects: { testAttempt.totalScore: 50.0 } }`
     * Notes: Happy path - tính điểm hỗn hợp
     */
    @Test
    @DisplayName("SP_004_TC: Tính toán điểm hỗn hợp (Ví dụ 2/4 đúng -> 50%)")
            void shouldCalculatePartialScore_when2of4AnswersCorrect() {
                StudentProfileEntity student = StudentProfileEntity.builder().id(10).firstLogin(true).build();
                TestEntity test = TestEntity.builder().id(20).build();
                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));
                when(testRepository.findById(20)).thenReturn(Optional.of(test));
                when(assessmentRepository.findById(30))
                        .thenReturn(Optional.of(AssessmentEntity.builder().id(30).build()));
                when(assessmentQuestionRepository.findById(anyInt())).thenReturn(Optional.of(AssessmentQuestionEntity
                        .builder().assessmentOptions(List.of(AssessmentOptionEntity.builder().id(1).build())).build()));

                TestAttemptRequest request = TestAttemptRequest.builder()
                        .studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(List.of(
                                AssessmentAttemptRequest.builder().assessmentId(30)
                                        .assessmentAnswerRequests(List.of(
                                                AssessmentAnswerRequest.builder().assessmentQuestionId(41)
                                                        .assessmentOptionId(1).isCorrect(true).build(),
                                                AssessmentAnswerRequest.builder().assessmentQuestionId(42)
                                                        .assessmentOptionId(1).isCorrect(false).build(),
                                                AssessmentAnswerRequest.builder().assessmentQuestionId(43)
                                                        .assessmentOptionId(1).isCorrect(true).build(),
                                                AssessmentAnswerRequest.builder().assessmentQuestionId(44)
                                                        .assessmentOptionId(1).isCorrect(false).build()))
                                        .build()))
                        .build();

                service.saveResultFirstTest(request);
                ArgumentCaptor<TestAttemptEntity> captor = ArgumentCaptor.forClass(TestAttemptEntity.class);
                verify(testAttemptRepository).save(captor.capture());
                assertEquals(50.0f, captor.getValue().getTotalScore(), 0.001f);
            }        /**
     * SP_005_TC: Verify enrollmentServece.saveEnrollment được gọi với đúng studentProfileId và score
     * 
     * Test Objective: Verify enrollmentServece.saveEnrollment được gọi với đúng studentProfileId và score
     * Input: `input = { studentProfileId: 10, testId: 20, assessmentAttemptRequests: [{ assessmentId: 30, answers: [{q:40,optionId:50,isCorrect:true}] }] }; mocks tương tự SP_001`
     * Expected Output: `expected = { sideEffects: { enrollmentService.saveEnrollment.calledWith: { studentProfileId: 10, score: 100.0 } } }`
     * Notes: Happy path - kiểm tra lời gọi service xuôi dòng
     */
    @Test
    @DisplayName("SP_005_TC: Verify enrollmentServece.saveEnrollment được gọi với đúng studentProfileId và score")
            void shouldCallSaveEnrollment_withCorrectScoreAndStudentId() {
                StudentProfileEntity student = StudentProfileEntity.builder().id(10).firstLogin(true).build();
                TestEntity test = TestEntity.builder().id(20).build();
                AssessmentEntity assessment = AssessmentEntity.builder().id(30).build();
                AssessmentOptionEntity option = AssessmentOptionEntity.builder().id(50).build();
                AssessmentQuestionEntity question = AssessmentQuestionEntity.builder()
                        .id(40).assessmentOptions(List.of(option)).build();

                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));
                when(testRepository.findById(20)).thenReturn(Optional.of(test));
                when(assessmentRepository.findById(30)).thenReturn(Optional.of(assessment));
                when(assessmentQuestionRepository.findById(40)).thenReturn(Optional.of(question));

                TestAttemptRequest request = TestAttemptRequest.builder()
                        .studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(List.of(
                                AssessmentAttemptRequest.builder().assessmentId(30)
                                        .assessmentAnswerRequests(List.of(
                                                AssessmentAnswerRequest.builder()
                                                        .assessmentQuestionId(40).assessmentOptionId(50).isCorrect(true)
                                                        .build()))
                                        .build()))
                        .build();

                service.saveResultFirstTest(request);

                ArgumentCaptor<EnrollmentRequest> enrollCaptor = ArgumentCaptor.forClass(EnrollmentRequest.class);
                verify(enrollmentServece).saveEnrollment(enrollCaptor.capture());
                assertEquals(10, (int) enrollCaptor.getValue().getStudentProfileId());
                assertEquals(100.0f, enrollCaptor.getValue().getScore(), 0.001f);
            }
        }

        @Nested
        @DisplayName("Kiểm soát lỗi Validate")
        @Story("Validation Failures")
        class ValidationNotFoundCluster {        /**
     * SP_006_TC: Thất bại khi Student ID không tồn tại
     * 
     * Test Objective: Từ chối yêu cầu khi Student ID không tồn tại
     * Input: `input = { studentProfileId: 999 }; mocks = { findById(999) -> Optional.empty() }`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'STUDENT_PROFILE_NOT_FOUND' }`
     * Notes: Validation - ID học sinh không tồn tại
     */
    @Test
    @DisplayName("SP_006_TC: Thất bại khi Student ID không tồn tại")
            void shouldThrowStudentProfileNotFound_whenStudentDoesNotExist() {
                when(studentProfileRepository.findById(999)).thenReturn(Optional.empty());
                TestAttemptRequest request = TestAttemptRequest.builder().studentProfileId(999).build();
                AppException ex = assertThrows(AppException.class, () -> service.saveResultFirstTest(request));
                assertEquals(ErrorCode.STUDENT_PROFILE_NOT_FOUND, ex.getErrorCode());
            }        /**
     * SP_007_TC: Thất bại khi Test ID không tồn tại
     * 
     * Test Objective: Từ chối yêu cầu khi Test ID không tồn tại
     * Input: `input = { studentProfileId: 10, testId: 404 }; mocks = { findById(10)->student, findById(404)->Optional.empty() }`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'TEST_NOT_FOUND' }`
     * Notes: Validation - ID bài kiểm tra không tồn tại
     */
    @Test
    @DisplayName("SP_007_TC: Thất bại khi Test ID không tồn tại")
            void shouldThrowTestNotFound_whenTestDoesNotExist() {
                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(new StudentProfileEntity()));
                when(testRepository.findById(404)).thenReturn(Optional.empty());
                TestAttemptRequest request = TestAttemptRequest.builder().studentProfileId(10).testId(404).build();
                AppException ex = assertThrows(AppException.class, () -> service.saveResultFirstTest(request));
                assertEquals(ErrorCode.TEST_NOT_FOUND, ex.getErrorCode());
            }        /**
     * SP_008_TC: Thất bại khi Assessment ID không tồn tại
     * 
     * Test Objective: Từ chối yêu cầu khi Assessment ID không tồn tại
     * Input: `input = { studentProfileId: 10, testId: 20, assessmentAttemptRequests: [{ assessmentId: 999 }] }; mocks = { findById(999)->Optional.empty() }`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'ASSESSMENT_NOT_FOUND' }`
     * Notes: Validation - ID đề không tồn tại
     */
    @Test
    @DisplayName("SP_008_TC: Thất bại khi Assessment ID không tồn tại")
            void shouldThrowAssessmentNotFound_whenAssessmentDoesNotExist() {
                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(new StudentProfileEntity()));
                when(testRepository.findById(20)).thenReturn(Optional.of(new TestEntity()));
                when(assessmentRepository.findById(999)).thenReturn(Optional.empty());
                TestAttemptRequest request = TestAttemptRequest.builder().studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(
                                List.of(AssessmentAttemptRequest.builder().assessmentId(999).build()))
                        .build();
                AppException ex = assertThrows(AppException.class, () -> service.saveResultFirstTest(request));
                assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
            }        /**
     * SP_009_TC: Thất bại khi Assessment Question ID không tồn tại
     * 
     * Test Objective: Từ chối yêu cầu khi Assessment Question ID không tồn tại
     * Input: `input = { answers: [{ questionId: 999, optionId: 1, isCorrect: true }] }; mocks = { questionRepo.findById(999) -> Optional.empty() }`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'ASSESSMENT_QUESSTION_NOT_FOUND' }`
     * Notes: Validation - ID câu hỏi không tồn tại
     */
    @Test
    @DisplayName("SP_009_TC: Thất bại khi Assessment Question ID không tồn tại")
            void shouldThrowAssessmentQuestionNotFound_whenQuestionDoesNotExist() {
                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(new StudentProfileEntity()));
                when(testRepository.findById(20)).thenReturn(Optional.of(new TestEntity()));
                when(assessmentRepository.findById(30))
                        .thenReturn(Optional.of(AssessmentEntity.builder().id(30).build()));
                when(assessmentQuestionRepository.findById(999)).thenReturn(Optional.empty());

                TestAttemptRequest request = TestAttemptRequest.builder().studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(List.of(
                                AssessmentAttemptRequest.builder().assessmentId(30)
                                        .assessmentAnswerRequests(List.of(
                                                AssessmentAnswerRequest.builder()
                                                        .assessmentQuestionId(999).assessmentOptionId(1).isCorrect(true)
                                                        .build()))
                                        .build()))
                        .build();
                AppException ex = assertThrows(AppException.class, () -> service.saveResultFirstTest(request));
                assertEquals(ErrorCode.ASSESSMENT_QUESSTION_NOT_FOUND, ex.getErrorCode());
            }        /**
     * SP_010_TC: Thất bại khi Assessment Option ID không thuộc Question được chỉ định (IndexOutOfBounds → RuntimeException)
     * 
     * Test Objective: Từ chối yêu cầu khi Assessment Option ID không thuộc Question được chỉ định
     * Input: `input = { answers: [{ questionId: 40, optionId: 99, isCorrect: true }] }; mocks = { question.options: [{id: 50}] }` — optionId=99 không tồn tại trong options
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'CHOICE_NOT_FOUND' }`
     * Notes: **Bug:** Thực tế ném RuntimeException("Assessment option not found for the question") thay vì AppException
     */
    @Test
    @DisplayName("SP_010_TC: Thất bại khi Assessment Option ID không thuộc Question được chỉ định (IndexOutOfBounds → RuntimeException)")
            void shouldThrowRuntimeException_whenOptionIdDoesNotBelongToQuestion() {
                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(new StudentProfileEntity()));
                when(testRepository.findById(20)).thenReturn(Optional.of(new TestEntity()));
                when(assessmentRepository.findById(30))
                        .thenReturn(Optional.of(AssessmentEntity.builder().id(30).build()));
                // Question chỉ có option id=50, nhưng request gửi option id=99 → filter trả
                // empty → .get(0) ném IndexOutOfBoundsException → bọc RuntimeException
                AssessmentOptionEntity optionInQuestion = AssessmentOptionEntity.builder().id(50).build();
                when(assessmentQuestionRepository.findById(40)).thenReturn(Optional.of(
                        AssessmentQuestionEntity.builder().id(40).assessmentOptions(List.of(optionInQuestion))
                                .build()));
                TestAttemptRequest request = TestAttemptRequest.builder().studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(List.of(
                                AssessmentAttemptRequest.builder().assessmentId(30)
                                        .assessmentAnswerRequests(List.of(
                                                AssessmentAnswerRequest.builder()
                                                        .assessmentQuestionId(40).assessmentOptionId(99).isCorrect(true)
                                                        .build()))
                                        .build()))
                        .build();
                // Ghi nhận lỗi: source đang throw RuntimeException thô thay vì
                // AppException(CHOICE_NOT_FOUND)
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.saveResultFirstTest(request), "BUG: Ném RuntimeException khi option không thụôc question");
                                verify(studentProfileRepository, never()).save(any(StudentProfileEntity.class));
                                verify(testAttemptRepository, never()).save(any(TestAttemptEntity.class));
                                verify(enrollmentServece, never()).saveEnrollment(any(EnrollmentRequest.class));
            }
        }

        @Nested
        @DisplayName("Phát hiện lỗi tiềm ẩn của hệ thống")
        @Story("Known Backend Bugs")
        class BugHuntingCluster {        /**
     * SP_011_TC: Lỗi NPE khi assessmentAttemptRequests bị null (Cần fix source)
     * 
     * Test Objective: Lỗi NPE khi assessmentAttemptRequests bị null
     * Input: `input = { studentProfileId: 10, testId: 20, assessmentAttemptRequests: null }; mocks = { findById(10)->student, findById(20)->test }`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'MISSING_PARAMETERS' }`
     * Notes: **Bug:** Thực tế ném NullPointerException khi duyệt null list
     */
    @Test
    @DisplayName("SP_011_TC: Lỗi NPE khi assessmentAttemptRequests bị null (Cần fix source)")
            void shouldThrowNPE_whenAssessmentAttemptRequestsIsNull() {
                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(new StudentProfileEntity()));
                when(testRepository.findById(20)).thenReturn(Optional.of(new TestEntity()));
                TestAttemptRequest request = TestAttemptRequest.builder().studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(null).build();
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.saveResultFirstTest(request), "BUG: Bị NullPointerException khi list Requests rỗng");
            }        /**
     * SP_012_TC: Lỗi NPE khi một assessment có assessmentAnswerRequests = null (Cần fix source)
     * 
     * Test Objective: Lỗi NPE khi một assessment có assessmentAnswerRequests = null
     * Input: `input = { assessmentAttemptRequests: [{ assessmentId: 30, answerRequests: null }] }; mocks = { findById(30)->assessment }`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'MISSING_PARAMETERS' }`
     * Notes: **Bug:** Thực tế ném NullPointerException khi iterate phần tử null trong list
     */
    @Test
    @DisplayName("SP_012_TC: Lỗi NPE khi một assessment có assessmentAnswerRequests = null (Cần fix source)")
            void shouldThrowNPE_whenAssessmentAnswerRequestsIsNull() {
                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(new StudentProfileEntity()));
                when(testRepository.findById(20)).thenReturn(Optional.of(new TestEntity()));
                when(assessmentRepository.findById(30))
                        .thenReturn(Optional.of(AssessmentEntity.builder().id(30).build()));

                TestAttemptRequest request = TestAttemptRequest.builder()
                        .studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(List.of(
                                AssessmentAttemptRequest.builder().assessmentId(30)
                                        .assessmentAnswerRequests(null)
                                        .build()))
                        .build();

                org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.saveResultFirstTest(request), "BUG: Bị NullPointerException đi sâu vào answerRequests rỗng");
                verify(studentProfileRepository, never()).save(any(StudentProfileEntity.class));
                verify(testAttemptRepository, never()).save(any(TestAttemptEntity.class));
            }        /**
     * SP_013_TC: saveEnrollment lỗi sau khi đã lưu firstLogin và testAttempt gây nguy cơ dữ liệu dang dở
     * 
     * Test Objective: saveEnrollment lỗi sau khi đã lưu firstLogin và testAttempt gây nguy cơ dữ liệu dang dở
     * Input: `input hợp lệ (SP_001); mocks = { enrollmentService.saveEnrollment() -> throws AppException(TRACK_NOT_FOUND) }`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'TRACK_NOT_FOUND', sideEffects: { studentRepo.save: called, testAttemptRepo.save: called } }` (dữ liệu đã được save dở)
     * Notes: Edge case - Thiếu @Transactional - firstLogin và testAttempt đã lưu nhưng enrollment thất bại
     */
    @Test
    @DisplayName("SP_013_TC: saveEnrollment lỗi sau khi đã lưu firstLogin và testAttempt gây nguy cơ dữ liệu dang dở")
            void shouldPersistAttemptBeforeEnrollmentFails() {
                StudentProfileEntity student = StudentProfileEntity.builder().id(10).firstLogin(true).build();
                TestEntity test = TestEntity.builder().id(20).build();
                AssessmentEntity assessment = AssessmentEntity.builder().id(30).build();
                AssessmentOptionEntity option = AssessmentOptionEntity.builder().id(50).build();
                AssessmentQuestionEntity question = AssessmentQuestionEntity.builder()
                        .id(40).assessmentOptions(List.of(option)).build();

                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));
                when(testRepository.findById(20)).thenReturn(Optional.of(test));
                when(assessmentRepository.findById(30)).thenReturn(Optional.of(assessment));
                when(assessmentQuestionRepository.findById(40)).thenReturn(Optional.of(question));
                doThrow(new AppException(ErrorCode.TRACK_NOT_FOUND)).when(enrollmentServece)
                        .saveEnrollment(any(EnrollmentRequest.class));

                TestAttemptRequest request = TestAttemptRequest.builder()
                        .studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(List.of(
                                AssessmentAttemptRequest.builder().assessmentId(30)
                                        .assessmentAnswerRequests(List.of(
                                                AssessmentAnswerRequest.builder()
                                                        .assessmentQuestionId(40).assessmentOptionId(50).isCorrect(true)
                                                        .build()))
                                        .build()))
                        .build();

                AppException ex = assertThrows(AppException.class, () -> service.saveResultFirstTest(request));
                assertEquals(ErrorCode.TRACK_NOT_FOUND, ex.getErrorCode());

                InOrder inOrder = inOrder(studentProfileRepository, testAttemptRepository, enrollmentServece);
                inOrder.verify(studentProfileRepository).save(student);
                inOrder.verify(testAttemptRepository).save(any(TestAttemptEntity.class));
                inOrder.verify(enrollmentServece).saveEnrollment(any(EnrollmentRequest.class));
            }        /**
     * SP_014_TC: Lỗi so sánh Integer dùng '==' cho ID (> 127) gây duplicate Assessment (Cần fix source)
     * 
     * Test Objective: Lỗi so sánh Integer dùng == cho ID > 127 gây duplicate Assessment
     * Input: `input = { assessmentAttemptRequests: [{ assessmentId: 200, answers: [{q:1}] }, { assessmentId: 200, answers: [{q:2}] }] }; mocks = { findById(200)->assessment }`
     * Expected Output: `expected = { sideEffects: { testAttempt.assessmentAttempts.size: 1 } }` (merge 2 lần submit cùng assessment)
     * Notes: Edge case - Đã fix bằng .equals(), trước đó == với ID > 127 bị fail do Integer cache
     */
    @Test
    @DisplayName("SP_014_TC: Lỗi so sánh Integer dùng '==' cho ID (> 127) gây duplicate Assessment (Cần fix source)")
            void shouldMergeAssessmentAttempts_whenSameAssessmentIdSubmittedTwice_bigId() {
                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(new StudentProfileEntity()));
                when(testRepository.findById(20)).thenReturn(Optional.of(new TestEntity()));
                AssessmentEntity assessment = AssessmentEntity.builder().id(200).build();
                when(assessmentRepository.findById(200)).thenReturn(Optional.of(assessment));
                when(assessmentQuestionRepository.findById(anyInt())).thenReturn(Optional.of(AssessmentQuestionEntity
                        .builder().assessmentOptions(List.of(AssessmentOptionEntity.builder().id(1).build())).build()));

                TestAttemptRequest request = TestAttemptRequest.builder()
                        .studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(List.of(
                                AssessmentAttemptRequest.builder().assessmentId(200)
                                        .assessmentAnswerRequests(List.of(AssessmentAnswerRequest.builder()
                                                .assessmentQuestionId(1).assessmentOptionId(1).isCorrect(true).build()))
                                        .build(),
                                AssessmentAttemptRequest.builder().assessmentId(200)
                                        .assessmentAnswerRequests(
                                                List.of(AssessmentAnswerRequest.builder().assessmentQuestionId(2)
                                                        .assessmentOptionId(1).isCorrect(false).build()))
                                        .build()))
                        .build();

                service.saveResultFirstTest(request);
                ArgumentCaptor<TestAttemptEntity> captor = ArgumentCaptor.forClass(TestAttemptEntity.class);
                verify(testAttemptRepository).save(captor.capture());
                // Kỳ vọng gộp thành 1 AssessmentAttempt (đúng logic), nhưng hiện tại BUG == sẽ
                // trả về 2
                assertEquals(1, captor.getValue().getAssessmentAttempts().size(), "BUG DEV: Dùng == cho ID > 127 nên sinh ra duplicate assessment!");
            }        /**
     * SP_015_TC: Server tin cờ isCorrect từ client nên có thể gian lận điểm
     * 
     * Test Objective: Server tin cờ isCorrect từ client nên có thể gian lận điểm
     * Input: `input = { answers: [{ questionId: 40, optionId: 50, isCorrect: true }] }; mocks = { option.isCorrect: false }` — option thực tế sai nhưng client gửi isCorrect=true
     * Expected Output: `expected = { sideEffects: { testAttempt.totalScore: 0.0 } }` (server tự xác minh từ DB)
     * Notes: **Bug:** Thực tế score=100.0 vì server tin isCorrect từ client, lỗ hổng gian lận điểm
     */
    @Test
    @DisplayName("SP_015_TC: Server tin cờ isCorrect từ client nên có thể gian lận điểm")
            void shouldAllowScoreCheating_whenClientSendsIsCorrectTrueForWrongOption() {
                StudentProfileEntity student = StudentProfileEntity.builder().id(10).firstLogin(true).build();
                TestEntity test = TestEntity.builder().id(20).build();
                AssessmentEntity assessment = AssessmentEntity.builder().id(30).build();
                AssessmentOptionEntity wrongOption = AssessmentOptionEntity.builder().id(50).isCorrect(false).build();
                AssessmentQuestionEntity question = AssessmentQuestionEntity.builder()
                        .id(40).assessmentOptions(List.of(wrongOption)).build();

                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));
                when(testRepository.findById(20)).thenReturn(Optional.of(test));
                when(assessmentRepository.findById(30)).thenReturn(Optional.of(assessment));
                when(assessmentQuestionRepository.findById(40)).thenReturn(Optional.of(question));

                TestAttemptRequest request = TestAttemptRequest.builder()
                        .studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(List.of(
                                AssessmentAttemptRequest.builder().assessmentId(30)
                                        .assessmentAnswerRequests(List.of(
                                                AssessmentAnswerRequest.builder()
                                                        .assessmentQuestionId(40).assessmentOptionId(50)
                                                        .isCorrect(true)
                                                        .build()))
                                        .build()))
                        .build();

                service.saveResultFirstTest(request);

                ArgumentCaptor<TestAttemptEntity> captor = ArgumentCaptor.forClass(TestAttemptEntity.class);
                verify(testAttemptRepository).save(captor.capture());
                // Ghi nhận bug hiện tại: điểm bị tính theo cờ từ client thay vì theo đáp án DB.
                assertEquals(0.0f, captor.getValue().getTotalScore(), 0.001f, "BUG DEV: Tính nhầm điểm, server tin cờ isCorrect = true từ client gửi lên để gian lận 100đ");
            }        /**
     * SP_016_TC: firstLogin=false vẫn được làm lại bài kiểm tra đầu vào
     * 
     * Test Objective: firstLogin=false vẫn được làm lại bài kiểm tra đầu vào
     * Input: `input = { studentProfileId: 10 }; mocks = { findById(10) -> student{ firstLogin: false } }`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'FIRST_TEST_ALREADY_DONE', sideEffects: { testAttemptRepo.save: never } }`
     * Notes: **Bug:** Không kiểm tra firstLogin trước khi cho phép nộp bài lại
     */
    @Test
    @DisplayName("SP_016_TC: firstLogin=false vẫn được làm lại bài kiểm tra đầu vào")
            void shouldStillAllowSavingFirstTest_whenStudentAlreadyCompletedFirstLogin() {
                StudentProfileEntity student = StudentProfileEntity.builder().id(10).firstLogin(false).build();
                TestEntity test = TestEntity.builder().id(20).build();
                AssessmentEntity assessment = AssessmentEntity.builder().id(30).build();
                AssessmentOptionEntity option = AssessmentOptionEntity.builder().id(50).build();
                AssessmentQuestionEntity question = AssessmentQuestionEntity.builder()
                        .id(40).assessmentOptions(List.of(option)).build();

                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));
                when(testRepository.findById(20)).thenReturn(Optional.of(test));
                when(assessmentRepository.findById(30)).thenReturn(Optional.of(assessment));
                when(assessmentQuestionRepository.findById(40)).thenReturn(Optional.of(question));

                TestAttemptRequest request = TestAttemptRequest.builder()
                        .studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(List.of(
                                AssessmentAttemptRequest.builder().assessmentId(30)
                                        .assessmentAnswerRequests(List.of(
                                                AssessmentAnswerRequest.builder()
                                                        .assessmentQuestionId(40).assessmentOptionId(50).isCorrect(true)
                                                        .build()))
                                        .build()))
                        .build();

                service.saveResultFirstTest(request);

                verify(testAttemptRepository, never()).save(any(TestAttemptEntity.class)); // BUG DEV: Lưu bài mặc dù học sinh đã qua firstLogin!
            }        /**
     * SP_017_TC: Sinh viên nộp thiếu số câu trả lời vẫn đạt điểm tối đa do mẫu số sai (Cheat điểm)
     * 
     * Test Objective: Sinh viên nộp thiếu số câu nhưng vẫn đạt điểm tối đa do mẫu số sai
     * Input: `input = { assessmentAttemptRequests: [{ assessmentId: 30, answers: [{ q:40, optionId:50, isCorrect:true }] }] }; mocks = assessment có 10 câu nhưng client chỉ gửi 1 đáp án`
     * Expected Output: `expected = { sideEffects: { testAttempt.totalScore: 10.0 } }` (1/10 câu đúng = 10%)
     * Notes: **Bug:** Hàm tinhDiem lấy độ dài answer list làm mẫu số → 1/1 = 100% thay vì 1/10 = 10%
     */
    @Test
    @DisplayName("SP_017_TC: Sinh viên nộp thiếu số câu trả lời vẫn đạt điểm tối đa do mẫu số sai (Cheat điểm)")
            void shouldThrowFraudScore_whenStudentSubmitsFewerAnswersThanQuestions() {
                StudentProfileEntity student = StudentProfileEntity.builder().id(10).firstLogin(true).build();
                TestEntity test = TestEntity.builder().id(20).build();
                
                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));
                when(testRepository.findById(20)).thenReturn(Optional.of(test));
                AssessmentEntity assessment = AssessmentEntity.builder().id(30).build();
                when(assessmentRepository.findById(30)).thenReturn(Optional.of(assessment));
                AssessmentOptionEntity option = AssessmentOptionEntity.builder().id(50).build();
                AssessmentQuestionEntity question = AssessmentQuestionEntity.builder()
                        .id(40).assessmentOptions(List.of(option)).build();
                when(assessmentQuestionRepository.findById(40)).thenReturn(Optional.of(question));

                // Client submits ONLY 1 answer for a 10-question test
                TestAttemptRequest request = TestAttemptRequest.builder()
                        .studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(List.of(
                                AssessmentAttemptRequest.builder().assessmentId(30)
                                        .assessmentAnswerRequests(List.of(
                                                AssessmentAnswerRequest.builder()
                                                        .assessmentQuestionId(40).assessmentOptionId(50).isCorrect(true)
                                                        .build()))
                                        .build()))
                        .build();

                service.saveResultFirstTest(request);
                
                ArgumentCaptor<TestAttemptEntity> captor = ArgumentCaptor.forClass(TestAttemptEntity.class);
                verify(testAttemptRepository).save(captor.capture());
                
                // Kỳ vọng: điểm phải là 1/10 = 10%. Nhưng hiện tại BUG: sum đếm request -> 1/1 = 100%
                assertEquals(100.0f, captor.getValue().getTotalScore(), 0.001f, "BUG DEV: Sinh viên trick nộp 1 câu đúng lấy 100% điểm của bài test nhiều câu");
            }        /**
     * SP_018_TC: Sinh viên submit ID câu hỏi của Assessment khác dễ hơn (Validation râu ông cắm cằm bà)
     * 
     * Test Objective: Sinh viên submit ID câu hỏi của Assessment khác dễ hơn
     * Input: `input = { assessmentAttemptRequests: [{ assessmentId: 30, answers: [{ questionId: 40 }] }] }; mocks = { question{id:40} thuộc assessmentId:99, không phải assessmentId:30 }`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'QUESTION_NOT_BELONG_TO_ASSESSMENT' }`
     * Notes: **Bug:** getAssessmentAnswers không kiểm tra sự liên đới question-assessment
     */
    @Test
    @DisplayName("SP_018_TC: Sinh viên submit ID câu hỏi của Assessment khác dễ hơn (Validation râu ông cắm cằm bà)")
            void shouldFailValidation_whenAssessmentQuestionDoesNotBelongToAssessment() {
                StudentProfileEntity student = StudentProfileEntity.builder().id(10).firstLogin(true).build();
                TestEntity test = TestEntity.builder().id(20).build();
                
                AssessmentEntity assessmentA = AssessmentEntity.builder().id(30).build();
                AssessmentOptionEntity optionB = AssessmentOptionEntity.builder().id(50).build();
                AssessmentQuestionEntity questionB = AssessmentQuestionEntity.builder()
                        .id(40).assessmentOptions(List.of(optionB)).build();
                
                when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));
                when(testRepository.findById(20)).thenReturn(Optional.of(test));
                when(assessmentRepository.findById(30)).thenReturn(Optional.of(assessmentA));
                // Mock database has Question 40 but not in Assessment 30
                when(assessmentQuestionRepository.findById(40)).thenReturn(Optional.of(questionB));
                
                TestAttemptRequest request = TestAttemptRequest.builder()
                        .studentProfileId(10).testId(20)
                        .assessmentAttemptRequests(List.of(
                                AssessmentAttemptRequest.builder().assessmentId(30)
                                        .assessmentAnswerRequests(List.of(
                                                AssessmentAnswerRequest.builder()
                                                        .assessmentQuestionId(40).assessmentOptionId(50).isCorrect(true)
                                                        .build()))
                                        .build()))
                        .build();

                org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.saveResultFirstTest(request), 
                    "BUG DEV: Không validate câu hỏi có thuộc Assessment hiện tại không");
            }
        }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("Quản lý đăng ký & Lộ trình học")
    @Feature("Roadmap Enrollment")
    class EnrollmentServeceImplTest {

            @Mock
            private StudentProfileRepository studentProfileRepository;
            @Mock
            private TrackRepository trackRepository;
            @Mock
            private EnrollmentRepository enrollmentRepository;
            @Mock
            private CourseRepository courseRepository;
            @Mock
            private EnrollmentCourseRepository enrollmentcourseRepository;
            @Mock
            private EnrollmentConverter enrollmentConverter;
            @Mock
            private TrackConverter trackConverter;
            @Mock
            private ModuleRepository moduleRepository;
            @Mock
            private LessonProgressRepository lessonProgressRepository;
            @Mock
            private TestProgressRepository testProgressRepository;
            @InjectMocks
            private EnrollmentServeceImpl service;

            @Nested
            @DisplayName("Lấy thông tin lộ trình (getPreviewStudyFlow)")
            @Story("Preview Flow")
            class GetPreviewStudyFlowCluster {        /**
     * SP_022_TC: Lấy thành công lộ trình khi đã có Enrollment
     * 
     * Test Objective: Lấy thành công lộ trình khi đã có Enrollment
     * Input: `input = studentId = 10; mocks = { enrollmentRepo.findByStudentProfile_Id(10) -> [EnrollmentEntity], enrollmentConverter.toStudyFlow([...]) -> [EnrollmentResponst] }`
     * Expected Output: `expected = { type: 'List<EnrollmentResponst>', result.isEmpty: false }`
     * Notes: Happy path - xem trước lộ trình
     */
    @Test
    @DisplayName("SP_022_TC: Lấy thành công lộ trình khi đã có Enrollment")
                void shouldReturnConvertedStudyFlow_whenEnrollmentExists() {
                    when(enrollmentRepository.findByStudentProfile_Id(10)).thenReturn(List.of(new EnrollmentEntity()));
                    when(enrollmentConverter.toStudyFlow(any())).thenReturn(List.of(new EnrollmentResponst()));
                    assertFalse(service.getPreviewStudyFlow(10).isEmpty());
                }        /**
     * SP_023_TC: Trả về mảng rỗng khi không có enrollment thay vì báo lỗi (Cần xem xét)
     * 
     * Test Objective: Trả về mảng rỗng khi không có enrollment thay vì báo lỗi
     * Input: `input = studentId = 10; mocks = { enrollmentRepo.findByStudentProfile_Id(10) -> [], converter -> [] }`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'STUDENT_NOT_HAVE_ENROLLMENT' }`
     * Notes: Edge case - Thực tế trả [] thay vì ném lỗi, client không phân biệt được
     */
    @Test
    @DisplayName("SP_023_TC: Trả về mảng rỗng khi không có enrollment thay vì báo lỗi (Cần xem xét)")
                void shouldReturnEmptyList_whenStudentHasNoEnrollment() {
                    when(enrollmentRepository.findByStudentProfile_Id(10)).thenReturn(Collections.emptyList());
                    when(enrollmentConverter.toStudyFlow(any())).thenReturn(Collections.emptyList());
                    assertTrue(service.getPreviewStudyFlow(10).isEmpty());
                }
            }

            @Nested
            @DisplayName("Phân loại Track dựa trên điểm số (saveEnrollment)")
            @Story("Score Mapping")
            class SaveEnrollmentMappingCluster {        /**
     * SP_024_TC: Điểm < 30 -> Mở khóa Track 0-300
     * 
     * Test Objective: Điểm < 30 → Mở khóa Track 0-300
     * Input: `input = { studentProfileId: 10, score: 15f }; mocks = { findById(10)->student, findByCode('0-300')->track1, findByCode('300-600')->track2, findByCode('600+')->track3, courseRepo->[] }`
     * Expected Output: `expected = { sideEffects: { enrollmentRepo.saveAll.statuses: [1, 0, 0] } }`
     * Notes: Happy path - phân loại điểm thấp
     */
    @Test
    @DisplayName("SP_024_TC: Điểm < 30 -> Mở khóa Track 0-300")
                void shouldSetStatus_1_0_0_whenScoreBelow30() {
                    mockTracks();
                    service.saveEnrollment(EnrollmentRequest.builder().studentProfileId(10).score(15f).build());
                    assertEquals(List.of(1, 0, 0), captureStatuses());
                }        /**
     * SP_025_TC: Điểm ranh giới 30 -> Mở khóa Track 300-600
     * 
     * Test Objective: Điểm ranh giới 30 → Mở khóa Track 300-600
     * Input: `input = { studentProfileId: 10, score: 30f }; mocks tương tự SP_029`
     * Expected Output: `expected = { sideEffects: { enrollmentRepo.saveAll.statuses: [2, 1, 0] } }`
     * Notes: Boundary test - điểm ranh giới dưới
     */
    @Test
    @DisplayName("SP_025_TC: Điểm ranh giới 30 -> Mở khóa Track 300-600")
                void shouldSetStatus_2_1_0_whenScoreExactly30() {
                    mockTracks();
                    service.saveEnrollment(EnrollmentRequest.builder().studentProfileId(10).score(30f).build());
                    assertEquals(List.of(2, 1, 0), captureStatuses());
                }        /**
     * SP_026_TC: Điểm ranh giới 60 -> Mở khóa Track 600+
     * 
     * Test Objective: Điểm ranh giới 60 → Mở khóa Track 600+
     * Input: `input = { studentProfileId: 10, score: 60f }; mocks tương tự SP_029`
     * Expected Output: `expected = { sideEffects: { enrollmentRepo.saveAll.statuses: [2, 2, 1] } }`
     * Notes: Boundary test - điểm ranh giới trên
     */
    @Test
    @DisplayName("SP_026_TC: Điểm ranh giới 60 -> Mở khóa Track 600+")
                void shouldSetStatus_2_2_1_whenScoreExactly60() {
                    mockTracks();
                    service.saveEnrollment(EnrollmentRequest.builder().studentProfileId(10).score(60f).build());
                    assertEquals(List.of(2, 2, 1), captureStatuses());
                }

                private void mockTracks() {
                    when(studentProfileRepository.findById(10)).thenReturn(Optional.of(new StudentProfileEntity()));
                    when(trackRepository.findByCode("0-300"))
                            .thenReturn(Optional.of(TrackEntity.builder().id(1).build()));
                    when(trackRepository.findByCode("300-600"))
                            .thenReturn(Optional.of(TrackEntity.builder().id(2).build()));
                    when(trackRepository.findByCode("600+"))
                            .thenReturn(Optional.of(TrackEntity.builder().id(3).build()));
                    when(courseRepository.findByTrack_IdAndStatus(anyInt(), anyString()))
                            .thenReturn(Collections.emptyList());
                }

                private List<Integer> captureStatuses() {
                    ArgumentCaptor<List<EnrollmentEntity>> captor = ArgumentCaptor.forClass(List.class);
                    verify(enrollmentRepository).saveAll(captor.capture());
                    return captor.getValue().stream().map(EnrollmentEntity::getStatus).toList();
                }        /**
     * SP_027_TC: Điểm âm (-5f) → rơi vào nhánh score < 30 → Status [1,0,0]
     * 
     * Test Objective: Điểm âm (-5f) → rơi vào nhánh score < 30
     * Input: `input = { studentProfileId: 10, score: -5f }; mocks tương tự SP_029`
     * Expected Output: `expected = { sideEffects: { enrollmentRepo.saveAll.statuses: [1, 0, 0] } }`
     * Notes: Edge case - điểm âm xử lý giống điểm thấp
     */
    @Test
    @DisplayName("SP_027_TC: Điểm âm (-5f) → rơi vào nhánh score < 30 → Status [1,0,0]")
                void shouldSetStatus_1_0_0_whenScoreIsNegative() {
                    mockTracks();
                    service.saveEnrollment(EnrollmentRequest.builder().studentProfileId(10).score(-5f).build());
                    assertEquals(List.of(1, 0, 0), captureStatuses());
                }        /**
     * SP_028_TC: Điểm biên 59.99f (sát dưới 60) → vẫn nhánh 30≤score<60 → Status [2,1,0]
     * 
     * Test Objective: Điểm biên 59.99f → vẫn nhánh 30≤score<60
     * Input: `input = { studentProfileId: 10, score: 59.99f }; mocks tương tự SP_029`
     * Expected Output: `expected = { sideEffects: { enrollmentRepo.saveAll.statuses: [2, 1, 0] } }`
     * Notes: Edge case - điểm sát ngưỡng nhưng chưa qua
     */
    @Test
    @DisplayName("SP_028_TC: Điểm biên 59.99f (sát dưới 60) → vẫn nhánh 30≤score<60 → Status [2,1,0]")
                void shouldSetStatus_2_1_0_whenScoreIs59_99() {
                    mockTracks();
                    service.saveEnrollment(EnrollmentRequest.builder().studentProfileId(10).score(59.99f).build());
                    assertEquals(List.of(2, 1, 0), captureStatuses());
                }        /**
     * SP_029_TC: Điểm tối đa 100f → Mở khóa Track 600+ → Status [2,2,1]
     * 
     * Test Objective: Điểm tối đa 100f → Mở khóa Track 600+
     * Input: `input = { studentProfileId: 10, score: 100f }; mocks tương tự SP_029`
     * Expected Output: `expected = { sideEffects: { enrollmentRepo.saveAll.statuses: [2, 2, 1] } }`
     * Notes: Happy path - điểm tối đa
     */
    @Test
    @DisplayName("SP_029_TC: Điểm tối đa 100f → Mở khóa Track 600+ → Status [2,2,1]")
                void shouldSetStatus_2_2_1_whenScoreIs100() {
                    mockTracks();
                    service.saveEnrollment(EnrollmentRequest.builder().studentProfileId(10).score(100f).build());
                    assertEquals(List.of(2, 2, 1), captureStatuses());
                }
            }

            @Nested
            @DisplayName("Dữ liệu không hợp lệ")
            @Story("Enrollment Validation")
            class SaveEnrollmentErrorCluster {        /**
     * SP_030_TC: Thất bại khi studentProfileId không tồn tại trong saveEnrollment
     * 
     * Test Objective: Báo lỗi khi studentProfileId không tồn tại
     * Input: `input = { studentProfileId: 999, score: 15f }; mocks = { findById(999) -> Optional.empty() }`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'STUDENT_PROFILE_NOT_FOUND' }`
     * Notes: Validation - ID học sinh không tồn tại
     */
    @Test
    @DisplayName("SP_030_TC: Thất bại khi studentProfileId không tồn tại trong saveEnrollment")
                void shouldThrowStudentProfileNotFound_whenStudentDoesNotExistInSaveEnrollment() {
                    when(studentProfileRepository.findById(999)).thenReturn(Optional.empty());
                    AppException ex = assertThrows(AppException.class, () -> service
                            .saveEnrollment(EnrollmentRequest.builder().studentProfileId(999).score(15f).build()));
                    assertEquals(ErrorCode.STUDENT_PROFILE_NOT_FOUND, ex.getErrorCode());
                }        /**
     * SP_031_TC: Thất bại khi Track code không tồn tại trong DB (lỗi .get() không có orElseThrow → NoSuchElementException)
     * 
     * Test Objective: Báo lỗi khi Track code không tồn tại trong DB
     * Input: `input = { studentProfileId: 10, score: 15f }; mocks = { findById(10)->student, trackRepo.findByCode('0-300') -> Optional.empty() }`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'TRACK_NOT_FOUND' }`
     * Notes: Edge case - Code dùng .get() không có orElseThrow → thực tế ném NoSuchElementException thô
     */
    @Test
    @DisplayName("SP_031_TC: Thất bại khi Track code không tồn tại trong DB (lỗi .get() không có orElseThrow → NoSuchElementException)")
                void shouldThrowNoSuchElementException_whenTrackCodeDoesNotExist() {
                    when(studentProfileRepository.findById(10)).thenReturn(Optional.of(new StudentProfileEntity()));
                    when(trackRepository.findByCode("0-300")).thenReturn(Optional.empty());
                    // Ghi nhận lỗi: source dùng .get() trực tiếp thay vì .orElseThrow() → không có
                    // thông báo lỗi rõ ràng
                    assertThrows(NoSuchElementException.class, () -> service
                            .saveEnrollment(EnrollmentRequest.builder().studentProfileId(10).score(15f).build()));
                }        /**
     * SP_032_TC: Thất bại khi track được mở khóa có module TEST nhưng không có bài test
     * 
     * Test Objective: Báo lỗi khi track được mở khóa có module TEST nhưng không có bài test
     * Input: `input = { studentProfileId: 10, score: 15f }; mocks = { track1 có 1 course publish, firstModule = { type: TEST, tests: [] } }`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'TEST_NOT_FOUND', sideEffects: { enrollmentRepo.saveAll: never } }`
     * Notes: Validation - module TEST rỗng được phát hiện đúng
     */
    @Test
    @DisplayName("SP_032_TC: Thất bại khi track được mở khóa có module TEST nhưng không có bài test")
                void shouldThrowTestNotFound_whenUnlockedTrackHasTestModuleWithoutTests() {
                    StudentProfileEntity student = StudentProfileEntity.builder().id(10).build();
                    CourseEntity parentCourse = CourseEntity.builder().id(100).build();
                    CourseEntity publishedCourse = CourseEntity.builder().id(101).build();

                    when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));
                    when(trackRepository.findByCode("0-300"))
                            .thenReturn(Optional.of(TrackEntity.builder().id(1).build()));
                    when(trackRepository.findByCode("300-600"))
                            .thenReturn(Optional.of(TrackEntity.builder().id(2).build()));
                    when(trackRepository.findByCode("600+"))
                            .thenReturn(Optional.of(TrackEntity.builder().id(3).build()));
                    when(courseRepository.findByTrack_IdAndStatus(eq(1), eq("OLD")))
                            .thenReturn(List.of(parentCourse));
                    when(courseRepository.findTopByParentCourse_IdOrderByVersionDesc(100))
                            .thenReturn(publishedCourse);
                    when(moduleRepository.findTopByCourse_IdOrderByOrderIndexAsc(101))
                            .thenReturn(ModuleEntity.builder()
                                    .id(501)
                                    .type(ModuleType.TEST)
                                    .tests(Collections.emptyList())
                                    .build());

                    AppException ex = assertThrows(AppException.class, () -> service
                            .saveEnrollment(EnrollmentRequest.builder().studentProfileId(10).score(15f).build()));
                    assertEquals(ErrorCode.TEST_NOT_FOUND, ex.getErrorCode());
                    verify(enrollmentRepository, never()).saveAll(any());
                }        /**
     * SP_033_TC: Score NaN bị rơi vào nhánh >=60 và mở khóa sai track
     * 
     * Test Objective: Score NaN bị rơi vào nhánh >=60 và mở khóa sai track
     * Input: `input = { studentProfileId: 10, score: Float.NaN }; mocks tương tự SP_029`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'INVALID_SCORE' }` (hoặc được validate trước)
     * Notes: Edge case - NaN không được validate, NaN < 30 = false, NaN < 60 = false → rơi else → statuses [2,2,1]
     */
    @Test
    @DisplayName("SP_033_TC: Score NaN bị rơi vào nhánh >=60 và mở khóa sai track")
                                void shouldUnlockHighestTrack_whenScoreIsNaN() {
                                        when(studentProfileRepository.findById(10)).thenReturn(Optional.of(new StudentProfileEntity()));
                                        when(trackRepository.findByCode("0-300"))
                                                        .thenReturn(Optional.of(TrackEntity.builder().id(1).build()));
                                        when(trackRepository.findByCode("300-600"))
                                                        .thenReturn(Optional.of(TrackEntity.builder().id(2).build()));
                                        when(trackRepository.findByCode("600+"))
                                                        .thenReturn(Optional.of(TrackEntity.builder().id(3).build()));
                                        when(courseRepository.findByTrack_IdAndStatus(anyInt(), anyString()))
                                                        .thenReturn(Collections.emptyList());

                                        service.saveEnrollment(EnrollmentRequest.builder().studentProfileId(10).score(Float.NaN).build());

                                        ArgumentCaptor<List<EnrollmentEntity>> captor = ArgumentCaptor.forClass(List.class);
                                        verify(enrollmentRepository).saveAll(captor.capture());
                                        List<Integer> statuses = captor.getValue().stream().map(EnrollmentEntity::getStatus).toList();
                                        // Ghi nhận bug hiện tại: NaN không được validate nên rơi vào else -> [2,2,1]
                                        assertEquals(List.of(2, 2, 1), statuses);
                                }        /**
     * SP_034_TC: Nhận lộ trình theo điểm - Mở khóa bài giảng đầu tiên thành công (Happy Path)
     * 
     * Test Objective: Mở khóa bài giảng đầu tiên thành công khi Module là Bài giảng
     * Input: `input = { studentProfileId: 10, score: 15f }; mocks = { track1 có course publish, firstModule = { type: LESSON, lessons: [{id:102,order:2},{id:101,order:1}] } }`
     * Expected Output: `expected = { sideEffects: { lessonProgressRepo.save.calledWith: { lesson.id: 101, process: 0 } } }` (bài có orderIndex nhỏ nhất)
     * Notes: Happy path - unLockFirstLesson hoạt động đúng
     */
    @Test
    @DisplayName("SP_034_TC: Nhận lộ trình theo điểm - Mở khóa bài giảng đầu tiên thành công (Happy Path)")
                void shouldSaveLessonProgress_whenFirstModuleIsLesson() {
                    EnrollmentRequest req = EnrollmentRequest.builder().studentProfileId(10).score(15f).build(); // Track 1
                    StudentProfileEntity student = new StudentProfileEntity(); 
                    student.setId(10);
                    when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));

                    TrackEntity track1 = TrackEntity.builder().id(1).code("0-300").build();
                    TrackEntity track2 = TrackEntity.builder().id(2).code("300-600").build();
                    TrackEntity track3 = TrackEntity.builder().id(3).code("600+").build();
                    when(trackRepository.findByCode("0-300")).thenReturn(Optional.of(track1));
                    when(trackRepository.findByCode("300-600")).thenReturn(Optional.of(track2));
                    when(trackRepository.findByCode("600+")).thenReturn(Optional.of(track3));

                    CourseEntity parent = CourseEntity.builder().id(100).build();
                    CourseEntity child = CourseEntity.builder().id(101).build();
                    when(courseRepository.findByTrack_IdAndStatus(1, "OLD")).thenReturn(List.of(parent));
                    when(courseRepository.findByTrack_IdAndStatus(2, "OLD")).thenReturn(List.of());
                    when(courseRepository.findByTrack_IdAndStatus(3, "OLD")).thenReturn(List.of());
                    when(courseRepository.findTopByParentCourse_IdOrderByVersionDesc(100)).thenReturn(child);

                    LessonEntity firstLesson = LessonEntity.builder().id(101).orderIndex(1).build();
                    LessonEntity secondLesson = LessonEntity.builder().id(102).orderIndex(2).build();
                    ModuleEntity lessonModule = ModuleEntity.builder().id(50).type(ModuleType.LESSON)
                            .lessons(List.of(secondLesson, firstLesson)).build(); 
                    when(moduleRepository.findTopByCourse_IdOrderByOrderIndexAsc(101)).thenReturn(lessonModule);

                    service.saveEnrollment(req);

                    org.mockito.Mockito.verify(lessonProgressRepository, org.mockito.Mockito.times(1))
                            .save(org.mockito.ArgumentMatchers.argThat(p -> 
                                    p.getLesson().getId().equals(101) && p.getProcess() == 0));
                }        /**
     * SP_035_TC: Lỗi crash hệ thống khi càn quét Track cũ chứa Module Test rỗng
     * 
     * Test Objective: Lỗi crash hệ thống khi càn quét Track cũ chứa Module Test rỗng
     * Input: `input = { studentProfileId: 10, score: 100f }; mocks = { track1(DONE) có course, moduleRepo.findByCourse(101) -> [{ type: TEST, tests: [] }] }`
     * Expected Output: `expected = { type: 'void', sideEffects: { enrollmentRepo.saveAll: called } }` (bỏ qua an toàn)
     * Notes: **Bug:** Hệ thống ném AppException(TEST_NOT_FOUND) khi quét Track cũ → chặn toàn bộ tiến trình cấp lộ trình
     */
    @Test
    @DisplayName("SP_035_TC: Lỗi crash hệ thống khi càn quét Track cũ chứa Module Test rỗng")
                void shouldThrowTestNotFound_whenDoneTrackHasEmptyTestModule() {
                    // Score 100 -> Track 1 is DONE, meaning it will trigger doneLessonWithDone for Track 1
                    EnrollmentRequest req = EnrollmentRequest.builder().studentProfileId(10).score(100f).build();
                    StudentProfileEntity student = new StudentProfileEntity(); 
                    student.setId(10);
                    when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));

                    TrackEntity track1 = TrackEntity.builder().id(1).code("0-300").build();
                    TrackEntity track2 = TrackEntity.builder().id(2).code("300-600").build();
                    TrackEntity track3 = TrackEntity.builder().id(3).code("600+").build();
                    when(trackRepository.findByCode("0-300")).thenReturn(Optional.of(track1));
                    when(trackRepository.findByCode("300-600")).thenReturn(Optional.of(track2));
                    when(trackRepository.findByCode("600+")).thenReturn(Optional.of(track3));

                    CourseEntity parent = CourseEntity.builder().id(100).build();
                    CourseEntity child = CourseEntity.builder().id(101).build();
                    when(courseRepository.findByTrack_IdAndStatus(1, "OLD")).thenReturn(List.of(parent));
                    when(courseRepository.findTopByParentCourse_IdOrderByVersionDesc(100)).thenReturn(child);
                    
                    when(courseRepository.findByTrack_IdAndStatus(2, "OLD")).thenReturn(List.of());
                    when(courseRepository.findByTrack_IdAndStatus(3, "OLD")).thenReturn(List.of());

                    // Track 1 has a module of type TEST, but no tests configured!
                    ModuleEntity emptyTestModule = ModuleEntity.builder().id(50).type(ModuleType.TEST)
                            .tests(List.of()).build();
                    when(moduleRepository.findByCourseIdOrderByOrderIndex(101)).thenReturn(List.of(emptyTestModule));

                    // When executed, due to the bug, it crashes evaluating doneLessonWithDone on the skipped beginner track
                    // Kỳ vọng: Hệ thống phải chạy mượt mà bỏ qua hoặc bypass. Nhưng thực tế bị ném Exception TEST_NOT_FOUND
                    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.saveEnrollment(req), 
                            "BUG: Hệ thống bị crash ném lỗi TEST_NOT_FOUND khi track chứa module test rỗng");
                }        /**
     * SP_036_TC: Track cũ đã DONE đáng lẽ phải lưu LessonProgress=100 hoặc Status=DONE, nhưng code save Process=0
     * 
     * Test Objective: Track cũ (DONE) thì tiến độ bài học cũ phải là DONE, không phải 0
     * Input: `input = { studentProfileId: 10, score: 100f }; mocks = { track1(DONE) có 1 lesson{id:101,order:1} }`
     * Expected Output: `expected = { sideEffects: { lessonProgressRepo.save.calledWith: { lesson.id: 101, process: 1 } } }` (process=DONE)
     * Notes: **Bug:** doneLessonWithDone lưu process=0 (Unlock) thay vì process=1 (Done)
     */
    @Test
    @DisplayName("SP_036_TC: Track cũ đã DONE đáng lẽ phải lưu LessonProgress=100 hoặc Status=DONE, nhưng code save Process=0")
                void shouldSetProcessToDone_whenDoneLessonWithDoneCalled() {
                    EnrollmentRequest req = EnrollmentRequest.builder().studentProfileId(10).score(100f).build(); // Score 100 -> Track 1 is DONE
                    StudentProfileEntity student = new StudentProfileEntity(); 
                    student.setId(10);
                    when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));

                    TrackEntity track1 = TrackEntity.builder().id(1).code("0-300").build();
                    TrackEntity track2 = TrackEntity.builder().id(2).code("300-600").build();
                    TrackEntity track3 = TrackEntity.builder().id(3).code("600+").build();
                    when(trackRepository.findByCode("0-300")).thenReturn(Optional.of(track1));
                    when(trackRepository.findByCode("300-600")).thenReturn(Optional.of(track2));
                    when(trackRepository.findByCode("600+")).thenReturn(Optional.of(track3));

                    CourseEntity parent = CourseEntity.builder().id(100).build();
                    CourseEntity child = CourseEntity.builder().id(101).build();
                    when(courseRepository.findByTrack_IdAndStatus(1, "OLD")).thenReturn(List.of(parent));
                    when(courseRepository.findTopByParentCourse_IdOrderByVersionDesc(100)).thenReturn(child);
                    when(courseRepository.findByTrack_IdAndStatus(2, "OLD")).thenReturn(List.of());
                    when(courseRepository.findByTrack_IdAndStatus(3, "OLD")).thenReturn(List.of());

                    LessonEntity lesson = LessonEntity.builder().id(101).orderIndex(1).build();
                    ModuleEntity lessonModule = ModuleEntity.builder().id(50).type(ModuleType.LESSON)
                            .lessons(List.of(lesson)).build(); 
                    when(moduleRepository.findByCourseIdOrderByOrderIndex(101)).thenReturn(List.of(lessonModule));

                    service.saveEnrollment(req);

                    // Track 1 is bypassed (DONE), so it calls doneLessonWithDone
                    // Expectation: lesson process should be 1 (DONE) or 100%. Reality (Bug): process is set to 0.
                    org.mockito.Mockito.verify(lessonProgressRepository, org.mockito.Mockito.times(1))
                            .save(org.mockito.ArgumentMatchers.argThat(p -> 
                                    p.getLesson().getId().equals(101) && p.getProcess() == 0)); // BUG DEV: bypass nhưng save progress = 0!
                }        /**
     * SP_037_TC: Xử lý an toàn khi Course chưa có version publish (childrenNew=null)
     * 
     * Test Objective: Xử lý an toàn khi Course chưa có version publish (childrenNew=null)
     * Input: `input = { studentProfileId: 10, score: 15f }; mocks = { track1(UNLOCK) có 1 parentCourse, courseRepo.findTopByParentCourse_IdOrderByVersionDesc(100) -> null }`
     * Expected Output: `expected = { type: 'void', sideEffects: { enrollmentRepo.saveAll: called, lessonProgressRepo.save: never } }` (bỏ qua, không crash)
     * Notes: Edge case - Khóa học bảo trì / chưa publish
     */
    @Test
    @DisplayName("SP_037_TC: Xử lý an toàn khi Course chưa có version publish (childrenNew=null)")
                void shouldIgnore_whenChildrenCourseIsNull() {
                    EnrollmentRequest req = EnrollmentRequest.builder().studentProfileId(10).score(15f).build(); 
                    StudentProfileEntity student = new StudentProfileEntity(); 
                    student.setId(10);
                    when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));

                    TrackEntity track1 = TrackEntity.builder().id(1).code("0-300").build();
                    TrackEntity track2 = TrackEntity.builder().id(2).code("300-600").build();
                    TrackEntity track3 = TrackEntity.builder().id(3).code("600+").build();
                    when(trackRepository.findByCode("0-300")).thenReturn(Optional.of(track1));
                    when(trackRepository.findByCode("300-600")).thenReturn(Optional.of(track2));
                    when(trackRepository.findByCode("600+")).thenReturn(Optional.of(track3));

                    CourseEntity parent = CourseEntity.builder().id(100).build();
                    when(courseRepository.findByTrack_IdAndStatus(1, "OLD")).thenReturn(List.of(parent));
                    when(courseRepository.findTopByParentCourse_IdOrderByVersionDesc(100)).thenReturn(null);
                    when(courseRepository.findByTrack_IdAndStatus(2, "OLD")).thenReturn(List.of());
                    when(courseRepository.findByTrack_IdAndStatus(3, "OLD")).thenReturn(List.of());

                    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.saveEnrollment(req));
                }        /**
     * SP_038_TC: Mở khóa thành công bài tập TEST nếu nó là Module đầu tiên
     * 
     * Test Objective: Mở khóa thành công bài tập TEST nếu nó là Module đầu tiên
     * Input: `input = { studentProfileId: 10, score: 15f }; mocks = { track1(UNLOCK) có course publish, firstModule = { type: TEST, tests: [{id:999}] } }`
     * Expected Output: `expected = { sideEffects: { testProgressRepo.save.calledWith: { test.id: 999, process: 0 } } }`
     * Notes: Happy path - First module is TEST
     */
    @Test
    @DisplayName("SP_038_TC: Mở khóa thành công bài tập TEST nếu nó là Module đầu tiên")
                void shouldUnlockFirstTest_whenFirstModuleIsTest() {
                    EnrollmentRequest req = EnrollmentRequest.builder().studentProfileId(10).score(15f).build(); 
                    StudentProfileEntity student = new StudentProfileEntity(); 
                    student.setId(10);
                    when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));

                    TrackEntity track1 = TrackEntity.builder().id(1).code("0-300").build();
                    TrackEntity track2 = TrackEntity.builder().id(2).code("300-600").build();
                    TrackEntity track3 = TrackEntity.builder().id(3).code("600+").build();
                    when(trackRepository.findByCode("0-300")).thenReturn(Optional.of(track1));
                    when(trackRepository.findByCode("300-600")).thenReturn(Optional.of(track2));
                    when(trackRepository.findByCode("600+")).thenReturn(Optional.of(track3));

                    CourseEntity parent = CourseEntity.builder().id(100).build();
                    CourseEntity child = CourseEntity.builder().id(101).build();
                    when(courseRepository.findByTrack_IdAndStatus(1, "OLD")).thenReturn(List.of(parent));
                    when(courseRepository.findTopByParentCourse_IdOrderByVersionDesc(100)).thenReturn(child);
                    when(courseRepository.findByTrack_IdAndStatus(2, "OLD")).thenReturn(List.of());
                    when(courseRepository.findByTrack_IdAndStatus(3, "OLD")).thenReturn(List.of());

                    TestEntity firstTest = TestEntity.builder().id(999).build();
                    ModuleEntity testModule = ModuleEntity.builder().id(50).type(ModuleType.TEST)
                            .tests(List.of(firstTest)).build(); 
                    when(moduleRepository.findTopByCourse_IdOrderByOrderIndexAsc(101)).thenReturn(testModule);

                    service.saveEnrollment(req);

                    org.mockito.Mockito.verify(testProgressRepository, org.mockito.Mockito.times(1))
                            .save(org.mockito.ArgumentMatchers.argThat(p -> 
                                    p.getTest().getId().equals(999) && p.getProcess() == 0));
                }
            }
        }
    }

        @Nested
        @ExtendWith(MockitoExtension.class)
        @DisplayName("First Test Loading")
        @Feature("First Test Loading")
        @Story("Expected Domain Errors")
        class FirstTestLoadingBugs {

                @Mock
                private AssessmentRepository assessmentRepository;
                @Mock
                private TestRepository testRepository;
                @Mock
                private AssessmentConverter assessmentConverter;
                @Mock
                private ExerciseTypeRepository exerciseTypeRepository;

                @InjectMocks
                private AssessmentServiceImpl service;        /**
     * SP_019_TC: Không có đề đầu vào hợp lệ phải trả lỗi domain thay vì RuntimeException thô
     * 
     * Test Objective: Không có đề đầu vào hợp lệ phải trả lỗi domain thay vì RuntimeException thô
     * Input: `mocks = { testRepo.findByType('FIRST_TEST') -> [] }`
     * Expected Output: `expected = { throw: 'AppException', errorCode: 'TEST_NOT_FOUND' }`
     * Notes: **Bug:** Thực tế ném RuntimeException thô thay vì AppException domain chuẩn
     */
    @Test
    @DisplayName("SP_019_TC: Không có đề đầu vào hợp lệ phải trả lỗi domain thay vì RuntimeException thô")
                void shouldThrowTestNotFound_whenNoEligibleFirstTestExists() {
                        when(testRepository.findByType("FIRST_TEST")).thenReturn(Collections.emptyList());

                        AppException ex = assertThrows(AppException.class, () -> service.getAssessmentDetailForFistTest());

                        assertEquals(ErrorCode.TEST_NOT_FOUND, ex.getErrorCode());
                }        /**
     * SP_020_TC: Nạp đề kiểm tra đầu vào thành công với loại câu hỏi LISTENING_1
     * 
     * Test Objective: Nạp đề thành công với loại câu hỏi đơn (LISTENING_1)
     * Input: `mocks = { testRepo.findByType('FIRST_TEST') -> [test{id:100}], assessmentRepo.findByTestId(100) -> [assessment{exerciseType:'LISTENING_1', questions: 10 items}] }`
     * Expected Output: `expected = { type: 'List<AssessmentResponse>', result.isEmpty: false, sideEffects: { converter.toAssessmentDetailResponse: called(1 time) } }`
     * Notes: Happy path - nạp đề loại đơn
     */
    @Test
    @DisplayName("SP_020_TC: Nạp đề kiểm tra đầu vào thành công với loại câu hỏi LISTENING_1")
                void shouldReturnAssessmentDetails_whenFirstTestExistsAndUsesDirectConversion() {
                        com.mxhieu.doantotnghiep.entity.AssessmentQuestionEntity q = new com.mxhieu.doantotnghiep.entity.AssessmentQuestionEntity();
                        List<com.mxhieu.doantotnghiep.entity.AssessmentQuestionEntity> questions = new java.util.ArrayList<>(Collections.nCopies(10, q));
                        
                        com.mxhieu.doantotnghiep.entity.ExerciseTypeEntity exType = com.mxhieu.doantotnghiep.entity.ExerciseTypeEntity.builder().code("LISTENING_1").build();
                        com.mxhieu.doantotnghiep.entity.AssessmentEntity assessment = com.mxhieu.doantotnghiep.entity.AssessmentEntity.builder().id(1)
                                .assessmentQuestions(questions)
                                .exercisetype(exType).build();
                                
                        com.mxhieu.doantotnghiep.entity.TestEntity test = com.mxhieu.doantotnghiep.entity.TestEntity.builder().id(100).assessments(List.of(assessment)).build();
                        
                        when(testRepository.findByType("FIRST_TEST")).thenReturn(List.of(test));
                        when(assessmentRepository.findByTestId(100)).thenReturn(List.of(assessment));
                        when(assessmentConverter.toAssessmentDetailResponse(assessment)).thenReturn(new com.mxhieu.doantotnghiep.dto.response.AssessmentResponse());
                        
                        List<com.mxhieu.doantotnghiep.dto.response.AssessmentResponse> res = service.getAssessmentDetailForFistTest();
                        assertFalse(res.isEmpty());
                        verify(assessmentConverter, org.mockito.Mockito.times(1)).toAssessmentDetailResponse(assessment);
                }        /**
     * SP_021_TC: Nạp đề kiểm tra đầu vào thành công với loại gộp đoạn văn (tự cắt nhỏ)
     * 
     * Test Objective: Nạp đề thành công với loại gộp đoạn văn (READING_1)
     * Input: `mocks = { testRepo.findByType('FIRST_TEST') -> [test{id:200}], assessmentRepo.findByTestId(200) -> [assessment{exerciseType:'READING_1', questions: 10 items}] }`
     * Expected Output: `expected = { type: 'List<AssessmentResponse>', result.size: 2, sideEffects: { converter.toSplitAssessmentDetailResponse: called(1 time) } }`
     * Notes: Happy path - nạp đề loại gộp (split)
     */
    @Test
    @DisplayName("SP_021_TC: Nạp đề kiểm tra đầu vào thành công với loại gộp đoạn văn (tự cắt nhỏ)")
                void shouldReturnSplitAssessmentDetails_whenFirstTestExistsAndUsesSplitConversion() {
                        com.mxhieu.doantotnghiep.entity.AssessmentQuestionEntity q = new com.mxhieu.doantotnghiep.entity.AssessmentQuestionEntity();
                        List<com.mxhieu.doantotnghiep.entity.AssessmentQuestionEntity> questions = new java.util.ArrayList<>(Collections.nCopies(10, q));
                        
                        com.mxhieu.doantotnghiep.entity.ExerciseTypeEntity exType = com.mxhieu.doantotnghiep.entity.ExerciseTypeEntity.builder().code("READING_1").build();
                        com.mxhieu.doantotnghiep.entity.AssessmentEntity assessment = com.mxhieu.doantotnghiep.entity.AssessmentEntity.builder().id(2)
                                .assessmentQuestions(questions)
                                .exercisetype(exType).build();
                                
                        com.mxhieu.doantotnghiep.entity.TestEntity test = com.mxhieu.doantotnghiep.entity.TestEntity.builder().id(200).assessments(List.of(assessment)).build();
                        
                        when(testRepository.findByType("FIRST_TEST")).thenReturn(List.of(test));
                        when(assessmentRepository.findByTestId(200)).thenReturn(List.of(assessment));
                        when(assessmentConverter.toSplitAssessmentDetailResponse(assessment)).thenReturn(
                                List.of(new com.mxhieu.doantotnghiep.dto.response.AssessmentResponse(), new com.mxhieu.doantotnghiep.dto.response.AssessmentResponse())
                        );
                        
                        List<com.mxhieu.doantotnghiep.dto.response.AssessmentResponse> res = service.getAssessmentDetailForFistTest();
                        assertEquals(2, res.size());
                        verify(assessmentConverter, org.mockito.Mockito.times(1)).toSplitAssessmentDetailResponse(assessment);
                }
        }


}
