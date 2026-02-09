package com.blaybus.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.blaybus.backend.domain.scene.SceneCategory;
import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.scene.UserScene;
import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.dto.scene.UserSceneNoteRequest;
import com.blaybus.backend.repository.SceneInformationRepository;
import com.blaybus.backend.repository.UserRepository;
import com.blaybus.backend.repository.UserSceneRepository;
import com.blaybus.backend.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserSceneNoteIntegrationTest {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SceneInformationRepository sceneInformationRepository;

	@Autowired
	private UserSceneRepository userSceneRepository;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
			.webAppContextSetup(context)
			.apply(springSecurity())
			.build();

		userSceneRepository.deleteAll();
		sceneInformationRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("사용자는 Scene에 대한 노트를 작성하고 조회할 수 있다.")
	void updateAndGetNote() throws Exception {
		// given
		User user = userRepository.save(User.builder()
			.username("note_user")
			.password("pass")
			.name("Note User")
			.isMockUser(false)
			.onBoardingCompleted(true)
			.build());

		CustomUserDetails userDetails = new CustomUserDetails(user.getId(), user.getUsername(), null);

		SceneInformation scene = sceneInformationRepository.save(SceneInformation.builder()
			.title("Note Test Scene")
			.engTitle("Note Test Scene")
			.category(SceneCategory.MANUFACTURING_ENGINEERING)
			.assetPath("path")
			.description("Desc")
			.participantsCount(0L)
			.defaultAlignmentId(0L)
			.build());

		String noteContent = "# My First Note\nThis is **important**.";
		UserSceneNoteRequest request = new UserSceneNoteRequest(noteContent);

		// when & then: 1. 노트 저장 (Create)
		mockMvc.perform(put("/scenes/" + scene.getId() + "/note")
			.with(user(userDetails))
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").value(noteContent));

		// then: 2. DB 확인
		UserScene userScene = userSceneRepository.findByUserIdAndSceneId(user.getId(), scene.getId())
			.orElseThrow();
		assertThat(userScene.getNote()).isEqualTo(noteContent);

		// when & then: 3. 노트 조회
		mockMvc.perform(get("/scenes/" + scene.getId() + "/note")
			.with(user(userDetails)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").value(noteContent));

		// when & then: 4. 노트 수정 (Update)
		String updatedContent = "# Updated Note\nNew content.";
		UserSceneNoteRequest updateRequest = new UserSceneNoteRequest(updatedContent);

		mockMvc.perform(put("/scenes/" + scene.getId() + "/note")
			.with(user(userDetails))
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(updateRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").value(updatedContent));

		// then: 5. 수정된 DB 확인
		UserScene updatedUserScene = userSceneRepository.findById(userScene.getId()).orElseThrow();
		assertThat(updatedUserScene.getNote()).isEqualTo(updatedContent);
	}

	@Test
	@DisplayName("노트 내용이 3000자를 초과하면 400 에러가 발생한다.")
	void validateNoteLength() throws Exception {
		// given
		User user = userRepository.save(User.builder()
			.username("note_user_2")
			.password("pass")
			.name("Note User 2")
			.isMockUser(false)
			.onBoardingCompleted(true)
			.build());

		CustomUserDetails userDetails = new CustomUserDetails(user.getId(), user.getUsername(), null);

		SceneInformation scene = sceneInformationRepository.save(SceneInformation.builder()
			.title("Note Test Scene 2")
			.engTitle("Note Test Scene 2")
			.category(SceneCategory.MANUFACTURING_ENGINEERING)
			.assetPath("path")
			.description("Desc")
			.participantsCount(0L)
			.defaultAlignmentId(0L)
			.build());

		String longContent = "a".repeat(3001);
		UserSceneNoteRequest request = new UserSceneNoteRequest(longContent);

		// when & then
		mockMvc.perform(put("/scenes/" + scene.getId() + "/note")
			.with(user(userDetails))
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}
}
