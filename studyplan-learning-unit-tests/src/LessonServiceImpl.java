package com.mxhieu.doantotnghiep.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.mxhieu.doantotnghiep.controller.MaterialConverter;
import com.mxhieu.doantotnghiep.controller.MediaAssetConverter;
import com.mxhieu.doantotnghiep.converter.LessonConverter;
import com.mxhieu.doantotnghiep.dto.request.LessonOrTestAroundRequest;
import com.mxhieu.doantotnghiep.dto.request.LessonRequest;
import com.mxhieu.doantotnghiep.dto.response.LessonOrTestAroundResponse;
import com.mxhieu.doantotnghiep.dto.response.LessonResponse;
import com.mxhieu.doantotnghiep.entity.CourseEntity;
import com.mxhieu.doantotnghiep.entity.LessonEntity;
import com.mxhieu.doantotnghiep.entity.LessonProgressEntity;
import com.mxhieu.doantotnghiep.entity.MaterialEntity;
import com.mxhieu.doantotnghiep.entity.MediaAssetEntity;
import com.mxhieu.doantotnghiep.entity.ModuleEntity;
import com.mxhieu.doantotnghiep.entity.TestEntity;
import com.mxhieu.doantotnghiep.exception.AppException;
import com.mxhieu.doantotnghiep.exception.ErrorCode;
import com.mxhieu.doantotnghiep.repository.EnrollmentCourseRepository;
import com.mxhieu.doantotnghiep.repository.ExerciseRepository;
import com.mxhieu.doantotnghiep.repository.LessonProgressRepository;
import com.mxhieu.doantotnghiep.repository.LessonRepository;
import com.mxhieu.doantotnghiep.repository.MaterialRepository;
import com.mxhieu.doantotnghiep.repository.MediaAssetRepository;
import com.mxhieu.doantotnghiep.repository.ModuleRepository;
import com.mxhieu.doantotnghiep.repository.TestRepository;
import com.mxhieu.doantotnghiep.service.LessonService;
import com.mxhieu.doantotnghiep.utils.ModuleType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {
    private final LessonRepository lessonRepository;
    private final LessonConverter lessonConverter;
    private final ExerciseRepository exerciseRepository;
    private final EnrollmentCourseRepository enrollmentcourseRepository;

    private final MediaAssetRepository mediaAssetRepository;
    private final MaterialConverter materialConverter;
    private final MaterialRepository materialRepository;
    private final MediaAssetConverter mediaAssetConverter;
    private final ModuleRepository moduleRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final TestRepository testRepository;

    @Transactional
    @Override
    public void createLesson(LessonRequest lessonRequest, String videoPath, List<MultipartFile> materials) {

        // convert nội dung lesson
        LessonEntity lessonEntity = lessonConverter.toEntity(lessonRequest, LessonEntity.class);

        // Tạo MediaAssetEntity nhưng KHÔNG lưu byte[] video
        MediaAssetEntity mediaassetEntity = new MediaAssetEntity();
        mediaassetEntity.setName(videoPath.substring(videoPath.lastIndexOf("/") + 1)); // lấy tên file
        mediaassetEntity.setUrl(videoPath); // đường dẫn file đã merge
        mediaassetEntity.setType("video/mp4");   // hoặc lấy từ FE
        mediaassetEntity.setLesson(lessonEntity);
        mediaassetEntity.setLengthSec(lessonRequest.getDurationMinutes());

        // convert materials (vẫn giữ)
        List<MaterialEntity> materialEntities = materialConverter.toListMaterialEntity(materials, lessonEntity);

        // gán module
        lessonEntity.setDurationMinutes(80);
        lessonEntity.setModule(
                moduleRepository.findById(lessonRequest.getModuleId()).orElseThrow()
        );

        // Cập nhật orderIndex
        lessonRepository.flushOrderIndex(lessonRequest.getModuleId(), lessonRequest.getOrderIndex());

        // Lưu dữ liệu
        lessonRepository.save(lessonEntity);
        materialRepository.saveAll(materialEntities);
        mediaAssetRepository.save(mediaassetEntity);
    }

    @Override
    public void updateLesson(LessonRequest lessonRequest) {

        String videoPath = lessonRequest.getVideoPath();
        List<MultipartFile> materials = lessonRequest.getMaterials();
        LessonEntity lessonEntity = lessonRepository.findById(lessonRequest.getId()).orElseThrow(()-> new AppException(ErrorCode.LESSON_NOT_FOUND));
        List<MaterialEntity> materialEntities = lessonEntity.getMaterialEntities();
        List<MediaAssetEntity> mediaAssetEntities = lessonEntity.getMediaassets();

        int oldIndex = lessonRequest.getOrderIndex();
        int newIndex = lessonRequest.getOrderIndex();
        Integer moduleId = lessonEntity.getModule().getId();

        lessonRepository.decreaseOrderIndex(moduleId,oldIndex);

        int maxIndex = getMaxOrder(moduleId);

        if(newIndex < maxIndex){
            lessonRepository.flushOrderIndex(moduleId, newIndex);
        }else{
            newIndex = maxIndex;
        }



        lessonEntity.setGatingRules(lessonRequest.getGatingRules());
        lessonEntity.setTitle(lessonRequest.getTitle());
        lessonEntity.setDurationMinutes(lessonRequest.getDurationMinutes());
        lessonEntity.setOrderIndex(newIndex);
        lessonEntity.setSummary(lessonRequest.getSummary());

        // ====== DELETE OLD VIDEO ======
         // orphanRemoval → DELETE

        // ====== CREATE NEW VIDEO ======
        // Tạo MediaAssetEntity nhưng KHÔNG lưu byte[] video
        if (videoPath != null && !videoPath.isBlank()) {
            lessonEntity.getMediaassets().clear();
            MediaAssetEntity media = new MediaAssetEntity();
            media.setName(videoPath.substring(videoPath.lastIndexOf("/") + 1));
            media.setUrl(videoPath);
            media.setType("video/mp4");
            media.setLengthSec(lessonRequest.getDurationMinutes());
            media.setLesson(lessonEntity);

            lessonEntity.getMediaassets().add(media);
        }

        // ====== DELETE OLD MATERIALS ======


        // ====== CREATE NEW MATERIALS ======
        if (materials != null && !materials.isEmpty()) {
            lessonEntity.getMaterialEntities().clear();
            List<MaterialEntity> newMaterials =
                    materialConverter.toListMaterialEntity(materials, lessonEntity);
            lessonEntity.getMaterialEntities().addAll(newMaterials);
        }

        // ====== SAVE ======
        lessonRepository.save(lessonEntity);

    }

    @Override
    @Transactional
    public void deleteLesson(Integer id) {
        LessonEntity lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        lessonRepository.delete(lesson);
    }

    @Override
    public LessonResponse getLessonForStudent(Integer id, Integer studentId) {
        LessonEntity lessonEntity = lessonRepository.findById(id).orElseThrow(()-> new AppException(ErrorCode.LESSON_NOT_FOUND));
        if(isLockLesson(lessonEntity.getId(), studentId)){
            throw new AppException(ErrorCode.LESSON_IS_LOCK);
        }

        List<LessonProgressEntity> lessonProgress = lessonProgressRepository.findByLesson_IdAndStudentProfile_Id(id,studentId);
        int progressWatched = 0;
        if(!lessonProgress.isEmpty()){
            progressWatched = lessonProgress.get(0).getPercentageWatched();
        }
        LessonResponse response = lessonConverter.toResponse(lessonEntity, LessonResponse.class);
        response.setProgressWatched(progressWatched);
        if(lessonEntity.getExercises().isEmpty()){
            response.setHasExercise(false);
        }else {
            response.setHasExercise(true);
        }
        return response;
    }


    @Override
    public int getMaxOrder(Integer moduleId) {
        return lessonRepository.getMaxOrder(moduleId) + 1;
    }

    @Override
    public List<LessonResponse> getLessons(Integer moduleId) {
        List<LessonEntity> lessonEntities = lessonRepository.findByModuleId(moduleId);
        return lessonConverter.toResponseList(lessonEntities, LessonResponse.class);
    }

    @Override
    public List<LessonResponse> getLessons(Integer moduleId, Integer studentProfileId) {
        List<LessonEntity> lessonEntities = lessonRepository.findByModuleId(moduleId);
        return lessonEntities.stream()
                .map(entity -> {
                    LessonResponse response = lessonConverter.toResponse(entity, LessonResponse.class);
                    response.setCompletedStar(completedStar(entity.getId(), studentProfileId));
                    return response;
                })
                .toList();
    }

    @Override
    public List<LessonResponse> getListLessonResponseDetail(List<LessonEntity> lessonEntities, Integer studentProfileId) {
        List<LessonResponse> responses = new ArrayList<>();
        lessonEntities.forEach(entity -> {
            LessonResponse response = lessonConverter.toResponseNoMaterialMediaasset(entity);
            response.setCompletedStar(completedStar(entity.getId(),studentProfileId));
            response.setStatus(isLockLesson(entity.getId(), studentProfileId) ? "LOCK" : "UNLOCK");
            responses.add(response);
        });
        return responses;
    }

    @Override
    public LessonResponse getLesson(Integer id) {
        LessonEntity lessonEntity = lessonRepository.findById(id).orElseThrow(()-> new AppException(ErrorCode.LESSON_NOT_FOUND));
        return lessonConverter.toResponse(lessonEntity, LessonResponse.class);
    }

    @Override
    public Boolean isCompletedLesson(Integer lessonId, Integer studentProfileId) {
        List<LessonProgressEntity> lessonProgressEntities = lessonProgressRepository.findByLesson_IdAndStudentProfile_Id(lessonId, studentProfileId);
        if (lessonProgressEntities.size() == 0) {
            return false;
        } else if (lessonProgressEntities.get(0).getProcess() == 0 || lessonProgressEntities.get(0).getProcess() == 1) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean isLockLesson(Integer lessonId, Integer studentProfileId) {
        LessonEntity lessonEntity = lessonRepository.findById(lessonId).orElseThrow(()-> new AppException(ErrorCode.LESSON_NOT_FOUND));
        String statusOfCourse = enrollmentcourseRepository.findStatus(studentProfileId, lessonEntity.getModule().getCourse().getId());
        List<LessonProgressEntity> lessonProgressEntities = lessonProgressRepository.findByLesson_IdAndStudentProfile_Id(lessonEntity.getId(), studentProfileId);
        if (statusOfCourse == null) {
            return true;
        }
        if("LOCK".equals(statusOfCourse)) {
            return true;
        }else if("DONE".equals(statusOfCourse)) {
            return false;
        }else {
            if(lessonProgressEntities.isEmpty()) {
                return true;
            }else{
                return false;
            }
        }
    }

    @Override
    public String getLessonPath(Integer lessonId) {
        LessonEntity lessonEntity = lessonRepository.findById(lessonId).orElseThrow(()-> new AppException(ErrorCode.LESSON_NOT_FOUND));
        String path = lessonEntity.getModule().getCourse().getTrack().getName() + "/" +
                lessonEntity.getModule().getCourse().getTitle() + "/" +
                lessonEntity.getModule().getTitle() + "/" +
                lessonEntity.getTitle();

        return path;
    }

    @Override
    public LessonOrTestAroundResponse getNextLessonOrTest (LessonOrTestAroundRequest request) {
        CourseEntity course = null;
        ItemWrapper currentWraper = null;
        if(request.getType().trim().equals("TEST")) {
            TestEntity testEntity = testRepository.findById(request.getId()).orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_FOUND));
            course = testEntity.getModule().getCourse();
            currentWraper = new ItemWrapper(testEntity.getModule().getOrderIndex(), 0, testEntity.getId(), "TEST");
        }else if(request.getType().trim().equals("LESSON")) {
            LessonEntity lessonEntity = lessonRepository.findById(request.getId()).orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
            course = lessonEntity.getModule().getCourse();
            currentWraper = new ItemWrapper(lessonEntity.getModule().getOrderIndex(), lessonEntity.getOrderIndex(), lessonEntity.getId(), "LESSON");
        }

        List<ItemWrapper> items = new ArrayList<>();

        for (ModuleEntity module : course.getModules()) {
            Long moduleOrder = module.getOrderIndex();
            // Nếu module là LESSON → add tất cả lesson vào list
            if (module.getType() == ModuleType.LESSON) {
                module.getLessons()
                        .stream()
                        .sorted(Comparator.comparing(LessonEntity::getOrderIndex))
                        .forEach(lesson ->
                                items.add(new ItemWrapper(
                                        moduleOrder,
                                        lesson.getOrderIndex(),
                                        lesson.getId(),
                                        "LESSON"
                                ))
                        );
            }

            // Nếu module là TEST → add đúng 1 test vào list
            if (module.getType() == ModuleType.TEST && !module.getTests().isEmpty()) {
                TestEntity test = module.getTests().get(0);  // chỉ 1 bài test duy nhất
                items.add(new ItemWrapper(
                        moduleOrder,// vẫn nên dùng orderIndex cho test
                        0,          // test chỉ có 1 bài duy nhất nên orderIndex = 0
                        test.getId(),
                        "TEST"
                ));
            }
        }

        // Sort toàn bộ course theo thứ tự module → item
        items.sort(
                Comparator.comparing(ItemWrapper::moduleOrder)
                        .thenComparing(ItemWrapper::itemOrder)
        );

        final ItemWrapper target = currentWraper;
        int index = IntStream.range(0, items.size())
                .filter(i -> items.get(i).equals(target))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_HAS_NEXT));
        if(index == items.size()-1) {
            throw new AppException(ErrorCode.LESSON_NOT_HAS_NEXT);
        }

        ItemWrapper next = items.get(index + 1);
        return LessonOrTestAroundResponse.builder()
                .Id(next.id())
                .type(next.type())
                .build();
    }

    record ItemWrapper(Long moduleOrder, int itemOrder, Integer id, String type) {}

    @Override
    public LessonOrTestAroundResponse getPreviousLessonID (LessonOrTestAroundRequest request) {

        CourseEntity course = null;
        ItemWrapper currentWraper = null;

        // Xác định current item
        if(request.getType().equals("TEST")) {
            TestEntity testEntity = testRepository.findById(request.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.TEST_NOT_FOUND));

            course = testEntity.getModule().getCourse();

            currentWraper = new ItemWrapper(
                    testEntity.getModule().getOrderIndex(),
                    0,
                    testEntity.getId(),
                    "TEST"
            );

        } else if(request.getType().equals("LESSON")) {

            LessonEntity lessonEntity = lessonRepository.findById(request.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

            course = lessonEntity.getModule().getCourse();

            currentWraper = new ItemWrapper(
                    lessonEntity.getModule().getOrderIndex(),
                    lessonEntity.getOrderIndex(),
                    lessonEntity.getId(),
                    "LESSON"
            );
        }

        // Gom items
        List<ItemWrapper> items = new ArrayList<>();

        for (ModuleEntity module : course.getModules()) {

            Long moduleOrder = module.getOrderIndex();

            // Bài học
            if (module.getType() == ModuleType.LESSON) {

                module.getLessons().stream()
                        .sorted(Comparator.comparing(LessonEntity::getOrderIndex))
                        .forEach(lesson ->
                                items.add(new ItemWrapper(
                                        moduleOrder,
                                        lesson.getOrderIndex(),
                                        lesson.getId(),
                                        "LESSON"
                                ))
                        );
            }

            // Bài test
            if (module.getType() == ModuleType.TEST && !module.getTests().isEmpty()) {

                TestEntity test = module.getTests().get(0);

                items.add(new ItemWrapper(
                        moduleOrder,
                        0,
                        test.getId(),
                        "TEST"
                ));
            }
        }

        // Sort
        items.sort(
                Comparator.comparing(ItemWrapper::moduleOrder)
                        .thenComparing(ItemWrapper::itemOrder)
        );

        // Tìm index của current item
        final ItemWrapper target = currentWraper;

        int index = IntStream.range(0, items.size())
                .filter(i -> items.get(i).equals(target))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_HAS_PREVIOUS));

        // Nếu phần tử đầu tiên thì không có previous
        if (index == 0) {
            throw new AppException(ErrorCode.LESSON_NOT_HAS_PREVIOUS);
        }

        // Lấy previous item
        ItemWrapper previous = items.get(index - 1);

        return LessonOrTestAroundResponse.builder()
                .Id(previous.id())
                .type(previous.type())
                .build();
    }



    @Override
    public int completedStar(Integer lessonId, Integer userId) {
        LessonEntity lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        if(lesson.getExercises().isEmpty() && isCompletedLesson(lessonId, userId)) {
            return 3;
        }
        int tongDiemDatDuoc = lessonRepository.totalScroreOfLesson(lessonId, userId);
        int diemToiDa = exerciseRepository.countByLessonId(lessonId) * 100;
        float phanTramDiem = (float) tongDiemDatDuoc / diemToiDa * 100;
        if(phanTramDiem == 0 || tongDiemDatDuoc == 0) {
            return 0;
        } else if(phanTramDiem < 50) {
            return 1;
        } else if (phanTramDiem >= 50 && phanTramDiem < 80) {
            return 2;
        } else {
            return 3;
        }
    }
}
