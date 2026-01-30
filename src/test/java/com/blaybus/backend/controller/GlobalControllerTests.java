package com.blaybus.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.blaybus.backend.config.SecurityConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalController.class)
@Import(SecurityConfig.class)
public class GlobalControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthCheck() throws Exception {
		mockMvc.perform(get("/health-check"))
			.andExpect(status().isOk())
			.andExpect(content().string("OK"));
	}
}
