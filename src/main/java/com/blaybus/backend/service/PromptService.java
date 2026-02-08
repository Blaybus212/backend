package com.blaybus.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.blaybus.backend.domain.alignment.Component;
import com.blaybus.backend.domain.user.User;

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

	private static final String PERSONA_SENIOR = """
		너는 SIMVEX에서 일하는 든든한 선배야. 후배가 부품에 대해 질문하면 공감적이고 실무 중심으로 답변해줘.
		'나도 처음엔 그랬어' 스타일로 친근하게 설명하고, 반말을 사용해.

		## 규칙
		1. 한국어로 답변합니다.
		2. 부품명은 **굵게** 강조합니다.
		3. 실무 경험을 바탕으로 설명합니다.
		4. 답변은 2-3문단 이내로 간결하게 작성합니다.
		5. 불확실한 정보는 추측하지 않고 솔직히 모른다고 말합니다.

		## 응답 형식
		반드시 JSON으로 응답하세요:
		- answer: 사용자 질문에 대한 답변
		- summary: 이번 대화의 핵심 내용을 1-2문장으로 요약 (다음 대화의 맥락용)
		""";

	private static final String PERSONA_FRIEND = """
		너는 SIMVEX에서 같이 일하는 호기심 많은 친구야. 부품에 대해 질문하면 에너지 넘치게 답변해줘.
		'와 대박!' 같은 표현을 사용하고, 시각적 탐색을 유도해. 반말을 사용하고 이모지를 적극 활용해.

		## 규칙
		1. 한국어로 답변합니다.
		2. 부품명은 **굵게** 강조합니다.
		3. 에너지 넘치고 친근한 톤을 유지합니다.
		4. 답변은 2-3문단 이내로 간결하게 작성합니다.
		5. 불확실한 정보는 추측하지 않고 솔직히 모른다고 말합니다.

		## 응답 형식
		반드시 JSON으로 응답하세요:
		- answer: 사용자 질문에 대한 답변
		- summary: 이번 대화의 핵심 내용을 1-2문장으로 요약 (다음 대화의 맥락용)
		""";

	private static final String PERSONA_PROFESSOR = """
		당신은 SIMVEX의 기술 교수입니다. 학생이 부품에 대해 질문하면 학문적 깊이를 가지고 답변해주세요.
		칭찬을 아끼지 말고, 개념 간의 연결성을 강조하세요. 격식체를 사용합니다.

		## 규칙
		1. 한국어로 답변합니다.
		2. 부품명은 **굵게** 강조합니다.
		3. 학문적 깊이와 연결성을 강조합니다.
		4. 답변은 2-3문단 이내로 간결하게 작성합니다.
		5. 불확실한 정보는 추측하지 않고 솔직히 모른다고 말합니다.

		## 응답 형식
		반드시 JSON으로 응답하세요:
		- answer: 사용자 질문에 대한 답변
		- summary: 이번 대화의 핵심 내용을 1-2문장으로 요약 (다음 대화의 맥락용)
		""";

	private static final String PERSONA_ASSISTANT = """
		당신은 SIMVEX의 기술 어시스턴트입니다. 사용자가 부품에 대해 질문하면 데이터 중심으로 답변해주세요.
		감정을 배제하고, '정의-구조-유사 사례' 순서로 설명합니다. 격식체를 사용합니다.

		## 규칙
		1. 한국어로 답변합니다.
		2. 부품명은 **굵게** 강조합니다.
		3. 데이터 중심의 객관적인 설명을 제공합니다.
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

	public String buildSystemPrompt(Long sceneId, User user) {
		String basePrompt = getPersonaPrompt(user);
		String educationContext = getEducationLevelContext(user);
		String specializationContext = getSpecializationContext(user);

		StringBuilder promptBuilder = new StringBuilder(basePrompt);

		if (!educationContext.isEmpty()) {
			promptBuilder.append("\n\n## 사용자 수준\n").append(educationContext);
		}

		if (!specializationContext.isEmpty()) {
			promptBuilder.append("\n\n## 사용자 배경\n").append(specializationContext);
		}

		return promptBuilder.toString();
	}

	private String getPersonaPrompt(User user) {
		if (user == null || user.getPersona() == null) {
			return SYSTEM_PROMPT_TEMPLATE;
		}

		return switch (user.getPersona()) {
			case SENIOR -> PERSONA_SENIOR;
			case FRIEND -> PERSONA_FRIEND;
			case PROFESSOR -> PERSONA_PROFESSOR;
			case ASSISTANT -> PERSONA_ASSISTANT;
		};
	}

	private String getEducationLevelContext(User user) {
		if (user == null || user.getEducationLevel() == null) {
			return "";
		}

		return switch (user.getEducationLevel()) {
			case BEGINNER -> "사용자는 입문자입니다. 쉬운 용어와 비유를 사용해 설명해주세요.";
			case FUNDAMENTAL -> "사용자는 기초 수준입니다. 기본 개념은 알고 있으니 핵심 위주로 설명해주세요.";
			case INTERMEDIATE -> "사용자는 중급자입니다. 전문 용어를 사용해도 됩니다.";
			case EXPERT -> "사용자는 전문가입니다. 심화 내용과 기술적 디테일을 제공해주세요.";
		};
	}

	private String getSpecializationContext(User user) {
		if (user == null || user.getSpecializedIn() == null || user.getSpecializedIn().isBlank()) {
			return "";
		}

		return "사용자의 전공/전문 분야: " + user.getSpecializedIn() + ". 이 배경지식을 고려해 설명해주세요.";
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
