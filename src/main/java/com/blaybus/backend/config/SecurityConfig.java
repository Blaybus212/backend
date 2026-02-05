package com.blaybus.backend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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

import com.blaybus.backend.security.JwtAuthenticationFilter;
import com.blaybus.backend.security.JwtTokenProvider;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Value("${app.cors.allowed-origins}")
	private List<String> allowedOrigins;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/health-check", "/actuator/prometheus", "/login").permitAll()
				.anyRequest().authenticated())
			.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
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
		defaultCors.setAllowedOrigins(allowedOrigins);
		defaultCors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		defaultCors.setAllowedHeaders(List.of("*"));
		defaultCors.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/health-check", healthCheckCors);
		source.registerCorsConfiguration("/**", defaultCors);
		return source;
	}
}
