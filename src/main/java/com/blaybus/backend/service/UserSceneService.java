package com.blaybus.backend.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.scene.UserScene;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.scene.UserSceneNoteResponse;
import com.blaybus.backend.exception.BusinessException;
import com.blaybus.backend.exception.CommonErrorCode;
import com.blaybus.backend.repository.SceneInformationRepository;
import com.blaybus.backend.repository.UserRepository;
import com.blaybus.backend.repository.UserSceneRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSceneService {

	private final UserSceneRepository userSceneRepository;
	private final UserRepository userRepository;
	private final SceneInformationRepository sceneInformationRepository;

	private static final String DEFAULT_LOOK_AT = "{\"position\": {\"x\": 0, \"y\": 0, \"z\": 10}, \"target\": {\"x\": 0, \"y\": 0, \"z\": 0}}";

	public UserSceneNoteResponse getNote(Long userId, Long sceneId) {
		UserScene userScene = userSceneRepository.findByUserIdAndSceneId(userId, sceneId)
			.orElse(null);

		if (userScene == null || userScene.getNote() == null) {
			return UserSceneNoteResponse.of("");
		}

		return UserSceneNoteResponse.of(userScene.getNote());
	}

	@Transactional
	public UserSceneNoteResponse updateNote(Long userId, Long sceneId, String content) {
		UserScene userScene = userSceneRepository.findByUserIdAndSceneId(userId, sceneId)
			.orElseGet(() -> createNewUserScene(userId, sceneId));

		UserScene updatedUserScene = UserScene.builder()
			.id(userScene.getId())
			.user(userScene.getUser())
			.scene(userScene.getScene())
			.note(content)
			.lookAt(userScene.getLookAt()) // 기존 lookAt 유지
			.disassemblyLevel(userScene.getDisassemblyLevel()) // 기존 disassemblyLevel 유지
			.lastAccessedAt(LocalDateTime.now())
			.build();

		userSceneRepository.save(updatedUserScene);

		return UserSceneNoteResponse.of(updatedUserScene.getNote());
	}

	private UserScene createNewUserScene(Long userId, Long sceneId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.USER_NOT_FOUND));
		SceneInformation scene = sceneInformationRepository.findById(sceneId)
			.orElseThrow(() -> new BusinessException(CommonErrorCode.SCENE_NOT_FOUND));

		return UserScene.builder()
			.user(user)
			.scene(scene)
			.lookAt(DEFAULT_LOOK_AT) // 기본값 설정
			.note("")
			.lastAccessedAt(LocalDateTime.now())
			.build();
	}
}
