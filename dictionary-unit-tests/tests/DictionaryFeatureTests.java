package com.mxhieu.doantotnghiep;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mxhieu.doantotnghiep.dto.request.StudentDictionaryRequest;
import com.mxhieu.doantotnghiep.dto.response.DictionaryResponse;
import com.mxhieu.doantotnghiep.dto.response.StudentDictionaryResponse;
import com.mxhieu.doantotnghiep.entity.*;
import com.mxhieu.doantotnghiep.exception.AppException;
import com.mxhieu.doantotnghiep.exception.ErrorCode;
import com.mxhieu.doantotnghiep.repository.DefinitionExampleRepository;
import com.mxhieu.doantotnghiep.repository.DictionaryRepository;
import com.mxhieu.doantotnghiep.repository.PartOfSpeechRepository;
import com.mxhieu.doantotnghiep.repository.StudentDictionaryRepository;
import com.mxhieu.doantotnghiep.repository.StudentProfileRepository;
import com.mxhieu.doantotnghiep.service.impl.DictionaryServiceImpl;
import com.mxhieu.doantotnghiep.service.impl.StudentDictionaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dictionary Feature Unit Tests")
public class DictionaryFeatureTests {

    @Nested
    @DisplayName("Tra tÃƒÂ¡Ã‚Â»Ã‚Â« (DictionaryServiceImpl.search())")
    class DictionaryServiceInnerTests {
        @Mock
        private WebClient.Builder webClientBuilder;
        @Mock
        private WebClient webClient;
        @Mock
        private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
        @Mock
        private WebClient.RequestHeadersSpec requestHeadersSpec;
        @Mock
        private WebClient.ResponseSpec responseSpec;

        @Mock
        private DictionaryRepository dictionaryRepository;
        @Mock
        private StudentDictionaryRepository studentDictionaryRepository;
        @Mock
        private DefinitionExampleRepository definitionExampleRepository;
        @Mock
        private PartOfSpeechRepository partOfSpeechRepository;

        @InjectMocks
        private DictionaryServiceImpl dictionaryService;

        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
            objectMapper = new ObjectMapper();
            ReflectionTestUtils.setField(dictionaryService, "objectMapper", objectMapper);
            ReflectionTestUtils.setField(dictionaryService, "apiKey", "test-api-key");
        }

