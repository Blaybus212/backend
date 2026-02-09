package com.blaybus.backend.dto;

import com.blaybus.backend.domain.scene.SceneCategory;
import com.blaybus.backend.domain.user.EducationLevel;
import com.blaybus.backend.domain.user.Persona;
import com.blaybus.backend.domain.user.ThemeColor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EnumDeserializationTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void testEnumDeserialization_LooseFormat() throws Exception {
		// "automotive-engineering" vs "automotive_engineering"
		// "Automotive Engineering" vs "automotive_engineering"
		String json = """
			    {
			        "name": "Test User",
			        "preferCategory": "automotive-engineering",
			        "educationLevel": "beginner",
			        "specialized": "None",
			        "persona": "senior",
			        "themeColor": "blue"
			    }
			""";

		OnboardDto.OnboardRequest request = objectMapper.readValue(json, OnboardDto.OnboardRequest.class);
		assertThat(request.preferCategory()).isEqualTo(SceneCategory.AUTOMOTIVE_ENGINEERING);
		assertThat(request.educationLevel()).isEqualTo(EducationLevel.BEGINNER);
		assertThat(request.persona()).isEqualTo(Persona.SENIOR);
		assertThat(request.themeColor()).isEqualTo(ThemeColor.BLUE);
	}

	@Test
	void testEnumDeserialization_AllEnums_LooseFormat() throws Exception {
		String json = """
			    {
			        "name": "Test User",
			        "preferCategory": "Automotive Engineering",
			        "educationLevel": "Beginner",
			        "specialized": "None",
			        "persona": "SENIOR",
			        "themeColor": "Blue"
			    }
			""";

		OnboardDto.OnboardRequest request = objectMapper.readValue(json, OnboardDto.OnboardRequest.class);

		assertThat(request.preferCategory()).isEqualTo(SceneCategory.AUTOMOTIVE_ENGINEERING);
		assertThat(request.educationLevel()).isEqualTo(EducationLevel.BEGINNER);
		assertThat(request.persona()).isEqualTo(Persona.SENIOR);
		assertThat(request.themeColor()).isEqualTo(ThemeColor.BLUE);
	}

	@Test
	void testEnumDeserialization_MixedFormats() throws Exception {
		String json = """
			    {
			        "name": "Test User",
			        "preferCategory": "manufacturing-engineering",
			        "educationLevel": "INTERMEDIATE",
			        "specialized": "None",
			        "persona": "Assistant",
			        "themeColor": "PINK"
			    }
			""";

		OnboardDto.OnboardRequest request = objectMapper.readValue(json, OnboardDto.OnboardRequest.class);

		assertThat(request.preferCategory()).isEqualTo(SceneCategory.MANUFACTURING_ENGINEERING);
		assertThat(request.educationLevel()).isEqualTo(EducationLevel.INTERMEDIATE);
		assertThat(request.persona()).isEqualTo(Persona.ASSISTANT);
		assertThat(request.themeColor()).isEqualTo(ThemeColor.PINK);
	}
}
