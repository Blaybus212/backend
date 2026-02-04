package com.blaybus.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "prod"})
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("CORS 설정에 등록된 Origin으로 요청 시 Access-Control-Allow-Origin 헤더가 반환되어야 한다")
	void shouldReturnAllowOriginHeaderForAllowedOrigin() throws Exception {
		mockMvc.perform(options("/health-check")
			.header("Origin", "http://localhost:3000")
			.header("Access-Control-Request-Method", "GET"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
			.andExpect(header().string("Access-Control-Allow-Credentials", "true"));
	}

	@Test
	@DisplayName("허용되지 않은 Origin으로 요청 시 Access-Control-Allow-Origin 헤더가 반환되지 않아야 한다")
	void shouldNotReturnAllowOriginHeaderForUnallowedOrigin() throws Exception {
		mockMvc.perform(options("/health-check")
			.header("Origin", "http://unallowed-origin.com")
			.header("Access-Control-Request-Method", "GET"))
			.andExpect(status().isForbidden());
	}
}
