package com.blaybus.backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blaybus.backend.domain.alignment.Component;
import com.blaybus.backend.domain.conversation.Conversation;
import com.blaybus.backend.domain.conversation.Message;
import com.blaybus.backend.domain.conversation.Reference;
import com.blaybus.backend.domain.conversation.Sender;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.ConversationDto.ComponentInfo;
import com.blaybus.backend.dto.ConversationDto.ConversationResponse;
import com.blaybus.backend.dto.ConversationDto.MessageResponse;
import com.blaybus.backend.dto.ConversationDto.PageInfo;
import com.blaybus.backend.dto.ConversationDto.SendMessageRequest;
import com.blaybus.backend.dto.ConversationDto.SendMessageResponse;
import com.blaybus.backend.dto.OpenAiDto.AssistantResponse;
import com.blaybus.backend.exception.BusinessException;
import com.blaybus.backend.exception.CommonErrorCode;
import com.blaybus.backend.repository.ComponentRepository;
import com.blaybus.backend.repository.ConversationRepository;
import com.blaybus.backend.repository.MessageRepository;
import com.blaybus.backend.repository.ReferenceRepository;
import com.blaybus.backend.repository.SceneInformationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConversationService {

	private final ConversationRepository conversationRepository;
	private final MessageRepository messageRepository;
	private final ComponentRepository componentRepository;
	private final ReferenceRepository referenceRepository;
	private final SceneInformationRepository sceneInformationRepository;
	private final OpenAiService openAiService;
	private final PromptService promptService;

	@Transactional(readOnly = true)
	public ConversationResponse getConversation(User user, Long sceneId, Long cursor, int limit) {
		Conversation conversation = conversationRepository.findByUserAndSceneId(user, sceneId)
			.orElse(null);

		if (conversation == null) {
			return new ConversationResponse(List.of(), emptyPageInfo(limit));
		}

		Slice<Message> messageSlice;
		if (cursor != null) {
			messageSlice = messageRepository.findByConversationAndIdLessThanOrderByIdDesc(
				conversation, cursor, PageRequest.of(0, limit + 1));
		} else {
			messageSlice = messageRepository.findByConversationOrderByIdDesc(
				conversation, PageRequest.of(0, limit + 1));
		}

		List<Message> messages = messageSlice.getContent();
		boolean hasNext = messages.size() > limit;
		if (hasNext) {
			messages = messages.subList(0, limit);
		}

		List<Long> messageIds = messages.stream().map(Message::getId).toList();
		Map<Long, List<Reference>> referencesByMessageId = loadReferences(messageIds);

		List<MessageResponse> messageResponses = messages.stream()
			.map(msg -> MessageResponse.from(msg, buildComponentInfoMap(referencesByMessageId.get(msg.getId()))))
			.toList();

		List<MessageResponse> reversedMessages = new ArrayList<>(messageResponses);
		Collections.reverse(reversedMessages);

		String prevCursor = messages.isEmpty() ? null : String.valueOf(messages.get(0).getId());
		String nextCursor = hasNext ? String.valueOf(messages.get(messages.size() - 1).getId()) : null;

		PageInfo pageInfo = new PageInfo(
			prevCursor,
			nextCursor,
			cursor != null,
			hasNext,
			limit);

		return new ConversationResponse(reversedMessages, pageInfo);
	}

	@Transactional
	public SendMessageResponse sendMessage(User user, Long sceneId, SendMessageRequest request) {
		Conversation conversation = conversationRepository.findByUserAndSceneId(user, sceneId)
			.orElseGet(() -> {
				var scene = sceneInformationRepository.findById(sceneId)
					.orElseThrow(() -> new BusinessException(CommonErrorCode.SCENE_NOT_FOUND));
				return conversationRepository.save(
					Conversation.builder()
						.user(user)
						.scene(scene)
						.build());
			});

		List<Component> components = loadComponents(request.getComponentIds());
		Map<String, ComponentInfo> componentInfoMap = components.stream()
			.collect(Collectors.toMap(
				c -> String.valueOf(c.getId()),
				ComponentInfo::from));

		Message userMessage = Message.builder()
			.conversation(conversation)
			.sender(Sender.USER)
			.content(request.content())
			.postedAt(java.time.LocalDateTime.now())
			.build();
		messageRepository.save(userMessage);

		for (Component component : components) {
			Reference reference = Reference.builder()
				.message(userMessage)
				.component(component)
				.build();
			referenceRepository.save(reference);
		}

		String systemPrompt = promptService.buildSystemPrompt(sceneId, user);
		String userPrompt = promptService.buildUserPrompt(
			conversation.getSummary(),
			components,
			request.content());

		AssistantResponse aiResponse = openAiService.chat(systemPrompt, userPrompt);

		Message assistantMessage = Message.builder()
			.conversation(conversation)
			.sender(Sender.ASSISTANT)
			.content(aiResponse.answer())
			.postedAt(java.time.LocalDateTime.now())
			.build();
		messageRepository.save(assistantMessage);

		conversation.updateSummary(aiResponse.summary());

		return SendMessageResponse.from(assistantMessage, componentInfoMap);
	}

	private List<Component> loadComponents(List<Long> componentIds) {
		if (componentIds == null || componentIds.isEmpty()) {
			return List.of();
		}

		List<Component> components = componentRepository.findByIdIn(componentIds);
		if (components.size() != componentIds.size()) {
			throw new BusinessException(CommonErrorCode.COMPONENT_NOT_FOUND);
		}
		return components;
	}

	private Map<Long, List<Reference>> loadReferences(List<Long> messageIds) {
		if (messageIds.isEmpty()) {
			return Map.of();
		}

		List<Reference> references = referenceRepository.findByMessageIdInWithComponent(messageIds);
		return references.stream()
			.collect(Collectors.groupingBy(ref -> ref.getMessage().getId()));
	}

	private Map<String, ComponentInfo> buildComponentInfoMap(List<Reference> references) {
		if (references == null || references.isEmpty()) {
			return Map.of();
		}

		Map<String, ComponentInfo> result = new HashMap<>();
		for (Reference ref : references) {
			Component component = ref.getComponent();
			result.put(String.valueOf(component.getId()), ComponentInfo.from(component));
		}
		return result;
	}

	private PageInfo emptyPageInfo(int limit) {
		return new PageInfo(null, null, false, false, limit);
	}
}
