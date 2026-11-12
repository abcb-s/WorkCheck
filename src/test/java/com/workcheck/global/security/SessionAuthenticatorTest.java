package com.workcheck.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class SessionAuthenticatorTest {

    private final HttpSessionSecurityContextRepository repository = new HttpSessionSecurityContextRepository();
    private final SessionAuthenticator sessionAuthenticator = new SessionAuthenticator(repository);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private static void bind(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
    }

    @Test
    void login_savesMemberIdAsPrincipalInSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        bind(request);

        sessionAuthenticator.login(1L);

        Authentication saved = repository.loadDeferredContext(request).get().getAuthentication();
        assertThat(saved.isAuthenticated()).isTrue();
        assertThat(saved.getPrincipal()).isEqualTo(1L);
        assertThat(saved.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(saved);
    }

    @Test
    void login_withExistingSession_changesSessionId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        String beforeId = session.getId();
        request.setSession(session);
        bind(request);

        sessionAuthenticator.login(1L);

        assertThat(request.getSession(false).getId()).isNotEqualTo(beforeId);
    }
}
