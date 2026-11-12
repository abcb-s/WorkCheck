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

@SpringBootTest
@AutoConfigureMockMvc
class SessionAuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fullSessionFlow() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/members/csrf"))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie xsrf = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        assertThat(xsrf.isHttpOnly()).isFalse();
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/members").session(session)
                        .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"flow@b.com","password":"password1","name":"학생"}"""))
                .andExpect(status().isCreated());

        String beforeId = session.getId();
        MvcResult signin = mockMvc.perform(post("/api/members/signin").session(session)
                        .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"flow@b.com","password":"password1"}"""))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession signedIn = (MockHttpSession) signin.getRequest().getSession(false);
        assertThat(signedIn).isNotNull();
        assertThat(signedIn.getId()).isNotEqualTo(beforeId);

        assertThat(signin.getResponse().getCookie("XSRF-TOKEN")).isNull();

        mockMvc.perform(get("/api/members/me").session(signedIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("flow@b.com"));

        mockMvc.perform(post("/api/members/logout").session(signedIn))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INVALID_CSRF"));

        MvcResult logout = mockMvc.perform(post("/api/members/logout").session(signedIn)
                        .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isNoContent())
                .andReturn();
        assertThat(signedIn.isInvalid()).isTrue();
        assertThat(logout.getResponse().getCookie("JSESSIONID").getMaxAge()).isZero();

        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void demoMemberCanSignin() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/members/csrf")).andReturn();
        Cookie xsrf = csrf.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/members/signin")
                        .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"demo@workcheck.com","password":"password1234"}"""))
                .andExpect(status().isOk());
    }
}
