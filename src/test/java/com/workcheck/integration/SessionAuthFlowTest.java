package com.workcheck.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 프론트가 실제로 밟는 순서 그대로 검증한다:
 * CSRF 쿠키 발급 → 회원가입 → 로그인 → 내 정보 → 로그아웃 → 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionAuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fullSessionFlow() throws Exception {
        // 1. CSRF 쿠키 발급
        MvcResult csrfResult = mockMvc.perform(get("/api/members/csrf"))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie xsrf = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        assertThat(xsrf.isHttpOnly()).isFalse(); // JS에서 읽을 수 있어야 함
        MockHttpSession session = new MockHttpSession();

        // 2. 회원가입 (쿠키 값을 헤더로 그대로 전송)
        mockMvc.perform(post("/api/members").session(session)
                        .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"flow@b.com","password":"password1","name":"학생"}"""))
                .andExpect(status().isCreated());

        // 3. 로그인 → 세션 ID 변경
        String beforeId = session.getId();
        MvcResult login = mockMvc.perform(post("/api/members/login").session(session)
                        .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"flow@b.com","password":"password1"}"""))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession loggedIn = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(loggedIn).isNotNull();
        assertThat(loggedIn.getId()).isNotEqualTo(beforeId);

        // 로그인 시 CSRF 토큰도 교체되므로 새 쿠키를 받는다
        Cookie rotated = login.getResponse().getCookie("XSRF-TOKEN");
        Cookie currentXsrf = rotated != null && !rotated.getValue().isEmpty() ? rotated : xsrf;
        if (rotated != null && rotated.getValue().isEmpty()) {
            currentXsrf = mockMvc.perform(get("/api/members/csrf").session(loggedIn))
                    .andReturn().getResponse().getCookie("XSRF-TOKEN");
        }

        // 4. 내 정보
        mockMvc.perform(get("/api/members/me").session(loggedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("flow@b.com"));

        // 5. CSRF 없이 로그아웃 → 403
        mockMvc.perform(post("/api/members/logout").session(loggedIn))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INVALID_CSRF"));

        // 6. 로그아웃
        mockMvc.perform(post("/api/members/logout").session(loggedIn)
                        .cookie(currentXsrf).header("X-XSRF-TOKEN", currentXsrf.getValue()))
                .andExpect(status().isNoContent());
        assertThat(loggedIn.isInvalid()).isTrue();

        // 7. 로그아웃 후 → 401
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void demoMemberCanLogin() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/members/csrf")).andReturn();
        Cookie xsrf = csrf.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/members/login")
                        .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"demo@workcheck.com","password":"password1234"}"""))
                .andExpect(status().isOk());
    }
}
