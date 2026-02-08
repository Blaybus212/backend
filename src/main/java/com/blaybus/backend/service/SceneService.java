package com.blaybus.backend.service;

import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.scene.UserScene;
import com.blaybus.backend.dto.SceneResponse;
import com.blaybus.backend.repository.UserSceneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SceneService {

    private final UserSceneRepository userSceneRepository;

    public SceneResponse getLearningScenes(Long userId) {
        List<UserScene> top3UserScenes = userSceneRepository.findTop3ByUserIdOrderByLastAccessedAtDesc(userId,
                PageRequest.of(0, 3));

        List<SceneResponse.SceneDto> sceneDtos = top3UserScenes.stream()
                .map(userScene -> {
                    SceneInformation sceneInfo = userScene.getScene();
                    return SceneResponse.SceneDto.builder()
                            .id(sceneInfo.getId().toString())
                            .title(sceneInfo.getTitle())
                            .engTitle(sceneInfo.getEngTitle())
                            .category(sceneInfo.getCategory())
                            .imageUrl(sceneInfo.getThumbnailUrl())
                            // TODO: 진척도 로직 실제 데이터 기반으로 수정 필요
                            .progress(35)
                            .popular(sceneInfo.getParticipantsCount() >= 5)
                            .lastAccessedAt(userScene.getLastAccessedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return SceneResponse.builder()
                .scenes(sceneDtos)
                .build();
    }
}
