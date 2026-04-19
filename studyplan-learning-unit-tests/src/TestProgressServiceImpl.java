package com.mxhieu.doantotnghiep.service.impl;

import com.mxhieu.doantotnghiep.dto.request.LessonOrTestAroundRequest;
import com.mxhieu.doantotnghiep.dto.request.TestProgressRequest;
import com.mxhieu.doantotnghiep.dto.response.LessonOrTestAroundResponse;
import com.mxhieu.doantotnghiep.entity.*;
import com.mxhieu.doantotnghiep.exception.AppException;
import com.mxhieu.doantotnghiep.exception.ErrorCode;
import com.mxhieu.doantotnghiep.repository.*;
import com.mxhieu.doantotnghiep.service.LessonProgressService;
import com.mxhieu.doantotnghiep.service.LessonService;
import com.mxhieu.doantotnghiep.service.TestProgressService;
import com.mxhieu.doantotnghiep.service.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Service
public class TestProgressServiceImpl implements TestProgressService {
    private final TestProgressRepository testProgressRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TestRepository testRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final LessonService lessonService;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonProgressService lessonProgressService;
    private final TrackService trackService;
    private final EnrollmentRepository enrollmentRepository;
    @Override
    public Boolean checkCompletionCondition(TestProgressRequest request) {
        TestEntity testEntity = testRepository.findById(request.getTestId()).orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_FOUND));
        StudentProfileEntity studentProfileEntity = studentProfileRepository.findById(request.getStudentprofileId()).orElseThrow(()->new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
        List<TestProgressEntity> testProgressEntities = testProgressRepository.findByTest_IdAndStudentProfile_Id(request.getTestId(), request.getStudentprofileId());

        if(!testProgressEntities.isEmpty()){
            TestProgressEntity testProgressEntity = testProgressEntities.get(0);
            if(checkCompleted(testEntity, request)){
                testProgressEntity.setProcess(2);
                unLockNext(testEntity, studentProfileEntity);
                testProgressRepository.save(testProgressEntity);
                return true;
            } else {
                testProgressEntity.setProcess(1);
                testProgressRepository.save(testProgressEntity);
                return false;
            }

        } else {
            throw new AppException(ErrorCode.TEST_PROGRESS_NOT_EXISTS);
        }
    }

    private void unLockNext(TestEntity testEntity, StudentProfileEntity studentProfileEntity) {
        try{
            LessonOrTestAroundRequest request = new LessonOrTestAroundRequest(testEntity.getId(),"TEST");
            LessonOrTestAroundResponse nextLessonOrTest= lessonService.getNextLessonOrTest(request);
            if(nextLessonOrTest.getType().equals("LESSON")){
                LessonEntity nextLesson = lessonRepository.findById(nextLessonOrTest.getId()).orElseThrow(()->new AppException(ErrorCode.LESSON_NOT_FOUND));
                List<LessonProgressEntity> lessonProgressEntities = lessonProgressRepository.findByLesson_IdAndStudentProfile_Id(nextLesson.getId(), studentProfileEntity.getId());
                if(lessonProgressEntities.isEmpty()){
                    LessonProgressEntity lessonProgressEntity = new LessonProgressEntity();
                    lessonProgressEntity.setLesson(nextLesson);
                    lessonProgressEntity.setStudentProfile(studentProfileEntity);
                    lessonProgressEntity.setProcess(0); // unlock
                    lessonProgressEntity.setPercentageWatched(0);
                    lessonProgressRepository.save(lessonProgressEntity);
                }
            }else{
                TestEntity nextTest = testRepository.findById(nextLessonOrTest.getId()).orElseThrow(()->new AppException(ErrorCode.TEST_NOT_FOUND));
                List<TestProgressEntity> testProgressEntities = testProgressRepository.findByTest_IdAndStudentProfile_Id(nextTest.getId(), studentProfileEntity.getId());
                if(testProgressEntities.isEmpty()){
                    TestProgressEntity testProgressEntity = new TestProgressEntity();
                    testProgressEntity.setTest(nextTest);
                    testProgressEntity.setStudentProfile(studentProfileEntity);
                    testProgressEntity.setProcess(0); // unlock
                    testProgressRepository.save(testProgressEntity);

                }
            }
        }catch (AppException e){
            lessonProgressService.unLockNextCourse(testEntity.getModule().getCourse(), studentProfileEntity);
        }

    }

    private boolean checkCompleted(TestEntity testEntity, TestProgressRequest request) {
        TestAttemptEntity attempt = testAttemptRepository.findTopByTest_IdAndStudentProfile_IdOrderByTotalScoreDesc(testEntity.getId(), request.getStudentprofileId())
                .orElseThrow(() -> new AppException(ErrorCode.TEST_ATTEMPT_NOT_FOUND));


        return attempt.getTotalScore() >= 50;
    }

}
