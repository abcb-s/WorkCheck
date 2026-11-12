package com.workcheck.domain.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workcheck.domain.member.dto.response.MemberResponse;
import com.workcheck.domain.member.service.MemberService;
import com.workcheck.global.config.SecurityConfig;
import com.workcheck.global.exception.exception.CustomException;
import com.workcheck.global.exception.status_code.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MemberController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class MemberControllerTest {

    private static final MemberResponse MEMBER =
            new MemberResponse(1L, "a@b.com", "학생", LocalDateTime.of(2026, 1, 1, 0, 0));
    private static final String SIGNUP_JSON = """
            {"email":"a@b.com","password":"password1","name":"학생"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    private static UsernamePasswordAuthenticationToken signedIn(long memberId) {
        return UsernamePasswordAuthenticationToken.authenticated(
                memberId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void signup_returns201() throws Exception {
        given(memberService.signup(any())).willReturn(MEMBER);

        mockMvc.perform(post("/api/members").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(SIGNUP_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void signup_withoutCsrf_returns403() throws Exception {
        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content(SIGNUP_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INVALID_CSRF"));
    }

    @Test
    void signup_invalidInput_returns400() throws Exception {
        mockMvc.perform(post("/api/members").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-email","password":"short","name":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void signup_passwordOverBcryptLimit_returns400() throws Exception {
        String password = "가".repeat(30);

        mockMvc.perform(post("/api/members").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"" + password + "\",\"name\":\"학생\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void signup_duplicateEmail_returns409() throws Exception {
        given(memberService.signup(any())).willThrow(new CustomException(ErrorCode.DUPLICATE_EMAIL));

        mockMvc.perform(post("/api/members").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(SIGNUP_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    void signin_returns200() throws Exception {
        given(memberService.signin(any())).willReturn(MEMBER);

        mockMvc.perform(post("/api/members/signin").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.com","password":"password1"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void signin_wrongCredentials_returns401() throws Exception {
        given(memberService.signin(any())).willThrow(new CustomException(ErrorCode.SIGNIN_FAILED));

        mockMvc.perform(post("/api/members/signin").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.com","password":"wrong-pass"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("SIGNIN_FAILED"));
    }

    @Test
    void me_authenticated_returns200() throws Exception {
        given(memberService.getMember(1L)).willReturn(MEMBER);

        mockMvc.perform(get("/api/members/me").with(authentication(signedIn(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("a@b.com"));
    }

    @Test
    void me_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void unknownApi_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/assignments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cors_preflight_allowedOrigin() throws Exception {
        mockMvc.perform(options("/api/members/signin")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void cors_preflight_unknownOrigin_rejected() throws Exception {
        mockMvc.perform(options("/api/members/signin")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
