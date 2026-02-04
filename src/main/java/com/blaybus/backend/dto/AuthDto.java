package com.blaybus.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

public class AuthDto {

	private AuthDto() {}

	@Builder
	public record LoginRequest(
		@NotBlank(message = "아이디는 필수입니다.")
		String username,

		@NotBlank(message = "비밀번호는 필수입니다.") @Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.")
		String password) {
	}

	@Builder
	public record LoginResponse(String accessToken) {
	}
}
