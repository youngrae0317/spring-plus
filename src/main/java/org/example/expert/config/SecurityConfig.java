package org.example.expert.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, SecurityContextHolderAwareRequestFilter.class)

                // JWT 사용 시 불필요한 기능들 비활성화
                .formLogin(AbstractHttpConfigurer::disable)      // [SSR] 서버가 로그인 HTML 폼 렌더링
                .anonymous(AbstractHttpConfigurer::disable)      // 미인증 사용자를 익명으로 처리
                .httpBasic(AbstractHttpConfigurer::disable)      // [SSR] 인증 팝업
                .logout(AbstractHttpConfigurer::disable)         // [SSR] 서버가 세션 무효화 후 리다이렉트
                .rememberMe(AbstractHttpConfigurer::disable)     // 서버가 쿠키 발급하여 자동 로그인

                .authorizeHttpRequests(auth -> auth
                        // '/auth/**' 경로는 인증 없이 모두 허용
                        .requestMatchers("/auth/**", "/h2-console/**").permitAll()
                        // '/admin/**' 경로는 'ADMIN' 역할을 가진 사용자만 허용
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 그 외 모든 요청은 인증된 사용자만 허용
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .build();
    }
}
