package com.mxhieu.doantotnghiep.service.impl;

import com.mxhieu.doantotnghiep.dto.request.LessonOrTestAroundRequest;
import com.mxhieu.doantotnghiep.dto.request.LessonProgressRequest;
import com.mxhieu.doantotnghiep.dto.response.LessonOrTestAroundResponse;
import com.mxhieu.doantotnghiep.entity.*;
import com.mxhieu.doantotnghiep.exception.AppException;
import com.mxhieu.doantotnghiep.exception.ErrorCode;
import com.mxhieu.doantotnghiep.repository.*;
import com.mxhieu.doantotnghiep.service.LessonProgressService;
import com.mxhieu.doantotnghiep.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonProgressServiceImpl implements LessonProgressService {
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AttemptRepository attemptRepository;
    private final TestRepository testRepository;
    private final TestProgressRepository testProgressRepository;
    private final ExerciseRepository exerciseRepository;
    private final LessonService lessonService;
    private final CourseRepository courseRepository;
    private final EnrollmentCourseRepository enrollmentCourseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TrackRepository trackRepository;


    @Override
    public Boolean checkCompletionCondition(LessonProgressRequest request) {
        LessonEntity lessonEntity = lessonRepository.findById(request.getLessonId()).orElseThrow(()->new AppException(ErrorCode.LESSON_NOT_FOUND));
        StudentProfileEntity studentProfileEntity = studentProfileRepository.findById(request.getStudentProfileId()).orElseThrow(()->new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
        List<LessonProgressEntity> lessonProgressEntity = lessonProgressRepository.findByLesson_IdAndStudentProfile_Id(request.getLessonId(), request.getStudentProfileId());

        if(!lessonProgressEntity.isEmpty()){
            LessonProgressEntity lessonProgress = lessonProgressEntity.get(0);
            if(lessonProgress.getPercentageWatched() < request.getPercentageWatched()){
                lessonProgress.setPercentageWatched(request.getPercentageWatched());
            }
            if(checkCompleted(lessonProgress ,lessonEntity, request)){
                lessonProgress.setProcess(2);
                unLockNextLesson(lessonEntity, studentProfileEntity);
                lessonProgressRepository.save(lessonProgress);
                return true;
            } else {
                lessonProgress.setProcess(1);
                lessonProgressRepository.save(lessonProgress);
                return false;
            }
        }else{
            throw new AppException(ErrorCode.LESSON_PROGRESS_NOT_EXISTS);
        }
    }

    private void unLockNextLesson(LessonEntity lessonEntity, StudentProfileEntity studentProfileEntity) {
        LessonOrTestAroundRequest request = new LessonOrTestAroundRequest(lessonEntity.getId(),"LESSON");
        try{
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
            System.out.println("Loi o day");
            e.printStackTrace();
            unLockNextCourse(lessonEntity.getModule().getCourse(), studentProfileEntity);
        }
    }

    @Override
    public void unLockNextCourse(CourseEntity course, StudentProfileEntity studentProfileEntity) {
        setStatusCourse(course,studentProfileEntity,"DONE");
        try{
            List<EnrollmentCourseEntity> enrollmentCourseEntities = enrollmentCourseRepository.findByCourse_IdAndEnrollment_StudentProfile_Id(course.getId(), studentProfileEntity.getId());

            if(!enrollmentCourseEntities.isEmpty()){
                EnrollmentCourseEntity currentEnrollmentCourse = enrollmentCourseEntities.get(0);
                EnrollmentCourseEntity nextEnrollmentCourse = enrollmentCourseRepository.findTopByIdAfterAndEnrollment_Id(currentEnrollmentCourse.getId(),currentEnrollmentCourse.getEnrollment().getId()).orElseThrow(() ->new AppException(ErrorCode.NEXT_NOT_FOUND));
                setStatusCourse(nextEnrollmentCourse.getCourse(),studentProfileEntity,"UNLOCK");
            }
        }catch (AppException e){
            unLockNextTrack(course.getTrack(), studentProfileEntity);
        }
    }

    private void setStatusCourse(CourseEntity course, StudentProfileEntity studentProfileEntity, String status) {
        List<EnrollmentCourseEntity> enrollmentCourseEntities = enrollmentCourseRepository.findByCourse_IdAndEnrollment_StudentProfile_Id(course.getId(), studentProfileEntity.getId());
        if(!enrollmentCourseEntities.isEmpty()){

            EnrollmentCourseEntity enrollmentCourseEntity = enrollmentCourseEntities.get(0);
            if(!enrollmentCourseEntity.getStatus().equals("DONE")){
                enrollmentCourseEntity.setStatus(status);
                enrollmentCourseRepository.save(enrollmentCourseEntity);
            }
        }
    }

    @Override
    public void unLockNextTrack(TrackEntity trackEntity, StudentProfileEntity studentProfileEntity) {
        setStatusTrack(trackEntity,studentProfileEntity,2);
        try {
            TrackEntity nextTrack = trackRepository.findById(trackEntity.getId() + 1).orElseThrow(()->new AppException(ErrorCode.TRACK_NOT_FOUND));
            setStatusTrack(nextTrack,studentProfileEntity,1);
        }catch (AppException e){

        }
    }

    private void setStatusTrack(TrackEntity trackEntity, StudentProfileEntity studentProfileEntity, int status) {
        List<EnrollmentEntity> enrollmentEntities = enrollmentRepository.findByTrack_IdAndStudentProfile_Id(trackEntity.getId(), studentProfileEntity.getId());
        if(!enrollmentEntities.isEmpty()){
            EnrollmentEntity enrollmentEntity = enrollmentEntities.get(0);
            if(!enrollmentEntity.getStatus().equals(2)){
                enrollmentEntity.setStatus(status);
                enrollmentRepository.save(enrollmentEntity);
            }

        }
    }

    private boolean checkCompleted(LessonProgressEntity lessonProgress, LessonEntity lessonEntity, LessonProgressRequest request) {
        Integer percentageWatched = lessonProgress.getPercentageWatched();

        if(percentageWatched < lessonEntity.getGatingRules()){
            return false;
        }
        if(lessonEntity.getExercises().size() == 0){
            return true;
        }else{
            for (ExerciseEntity exercise : lessonEntity.getExercises()) {
                if(!exerciseRepository.isExerciseCompletedByStudent(exercise.getId(), request.getStudentProfileId())){
                    return false;
                }
            }
            return true;
        }

    }
}
