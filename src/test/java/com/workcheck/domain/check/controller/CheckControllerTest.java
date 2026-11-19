package com.workcheck.domain.check.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workcheck.domain.check.dto.response.CheckResponse;
import com.workcheck.domain.check.dto.response.CheckResultResponse;
import com.workcheck.domain.check.dto.response.CheckSummaryResponse;
import com.workcheck.domain.check.service.CheckService;
import com.workcheck.global.config.SecurityConfig;
import com.workcheck.global.data.CheckStatus;
import com.workcheck.global.data.RuleType;
import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CheckController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class CheckControllerTest {

    private static final UsernamePasswordAuthenticationToken AUTHENTICATION =
            UsernamePasswordAuthenticationToken.authenticated(1L, null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

    private static final MockMultipartFile FILE =
            new MockMultipartFile("file", "project.zip", "application/zip", new byte[]{0x50, 0x4B, 0x03, 0x04});

    private static final CheckResponse FAIL_CHECK = new CheckResponse(
            5L,
            "project.zip",
            CheckStatus.FAIL,
            List.of(new CheckResultResponse(10L, RuleType.ZIP_REQUIRED_PATH, "data.sql", CheckStatus.FAIL,
                    "압축 파일에서 data.sql 경로를 찾지 못했어요.")),
            LocalDateTime.of(2026, 11, 19, 9, 0)
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckService checkService;

    @Test
    void check_failResult_returns201() throws Exception {
        given(checkService.check(eq(1L), eq(1L), any())).willReturn(FAIL_CHECK);

        mockMvc.perform(multipart("/api/assignments/1/checks").file(FILE)
                        .with(authentication(AUTHENTICATION)).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.status").value("FAIL"))
                .andExpect(jsonPath("$.data.results[0].message").value("압축 파일에서 data.sql 경로를 찾지 못했어요."))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void check_corruptedZip_returns400() throws Exception {
        given(checkService.check(eq(1L), eq(1L), any())).willThrow(new CustomException(ErrorCode.INVALID_FILE));

        mockMvc.perform(multipart("/api/assignments/1/checks").file(FILE)
                        .with(authentication(AUTHENTICATION)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILE"));
    }

    @Test
    void check_unsupportedFileType_returns415() throws Exception {
        given(checkService.check(eq(1L), eq(1L), any()))
                .willThrow(new CustomException(ErrorCode.UNSUPPORTED_FILE_TYPE));

        mockMvc.perform(multipart("/api/assignments/1/checks").file(FILE)
                        .with(authentication(AUTHENTICATION)).with(csrf()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FILE_TYPE"));
    }

    @Test
    void check_withoutFilePart_returns400() throws Exception {
        mockMvc.perform(multipart("/api/assignments/1/checks")
                        .with(authentication(AUTHENTICATION)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("EMPTY_FILE"));

        verifyNoInteractions(checkService);
    }

    @Test
    void check_withoutSignin_returns401() throws Exception {
        mockMvc.perform(multipart("/api/assignments/1/checks").file(FILE).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(checkService);
    }

    @Test
    void check_withoutCsrf_returns403() throws Exception {
        mockMvc.perform(multipart("/api/assignments/1/checks").file(FILE).with(authentication(AUTHENTICATION)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INVALID_CSRF"));

        verifyNoInteractions(checkService);
    }

    @Test
    void getChecks_withStatus_returns200() throws Exception {
        given(checkService.getChecks(1L, 1L, CheckStatus.FAIL)).willReturn(List.of(
                new CheckSummaryResponse(5L, "project.zip", CheckStatus.FAIL, LocalDateTime.of(2026, 11, 19, 9, 0))));

        mockMvc.perform(get("/api/assignments/1/checks").param("status", "FAIL")
                        .with(authentication(AUTHENTICATION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(5))
                .andExpect(jsonPath("$.data[0].status").value("FAIL"))
                .andExpect(jsonPath("$.data[0].results").doesNotExist());
    }

    @Test
    void getChecks_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/assignments/1/checks").param("status", "DONE")
                        .with(authentication(AUTHENTICATION)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

        verifyNoInteractions(checkService);
    }

    @Test
    void getCheck_returns200() throws Exception {
        given(checkService.getCheck(1L, 1L, 5L)).willReturn(FAIL_CHECK);

        mockMvc.perform(get("/api/assignments/1/checks/5").with(authentication(AUTHENTICATION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].expected").value("data.sql"));
    }

    @Test
    void getCheck_notFound_returns404() throws Exception {
        given(checkService.getCheck(1L, 1L, 99L)).willThrow(new CustomException(ErrorCode.CHECK_NOT_FOUND));

        mockMvc.perform(get("/api/assignments/1/checks/99").with(authentication(AUTHENTICATION)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CHECK_NOT_FOUND"));
    }
}
