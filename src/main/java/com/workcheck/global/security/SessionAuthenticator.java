package com.workcheck.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class SessionAuthenticator {

    private static final List<SimpleGrantedAuthority> MEMBER_AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public void signin(Long memberId) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();

        preventSessionFixation(request);

        SecurityContext context = contextHolderStrategy.createEmptyContext();
        context.setAuthentication(memberAuthentication(memberId));
        contextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private void preventSessionFixation(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
    }

    private Authentication memberAuthentication(Long memberId) {
        return UsernamePasswordAuthenticationToken.authenticated(memberId, null, MEMBER_AUTHORITIES);
    }
}
