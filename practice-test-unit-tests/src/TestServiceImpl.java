package com.mxhieu.doantotnghiep.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.mxhieu.doantotnghiep.converter.TestConverter;
import com.mxhieu.doantotnghiep.dto.request.TestRequest;
import com.mxhieu.doantotnghiep.dto.response.TestResponse;
import com.mxhieu.doantotnghiep.entity.ModuleEntity;
import com.mxhieu.doantotnghiep.entity.TestAttemptEntity;
import com.mxhieu.doantotnghiep.entity.TestEntity;
import com.mxhieu.doantotnghiep.entity.TestProgressEntity;
import com.mxhieu.doantotnghiep.exception.AppException;
import com.mxhieu.doantotnghiep.exception.ErrorCode;
import com.mxhieu.doantotnghiep.repository.EnrollmentCourseRepository;
import com.mxhieu.doantotnghiep.repository.ModuleRepository;
import com.mxhieu.doantotnghiep.repository.TestAttemptRepository;
import com.mxhieu.doantotnghiep.repository.TestProgressRepository;
import com.mxhieu.doantotnghiep.repository.TestRepository;
import com.mxhieu.doantotnghiep.service.TestService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TestServiceImpl implements TestService {
    private final TestRepository testRepository;
    private final TestConverter testConverter;
    private final ModuleRepository moduleRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final EnrollmentCourseRepository enrollmentcourseRepository;
    private final TestProgressRepository testProgressRepository;

    @Override
    public void createTest(TestRequest testRequest) {
        TestEntity testEntity = testConverter.toEntity(testRequest,TestEntity.class);
        ModuleEntity moduleEntity = moduleRepository.findById(testRequest.getModuleId()).orElseThrow(()->new AppException(ErrorCode.MODULE_NOT_FOUND));
        testEntity.setModule(moduleEntity);
        testRepository.save(testEntity);
    }

    @Override
    public TestResponse getTestById(Integer id) {
        TestEntity testEntity = testRepository.findById(id).orElseThrow(()->new AppException(ErrorCode.TEST_NOT_FOUND));
        TestResponse testResponse = testConverter.toResponse(testEntity,TestResponse.class);
        return testResponse;
    }

    @Override
    public void createFirstTest(TestRequest testRequest) {
        TestEntity testEntity = testConverter.toEntity(testRequest,TestEntity.class);
        testRepository.save(testEntity);
    }

    @Override
    public List<TestResponse> getFirstTestsSummery() {
        List<TestEntity> testEntities = testRepository.findByType("FIRST_TEST");
        List<TestResponse> responses = new ArrayList<>();
        testEntities.stream().forEach(testEntity -> {
            TestResponse testResponse = testConverter.toResponseSummery(testEntity);
            responses.add(testResponse);
        });
        return responses;
    }

    @Override
    public int commpletedStar(Integer testId, Integer studentProfileId) {
        List<TestAttemptEntity> testAttemptEntitys = testAttemptRepository.findByTestIdAndStudentProfileId(testId, studentProfileId);
        if(testAttemptEntitys.isEmpty() || testAttemptEntitys.size() == 0){
            return 0;
        }
        TestAttemptEntity testAttemptEntity = testAttemptEntitys.stream().max((a,b) -> Float.compare(a.getTotalScore(), b.getTotalScore())).get();
        int completedStar = 0;
        if(testAttemptEntity.getTotalScore() == 100){
            completedStar = 3;
        }else if(testAttemptEntity.getTotalScore() >= 70){
            completedStar = 2;
        }else {
            completedStar = 1;
        }
        return completedStar;
    }

    @Override
    public List<TestResponse> getTestResponseDetail(List<TestEntity> tests, Integer studentProfileId) {
        List<TestResponse> responses = new ArrayList<>();
        for(TestEntity testEntity : tests){
            TestResponse testResponse = testConverter.toResponseNoQuestion(testEntity);
            testResponse.setCompletedStar(commpletedStar(testEntity.getId(), studentProfileId));
            testResponse.setStatus(isLockLesson(testEntity.getId(),studentProfileId)? "LOCK" : "UNLOCK");
            responses.add(testResponse);
        }
        return responses;
    }

    private boolean isLockLesson(Integer id, Integer studentProfileId) {
        TestEntity testEntity = testRepository.findById(id).orElseThrow(()->new AppException(ErrorCode.TEST_NOT_FOUND));
        String statusOfCourse = enrollmentcourseRepository.findStatus(studentProfileId, testEntity.getModule().getCourse().getId());
        List<TestProgressEntity> testProgressEntities = testProgressRepository.findByTest_IdAndStudentProfile_Id(id, studentProfileId);
        if (statusOfCourse == null) {
            return true;
        }
        if("LOCK".equals(statusOfCourse)) {
            return true;
        }else if("DONE".equals(statusOfCourse)) {
            return false;
        }else {
            if(testProgressEntities.isEmpty()) {
                return true;
            }else{
                return false;
            }
        }
    }

    @Override
    public int getCompletedTestsOfStudent(List<TestEntity> tests, Integer studentProfileId) {
        int completedTests = 0;
        for(TestEntity testEntity : tests){
            if(isCompletedTest(testEntity.getId(), studentProfileId)){
                completedTests++;
            }
        }
        return completedTests;
    }

    @Override
    public List<Integer> getTestAttemptIds(Integer testId, Integer studentProfileId) {
        List<TestAttemptEntity> testAttemptEntities = testAttemptRepository.findByTestIdAndStudentProfileId(testId, studentProfileId);
        List<Integer> testAttemptIds = new ArrayList<>();
        if(testAttemptEntities.size() == 0){
            return testAttemptIds;
        }
        testAttemptEntities.stream().forEach(testAttemptEntity -> {
            testAttemptIds.add(testAttemptEntity.getId());
        });
        return testAttemptIds;
    }

    @Override
    public TestResponse getMiniTestsSummery(Integer id) {
        TestEntity testEntity = testRepository.findById(id).orElseThrow(()->new AppException(ErrorCode.TEST_NOT_FOUND));
        TestResponse testResponse = TestResponse.builder()
                .id(testEntity.getId())
                .type(testEntity.getType())
                .name(testEntity.getName())
                .build();
        return testResponse;
    }

    @Override
    public float getMaxScore(Integer testId, Integer studentProfileId) {
        List<TestAttemptEntity> testAttemptEntitys = testAttemptRepository.findByTestIdAndStudentProfileId(testId, studentProfileId);
        if(testAttemptEntitys.isEmpty() || testAttemptEntitys.size() == 0){
            return 0;
        }
        TestAttemptEntity testAttemptEntity = testAttemptEntitys.stream().max((a,b) -> Float.compare(a.getTotalScore(), b.getTotalScore())).get();
        return testAttemptEntity.getTotalScore();
    }

    @Override
    public Boolean isLock(Integer id, Integer studentId) {
        TestEntity testEntity = testRepository.findById(id).orElseThrow(()->new AppException(ErrorCode.TEST_NOT_FOUND));
        String statusOfCourse = enrollmentcourseRepository.findStatus(studentId, testEntity.getModule().getCourse().getId());
        List<TestProgressEntity> testProgressEntities = testProgressRepository.findByTest_IdAndStudentProfile_Id(testEntity.getId(), studentId);

        if(statusOfCourse.equals("LOCK")) {
            return true;
        }else if(statusOfCourse.equals("DONE")) {
            return false;
        }else {
            if(testProgressEntities.size() == 0) {
                return true;
            }else{
                return false;
            }
        }
    }

    @Override
    public boolean isCompletedTest(Integer id, Integer studentProfileId) {
        List<TestProgressEntity> testProgressEntities = testProgressRepository.findByTest_IdAndStudentProfile_Id(id, studentProfileId);
        if(testProgressEntities.size() == 0) {
            return false;
        }else if(testProgressEntities.get(0).getProcess() == 0 || testProgressEntities.get(0).getProcess() == 1){
            return false;
        }
        return true;
    }

    @Override
    public void deleteTest(Integer id) {
        testRepository.deleteById(id);
    }

}