        private void mockWebClient(String jsonResponse) {
            when(webClientBuilder.build()).thenReturn(webClient);
            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jsonResponse));
        }

        private void mockWebClientError(Exception ex) {
            when(webClientBuilder.build()).thenReturn(webClient);
            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(ex));
        }        /**
     * DICT_001_TC: Tra tÃƒÂ¡Ã‚Â»Ã‚Â« Ãƒâ€žÃ¢â‚¬ËœÃƒÆ’Ã‚Â£ cÃƒÆ’Ã‚Â³ sÃƒÂ¡Ã‚ÂºÃ‚Âµn trong cÃƒâ€ Ã‚Â¡ sÃƒÂ¡Ã‚Â»Ã…Â¸ dÃƒÂ¡Ã‚Â»Ã‚Â¯ liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u (Happy Path)
     * 
     * Test Objective: Đảm bảo không gọi WebClient khi từ đã tồn tại trong DB
     * Input: word="hello", DB đã có bản ghi đầy đủ PartOfSpeech và DefinitionExample
     * Expected Output: Trả về DictionaryResponse, verify webClientBuilder.build() không được gọi
     * Notes: Happy path - cache hit
     */
    @Test
    @DisplayName("DICT_001_TC: Tra tÃƒÂ¡Ã‚Â»Ã‚Â« Ãƒâ€žÃ¢â‚¬ËœÃƒÆ’Ã‚Â£ cÃƒÆ’Ã‚Â³ sÃƒÂ¡Ã‚ÂºÃ‚Âµn trong cÃƒâ€ Ã‚Â¡ sÃƒÂ¡Ã‚Â»Ã…Â¸ dÃƒÂ¡Ã‚Â»Ã‚Â¯ liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u (Happy Path)")
        void DICT_001_TC_testSearch_ExistInDB() {
            String word = "hello";
            Integer studentId = 1;

            DictionaryEntity dictEntity = DictionaryEntity.builder().id(10).word(word).partOfSpeech(new ArrayList<>()).build();
            PartOfSpeechEntity posEntity = PartOfSpeechEntity.builder().id(20).partOfSpeech("noun").ipa("hÃƒâ€°Ã¢â€žÂ¢Ãƒâ€¹Ã‹â€ lÃƒâ€¦Ã‚Â").audio("hello").dictionary(dictEntity).definitionExample(new ArrayList<>()).build();
            DefinitionExampleEntity defEntity = DefinitionExampleEntity.builder().id(30).definition("a greeting").example("hello world").partOfSpeech(posEntity).build();
            
            posEntity.getDefinitionExample().add(defEntity);
            dictEntity.getPartOfSpeech().add(posEntity);

            when(dictionaryRepository.findByWord(word)).thenReturn(Optional.of(dictEntity));
            when(studentDictionaryRepository.existsByStudentProfile_IdAndDefinitionExample_Id(studentId, 30)).thenReturn(true);

            DictionaryResponse res = dictionaryService.search(word, studentId);

            assertNotNull(res);
            assertEquals(word, res.getWord());
            assertEquals(1, res.getPartsOfSpeech().size());
            assertEquals("noun", res.getPartsOfSpeech().get(0).getPartOfSpeech());
            assertTrue(res.getPartsOfSpeech().get(0).getSenses().get(0).getSaved());

            verify(webClientBuilder, never()).build();
        }        /**
     * DICT_002_TC: Tra tÃƒÂ¡Ã‚Â»Ã‚Â« chÃƒâ€ Ã‚Â°a cÃƒÆ’Ã‚Â³ trong DB, gÃƒÂ¡Ã‚Â»Ã‚Âi API dictionary thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng vÃƒÆ’Ã‚Â  luu DB
     * 
     * Test Objective: Đảm bảo gọi API khi từ chưa có trong DB và lưu kết quả vào dictionaryRepository
     * Input: word="test", Mock WebClient trả JSON hợp lệ
     * Expected Output: Trả về DictionaryResponse, gọi dictionaryRepository.save() đúng 1 lần
     * Notes: Happy path - cache miss
     */
    @Test
    @DisplayName("DICT_002_TC: Tra tÃƒÂ¡Ã‚Â»Ã‚Â« chÃƒâ€ Ã‚Â°a cÃƒÆ’Ã‚Â³ trong DB, gÃƒÂ¡Ã‚Â»Ã‚Âi API dictionary thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng vÃƒÆ’Ã‚Â  luu DB")
        void DICT_002_TC_testSearch_CallApiAndSave() {
            String word = "test";
            Integer studentId = 1;

            when(dictionaryRepository.findByWord(word)).thenReturn(Optional.empty());

            String apiResponseJson = "[{\"meta\":{\"id\":\"test\"},\"fl\":\"noun\",\"hwi\":{\"hw\":\"test\",\"prs\":[{\"mw\":\"Ãƒâ€¹Ã‹â€ test\",\"sound\":{\"audio\":\"test0001\"}}]},\"def\":[{\"sseq\":[[[\"sense\",{\"dt\":[[\"text\",\"{bc}a formal examination\"]]}]]]}]}]";
            mockWebClient(apiResponseJson);

            DictionaryEntity savedEntity = DictionaryEntity.builder().id(100).word(word).partOfSpeech(new ArrayList<>()).build();
            when(dictionaryRepository.save(any(DictionaryEntity.class))).thenAnswer(invocation -> {
                savedEntity.getPartOfSpeech().addAll(((DictionaryEntity) invocation.getArgument(0)).getPartOfSpeech());
                return savedEntity;
            });
            when(dictionaryRepository.findByWord(word)).thenReturn(Optional.empty()).thenReturn(Optional.of(savedEntity));

            DictionaryResponse res = dictionaryService.search(word, studentId);

            assertNotNull(res);
            assertEquals(word, res.getWord());
            
            ArgumentCaptor<DictionaryEntity> captor = ArgumentCaptor.forClass(DictionaryEntity.class);
            verify(dictionaryRepository).save(captor.capture());
            DictionaryEntity saved = captor.getValue();
            assertEquals(word, saved.getWord());
            assertEquals(1, saved.getPartOfSpeech().size());
            assertEquals("noun", saved.getPartOfSpeech().get(0).getPartOfSpeech());
            assertEquals("a formal examination", saved.getPartOfSpeech().get(0).getDefinitionExample().get(0).getDefinition());
        }        /**
     * DICT_003_TC: DÃƒÂ¡Ã‚Â»Ã‚Ân dÃƒÂ¡Ã‚ÂºÃ‚Â¹p vÃƒâ€žÃ†â€™n bÃƒÂ¡Ã‚ÂºÃ‚Â£n chÃƒÂ¡Ã‚Â»Ã‚Â©a markup tÃƒÂ¡Ã‚Â»Ã‚Â« API (cleanText)
     * 
     * Test Objective: Dọn dẹp đánh dấu {bc} {it} {phrase} trước khi lưu definition
     * Input: Json chứa chuỗi "{bc}to {it}test{/it} [=evaluate] {phrase}something{/phrase}"
     * Expected Output: Chuỗi sau clean là "to test  something", không còn markup
     * Notes: Format Validator
     */
    @Test
    @DisplayName("DICT_003_TC: DÃƒÂ¡Ã‚Â»Ã‚Ân dÃƒÂ¡Ã‚ÂºÃ‚Â¹p vÃƒâ€žÃ†â€™n bÃƒÂ¡Ã‚ÂºÃ‚Â£n chÃƒÂ¡Ã‚Â»Ã‚Â©a markup tÃƒÂ¡Ã‚Â»Ã‚Â« API (cleanText)")
        void DICT_003_TC_testSearch_CleanText() {
            String word = "markup";
            Integer studentId = 1;
            when(dictionaryRepository.findByWord(word)).thenReturn(Optional.empty());

            String apiResponseJson = "[{\"meta\":{\"id\":\"markup\"},\"fl\":\"verb\",\"def\":[{\"sseq\":[[[\"sense\",{\"dt\":[[\"text\",\"{bc}to {it}test{/it} [=evaluate] {phrase}something{/phrase}\"]]}]]]}]}]";
            mockWebClient(apiResponseJson);

            DictionaryEntity savedEntity = DictionaryEntity.builder().id(100).word(word).partOfSpeech(new ArrayList<>()).build();
            when(dictionaryRepository.findByWord(word)).thenReturn(Optional.empty()).thenReturn(Optional.of(savedEntity));

            dictionaryService.search(word, studentId);

            ArgumentCaptor<DictionaryEntity> captor = ArgumentCaptor.forClass(DictionaryEntity.class);
            verify(dictionaryRepository).save(captor.capture());
            DictionaryEntity saved = captor.getValue();
            assertEquals("to test  something", saved.getPartOfSpeech().get(0).getDefinitionExample().get(0).getDefinition());
        }        /**
     * DICT_004_TC: GÃƒÂ¡Ã‚Â»Ã‚Âi API trÃƒÂ¡Ã‚ÂºÃ‚Â£ vÃƒÂ¡Ã‚Â»Ã‚Â lÃƒÂ¡Ã‚Â»Ã¢â‚¬â€i Exception (RuntimeException)
     * 
     * Test Objective: Đảm bảo lỗi từ WebClient được ném ngược ra caller
     * Input: WebClient Mono.error(RuntimeException("API Timeout"))
     * Expected Output: Hệ thống ném ra RuntimeException với message "API Timeout"
     * Notes: API Recovery
     */
    @Test
    @DisplayName("DICT_004_TC: GÃƒÂ¡Ã‚Â»Ã‚Âi API trÃƒÂ¡Ã‚ÂºÃ‚Â£ vÃƒÂ¡Ã‚Â»Ã‚Â lÃƒÂ¡Ã‚Â»Ã¢â‚¬â€i Exception (RuntimeException)")
        void DICT_004_TC_testSearch_ApiThrowsException() {
            String word = "error";
            Integer studentId = 1;

            when(dictionaryRepository.findByWord(word)).thenReturn(Optional.empty());
            mockWebClientError(new RuntimeException("API Timeout"));

            RuntimeException ex = assertThrows(RuntimeException.class, () -> dictionaryService.search(word, studentId));
            assertTrue(ex.getMessage().contains("API Timeout"));
        }        /**
     * DICT_005_TC: API trÃƒÂ¡Ã‚ÂºÃ‚Â£ vÃƒÂ¡Ã‚Â»Ã‚Â ÃƒÆ’Ã‚Â¢m thanh bÃƒÂ¡Ã‚ÂºÃ‚Â¯t Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚ÂºÃ‚Â§u bix vÃƒÆ’Ã‚Â  gg Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã†â€™ test buildAudioUrl
     * 
     * Test Objective: Đảm bảo buildAudioUrl dùng folder /bix/ và /gg/ thay vì ký tự đầu
     * Input: json "audio":"bix123" và "audio":"gg456" trong 2 PartOfSpeech
     * Expected Output: PartOfSpeech[0].audio=".../bix/bix123.mp3", PartOfSpeech[1].audio=".../gg/gg456.mp3"
     * Notes: Edge case - Audio URL logic
     */
    @Test
    @DisplayName("DICT_005_TC: API trÃƒÂ¡Ã‚ÂºÃ‚Â£ vÃƒÂ¡Ã‚Â»Ã‚Â ÃƒÆ’Ã‚Â¢m thanh bÃƒÂ¡Ã‚ÂºÃ‚Â¯t Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚ÂºÃ‚Â§u bix vÃƒÆ’Ã‚Â  gg Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã†â€™ test buildAudioUrl")
        void DICT_005_TC_testSearch_BuildAudioUrlBixGg() {
            String word = "bixgg";
            Integer studentId = 1;
            when(dictionaryRepository.findByWord(word)).thenReturn(Optional.empty());

            String apiResponseJson = "[{\"meta\":{\"id\":\"bixgg:1\"},\"fl\":\"noun\",\"hwi\":{\"prs\":[{\"sound\":{\"audio\":\"bix123\"}}]},\"def\":[{\"sseq\":[[[\"sense\",{\"dt\":[[\"text\",\"{bc}def1\"]]}]]]}]}," +
                                     "{\"meta\":{\"id\":\"bixgg:2\"},\"fl\":\"verb\",\"hwi\":{\"prs\":[{\"sound\":{\"audio\":\"gg456\"}}]},\"def\":[{\"sseq\":[[[\"sense\",{\"dt\":[[\"text\",\"{bc}def2\"]]}]]]}]}]";
            mockWebClient(apiResponseJson);
            
            DictionaryEntity savedEntity = DictionaryEntity.builder().id(100).word(word).partOfSpeech(new ArrayList<>()).build();
            when(dictionaryRepository.findByWord(word)).thenReturn(Optional.empty()).thenReturn(Optional.of(savedEntity));

            dictionaryService.search(word, studentId);

            ArgumentCaptor<DictionaryEntity> captor = ArgumentCaptor.forClass(DictionaryEntity.class);
            verify(dictionaryRepository).save(captor.capture());
            DictionaryEntity saved = captor.getValue();
            assertEquals(2, saved.getPartOfSpeech().size());
            assertEquals("https://media.merriam-webster.com/audio/prons/en/us/mp3/bix/bix123.mp3", saved.getPartOfSpeech().get(0).getAudio());
            assertEquals("https://media.merriam-webster.com/audio/prons/en/us/mp3/gg/gg456.mp3", saved.getPartOfSpeech().get(1).getAudio());
        }        /**
     * DICT_006_TC: LÃƒÂ¡Ã‚ÂºÃ‚Â¥y gÃƒÂ¡Ã‚Â»Ã‚Â£i ÃƒÆ’Ã‚Â½ tÃƒÂ¡Ã‚Â»Ã‚Â« vÃƒÂ¡Ã‚Â»Ã‚Â±ng getSuggestionWord gÃƒÂ¡Ã‚Â»Ã‚Âi Repository
     * 
     * Test Objective: Tìm kiếm top 10 từ khớp prefix và trả về danh sách chuỗi
     * Input: word="app", Mock findTop10ByWordContainingIgnoreCase trả ["apple","application"]
     * Expected Output: Trả về List<String> đúng thứ tự ["apple", "application"]
     * Notes: Read-only
     */
    @Test
    @DisplayName("DICT_006_TC: LÃƒÂ¡Ã‚ÂºÃ‚Â¥y gÃƒÂ¡Ã‚Â»Ã‚Â£i ÃƒÆ’Ã‚Â½ tÃƒÂ¡Ã‚Â»Ã‚Â« vÃƒÂ¡Ã‚Â»Ã‚Â±ng getSuggestionWord gÃƒÂ¡Ã‚Â»Ã‚Âi Repository")
        void DICT_006_TC_testGetSuggestionWord() {
            String word = "app";
            DictionaryEntity d1 = DictionaryEntity.builder().word("apple").build();
            DictionaryEntity d2 = DictionaryEntity.builder().word("application").build();
            when(dictionaryRepository.findTop10ByWordContainingIgnoreCase(word)).thenReturn(List.of(d1, d2));

            List<String> suggestions = dictionaryService.getSuggestionWord(word);
            assertEquals(2, suggestions.size());
            assertEquals("apple", suggestions.get(0));
            assertEquals("application", suggestions.get(1));
        }    /**
     * DICT_012_TC_testSearch_EmptyAudioThrowsIndexOutOfBounds: DICT_012_TC_testSearch_EmptyAudioThrowsIndexOutOfBounds
     * 
     * Test Objective: Kiểm thử đánh giá chất lượng hệ thống cho hành vi: DICT_012_TC_testSearch_EmptyAudioThrowsIndexOutOfBounds
     * Input: Dữ liệu không toàn vẹn từ API WebClient (Node lỗi, thiếu Data)
     * Expected Output: Phải được chặn an toàn tránh Crash
     * Notes: Phát hiện lỗ hổng Crawl Dictionary API
     */
    @Test
    @DisplayName("DICT_012_TC_testSearch_EmptyAudioThrowsIndexOutOfBounds")
        void DICT_012_TC_testSearch_EmptyAudioThrowsIndexOutOfBounds() {
            String word = "bugaudio";
            Integer studentId = 1;
            when(dictionaryRepository.findByWord(word)).thenReturn(Optional.empty());

            // API returns empty audio string
            String apiResponseJson = "[{\"meta\":{\"id\":\"bugaudio:1\"},\"fl\":\"noun\",\"hwi\":{\"prs\":[{\"sound\":{\"audio\":\"\"}}]},\"def\":[{\"sseq\":[[[\"sense\",{\"dt\":[[\"text\",\"{bc}def1\"]]}]]]}]}]";
            mockWebClient(apiResponseJson);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> dictionaryService.search(word, studentId));
            assertTrue(ex.getCause() instanceof StringIndexOutOfBoundsException, "PhÃ¡t hiá»‡n Bug: KhÃ´ng kiá»ƒm tra audio rá»—ng dáº«n Ä‘áº¿n Crash");
        }    /**
     * DICT_013_TC_testSearch_MissingTextNodeThrowsNullPointer: DICT_013_TC_testSearch_MissingTextNodeThrowsNullPointer
     * 
     * Test Objective: Kiểm thử đánh giá chất lượng hệ thống cho hành vi: DICT_013_TC_testSearch_MissingTextNodeThrowsNullPointer
     * Input: Dữ liệu không toàn vẹn từ API WebClient (Node lỗi, thiếu Data)
     * Expected Output: Phải được chặn an toàn tránh Crash
     * Notes: Phát hiện lỗ hổng Crawl Dictionary API
     */
    @Test
    @DisplayName("DICT_013_TC_testSearch_MissingTextNodeThrowsNullPointer")
        void DICT_013_TC_testSearch_MissingTextNodeThrowsNullPointer() {
            String word = "bugtext";
            Integer studentId = 1;
            when(dictionaryRepository.findByWord(word)).thenReturn(Optional.empty());

            // dt[1] is an empty array, so dt[1].get(0) returns null, causing NPE when path("t") is called
            String apiResponseJson = "[{\"meta\":{\"id\":\"bugtext\"},\"fl\":\"noun\",\"hwi\":{\"prs\":[]},\"def\":[{\"sseq\":[[[\"sense\",{\"dt\":[[\"vis\",[]]]}]]]}]}]";
            mockWebClient(apiResponseJson);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> dictionaryService.search(word, studentId));
            assertTrue(ex.getCause() instanceof NullPointerException, "PhÃ¡t hiá»‡n Bug: KhÃ´ng kiá»ƒm tra node null dáº«n Ä‘áº¿n Crash");
        }    /**
     * DICT_014_TC_testSearch_EmptyResponseSavesEmptyEntity: DICT_014_TC_testSearch_EmptyResponseSavesEmptyEntity
     * 
     * Test Objective: Kiểm thử đánh giá chất lượng hệ thống cho hành vi: DICT_014_TC_testSearch_EmptyResponseSavesEmptyEntity
     * Input: Dữ liệu không toàn vẹn từ API WebClient (Node lỗi, thiếu Data)
     * Expected Output: Phải được chặn an toàn tránh Crash
     * Notes: Phát hiện lỗ hổng Crawl Dictionary API
     */
    @Test
    @DisplayName("DICT_014_TC_testSearch_EmptyResponseSavesEmptyEntity")
        void DICT_014_TC_testSearch_EmptyResponseSavesEmptyEntity() {
            String word = "noword";
            Integer studentId = 1;

            when(dictionaryRepository.findByWord(word)).thenReturn(Optional.empty());

            String apiResponseJson = "[]";
            mockWebClient(apiResponseJson);

            DictionaryEntity savedEntity = DictionaryEntity.builder().id(99).word(word).partOfSpeech(new ArrayList<>()).build();
            when(dictionaryRepository.save(any())).thenReturn(savedEntity);
            when(dictionaryRepository.findByWord(word)).thenReturn(Optional.empty()).thenReturn(Optional.of(savedEntity));

            DictionaryResponse res = dictionaryService.search(word, studentId);
            
            assertTrue(res.getPartsOfSpeech().isEmpty(), "PhÃ¡t hiá»‡n Edge Case/Bug: LÆ°u tá»« vÃ´ nghÄ©a vÃ o Database gÃ¢y ngháº½n Cache");
        }
    }
    @Nested
    @DisplayName("TÃƒÂ¡Ã‚Â»Ã‚Â« Ãƒâ€žÃ¢â‚¬ËœiÃƒÂ¡Ã‚Â»Ã†â€™n cÃƒÆ’Ã‚Â¡ nhÃƒÆ’Ã‚Â¢n (StudentDictionaryServiceImpl.getAllForStudent())")
    class StudentDictionaryServiceInnerTests {
        @Mock
        private StudentDictionaryRepository studentDictionaryRepository;
        @Mock
        private StudentProfileRepository studentProfileRepository;
        @Mock
        private DefinitionExampleRepository definitionExampleRepository;

        @InjectMocks
        private StudentDictionaryServiceImpl studentDictionaryService;        /**
     * DICT_007_TC: LÃƒâ€ Ã‚Â°u tÃƒÂ¡Ã‚Â»Ã‚Â« vÃƒÂ¡Ã‚Â»Ã‚Â±ng cÃƒÆ’Ã‚Â¡ nhÃƒÆ’Ã‚Â¢n thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng
     * 
     * Test Objective: Đảm bảo lưu đúng quan hệ Student - DefinitionExample vào repository
     * Input: StudentProfileId=10, DefinitionExampleId=20; mocks = { findById(10)->student, findById(20)->def }
     * Expected Output: Gọi studentDictionaryRepository.save() với entity chứa đúng student và def
     * Notes: Happy Path
     */
    @Test
    @DisplayName("DICT_007_TC: LÃƒâ€ Ã‚Â°u tÃƒÂ¡Ã‚Â»Ã‚Â« vÃƒÂ¡Ã‚Â»Ã‚Â±ng cÃƒÆ’Ã‚Â¡ nhÃƒÆ’Ã‚Â¢n thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng")
        void DICT_007_TC_testSaveStudentDictionary_Success() {
            StudentDictionaryRequest request = new StudentDictionaryRequest();
            request.setStudentProfileId(10);
            request.setDefinitionExampleId(20);

            StudentProfileEntity student = new StudentProfileEntity();
            when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));

            DefinitionExampleEntity def = new DefinitionExampleEntity();
            when(definitionExampleRepository.findById(20)).thenReturn(Optional.of(def));

            studentDictionaryService.save(request);

            ArgumentCaptor<StudentDictionaryEntity> captor = ArgumentCaptor.forClass(StudentDictionaryEntity.class);
            verify(studentDictionaryRepository).save(captor.capture());
            assertEquals(student, captor.getValue().getStudentProfile());
            assertEquals(def, captor.getValue().getDefinitionExample());
        }        /**
     * DICT_008_TC: LÃƒÂ¡Ã‚Â»Ã¢â‚¬â€i bÃƒÆ’Ã‚Â¡o STUDENT_PROFILE_NOT_FOUND khi StudentProfileId khÃƒÆ’Ã‚Â´ng tÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“n tÃƒÂ¡Ã‚ÂºÃ‚Â¡i
     * 
     * Test Objective: Đảm bảo ném đúng domain exception khi học sinh không tồn tại
     * Input: StudentProfileId=999; mocks = { findById(999)->Optional.empty() }
     * Expected Output: Ném AppException với errorCode=STUDENT_PROFILE_NOT_FOUND
     * Notes: Validation
     */
    @Test
    @DisplayName("DICT_008_TC: LÃƒÂ¡Ã‚Â»Ã¢â‚¬â€i bÃƒÆ’Ã‚Â¡o STUDENT_PROFILE_NOT_FOUND khi StudentProfileId khÃƒÆ’Ã‚Â´ng tÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“n tÃƒÂ¡Ã‚ÂºÃ‚Â¡i")
        void DICT_008_TC_testSaveStudentDictionary_StudentNotFound() {
            StudentDictionaryRequest request = new StudentDictionaryRequest();
            request.setStudentProfileId(999);
            when(studentProfileRepository.findById(999)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> studentDictionaryService.save(request));
            assertEquals(ErrorCode.STUDENT_PROFILE_NOT_FOUND, ex.getErrorCode());
        }        /**
     * DICT_009_TC: LÃƒÂ¡Ã‚Â»Ã¢â‚¬â€i bÃƒÆ’Ã‚Â¡o DEFINITION_EXAMPLE_NOT_FOUND khi id khÃƒÆ’Ã‚Â´ng tÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“n tÃƒÂ¡Ã‚ÂºÃ‚Â¡i
     * 
     * Test Objective: Đảm bảo ném đúng domain exception khi definition không tồn tại
     * Input: StudentProfileId=10, DefinitionExampleId=999; mocks = { findById(999)->Optional.empty() }
     * Expected Output: Ném AppException với errorCode=DEFINITION_EXAMPLE_NOT_FOUND
     * Notes: Validation
     */
    @Test
    @DisplayName("DICT_009_TC: LÃƒÂ¡Ã‚Â»Ã¢â‚¬â€i bÃƒÆ’Ã‚Â¡o DEFINITION_EXAMPLE_NOT_FOUND khi id khÃƒÆ’Ã‚Â´ng tÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“n tÃƒÂ¡Ã‚ÂºÃ‚Â¡i")
        void DICT_009_TC_testSaveStudentDictionary_DefinitionNotFound() {
            StudentDictionaryRequest request = new StudentDictionaryRequest();
            request.setStudentProfileId(10);
            request.setDefinitionExampleId(999);

            when(studentProfileRepository.findById(10)).thenReturn(Optional.of(new StudentProfileEntity()));
            when(definitionExampleRepository.findById(999)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> studentDictionaryService.save(request));
            assertEquals(ErrorCode.DEFINITION_EXAMPLE_NOT_FOUND, ex.getErrorCode());
        }        /**
     * DICT_010_TC: CÃƒÂ¡Ã‚ÂºÃ‚Â¥p dÃƒÂ¡Ã‚Â»Ã‚Â¯ liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u Flashcard - LÃƒÂ¡Ã‚ÂºÃ‚Â¥y toÃƒÆ’Ã‚Â n bÃƒÂ¡Ã‚Â»Ã¢â€žÂ¢ tÃƒÂ¡Ã‚Â»Ã‚Â« cÃƒÆ’Ã‚Â¡ nhÃƒÆ’Ã‚Â¢n thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng
     * 
     * Test Objective: Đảm bảo map đúng dữ liệu nested StudentDictionary -> DictionaryResponse
     * Input: studentID=10, DB có 1 từ "cat" với đầy đủ ipa, audio, definition
     * Expected Output: Trả về StudentDictionaryResponse với 1 DictionaryResponse có word=cat, ipa=kat, definition đúng
     * Notes: Flashcard Data provider
     */
    @Test
    @DisplayName("DICT_010_TC: CÃƒÂ¡Ã‚ÂºÃ‚Â¥p dÃƒÂ¡Ã‚Â»Ã‚Â¯ liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u Flashcard - LÃƒÂ¡Ã‚ÂºÃ‚Â¥y toÃƒÆ’Ã‚Â n bÃƒÂ¡Ã‚Â»Ã¢â€žÂ¢ tÃƒÂ¡Ã‚Â»Ã‚Â« cÃƒÆ’Ã‚Â¡ nhÃƒÆ’Ã‚Â¢n thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng")
        void DICT_010_TC_testGetAllForStudent_Success() {
            Integer studentId = 10;
            
            DictionaryEntity dictEntity = DictionaryEntity.builder().word("cat").build();
            PartOfSpeechEntity posEntity = PartOfSpeechEntity.builder().partOfSpeech("noun").ipa("kat").audio("cat_audio").dictionary(dictEntity).build();
            DefinitionExampleEntity defEntity = DefinitionExampleEntity.builder().definition("A small feli").example("My cat").partOfSpeech(posEntity).build();
            
            StudentDictionaryEntity stdDict = StudentDictionaryEntity.builder().id(100).definitionExample(defEntity).build();

            when(studentDictionaryRepository.findByStudentProfile_Id(studentId)).thenReturn(List.of(stdDict));

            StudentDictionaryResponse response = studentDictionaryService.getAllForStudent(studentId);

            assertNotNull(response);
            assertEquals(studentId, response.getStudentProfileId());
            assertEquals(1, response.getDictionaries().size());
            DictionaryResponse dr = response.getDictionaries().get(0);
            assertEquals("cat", dr.getWord());
            assertEquals("noun", dr.getPartOfSpeechString());
            assertEquals("A small feli", dr.getDefinition());
            assertEquals("kat", dr.getIpa());
        }        /**
     * DICT_011_TC: CÃƒÂ¡Ã‚ÂºÃ‚Â¥p dÃƒÂ¡Ã‚Â»Ã‚Â¯ liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u Flashcard - TrÃƒÂ¡Ã‚ÂºÃ‚Â£ lÃƒÂ¡Ã‚ÂºÃ‚Â¡i mÃƒÂ¡Ã‚ÂºÃ‚Â£ng rÃƒÂ¡Ã‚Â»Ã¢â‚¬â€ng khi hÃƒÂ¡Ã‚Â»Ã‚Âc sinh chÃƒâ€ Ã‚Â°a lÃƒâ€ Ã‚Â°u tÃƒÂ¡Ã‚Â»Ã‚Â« nÃƒÆ’Ã‚Â o
     * 
     * Test Objective: Đảm bảo không crash khi học sinh chưa lưu từ nào
     * Input: studentID=99, findByStudentProfile_Id->[]
     * Expected Output: Trả về StudentDictionaryResponse với dictionaries=[], không ném exception
     * Notes: Edge case
     */
    @Test
    @DisplayName("DICT_011_TC: CÃƒÂ¡Ã‚ÂºÃ‚Â¥p dÃƒÂ¡Ã‚Â»Ã‚Â¯ liÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡u Flashcard - TrÃƒÂ¡Ã‚ÂºÃ‚Â£ lÃƒÂ¡Ã‚ÂºÃ‚Â¡i mÃƒÂ¡Ã‚ÂºÃ‚Â£ng rÃƒÂ¡Ã‚Â»Ã¢â‚¬â€ng khi hÃƒÂ¡Ã‚Â»Ã‚Âc sinh chÃƒâ€ Ã‚Â°a lÃƒâ€ Ã‚Â°u tÃƒÂ¡Ã‚Â»Ã‚Â« nÃƒÆ’Ã‚Â o")
        void DICT_011_TC_testGetAllForStudent_Empty() {
            Integer studentId = 99;
            when(studentDictionaryRepository.findByStudentProfile_Id(studentId)).thenReturn(new ArrayList<>());

            StudentDictionaryResponse response = studentDictionaryService.getAllForStudent(studentId);

            assertNotNull(response);
            assertEquals(studentId, response.getStudentProfileId());
            assertTrue(response.getDictionaries().isEmpty());
        }    /**
     * DICT_015_TC_testSaveStudentDictionary_DuplicateFlashcard: DICT_015_TC_testSaveStudentDictionary_DuplicateFlashcard
     * 
     * Test Objective: Mô đun Từ vựng cá nhân (Student Dictionary)
     * Input: Tham số hoặc payload giả lập (Mocked Request/Data)
     * Expected Output: Thực thi theo luồng chức năng tự điển đã đề ra
     * Notes: Dictionary / Flashcard Module
     */
    @Test
    @DisplayName("DICT_015_TC_testSaveStudentDictionary_DuplicateFlashcard")
        void DICT_015_TC_testSaveStudentDictionary_DuplicateFlashcard() {
            StudentDictionaryRequest request = new StudentDictionaryRequest();
            request.setStudentProfileId(10);
            request.setDefinitionExampleId(20);

            StudentProfileEntity student = new StudentProfileEntity();
            when(studentProfileRepository.findById(10)).thenReturn(Optional.of(student));

            DefinitionExampleEntity def = new DefinitionExampleEntity();
            when(definitionExampleRepository.findById(20)).thenReturn(Optional.of(def));

            studentDictionaryService.save(request);

            ArgumentCaptor<StudentDictionaryEntity> captor = ArgumentCaptor.forClass(StudentDictionaryEntity.class);
            verify(studentDictionaryRepository).save(captor.capture());
            
            studentDictionaryService.save(request);
            verify(studentDictionaryRepository, times(2)).save(any());
        }
    }
}