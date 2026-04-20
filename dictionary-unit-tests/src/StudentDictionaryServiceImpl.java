package com.mxhieu.doantotnghiep.service.impl;

import com.mxhieu.doantotnghiep.dto.request.StudentDictionaryRequest;
import com.mxhieu.doantotnghiep.dto.response.DictionaryResponse;
import com.mxhieu.doantotnghiep.dto.response.StudentDictionaryResponse;
import com.mxhieu.doantotnghiep.entity.*;
import com.mxhieu.doantotnghiep.exception.AppException;
import com.mxhieu.doantotnghiep.exception.ErrorCode;
import com.mxhieu.doantotnghiep.repository.DefinitionExampleRepository;
import com.mxhieu.doantotnghiep.repository.StudentDictionaryRepository;
import com.mxhieu.doantotnghiep.repository.StudentProfileRepository;
import com.mxhieu.doantotnghiep.service.StudentDictionaryService;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentDictionaryServiceImpl implements StudentDictionaryService {
    private final StudentDictionaryRepository studentDictionaryRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final DefinitionExampleRepository definitionExampleRepository;

    @Override
    public void save(StudentDictionaryRequest studentDictionaryRequest) {
        StudentProfileEntity studentProfile = studentProfileRepository.findById(studentDictionaryRequest.getStudentProfileId()).orElseThrow(()-> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
        DefinitionExampleEntity definitionExample = definitionExampleRepository.findById(studentDictionaryRequest.getDefinitionExampleId()).orElseThrow(()->new AppException(ErrorCode.DEFINITION_EXAMPLE_NOT_FOUND));
        StudentDictionaryEntity studentDictionaryEntity = StudentDictionaryEntity.builder()
                .studentProfile(studentProfile)
                .definitionExample(definitionExample)
                .build();

        studentDictionaryRepository.save(studentDictionaryEntity);
    }

    @Override
    public StudentDictionaryResponse getAllForStudent(Integer studentID) {
        List<StudentDictionaryEntity> studentDictionaryEntities = studentDictionaryRepository.findByStudentProfile_Id(studentID);
        StudentDictionaryResponse response = new StudentDictionaryResponse();
        response.setStudentProfileId(studentID);

        List<DictionaryResponse> dictionaryResponses = new ArrayList<>();
        for (StudentDictionaryEntity studentDictionaryEntity : studentDictionaryEntities) {
            DefinitionExampleEntity definitionExample = studentDictionaryEntity.getDefinitionExample();
            PartOfSpeechEntity partOfSpeechEntity = definitionExample.getPartOfSpeech();
            DictionaryEntity dictionaryEntity = partOfSpeechEntity.getDictionary();
            DictionaryResponse dictionaryResponse = DictionaryResponse.builder()
                    .word(dictionaryEntity.getWord())
                    .partOfSpeechString(partOfSpeechEntity.getPartOfSpeech())
                    .ipa(partOfSpeechEntity.getIpa())
                    .audio(partOfSpeechEntity.getAudio())
                    .definition(definitionExample.getDefinition())
                    .example(definitionExample.getExample())
                    .build();
            dictionaryResponses.add(dictionaryResponse);
        }
        response.setDictionaries(dictionaryResponses);
        return response;
    }
}
