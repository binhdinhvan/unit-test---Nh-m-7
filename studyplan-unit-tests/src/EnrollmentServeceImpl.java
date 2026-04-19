package com.mxhieu.doantotnghiep.service.impl;

import com.mxhieu.doantotnghiep.converter.EnrollmentConverter;
import com.mxhieu.doantotnghiep.converter.TrackConverter;
import com.mxhieu.doantotnghiep.dto.request.EnrollmentRequest;
import com.mxhieu.doantotnghiep.dto.response.EnrollmentResponst;
import com.mxhieu.doantotnghiep.entity.*;
import com.mxhieu.doantotnghiep.exception.AppException;
import com.mxhieu.doantotnghiep.exception.ErrorCode;
import com.mxhieu.doantotnghiep.repository.*;
import com.mxhieu.doantotnghiep.service.EnrollmentServece;
import com.mxhieu.doantotnghiep.utils.ModuleType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class EnrollmentServeceImpl implements EnrollmentServece {
    private final StudentProfileRepository studentProfileRepository;
    private final TrackRepository trackRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentCourseRepository enrollmentcourseRepository;
    private final EnrollmentConverter enrollmentConverter;
    private final TrackConverter trackConverter;
    private final ModuleRepository moduleRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final TestProgressRepository testProgressRepository;


    @Override
    public void saveEnrollment(EnrollmentRequest enrollmentRequest) {
        StudentProfileEntity studentProfile = studentProfileRepository.findById(enrollmentRequest.getStudentProfileId()).orElseThrow(()->new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
        LocalDateTime enrollmentAt = LocalDateTime.now();
        EnrollmentEntity enrollmentEntity1 = new EnrollmentEntity(enrollmentAt,0,trackRepository.findByCode("0-300").get(),studentProfile);
        EnrollmentEntity enrollmentEntity2 = new EnrollmentEntity(enrollmentAt,0,trackRepository.findByCode("300-600").get(),studentProfile);
        EnrollmentEntity enrollmentEntity3 = new EnrollmentEntity(enrollmentAt,0,trackRepository.findByCode("600+").get(),studentProfile);


        if(enrollmentRequest.getScore() < 30){
            enrollmentEntity1.setStatus(1);
            enrollmentEntity2.setStatus(0);
            enrollmentEntity3.setStatus(0);
        }else if(enrollmentRequest.getScore() < 60){
            enrollmentEntity1.setStatus(2);
            enrollmentEntity2.setStatus(1);
            enrollmentEntity3.setStatus(0);
        }else{
            enrollmentEntity1.setStatus(2);
            enrollmentEntity2.setStatus(2);
            enrollmentEntity3.setStatus(1);
        }
        List<EnrollmentEntity> enrollmentEntities = List.of(enrollmentEntity1,enrollmentEntity2,enrollmentEntity3);
        setEnrollmentCoures(enrollmentEntities,enrollmentRequest.getStudentProfileId());
        enrollmentRepository.saveAll(enrollmentEntities);
    }

    @Override
    public List<EnrollmentResponst> getStudentEnrollmenteds(Integer studentId) {
        List<EnrollmentEntity> enrollmentEntities = enrollmentRepository.findByStudentProfile_Id(studentId);
        List<EnrollmentResponst> enrollmentResponsts = new ArrayList<>();
        enrollmentEntities.forEach(enrollmentEntity -> {
            EnrollmentResponst enrollmentResponst = enrollmentConverter.toResponseSummary(enrollmentEntity);
            enrollmentResponst.setTrackResponse(trackConverter.toTrackForStudent(enrollmentEntity));
            enrollmentResponsts.add(enrollmentResponst);
        });
        return enrollmentResponsts;
    }

    @Override
    public List<EnrollmentResponst> getPreviewStudyFlow(Integer studentId) {
        List<EnrollmentEntity> enrollmentEntities = enrollmentRepository.findByStudentProfile_Id(studentId);
        return enrollmentConverter.toStudyFlow(enrollmentEntities);
    }

    private void setEnrollmentCoures(List<EnrollmentEntity> enrollmentEntities, Integer studentProfileId) {
        AtomicBoolean checkFirstLessonWillUnlock = new AtomicBoolean(true);
        AtomicBoolean checkFirstCouresWillUnlock = new AtomicBoolean(true);
        enrollmentEntities.forEach(enrollmentEntity -> {
            String status = switch (enrollmentEntity.getStatus()) {
                case 1 -> "UNLOCK";
                case 2 -> "DONE";
                default -> "LOCK";
            };
            List<CourseEntity> parentCourse = courseRepository.findByTrack_IdAndStatus(enrollmentEntity.getTrack().getId(), "OLD");
            List<EnrollmentCourseEntity> enrollmentCourseEntitys = new ArrayList<>();
            for (CourseEntity courseEntity : parentCourse) {
                CourseEntity childrenNew = courseRepository.findTopByParentCourse_IdOrderByVersionDesc(courseEntity.getId());
                if(childrenNew != null){
                    // lưu tiến độ với các khóa học done
                    if(status.equals("DONE")){
                        doneLessonWithDone(childrenNew,studentProfileId);
                    }
                    // chỉ mở khóa 1 khóa học đầu tiên của track được mở khóa
                    if(status.equals("UNLOCK")){
                        if(checkFirstCouresWillUnlock.get()){
                            checkFirstCouresWillUnlock.set(false);
                        }else{
                            status = "LOCK";
                        }
                    }
                    //mở khóa bài học đầu tiên của khóa học đầu tiên được mở khóa
                    if(checkFirstLessonWillUnlock.get() && status.equals("UNLOCK")){
                        checkFirstLessonWillUnlock.set(false);
                        unLockFirstLesson(childrenNew, studentProfileId);
                    }
                    EnrollmentCourseEntity enrollmentCourseEntity = EnrollmentCourseEntity.builder()
                            .enrollment(enrollmentEntity)
                            .course(childrenNew)
                            .status(status)
                            .build();
                    enrollmentCourseEntitys.add(enrollmentCourseEntity);

                }
            }
            enrollmentEntity.setEnrollmentCourses(enrollmentCourseEntitys);
        });
    }

    private void doneLessonWithDone(CourseEntity childrenNew, Integer studentProfileId) {
        List<ModuleEntity> moduleEntities = moduleRepository.findByCourseIdOrderByOrderIndex(childrenNew.getId());
        StudentProfileEntity studentProfile = studentProfileRepository.findById(studentProfileId).orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
        for (ModuleEntity moduleEntity : moduleEntities) {
            if(moduleEntity.getType() == ModuleType.LESSON){
                List<LessonEntity> lessonEntities = moduleEntity.getLessons();
                for (LessonEntity lessonEntity : lessonEntities) {
                    LessonProgressEntity lessonProgressEntity = new LessonProgressEntity();
                    lessonProgressEntity.setLesson(lessonEntity);
                    lessonProgressEntity.setStudentProfile(studentProfile);
                    lessonProgressEntity.setProcess(0); // unlock
                    lessonProgressEntity.setPercentageWatched(0);
                    lessonProgressRepository.save(lessonProgressEntity);
                }
            }else{
                List<TestEntity> firstTest = moduleEntity.getTests();
                if(!firstTest.isEmpty()){
                    TestProgressEntity firstTestProgressEntity = new TestProgressEntity();
                    firstTestProgressEntity.setTest(firstTest.get(0));
                    firstTestProgressEntity.setStudentProfile(studentProfile);
                    firstTestProgressEntity.setProcess(0);
                    testProgressRepository.save(firstTestProgressEntity);
                }else{
                    throw new AppException(ErrorCode.TEST_NOT_FOUND);
                }
            }
        }
    }

    private void unLockFirstLesson(CourseEntity childrenNew, Integer studentProfileId) {
        ModuleEntity firstModule = moduleRepository.findTopByCourse_IdOrderByOrderIndexAsc(childrenNew.getId());
        StudentProfileEntity studentProfile = studentProfileRepository.findById(studentProfileId).orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
        if(firstModule != null){
            if(firstModule.getType() == ModuleType.LESSON){
                LessonEntity firstLesson = firstModule.getLessons().stream().sorted((l1, l2) -> l1.getOrderIndex().compareTo(l2.getOrderIndex())).findFirst().orElse(null);
                LessonProgressEntity lessonProgressEntity = new LessonProgressEntity();
                lessonProgressEntity.setLesson(firstLesson);
                lessonProgressEntity.setStudentProfile(studentProfile);
                lessonProgressEntity.setProcess(0); // unlock
                lessonProgressEntity.setPercentageWatched(0);
                lessonProgressRepository.save(lessonProgressEntity);
            }else{
                List<TestEntity> firstTest = firstModule.getTests();
                if(!firstTest.isEmpty()){
                    TestProgressEntity firstTestProgressEntity = new TestProgressEntity();
                    firstTestProgressEntity.setTest(firstTest.get(0));
                    firstTestProgressEntity.setStudentProfile(studentProfile);
                    firstTestProgressEntity.setProcess(0);
                    testProgressRepository.save(firstTestProgressEntity);
                }else{
                    throw new AppException(ErrorCode.TEST_NOT_FOUND);
                }
            }
        }
    }
}
