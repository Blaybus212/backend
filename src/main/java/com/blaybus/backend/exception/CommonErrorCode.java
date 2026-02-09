package com.blaybus.backend.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 여기에 모든 Exception을 정의해주세요!
 * 시스템에 존재하는 모든 예외들을 파악하고, 중복되는 예외를 최소한으로 하기 위함입니다.
 */

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

	INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "파라미터가 유효하지 않습니다."),

	LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),

	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
	CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "대화를 찾을 수 없습니다."),
	COMPONENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부품 정보를 찾을 수 없습니다."),
	SCENE_NOT_FOUND(HttpStatus.NOT_FOUND, "씬 정보를 찾을 수 없습니다."),

	QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "퀴즈를 찾을 수 없습니다."),
	QUIZ_PROGRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "퀴즈 진행 상황을 찾을 수 없습니다."),

	EMBEDDING_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "임베딩 API 호출에 실패했습니다."),
	OPENAI_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스 연결에 실패했습니다."),
	OPENAI_TOKEN_EXCEEDED(HttpStatus.BAD_REQUEST, "응답이 너무 깁니다. 질문을 더 짧게 해주세요."),
	OPENAI_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 처리 중 오류가 발생했습니다."),

	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
