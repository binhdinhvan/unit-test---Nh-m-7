package com.mxhieu.doantotnghiep.service.impl;

import com.mxhieu.doantotnghiep.converter.AttemptConverter;
import com.mxhieu.doantotnghiep.dto.request.AttemptRequest;
import com.mxhieu.doantotnghiep.dto.request.AttemptanswerRequest;
import com.mxhieu.doantotnghiep.entity.AttemptAnswerEntity;
import com.mxhieu.doantotnghiep.entity.AttemptEntity;
import com.mxhieu.doantotnghiep.entity.ChoiceEntity;
import com.mxhieu.doantotnghiep.entity.StudentProfileEntity;
import com.mxhieu.doantotnghiep.exception.AppException;
import com.mxhieu.doantotnghiep.exception.ErrorCode;
import com.mxhieu.doantotnghiep.repository.*;
import com.mxhieu.doantotnghiep.service.AttemptSevice;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttemptSeviceImpl implements AttemptSevice {

    private final AttemptRepository attemptRepository;
    private final AttemptConverter attemptConverter;
    private final QuestionRepository questionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final ChoiceRepository choiceRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ExerciseRepository exerciseRepository;
    private final ModelMapper modelMapper;
    @Override
    public void saveAttempt(AttemptRequest attemptRequest) {

        AttemptEntity attemptEntity = new AttemptEntity();
        modelMapper.map(attemptRequest, attemptEntity);

        attemptEntity.setStartedAt(LocalDateTime.now());

        // 5. Tính điểm
        attemptEntity.setScorePercent(tinhDiem(attemptRequest.getAttemptanswerRequests()));
        attemptEntity.setSubmittedAt(LocalDateTime.now());

        // 6. Gán Student
        StudentProfileEntity student = studentProfileRepository
                .findById(attemptRequest.getStudentProfileId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
        attemptEntity.setStudentProfile(student);

        // 7. Gán Exercise
        attemptEntity.setExercise(
                exerciseRepository.findById(attemptRequest.getExerciseId())
                        .orElseThrow(() -> new AppException(ErrorCode.EXERCISE_NOT_FOUND))
        );

        // 8. Tạo answer mới
        List<AttemptAnswerEntity> answers = new ArrayList<>();

        for (AttemptanswerRequest req : attemptRequest.getAttemptanswerRequests()) {
            ChoiceEntity choice = choiceRepository.findById(req.getChoiceId()).orElseThrow(() -> new AppException(ErrorCode.CHOICE_NOT_FOUND));
            AttemptAnswerEntity answer = AttemptAnswerEntity.builder()
                    .question(questionRepository.findById(req.getQuestionId()).orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND)))
                    .choice(choice)
                    .isCorrect(choice.getIsCorrect())
                    .attempt(attemptEntity)        // PHẢI CÓ DÒNG NÀY
                    .build();

            answers.add(answer);
        }

        attemptEntity.setAttemptAnswers(answers);

        // 9. Lưu
        attemptRepository.save(attemptEntity);
    }



    private Integer tinhDiem(List<AttemptanswerRequest> attemptanswerRequests) {
        int soCauDung = 0;
        for (AttemptanswerRequest attemptanswerRequest : attemptanswerRequests) {
            Integer questionId = attemptanswerRequest.getQuestionId();
            Integer choiceId = attemptanswerRequest.getChoiceId();
            ChoiceEntity choice = choiceRepository.findById(choiceId).orElseThrow(() -> new AppException(ErrorCode.CHOICE_NOT_FOUND));
            if (choice.getIsCorrect()) {
                soCauDung++;
            }
        }
        int diem = (soCauDung * 100) / attemptanswerRequests.size();
        return diem;
    }
}
