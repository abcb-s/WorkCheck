package com.workcheck.domain.checkresult.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workcheck.domain.checkresult.dto.response.CheckResultResponse;
import com.workcheck.domain.checkresult.dto.response.RuleCheckResultResponse;
import com.workcheck.domain.checkresult.service.CheckResultService;
import com.workcheck.global.config.SecurityConfig;
import com.workcheck.global.data.CheckStatus;
import com.workcheck.global.data.FileType;
import com.workcheck.global.data.RuleType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CheckResultController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class CheckResultControllerTest {

    private static final UsernamePasswordAuthenticationToken AUTHENTICATION =
            UsernamePasswordAuthenticationToken.authenticated(1L, null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

    private static final String VALID_REQUEST = """
            {
              "assignmentId": 1,
              "fileName": "submission.zip",
              "fileType": "ZIP",
              "ruleResults": [
                {
                  "type": "ZIP_REQUIRED_PATH",
                  "expected": "src/Main.java",
                  "status": "FAIL",
                  "missingItems": ["src/Main.java"]
                }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckResultService checkResultService;

    @Test
    void createCheckResult_returnsCheckResult() throws Exception {
        given(checkResultService.createCheckResult(eq(1L), any())).willReturn(new CheckResultResponse(
                1L,
                "submission.zip",
                FileType.ZIP,
                CheckStatus.FAIL,
                List.of(new RuleCheckResultResponse(
                        RuleType.ZIP_REQUIRED_PATH,
                        "src/Main.java",
                        CheckStatus.FAIL,
                        "압축 파일에서 src/Main.java 경로를 찾지 못했어요.",
                        List.of("src/Main.java")))
        ));

        mockMvc.perform(post("/api/check-results")
                        .with(authentication(AUTHENTICATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.assignmentId").value(1))
                .andExpect(jsonPath("$.data.fileName").value("submission.zip"))
                .andExpect(jsonPath("$.data.status").value("FAIL"))
                .andExpect(jsonPath("$.data.results[0].type").value("ZIP_REQUIRED_PATH"))
                .andExpect(jsonPath("$.data.results[0].expected").value("src/Main.java"))
                .andExpect(jsonPath("$.data.results[0].message").value("압축 파일에서 src/Main.java 경로를 찾지 못했어요."))
                .andExpect(jsonPath("$.data.results[0].missingItems[0]").value("src/Main.java"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void createCheckResult_withoutSignin_returns401() throws Exception {
        mockMvc.perform(post("/api/check-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(checkResultService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"assignmentId\":1,\"fileName\":\"submission.zip\",\"fileType\":\"ZIP\"}",
            "{\"assignmentId\":1,\"fileName\":\"submission.zip\",\"fileType\":\"ZIP\",\"ruleResults\":null}",
            "{\"assignmentId\":1,\"fileName\":\"submission.zip\",\"fileType\":\"ZIP\",\"ruleResults\":[]}",
            "{\"assignmentId\":null,\"fileName\":\"submission.zip\",\"fileType\":\"ZIP\",\"ruleResults\":[{\"type\":\"FILE_NAME_SUFFIX\",\"expected\":\".zip\",\"status\":\"PASS\"}]}",
            "{\"assignmentId\":1,\"fileName\":\" \",\"fileType\":\"ZIP\",\"ruleResults\":[{\"type\":\"FILE_NAME_SUFFIX\",\"expected\":\".zip\",\"status\":\"PASS\"}]}",
            "{\"assignmentId\":1,\"fileName\":\"submission.zip\",\"fileType\":null,\"ruleResults\":[{\"type\":\"FILE_NAME_SUFFIX\",\"expected\":\".zip\",\"status\":\"PASS\"}]}",
            "{\"assignmentId\":1,\"fileName\":\"submission.zip\",\"fileType\":\"ZIP\",\"ruleResults\":[{\"type\":null,\"expected\":\".zip\",\"status\":\"PASS\"}]}",
            "{\"assignmentId\":1,\"fileName\":\"submission.zip\",\"fileType\":\"ZIP\",\"ruleResults\":[{\"type\":\"FILE_NAME_SUFFIX\",\"expected\":\" \",\"status\":\"PASS\"}]}",
            "{\"assignmentId\":1,\"fileName\":\"submission.zip\",\"fileType\":\"ZIP\",\"ruleResults\":[{\"type\":\"FILE_NAME_SUFFIX\",\"expected\":\".zip\",\"status\":null}]}"
    })
    void createCheckResult_invalidInput_returns400(String request) throws Exception {
        mockMvc.perform(post("/api/check-results")
                        .with(authentication(AUTHENTICATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

        verifyNoInteractions(checkResultService);
    }
}