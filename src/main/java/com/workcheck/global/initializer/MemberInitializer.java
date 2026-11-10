package com.workcheck.global.initializer;

import com.workcheck.domain.member.entity.Member;
import com.workcheck.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 데모 회원을 만든다. 비밀번호를 BCrypt로 인코딩해야 해서 data.sql 대신 코드로 넣는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberInitializer implements ApplicationRunner {

    static final String DEMO_EMAIL = "demo@workcheck.com";
    static final String DEMO_PASSWORD = "password1234";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        if (memberRepository.existsByEmail(DEMO_EMAIL)) {
            return;
        }
        memberRepository.save(Member.builder()
                .email(DEMO_EMAIL)
                .password(passwordEncoder.encode(DEMO_PASSWORD))
                .name("데모 학생")
                .build());
        log.info("Demo member created: {}", DEMO_EMAIL);
    }
}
