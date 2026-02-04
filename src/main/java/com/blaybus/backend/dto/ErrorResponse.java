package com.blaybus.backend.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.validation.BindingResult;

import com.blaybus.backend.exception.ErrorCode;

import lombok.Getter;

@Getter
public class ErrorResponse {
	private final String code;
	private final String message;
	private final String traceId;
	private final List<ValidationError> errors;

	private ErrorResponse(ErrorCode code, List<ValidationError> errors, String traceId) {
		this.code = code.getClass().getSimpleName() + "." + ((Enum<?>)code).name();
		this.message = code.getMessage();
		this.traceId = traceId;
		this.errors = errors;
	}

	private ErrorResponse(ErrorCode code, String traceId) {
		this.code = code.getClass().getSimpleName() + "." + ((Enum<?>)code).name();
		this.message = code.getMessage();
		this.traceId = traceId;
		this.errors = new ArrayList<>();
	}

	public static ErrorResponse of(ErrorCode code, String traceId) {
		return new ErrorResponse(code, traceId);
	}

	public static ErrorResponse of(ErrorCode code, BindingResult bindingResult, String traceId) {
		return new ErrorResponse(code, ValidationError.of(bindingResult), traceId);
	}

	@Getter
	public static class ValidationError {
		private final String field;
		private final String value;
		private final String reason;

		private ValidationError(String field, String value, String reason) {
			this.field = field;
			this.value = value;
			this.reason = reason;
		}

		public static List<ValidationError> of(BindingResult bindingResult) {
			return bindingResult.getFieldErrors().stream()
				.map(error -> new ValidationError(
					error.getField(),
					error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
					error.getDefaultMessage()))
				.collect(Collectors.toList());
		}
	}
}
