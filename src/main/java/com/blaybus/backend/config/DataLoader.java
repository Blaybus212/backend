package com.blaybus.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.blaybus.backend.domain.User;
import com.blaybus.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 시점: Spring Container 초기화 -> 모든 Bean 생성 -> 서버 시작 완료 -> ApplicationRunner.run()
 * 목적: 초기 mock 데이터를 삽입하여 공모전 시연에 사용하기 위함
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("모든 Bean이 load된 이후에 초기 mock 데이터를 삽입합니다");

		// 초기 사용자 데이터 삽입 (이미 존재하면 스킵)
		createUserIfNotExists("admin", "admin1234!");

		log.info("삽입 완료!");
	}

	private void createUserIfNotExists(String username, String rawPassword) {
		if (userRepository.findByUsername(username).isEmpty()) {
			User user = User.builder()
				.username(username)
				.password(passwordEncoder.encode(rawPassword))
				.isMockUser(true) // Mock 사용자로 표시
				.build();
			userRepository.save(user);
			log.info("Created mock user: {}", username);
		} else {
			log.info("User already exists, skipping: {}", username);
		}
	}
}
