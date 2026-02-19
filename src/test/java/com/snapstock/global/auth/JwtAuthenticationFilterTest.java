package com.snapstock.global.auth;

import com.snapstock.domain.user.entity.Role;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final Long USER_ID = 1L;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenRedisService tokenRedisService;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 토큰이면 SecurityContext에 인증 정보를 설정한다")
    void doFilterInternal_유효한토큰_SecurityContext설정() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenProvider.resolveToken(request)).willReturn(VALID_TOKEN);
        given(tokenRedisService.isBlacklisted(VALID_TOKEN)).willReturn(false);
        given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
        given(jwtTokenProvider.getUserId(VALID_TOKEN)).willReturn(USER_ID);
        given(jwtTokenProvider.getRole(VALID_TOKEN)).willReturn(Role.USER);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        assertThat(principal.userId()).isEqualTo(USER_ID);
        assertThat(principal.role()).isEqualTo(Role.USER);
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰이 없으면 SecurityContext를 설정하지 않는다")
    void doFilterInternal_토큰없음_SecurityContext미설정() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenProvider.resolveToken(request)).willReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 SecurityContext를 설정하지 않는다")
    void doFilterInternal_유효하지않은토큰_SecurityContext미설정() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenProvider.resolveToken(request)).willReturn(VALID_TOKEN);
        given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(false);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("블랙리스트 토큰이면 SecurityContext를 설정하지 않는다")
    void doFilterInternal_블랙리스트토큰_SecurityContext미설정() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenProvider.resolveToken(request)).willReturn(VALID_TOKEN);
        given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
        given(tokenRedisService.isBlacklisted(VALID_TOKEN)).willReturn(true);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("role이 없는 토큰이면 SecurityContext를 설정하지 않는다")
    void doFilterInternal_role없는토큰_SecurityContext미설정() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenProvider.resolveToken(request)).willReturn(VALID_TOKEN);
        given(jwtTokenProvider.validateToken(VALID_TOKEN)).willReturn(true);
        given(tokenRedisService.isBlacklisted(VALID_TOKEN)).willReturn(false);
        given(jwtTokenProvider.getUserId(VALID_TOKEN)).willReturn(USER_ID);
        given(jwtTokenProvider.getRole(VALID_TOKEN)).willReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
