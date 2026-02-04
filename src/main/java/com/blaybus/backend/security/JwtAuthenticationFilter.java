package com.blaybus.backend.security;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String token = resolveToken(request);

		if (token != null && jwtTokenProvider.validateToken(token)) {
			String userId = jwtTokenProvider.getUserIdFromToken(token);

			// TODO: 추후 Repository까지 정의가 끝난 다음, userDetails 에 유저 정보 삽입
			// API 처리에 불필요한 userDetail은 제외 ex) password, email 등등...
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				userId, null, Collections.emptyList());

			SecurityContextHolder.getContext().setAuthentication(authentication);
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}
}
