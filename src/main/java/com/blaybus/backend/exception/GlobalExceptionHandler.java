package com.blaybus.backend.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.blaybus.backend.dto.ErrorResponse;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final Tracer tracer;

	// 1. user input 에 대한 controller단 공통 예외 처리 핸들러
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
		return ResponseEntity
			.badRequest()
			.body(ErrorResponse.of(CommonErrorCode.INVALID_PARAMETER, ex.getBindingResult(), getTraceId()));
	}

	// 2. business logic 에 대한 service단 공통 예외 처리 핸들러
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessExceptions(BusinessException ex) {
		return ResponseEntity
			.status(ex.getErrorCode().getHttpStatus())
			.body(ErrorResponse.of(ex.getErrorCode(), getTraceId()));
	}

	private String getTraceId() {
		return tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "N/A";
	}
}
