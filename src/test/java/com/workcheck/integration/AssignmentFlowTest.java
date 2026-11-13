package com.workcheck.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workcheck.domain.assignment.repository.AssignmentRepository;
import com.workcheck.domain.assignment.repository.CheckRuleRepository;
import com.workcheck.global.data.FileType;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AssignmentFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private CheckRuleRepository checkRuleRepository;

    private MockHttpSession session;
    private Cookie xsrf;

    @BeforeEach
    void login() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/members/csrf"))
                .andExpect(status().isNoContent()).andReturn();
        xsrf = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();

        MvcResult result = mockMvc.perform(post("/api/members/login")
                        .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"email":"demo@workcheck.com","password":"password1234"}
                                """))
                .andExpect(status().isOk()).andReturn();
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
    void createAndReadAssignment_withMixedFileRules() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/assignments").with(signedIn())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "title": "  자바 프로젝트  ",
                                  "rules": [
                                    {"fileType": "ZIP", "type": "ZIP_REQUIRED_PATH", "value": "src/main/resources/data.sql"},
                                    {"fileType": "PDF", "type": "PDF_REQUIRED_TEXT", "value": "참고문헌"}
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

        assertThat(checkRuleRepository.findByAssignment_IdAndFileType(assignmentId, FileType.ZIP)).hasSize(1);
        assertThat(checkRuleRepository.findByAssignment_IdAndFileType(assignmentId, FileType.PDF)).hasSize(1);
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
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

        assertThat(assignmentRepository.count()).isEqualTo(before);
    }
}
