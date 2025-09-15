package org.example.expert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String jwt = jwtUtil.substringToken(authorizationHeader);

        try {
            // SecurityContext에 인증 정보가 없는 경우에만 JWT 검증 및 인증 처리
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                Claims claims = jwtUtil.extractClaims(jwt);
                setAuthentication(claims);
            }
        } catch (ExpiredJwtException e) {
            log.info("JWT 만료: userId={}, URI={}", e.getClaims().getSubject(), request.getRequestURI());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
            return; // 인증 실패 시 필터 체인 중단
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("유효하지 않은 JWT 서명입니다. URI: {}", request.getRequestURI());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
            return; // 인증 실패 시 필터 체인 중단
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다. URI: {}", request.getRequestURI());
            sendErrorResponse(response, HttpStatus.BAD_REQUEST, "지원되지 않는 JWT 토큰입니다.");
            return; // 인증 실패 시 필터 체인 중단
        } catch (Exception e) {
            log.error("JWT 필터에서 예상치 못한 오류 발생: URI={}", request.getRequestURI(), e);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "요청 처리 중 오류가 발생했습니다.");
            return; // 인증 실패 시 필터 체인 중단
        }

        chain.doFilter(request, response);
    }

    // JWT Claims에서 사용자 정보 추출하여 Spring Security 인증 정보 설정
    private void setAuthentication(Claims claims) {

        // JWT subject claims에서 사용자 ID 추출
        Long userId = Long.valueOf(claims.getSubject());
        // 커스텀 claim에서 이메일 정보 추출
        String email = claims.get("email", String.class);
        String nickname = claims.get("nickname", String.class);
        // 커스텀 claim에서 사용자 권한 정보 추출 후 enum 으로 변환
        UserRole userRole = UserRole.of(claims.get("userRole", String.class));

        // 추출 정보로 인증된 사용자 객체 생성
        AuthUser authUser = new AuthUser(userId, email, nickname, userRole);
        // Spring security가 인식할 수 있는 Authentication 객체 생성
        Authentication authenticationToken = new JwtAuthenticationToken(authUser);
        // SecurityContext에 인증 정보 저장
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", status.name());
        errorResponse.put("code", status.value());
        errorResponse.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse)); // prettier-ignore
    }


}
