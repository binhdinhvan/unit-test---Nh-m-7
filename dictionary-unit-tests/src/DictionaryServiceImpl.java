package com.mxhieu.doantotnghiep.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mxhieu.doantotnghiep.dto.response.DefinitionAndExampleResponse;
import com.mxhieu.doantotnghiep.dto.response.DictionaryResponse;
import com.mxhieu.doantotnghiep.dto.response.PartOfSpeechResponse;
import com.mxhieu.doantotnghiep.entity.DefinitionExampleEntity;
import com.mxhieu.doantotnghiep.entity.DictionaryEntity;
import com.mxhieu.doantotnghiep.entity.PartOfSpeechEntity;
import com.mxhieu.doantotnghiep.repository.DefinitionExampleRepository;
import com.mxhieu.doantotnghiep.repository.DictionaryRepository;
import com.mxhieu.doantotnghiep.repository.PartOfSpeechRepository;
import com.mxhieu.doantotnghiep.repository.StudentDictionaryRepository;
import com.mxhieu.doantotnghiep.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.*;
@Service
@RequiredArgsConstructor
public class DictionaryServiceImpl implements DictionaryService {
    private final DictionaryRepository dictionaryRepository;
    private final PartOfSpeechRepository partOfSpeechRepository;
    private final DefinitionExampleRepository definitionExampleRepository;
    private final StudentDictionaryRepository studentDictionaryRepository;

    // API key của Merriam-Webster (lấy từ application.properties)
    @Value("${merriam.api-key}")
    private String apiKey;

    // Base URL của Learner's Dictionary API
    private static final String BASE_URL =
            "https://www.dictionaryapi.com/api/v3/references/learners/json/";

    // WebClient để gọi HTTP
    private final WebClient.Builder webClientBuilder;

