package com.blaybus.backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.blaybus.backend.security.CustomUserDetailsService;
import com.blaybus.backend.security.JwtAuthenticationFilter;
import com.blaybus.backend.security.JwtTokenProvider;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(
		HttpSecurity http,
		JwtTokenProvider jwtTokenProvider,
		CustomUserDetailsService userDetailsService) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint((request, response, authException) -> {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.setContentType("application/json;charset=UTF-8");
					response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\"}");
				}))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/health-check", "/actuator/prometheus", "/login", "/scenes/*/chat").permitAll()
				.anyRequest().authenticated())
			.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService),
				UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		// health-check 전용 CORS 설정 (모든 오리진 허용)
		CorsConfiguration healthCheckCors = new CorsConfiguration();
		healthCheckCors.setAllowedOriginPatterns(List.of("*"));
		healthCheckCors.setAllowedMethods(List.of("GET", "OPTIONS"));
		healthCheckCors.setAllowedHeaders(List.of("*"));

		// 일반 API용 CORS 설정
		CorsConfiguration defaultCors = new CorsConfiguration();
		defaultCors.setAllowedOriginPatterns(List.of("*"));
		defaultCors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		defaultCors.setAllowedHeaders(List.of("*"));
		defaultCors.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/health-check", healthCheckCors);
		source.registerCorsConfiguration("/**", defaultCors);
		return source;
	}
}
