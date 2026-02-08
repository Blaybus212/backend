package com.blaybus.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.blaybus.backend.domain.Component;

@Service
public class PromptService {

	private static final String SYSTEM_PROMPT_TEMPLATE = """
		당신은 SIMVEX의 공학 학습 어시스턴트입니다.

		## 역할
		- 3D 모델을 학습하는 공대생을 돕는 친근한 선배 역할
		- 부품의 구조, 원리, 활용에 대해 명확하고 간결하게 설명

		## 규칙
		1. 한국어로 답변합니다.
		2. 부품명은 **굵게** 강조합니다.
		3. 기술 용어는 쉽게 풀어서 설명합니다.
		4. 답변은 2-3문단 이내로 간결하게 작성합니다.
		5. 불확실한 정보는 추측하지 않고 솔직히 모른다고 말합니다.

		## 응답 형식
		반드시 JSON으로 응답하세요:
		- answer: 사용자 질문에 대한 답변
		- summary: 이번 대화의 핵심 내용을 1-2문장으로 요약 (다음 대화의 맥락용)
		""";

	private static final String USER_PROMPT_TEMPLATE = """
		%s
		## 참조된 부품 정보
		%s

		## 사용자 질문
		%s
		""";

	private static final String RUNNING_SUMMARY_SECTION = """
		## 이전 대화 요약
		%s

		""";

	public String buildSystemPrompt(Long sceneId) {
		return SYSTEM_PROMPT_TEMPLATE;
	}

	public String buildUserPrompt(String runningSummary, List<Component> components, String userQuery) {
		String summarySection = "";
		if (runningSummary != null && !runningSummary.isBlank()) {
			summarySection = String.format(RUNNING_SUMMARY_SECTION, runningSummary);
		}

		String componentContext = buildComponentContext(components);

		return String.format(USER_PROMPT_TEMPLATE, summarySection, componentContext, userQuery);
	}

	private String buildComponentContext(List<Component> components) {
		if (components == null || components.isEmpty()) {
			return "(참조된 부품 없음)";
		}

		StringBuilder sb = new StringBuilder();
		for (Component component : components) {
			sb.append(String.format("""
				### %s (ID: %d)
				- 설명: %s
				- 재질: %s
				- 용도: %s

				""",
				component.getName(),
				component.getId(),
				component.getDescription() != null ? component.getDescription() : "정보 없음",
				component.getTexture() != null ? component.getTexture() : "정보 없음",
				component.getUsage() != null ? component.getUsage() : "정보 없음"));
		}
		return sb.toString();
	}
}