    // ObjectMapper để parse JSON
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DictionaryResponse search(String word, Integer studentId) {
        Optional<DictionaryEntity> existing =
                dictionaryRepository.findByWord(word);
        if (existing.isPresent()) {
            return mapEntityToResponse(existing.get(), studentId);
        }
        // Tạo URL gọi API, ví dụ:
        // https://www.dictionaryapi.com/api/v3/references/learners/json/afraid?key=xxx
        String url = BASE_URL + word + "?key=" + apiKey;

        // Gọi API Merriam-Webster và lấy raw JSON (dạng String)
        String rawJson = webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            // Parse JSON string thành JsonNode
            // root là một mảng các entry Merriam
            JsonNode root = objectMapper.readTree(rawJson);

            // Map để gom dữ liệu theo part of speech
            // key: noun / verb / adjective
            // value: PartOfSpeechResponse
            Map<String, PartOfSpeechResponse> posMap = new LinkedHashMap<>();

            // 🔑 Dùng để fallback IPA / audio
            // (khi adjective không có audio thì lấy của noun trước đó)
            String lastIpa = null;
            String lastAudio = null;

            // Duyệt từng entry Merriam trả về
            for (JsonNode entry : root) {

                // meta.id có thể là:
                // - afraid
                // - record:1, record:2
                // - record player (sẽ bị loại)
                String metaId = entry.path("meta").path("id").asText();

                // Chỉ giữ:
                // - meta.id == word (afraid)
                // - meta.id bắt đầu bằng word: (record:1, record:2)
                if (!metaId.equals(word) && !metaId.startsWith(word + ":")) {
                    continue;
                }

                // Lấy từ loại (noun / verb / adjective)
                String partOfSpeech = entry.path("fl").asText();
                if (partOfSpeech == null || partOfSpeech.isBlank()) continue;

                // Nếu POS chưa tồn tại trong map thì tạo mới
                posMap.putIfAbsent(
                        partOfSpeech,
                        PartOfSpeechResponse.builder()
                                .partOfSpeech(partOfSpeech)
                                .senses(new ArrayList<>()) // danh sách nghĩa
                                .build()
                );

                // Lấy object POS hiện tại
                PartOfSpeechResponse pos = posMap.get(partOfSpeech);

                // --- LẤY IPA & AUDIO ---
                String ipa = null;
                String audio = null;

                // hwi.prs chứa thông tin phát âm
                JsonNode prs = entry.path("hwi").path("prs");
                if (prs.isArray() && prs.size() > 0) {

                    // Lấy IPA đầu tiên
                    ipa = prs.get(0).path("ipa").asText(null);

                    // Lấy audio id (có thể null)
                    String audioId = prs.get(0)
                            .path("sound")
                            .path("audio")
                            .asText(null);

                    if (audioId != null) {
                        audio = buildAudioUrl(audioId);
                    }
                }

                // Nếu POS này có IPA → set và lưu làm fallback
                if (ipa != null) {
                    pos.setIpa(ipa);
                    lastIpa = ipa;
                }
                // Nếu POS này không có IPA → dùng IPA của POS trước
                else if (pos.getIpa() == null) {
                    pos.setIpa(lastIpa);
                }

                // Nếu POS này có audio → set và lưu làm fallback
                if (audio != null) {
                    pos.setAudio(audio);
                    lastAudio = audio;
                }
                // Nếu không có audio → dùng audio của POS trước
                else if (pos.getAudio() == null) {
                    pos.setAudio(lastAudio);
                }

                // --- PARSE NGHĨA ---
                JsonNode defArray = entry.path("def");
                if (!defArray.isArray() || defArray.isEmpty()) continue;

                // sseq là cấu trúc chứa các sense (nghĩa)
                JsonNode sseq = defArray.get(0).path("sseq");

                // sseq là mảng 2 tầng → cần duyệt 2 vòng
                for (JsonNode senseGroup : sseq) {
                    for (JsonNode senseNode : senseGroup) {

                        // sense thật nằm ở index 1
                        JsonNode sense = senseNode.get(1);
                        if (sense == null) continue;

                        String definition = null;
                        String example = null;

                        // dt chứa definition, example, note...
                        for (JsonNode dt : sense.path("dt")) {

                            // ❗ BỎ QUA nếu dt không phải array hoặc thiếu phần tử
                            if (!dt.isArray() || dt.size() < 2 || dt.get(0) == null) {
                                continue;
                            }

                            String type = dt.get(0).asText();

                            // ===== CASE 1: text trực tiếp =====
                            if ("text".equals(type) && definition == null) {
                                definition = cleanText(dt.get(1).asText());
                            }

                            // ===== CASE 2: uns (usage note) =====
                            else if ("uns".equals(type) && definition == null && dt.get(1).isArray()) {

                                JsonNode unsArray = dt.get(1);

                                if (unsArray.size() > 0) {
                                    JsonNode firstGroup = unsArray.get(0);

                                    if (firstGroup.isArray() && firstGroup.size() > 0) {
                                        JsonNode firstItem = firstGroup.get(0);

                                        if (firstItem.isArray() && firstItem.size() > 1) {
                                            String innerType = firstItem.get(0).asText();

                                            if ("text".equals(innerType)) {
                                                definition = cleanText(firstItem.get(1).asText());
                                            }
                                        }
                                    }
                                }
                            }
                            // ===== CASE 3: ví dụ =====
                            else if ("vis".equals(type) && example == null && dt.get(1).isArray()) {
                                example = cleanText(
                                        dt.get(1).get(0).path("t").asText()
                                );
                            }
                        }

                        // Loại các nghĩa không mong muốn (see also, synonym, ...)
                        if (definition != null && !isInvalidDefinition(definition)) {
                            pos.getSenses().add(
                                    DefinitionAndExampleResponse.builder()
                                            .definition(definition)
                                            .example(example)
                                            .build()
                            );
                        }
                    }
                }
            }

            saveToDataBase(word,posMap);
            Optional<DictionaryEntity> exist =
                    dictionaryRepository.findByWord(word);

            if (exist.isPresent()) {
                return mapEntityToResponse(exist.get(), studentId);
            }
            // Build response cuối cùng
            return DictionaryResponse.builder()
                    .word(word)
                    .partsOfSpeech(new ArrayList<>(posMap.values()))
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to parse dictionary response", e);
        }
    }

    @Override
    public List<String> getSuggestionWord(String word) {
        List<DictionaryEntity> dictionaryEntityList = dictionaryRepository.findTop10ByWordContainingIgnoreCase(word);
        List<String> listResponse = dictionaryEntityList.stream().map(DictionaryEntity::getWord).toList();
        return listResponse;
    }

    private DictionaryResponse mapEntityToResponse(DictionaryEntity dictionaryEntity, Integer studentId) {

        // Danh sách PartOfSpeechResponse trả về
        List<PartOfSpeechResponse> partsOfSpeech = new ArrayList<>();

        // Duyệt từng PartOfSpeechEntity (noun / verb / adjective)
        for (PartOfSpeechEntity posEntity : dictionaryEntity.getPartOfSpeech()) {

            // Danh sách nghĩa của POS
            List<DefinitionAndExampleResponse> senses = new ArrayList<>();

            // Duyệt từng DefinitionExampleEntity
            for (DefinitionExampleEntity defEntity : posEntity.getDefinitionExample()) {
                Boolean exist = studentDictionaryRepository.existsByStudentProfile_IdAndDefinitionExample_Id(studentId, defEntity.getId());
                senses.add(
                        DefinitionAndExampleResponse.builder()
                                .id(defEntity.getId())
                                .definition(defEntity.getDefinition())
                                .example(defEntity.getExample())
                                .saved(exist)
                                .build()
                );
            }

            // Tạo PartOfSpeechResponse
            PartOfSpeechResponse posResponse =
                    PartOfSpeechResponse.builder()
                            .id(posEntity.getId())
                            .partOfSpeech(posEntity.getPartOfSpeech())
                            .ipa(posEntity.getIpa())
                            .audio(posEntity.getAudio())
                            .senses(senses)
                            .build();

            partsOfSpeech.add(posResponse);
        }

        // Build DictionaryResponse cuối cùng
        return DictionaryResponse.builder()
                .id(dictionaryEntity.getId())
                .word(dictionaryEntity.getWord())
                .partsOfSpeech(partsOfSpeech)
                .build();
    }


    private void saveToDataBase(String word, Map<String, PartOfSpeechResponse> posMap) {
        // ===== SAVE TO DATABASE =====

// 1️⃣ Tạo DictionaryEntity
        DictionaryEntity dictionaryEntity = DictionaryEntity.builder()
                .word(word)
                .partOfSpeech(new ArrayList<>())
                .build();

// 2️⃣ Map PartOfSpeechResponse → PartOfSpeechEntity
        for (PartOfSpeechResponse posResponse : posMap.values()) {

            PartOfSpeechEntity posEntity = PartOfSpeechEntity.builder()
                    .partOfSpeech(posResponse.getPartOfSpeech())
                    .ipa(posResponse.getIpa())
                    .audio(posResponse.getAudio())
                    .dictionary(dictionaryEntity)
                    .definitionExample(new ArrayList<>())
                    .build();

            // 3️⃣ Map Definition → Entity
            for (DefinitionAndExampleResponse sense : posResponse.getSenses()) {

                DefinitionExampleEntity defEntity =
                        DefinitionExampleEntity.builder()
                                .definition(sense.getDefinition())
                                .example(sense.getExample())
                                .partOfSpeech(posEntity)
                                .build();

                posEntity.getDefinitionExample().add(defEntity);
            }

            dictionaryEntity.getPartOfSpeech().add(posEntity);
        }

// 4️⃣ Save CHA (cascade sẽ save toàn bộ con)
        dictionaryRepository.save(dictionaryEntity);
    }

    // Loại các nghĩa chỉ mang tính tham chiếu (see also, synonym)
    private boolean isInvalidDefinition(String definition) {
        return definition.contains("{dx}")
                || definition.contains("{dxt|")
                || definition.contains("{sx|");
    }

    // Chuẩn hóa text Merriam (xoá markup)
    private String cleanText(String text) {
        return text
                .replace("{bc}", "")
                .replaceAll("\\{it\\}|\\{/it\\}", "")
                .replaceAll("\\{phrase\\}|\\{/phrase\\}", "")
                .replaceAll("\\[=.*?\\]", "") // xoá [= explanation]
                .trim();
    }

    // Build URL audio mp3 theo quy tắc của Merriam
    private String buildAudioUrl(String audio) {
        String folder;
        if (audio.startsWith("bix")) folder = "bix";
        else if (audio.startsWith("gg")) folder = "gg";
        else folder = audio.substring(0, 1);

        return "https://media.merriam-webster.com/audio/prons/en/us/mp3/"
                + folder + "/" + audio + ".mp3";
    }
}
