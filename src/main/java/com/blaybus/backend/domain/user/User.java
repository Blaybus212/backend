package com.blaybus.backend.domain.user;

import com.blaybus.backend.domain.UserEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "users")
@EntityListeners(UserEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false)
	private String password;

	@Column(length = 100)
	private String name;

	@Column(nullable = false)
	private boolean isMockUser = false;

	@Column(nullable = false)
	private boolean onBoardingCompleted = false;

	@Enumerated(EnumType.STRING)
	@Column(length = 50)
	private Persona persona;

	@Enumerated(EnumType.STRING)
	@Column(length = 50)
	private EducationLevel educationLevel;

	@Column(length = 50)
	private String preferCategory;

	@Column(length = 150)
	private String specializedIn;

	@Enumerated(EnumType.STRING)
	@Column(length = 50)
	private ThemeColor themeColor;

	@Builder
	public User(
		String username,
		String password,
		String name,
		boolean isMockUser,
		boolean onBoardingCompleted,
		Persona persona,
		EducationLevel educationLevel,
		String preferCategory,
		String specializedIn,
		ThemeColor themeColor) {
		this.username = username;
		this.password = password;
		this.name = name;
		this.isMockUser = isMockUser;
		this.onBoardingCompleted = onBoardingCompleted;
		this.persona = persona;
		this.educationLevel = educationLevel;
		this.preferCategory = preferCategory;
		this.specializedIn = specializedIn;
		this.themeColor = themeColor;
	}
}
