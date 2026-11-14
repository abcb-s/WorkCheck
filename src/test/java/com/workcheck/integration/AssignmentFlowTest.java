package com.workcheck.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workcheck.domain.assignment.repository.AssignmentRepository;
import com.workcheck.domain.assignment.repository.CheckRuleRepository;
import com.workcheck.domain.assignment.entity.CheckRule;
import com.workcheck.domain.assignment.entity.Assignment;
import com.workcheck.domain.member.repository.MemberRepository;
import com.workcheck.global.data.FileType;
import com.workcheck.global.data.RuleType;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AssignmentFlowTest {

    private static final String REQUEST = """
            {
              "title": "처음 과제",
              "rules": [
                {"fileType": "ZIP", "type": "ZIP_REQUIRED_PATH", "value": "data.sql"},
                {"fileType": "PDF", "type": "PDF_REQUIRED_TEXT", "value": "참고문헌"},
                {"fileType": "PDF", "type": "FILE_NAME_SUFFIX", "value": "_보고서.pdf"}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private CheckRuleRepository checkRuleRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;

    private MockHttpSession session;
    private Cookie xsrf;
    private Long memberId;

    @BeforeEach
    void signin() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/members/csrf"))
                .andExpect(status().isNoContent()).andReturn();
        xsrf = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();

        MvcResult result = mockMvc.perform(post("/api/members/signin")
                        .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"email":"demo@workcheck.com","password":"password1234"}
                                """))
                .andExpect(status().isOk()).andReturn();
        memberId = jsonMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asLong();
        session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
    }

    private RequestPostProcessor signedIn() {
        return request -> {
            request.setSession(session);
            request.setCookies(xsrf);
            request.addHeader("X-XSRF-TOKEN", xsrf.getValue());
            return request;
        };
    }

    @Test
    void createUpdateAndDeleteAssignment_replacesAndDeletesRules() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/assignments").with(signedIn())
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rules.length()").value(3))
                .andReturn();
        var original = jsonMapper.readTree(created.getResponse().getContentAsString()).get("data");
        long assignmentId = original.get("id").asLong();
        List<Long> originalRuleIds = List.of(original.get("rules").get(0).get("id").asLong(),
                original.get("rules").get(1).get("id").asLong(),
                original.get("rules").get(2).get("id").asLong());
        entityManager.flush();
        entityManager.clear();

        MvcResult updated = mockMvc.perform(put("/api/assignments/" + assignmentId)
                        .with(signedIn())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "title": "  수정한 과제  ",
                                  "rules": [
                                    {"fileType": "PDF", "type": "PDF_REQUIRED_TEXT", "value": "  결론 문구  "}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(assignmentId))
                .andExpect(jsonPath("$.data.title").value("수정한 과제"))
                .andExpect(jsonPath("$.data.rules.length()").value(1))
                .andExpect(jsonPath("$.data.rules[0].id").isNumber())
                .andExpect(jsonPath("$.data.rules[0].value").value("결론 문구"))
                .andExpect(jsonPath("$.data.createdAt").value(original.get("createdAt").asText()))
                .andReturn();
        long newRuleId = jsonMapper.readTree(updated.getResponse().getContentAsString())
                .get("data").get("rules").get(0).get("id").asLong();
        entityManager.flush();
        entityManager.clear();

        assertThat(newRuleId).isNotIn(originalRuleIds);
        assertThat(checkRuleRepository.findAllById(originalRuleIds)).isEmpty();
        assertThat(checkRuleRepository.findById(newRuleId)).isPresent();
        mockMvc.perform(get("/api/assignments/" + assignmentId).with(signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정한 과제"))
                .andExpect(jsonPath("$.data.rules[0].value").value("결론 문구"));

        mockMvc.perform(delete("/api/assignments/" + assignmentId).with(signedIn()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        entityManager.flush();
        entityManager.clear();

        assertThat(assignmentRepository.findById(assignmentId)).isEmpty();
        assertThat(memberRepository.findById(memberId)).isPresent();
        assertThat(checkRuleRepository.findById(newRuleId)).isEmpty();
        mockMvc.perform(get("/api/assignments/" + assignmentId).with(signedIn()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ASSIGNMENT_NOT_FOUND"));
    }

    static Stream<Arguments> invalidUpdates() {
        return Stream.of(
                Arguments.of("""
                        {"title":"수정한 과제","rules":[
                          {"fileType":"ZIP","type":"FILE_NAME_SUFFIX","value":".zip"},
                          {"fileType":"PDF","type":"ZIP_REQUIRED_PATH","value":"data.sql"}
                        ]}
                        """, "INVALID_RULE_COMBINATION"),
                Arguments.of("""
                        {"title":"수정한 과제","rules":[
                          {"fileType":"PDF","type":"FILE_NAME_SUFFIX","value":".pdf"},
                          {"fileType":"ZIP","type":"PDF_REQUIRED_TEXT","value":"결론"}
                        ]}
                        """, "INVALID_RULE_COMBINATION"),
                Arguments.of("{\"title\":\" \",\"rules\":[]}", "INVALID_INPUT")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidUpdates")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void updateAssignment_invalidInput_keepsPersistedTitleAndRules(String request, String errorCode) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/assignments").with(signedIn())
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated()).andReturn();
        var original = jsonMapper.readTree(created.getResponse().getContentAsString()).get("data");
        long assignmentId = original.get("id").asLong();
        var originalRules = assignmentRepository.findByIdWithRules(assignmentId, memberId).orElseThrow().getRules();
        List<Long> originalRuleIds = originalRules.stream().map(CheckRule::getId).toList();

        try {
            mockMvc.perform(put("/api/assignments/" + assignmentId).with(signedIn())
                            .contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(errorCode));

            MvcResult found = mockMvc.perform(get("/api/assignments/" + assignmentId).with(signedIn()))
                    .andExpect(status().isOk()).andReturn();
            assertThat(jsonMapper.readTree(found.getResponse().getContentAsString()).get("data"))
                    .isEqualTo(original);

            var persisted = assignmentRepository.findByIdWithRules(assignmentId, memberId).orElseThrow();
            assertThat(persisted.getTitle()).isEqualTo("처음 과제");
            assertThat(persisted.getRules()).extracting(CheckRule::getId)
                    .containsExactlyElementsOf(originalRuleIds);
            assertThat(checkRuleRepository.findAllById(originalRuleIds)).hasSize(3);
        } finally {
            assignmentRepository.deleteById(assignmentId);
        }
    }

    @Test
    void createAndReadAssignment_withMixedFileRules() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/assignments").with(signedIn())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "title": "  자바 프로젝트  ",
                                  "rules": [
                                    {"fileType": "ZIP", "type": "ZIP_REQUIRED_PATH", "value": "  src/main/resources/data.sql  "},
                                    {"fileType": "PDF", "type": "PDF_REQUIRED_TEXT", "value": "  참고문헌  "}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("자바 프로젝트"))
                .andExpect(jsonPath("$.data.rules.length()").value(2))
                .andExpect(jsonPath("$.data.rules[0].id").isNumber())
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andReturn();

        long assignmentId = jsonMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        mockMvc.perform(get("/api/assignments/" + assignmentId).with(signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rules[0].value").value("src/main/resources/data.sql"))
                .andExpect(jsonPath("$.data.rules[1].value").value("참고문헌"));

        mockMvc.perform(get("/api/assignments").with(signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(assignmentId));

        entityManager.flush();
        entityManager.clear();
        Assignment persisted = assignmentRepository.findByIdWithRules(assignmentId, memberId).orElseThrow();
        assertThat(persisted.getMember().getId()).isEqualTo(memberId);
        assertThat(persisted.getRules()).extracting(CheckRule::getFileType, CheckRule::getValue)
                .containsExactly(tuple(FileType.ZIP, "src/main/resources/data.sql"),
                        tuple(FileType.PDF, "참고문헌"));
    }

    @Test
    void createAssignment_wrongCombination_returns400WithoutSaving() throws Exception {
        long before = assignmentRepository.count();

        mockMvc.perform(post("/api/assignments").with(signedIn())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "title": "잘못된 과제",
                                  "rules": [
                                    {"fileType": "ZIP", "type": "FILE_NAME_SUFFIX", "value": ".zip"},
                                    {"fileType": "PDF", "type": "ZIP_REQUIRED_PATH", "value": "data.sql"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_RULE_COMBINATION"))
                .andExpect(jsonPath("$.error.message").value("파일 형식과 검사 규칙 종류가 일치하지 않습니다."));

        assertThat(assignmentRepository.count()).isEqualTo(before);
    }

    @Test
    void demoAssignments_loadPdfAndZipRules() throws Exception {
        List<Assignment> assignments = assignmentRepository.findAllWithRules(memberId);

        assertThat(assignments).allSatisfy(assignment ->
                assertThat(assignment.getMember().getId()).isEqualTo(memberId));
        assertThat(assignments).filteredOn("title", "과학 탐구 보고서")
                .singleElement().satisfies(assignment ->
                        assertThat(assignment.getRules())
                                .extracting(CheckRule::getFileType, CheckRule::getType, CheckRule::getValue)
                                .containsExactly(
                                        tuple(FileType.PDF, RuleType.FILE_NAME_SUFFIX, "_보고서.pdf"),
                                        tuple(FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "참고문헌")));
        assertThat(assignments).filteredOn("title", "자바 프로젝트")
                .singleElement().satisfies(assignment ->
                        assertThat(assignment.getRules())
                                .extracting(CheckRule::getFileType, CheckRule::getType, CheckRule::getValue)
                                .containsExactly(
                                        tuple(FileType.ZIP, RuleType.FILE_NAME_SUFFIX, "_프로젝트.zip"),
                                        tuple(FileType.ZIP, RuleType.ZIP_REQUIRED_PATH, "src/main/resources/data.sql")));

        mockMvc.perform(get("/api/assignments").with(signedIn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
