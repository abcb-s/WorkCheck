package com.workcheck.domain.assignment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workcheck.domain.assignment.dto.response.AssignmentResponse;
import com.workcheck.domain.assignment.dto.response.CheckRuleResponse;
import com.workcheck.domain.assignment.service.AssignmentService;
import com.workcheck.global.config.SecurityConfig;
import com.workcheck.global.data.FileType;
import com.workcheck.global.data.RuleType;
import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.time.LocalDateTime;
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

@WebMvcTest(AssignmentController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class AssignmentControllerTest {

    private static final String REQUEST = """
            {
              "title": "과학 탐구 보고서",
              "rules": [{"fileType": "PDF", "type": "PDF_REQUIRED_TEXT", "value": "참고문헌"}]
            }
            """;

    private static final AssignmentResponse ASSIGNMENT = new AssignmentResponse(
            1L,
            "과학 탐구 보고서",
            List.of(new CheckRuleResponse(10L, FileType.PDF, RuleType.PDF_REQUIRED_TEXT, "참고문헌")),
            LocalDateTime.of(2026, 11, 13, 9, 0)
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssignmentService assignmentService;

    private static UsernamePasswordAuthenticationToken signedIn(long memberId) {
        return UsernamePasswordAuthenticationToken.authenticated(
                memberId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void createAssignment_returns201() throws Exception {
        given(assignmentService.createAssignment(eq(1L), any())).willReturn(ASSIGNMENT);

        mockMvc.perform(post("/api/assignments").with(authentication(signedIn(1L)))
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.rules[0].type").value("PDF_REQUIRED_TEXT"))
                .andExpect(jsonPath("$.data.rules[0].assignment").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void createAssignment_wrongRuleCombination_returns400WithMessage() throws Exception {
        String request = REQUEST.replace("\"PDF\"", "\"ZIP\"");
        given(assignmentService.createAssignment(eq(1L), any()))
                .willThrow(new CustomException(ErrorCode.INVALID_RULE_COMBINATION));

        mockMvc.perform(post("/api/assignments").with(authentication(signedIn(1L)))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_RULE_COMBINATION"))
                .andExpect(jsonPath("$.error.message").value("파일 형식에 맞지 않는 검사 규칙이 있어요."));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"title\":\" \",\"rules\":[{\"fileType\":\"PDF\",\"type\":\"PDF_REQUIRED_TEXT\",\"value\":\"참고문헌\"}]}",
            "{\"title\":\"보고서\",\"rules\":[]}",
            "{\"title\":\"보고서\",\"rules\":null}",
            "{\"title\":\"보고서\",\"rules\":[null]}",
            "{\"title\":\"보고서\",\"rules\":[{\"fileType\":null,\"type\":\"PDF_REQUIRED_TEXT\",\"value\":\"참고문헌\"}]}",
            "{\"title\":\"보고서\",\"rules\":[{\"fileType\":\"PDF\",\"type\":null,\"value\":\"참고문헌\"}]}",
            "{\"title\":\"보고서\",\"rules\":[{\"fileType\":\"PDF\",\"type\":\"PDF_REQUIRED_TEXT\",\"value\":\" \"}]}",
            "{\"title\":\"보고서\",\"rules\":[{\"fileType\":\"TXT\",\"type\":\"PDF_REQUIRED_TEXT\",\"value\":\"참고문헌\"}]}"
    })
    void createAndUpdateAssignment_invalidInput_returns400(String request) throws Exception {
        mockMvc.perform(post("/api/assignments").with(authentication(signedIn(1L)))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

        mockMvc.perform(put("/api/assignments/1").with(authentication(signedIn(1L)))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

        verifyNoInteractions(assignmentService);
    }

    @Test
    void createAndUpdateAssignment_longTitle_returns400() throws Exception {
        String request = REQUEST.replace("과학 탐구 보고서", "가".repeat(101));

        mockMvc.perform(post("/api/assignments").with(authentication(signedIn(1L)))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

        mockMvc.perform(put("/api/assignments/1").with(authentication(signedIn(1L)))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

        verifyNoInteractions(assignmentService);
    }

    @Test
    void createAndUpdateAssignment_longRuleValue_returns400() throws Exception {
        String request = REQUEST.replace("참고문헌", "가".repeat(501));

        mockMvc.perform(post("/api/assignments").with(authentication(signedIn(1L)))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

        mockMvc.perform(put("/api/assignments/1").with(authentication(signedIn(1L)))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

        verifyNoInteractions(assignmentService);
    }

    @Test
    void getAssignments_returnsList() throws Exception {
        given(assignmentService.getAssignments(1L)).willReturn(List.of(ASSIGNMENT));

        mockMvc.perform(get("/api/assignments").with(authentication(signedIn(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("과학 탐구 보고서"));
    }

    @Test
    void getAssignment_returnsRules() throws Exception {
        given(assignmentService.getAssignment(1L, 1L)).willReturn(ASSIGNMENT);

        mockMvc.perform(get("/api/assignments/1").with(authentication(signedIn(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rules[0].value").value("참고문헌"));
    }

    @Test
    void getAssignment_notOwnedByMember_returns404() throws Exception {
        given(assignmentService.getAssignment(2L, 99L))
                .willThrow(new CustomException(ErrorCode.ASSIGNMENT_NOT_FOUND));

        mockMvc.perform(get("/api/assignments/99").with(authentication(signedIn(2L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ASSIGNMENT_NOT_FOUND"));
    }

    @Test
    void getAssignments_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/assignments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(assignmentService);
    }

    @Test
    void updateAssignment_returns200() throws Exception {
        given(assignmentService.updateAssignment(eq(1L), eq(1L), any())).willReturn(ASSIGNMENT);

        mockMvc.perform(put("/api/assignments/1").with(authentication(signedIn(1L)))
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.rules[0].value").value("참고문헌"));
    }

    @Test
    void updateAndDeleteAssignment_notOwnedByMember_returns404() throws Exception {
        given(assignmentService.updateAssignment(eq(2L), eq(99L), any()))
                .willThrow(new CustomException(ErrorCode.ASSIGNMENT_NOT_FOUND));
        doThrow(new CustomException(ErrorCode.ASSIGNMENT_NOT_FOUND))
                .when(assignmentService).deleteAssignment(2L, 99L);

        mockMvc.perform(put("/api/assignments/99").with(authentication(signedIn(2L)))
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ASSIGNMENT_NOT_FOUND"));
        mockMvc.perform(delete("/api/assignments/99").with(authentication(signedIn(2L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ASSIGNMENT_NOT_FOUND"));
    }

    @Test
    void deleteAssignment_returns204WithoutBody() throws Exception {
        mockMvc.perform(delete("/api/assignments/1").with(authentication(signedIn(1L))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(assignmentService).deleteAssignment(1L, 1L);
    }

    @Test
    void updateAndDeleteAssignment_withoutToken_returns401() throws Exception {
        mockMvc.perform(put("/api/assignments/1")
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/assignments/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(assignmentService);
    }
}
