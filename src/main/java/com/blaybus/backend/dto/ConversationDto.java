package com.blaybus.backend.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.blaybus.backend.domain.alignment.Component;
import com.blaybus.backend.domain.conversation.Message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ConversationDto {

	private static final DateTimeFormatter POSTED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	public record SendMessageRequest(
		@NotBlank(message = "메시지 내용은 필수입니다.")
		String content,
		List<ComponentReference> references) {
		public List<Long> getComponentIds() {
			if (references == null || references.isEmpty()) {
				return List.of();
			}
			return references.stream()
				.map(ComponentReference::componentId)
				.toList();
		}
	}

	public record ComponentReference(
		@NotNull(message = "컴포넌트 ID는 필수입니다.")
		Long componentId) {
	}

	public record SendMessageResponse(
		String sender,
		String content,
		String postedAt,
		Map<String, ComponentInfo> references) {
		public static SendMessageResponse from(Message message, Map<String, ComponentInfo> references) {
			return new SendMessageResponse(
				message.getSender().name(),
				message.getContent(),
				message.getPostedAt().format(POSTED_AT_FORMATTER),
				references);
		}
	}

	public record ConversationResponse(
		List<MessageResponse> messages,
		PageInfo pages) {
	}

	public record MessageResponse(
		String sender,
		String content,
		String postedAt,
		Map<String, ComponentInfo> references) {
		public static MessageResponse from(Message message, Map<String, ComponentInfo> references) {
			return new MessageResponse(
				message.getSender().name(),
				message.getContent(),
				formatPostedAt(message.getPostedAt()),
				references);
		}

		private static String formatPostedAt(LocalDateTime postedAt) {
			return postedAt.format(POSTED_AT_FORMATTER);
		}
	}

	public record ComponentInfo(
		String name,
		String description,
		String texture,
		String usage) {
		public static ComponentInfo from(Component component) {
			return new ComponentInfo(
				component.getName(),
				component.getDescription(),
				component.getTexture(),
				component.getUsage());
		}
	}

	public record PageInfo(
		String prevCursor,
		String nextCursor,
		boolean hasPrevious,
		boolean hasNext,
		int limit) {
	}

	public record ConversationSummaryResponse(
		String summary,
		int totalConversations,
		int totalMessages) {
	}
}
