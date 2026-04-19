package com.mxhieu.doantotnghiep.service.impl;

import com.mxhieu.doantotnghiep.converter.TestAttemptConverter;
import com.mxhieu.doantotnghiep.dto.request.AssessmentAnswerRequest;
import com.mxhieu.doantotnghiep.dto.request.AssessmentAttemptRequest;
import com.mxhieu.doantotnghiep.dto.request.EnrollmentRequest;
import com.mxhieu.doantotnghiep.dto.request.TestAttemptRequest;
import com.mxhieu.doantotnghiep.dto.response.*;
import com.mxhieu.doantotnghiep.entity.*;
import com.mxhieu.doantotnghiep.exception.AppException;
import com.mxhieu.doantotnghiep.exception.ErrorCode;
import com.mxhieu.doantotnghiep.repository.*;
import com.mxhieu.doantotnghiep.service.EnrollmentServece;
import com.mxhieu.doantotnghiep.service.TestAttemptService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class TestAttemptServiceImpl implements TestAttemptService {
    private final TestRepository testRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final EnrollmentServece enrollmentServece;
    private final TestAttemptConverter testAttemptConverter;
    private final AssessmentOptionRepository assessmentOptionRepository;
    private final AssessmentAnswerRepository assessmentAnswerRepository;

    private final ModelMapper modelMapper;

    @Override
    public void saveResultFirstTest(TestAttemptRequest testAttemptRequest) {
        StudentProfileEntity studentProfile = studentProfileRepository.findById(testAttemptRequest.getStudentProfileId()).orElseThrow(()-> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
        studentProfile.setFirstLogin(false);
        TestAttemptEntity testAttemptEntity = TestAttemptEntity.builder()
                .test(testRepository.findById(testAttemptRequest.getTestId()).orElseThrow(()-> new AppException(ErrorCode.TEST_NOT_FOUND)))
                .studentProfile(studentProfile)
                .testAt(LocalDateTime.now())
                .totalScore(tinhDiem(testAttemptRequest.getAssessmentAttemptRequests()))
                .build();
        List<AssessmentAttemptEntity> attemptEntities = getAssessmentAttemp(testAttemptRequest.getAssessmentAttemptRequests(), testAttemptEntity);
        testAttemptEntity.setAssessmentAttempts(attemptEntities);
        studentProfileRepository.save(studentProfile);
        testAttemptRepository.save(testAttemptEntity);
        saveEnrollment(testAttemptEntity);
    }

    @Override
    public void saveResultMiniTest(TestAttemptRequest testAttemptRequest) {
        TestAttemptEntity testAttemptEntity = TestAttemptEntity.builder()
                .test(testRepository.findById(testAttemptRequest.getTestId()).orElseThrow(()-> new AppException(ErrorCode.TEST_NOT_FOUND)))
                .studentProfile(studentProfileRepository.findById(testAttemptRequest.getStudentProfileId()).orElseThrow(()-> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND)))
                .testAt(LocalDateTime.now())
                .totalScore(tinhDiem(testAttemptRequest.getAssessmentAttemptRequests()))
                .build();
        List<AssessmentAttemptEntity> attemptEntities = getAssessmentAttemp(testAttemptRequest.getAssessmentAttemptRequests(), testAttemptEntity);
        testAttemptEntity.setAssessmentAttempts(attemptEntities);
        testAttemptRepository.save(testAttemptEntity);
    }

    @Override
    public TestAttemptResponse getTestAttemptDetailById(Integer id) {
        TestAttemptEntity testAttemptEntity = testAttemptRepository.findById(id).orElseThrow(()-> new AppException(ErrorCode.TEST_ATTEMPT_NOT_FOUND));
        TestAttemptResponse response = testAttemptConverter.toResponseSummery(testAttemptEntity);
        List<AssessmentResponse> assessmentResponses = new ArrayList<>();
        testAttemptEntity.getAssessmentAttempts().forEach(assessmentAttemptEntity -> {
            AssessmentEntity assessmentEntity = assessmentAttemptEntity.getAssessment();
            List<AssessmentAnswerEntity> answerEntities = assessmentAttemptEntity.getAssessmentAnswers();

            switch (assessmentEntity.getExercisetype().getCode()) {
                case "LISTENING_1" :
                case "LISTENING_2" :
                case "READING_6":
                case "READING_7":
                case "LISTENING_3_4":
                    assessmentResponses.add(toAssessmentDetailResponse(assessmentEntity, answerEntities));
                    break;
                default:
                    assessmentResponses.addAll(toSplitAssessmentDetailResponse(assessmentEntity, answerEntities));
            }
        });
        response.setAssessmentResponses(assessmentResponses);
        return response;
    }

    private List<AssessmentResponse> toSplitAssessmentDetailResponse(AssessmentEntity assessmentEntity, List<AssessmentAnswerEntity> answerEntities) {
        List<AssessmentResponse> assessmentResponses = new ArrayList<>();
        assessmentEntity.getAssessmentQuestions().stream().forEach(assessmentQuestionEntity -> {
            AssessmentEntity assessmentEntityCoppy = modelMapper.map(assessmentEntity, AssessmentEntity.class);
            assessmentEntityCoppy.setAssessmentQuestions(List.of(assessmentQuestionEntity));
            assessmentResponses.add(toAssessmentDetailResponse(assessmentEntityCoppy, answerEntities));
        });
        return assessmentResponses;
    }

    private AssessmentResponse toAssessmentDetailResponse(AssessmentEntity assessmentEntity, List<AssessmentAnswerEntity> answerEntities) {
        AssessmentResponse assessmentResponse = AssessmentResponse.builder()
                .id(assessmentEntity.getId())
                .title(assessmentEntity.getTitle())
                .mediaData(assessmentEntity.getMediaData())
                .typeName(assessmentEntity.getExercisetype().getDescription())
                .imageData(assessmentEntity.getImageData())
                .paragraphs(assessmentEntity.getParagraphs())
                .build();
        List<AssessmentQuestionResponse> assessmentQuestionResponses = new ArrayList<>();
        assessmentEntity.getAssessmentQuestions().stream().forEach(assessmentQuestionEntity -> {
            AssessmentQuestionResponse assessmentQuestionResponse = new AssessmentQuestionResponse();
            assessmentQuestionResponse.setId(assessmentQuestionEntity.getId());
            assessmentQuestionResponse.setQuestionText(assessmentQuestionEntity.getStem());
            assessmentQuestionResponse.setExplain(assessmentQuestionEntity.getExplain());
            assessmentQuestionResponse.setMediData(assessmentQuestionEntity.getMediData());
            List<AssessmentOptionResponse> optionResponses = new ArrayList<>();
            assessmentQuestionEntity.getAssessmentOptions().stream().forEach(assessmentOptionEntity -> {
                AssessmentOptionResponse assessmentOptionResponse = AssessmentOptionResponse.builder()
                        .content(assessmentOptionEntity.getContent())
                        .id(assessmentOptionEntity.getId())
                        .isCorrect(assessmentOptionEntity.getIsCorrect())
                        .selected(checkChoiced(answerEntities, assessmentQuestionEntity.getId(), assessmentOptionEntity.getId()))
                        .build();
                optionResponses.add(assessmentOptionResponse);
            });
            assessmentQuestionResponse.setChoices(optionResponses);
            assessmentQuestionResponses.add(assessmentQuestionResponse);
        });
        assessmentResponse.setAssessmentQuestions(assessmentQuestionResponses);
        return assessmentResponse;
    }


    private Boolean checkChoiced(List<AssessmentAnswerEntity> answerEntities, Integer questionId, Integer choiceId) {
        for (AssessmentAnswerEntity answerEntity : answerEntities){
            if(answerEntity.getAssessmentQuestion().getId().equals(questionId) && answerEntity.getAssessmentOption().getId().equals(choiceId)){
                return true;
            }
        }
        return false;
    }

    private void saveEnrollment(TestAttemptEntity testAttemptEntity) {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .studentProfileId(testAttemptEntity.getStudentProfile().getId())
                .score(testAttemptEntity.getTotalScore())
                .build();
        // lưu enrollment và enrollmentCourse
        enrollmentServece.saveEnrollment(request);
    }

    private List<AssessmentAttemptEntity> getAssessmentAttemp(List<AssessmentAttemptRequest> assessmentAttemptRequests, TestAttemptEntity testAttemptEntity) {
        List<AssessmentAttemptEntity> attemptEntities = new ArrayList<>();
        for (AssessmentAttemptRequest assessmentAttemptRequest : assessmentAttemptRequests) {
            AssessmentAttemptEntity existing = checkExistence(attemptEntities, assessmentAttemptRequest);
            if (attemptEntities.isEmpty() || existing == null) {
                AssessmentAttemptEntity attempt = new AssessmentAttemptEntity();
                attempt.setAssessment(assessmentRepository.findById(assessmentAttemptRequest.getAssessmentId()).orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND)));
                attempt.setAssessmentAnswers(getAssessmentAnswers(assessmentAttemptRequest.getAssessmentAnswerRequests(), attempt));
                attempt.setTestAttempt(testAttemptEntity);
                attemptEntities.add(attempt);
            } else {
                existing.getAssessmentAnswers().addAll(getAssessmentAnswers(assessmentAttemptRequest.getAssessmentAnswerRequests(), existing));
            }
        }
        return attemptEntities;
    }

    private List<AssessmentAnswerEntity> getAssessmentAnswers(List<AssessmentAnswerRequest> assessmentAnswerRequests, AssessmentAttemptEntity attemptEntity) {
        List<AssessmentAnswerEntity> answerEntities = new ArrayList<>();
        for (AssessmentAnswerRequest assessmentAnswerRequest : assessmentAnswerRequests) {
            AssessmentAnswerEntity answerEntity = new AssessmentAnswerEntity();
            AssessmentOptionEntity assessmentOptionEntity = null;
            AssessmentQuestionEntity assessmentQuestionEntity = assessmentQuestionRepository.findById(assessmentAnswerRequest.getAssessmentQuestionId()).orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_QUESSTION_NOT_FOUND));
            try {
                assessmentOptionEntity = assessmentQuestionEntity.getAssessmentOptions().stream().filter(option -> option.getId().equals(assessmentAnswerRequest.getAssessmentOptionId())).toList().get(0);
            } catch (Exception e) {
                System.out.println(assessmentAnswerRequest.getAssessmentQuestionId());
                System.out.println(assessmentQuestionEntity.getAssessmentOptions().stream().map(AssessmentOptionEntity::getId).toList());
                System.out.println(assessmentAnswerRequest.getAssessmentOptionId());
                throw new RuntimeException("Assessment option not found for the question");
            }
            answerEntity.setAssessmentQuestion(assessmentQuestionEntity);
            answerEntity.setAssessmentOption(assessmentOptionEntity);
            answerEntity.setAssessmentAttempt(attemptEntity);
            answerEntity.setIsCorrect(assessmentAnswerRequest.getIsCorrect());
            answerEntities.add(answerEntity);
        }
        return answerEntities;
    }

    private AssessmentAttemptEntity checkExistence(List<AssessmentAttemptEntity> attemptEntities, AssessmentAttemptRequest assessmentAttemptRequest) {
        for (AssessmentAttemptEntity attemptEntity : attemptEntities) {
            if (attemptEntity.getAssessment().getId().equals(assessmentAttemptRequest.getAssessmentId())) {
                return attemptEntity;
            }
        }
        return null;
    }


    private Float tinhDiem(List<AssessmentAttemptRequest> assessmentAttemptRequests) {
        int sum = 0;
        int soCauDung = 0;
        float total = 0;
        for (AssessmentAttemptRequest assessmentAttemptRequest : assessmentAttemptRequests) {
            if (assessmentAttemptRequest.getAssessmentAnswerRequests() != null) {
                for (AssessmentAnswerRequest assessmentAnswerRequest : assessmentAttemptRequest.getAssessmentAnswerRequests()) {
                    sum++;
                    if (assessmentAnswerRequest.getIsCorrect()) {
                        soCauDung++;
                    }
                }
            }
        }
        if (sum != 0) {
            total = (float) (100 * soCauDung) / sum;
        } else {
            total = 0;
        }
        return total;
    }
}
