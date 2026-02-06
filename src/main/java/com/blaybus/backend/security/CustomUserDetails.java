package com.blaybus.backend.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;

/**
 * JWT 인증 후 SecurityContext에 저장되는 사용자 정보
 * Entity를 직접 들고 있지 않고 필요한 정보만 포함
 */
@Getter
public class CustomUserDetails implements UserDetails {

	private final Long userId;
	private final String username;
	private final String preferCategory;

	public CustomUserDetails(Long userId, String username, String preferCategory) {
		this.userId = userId;
		this.username = username;
		this.preferCategory = preferCategory;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.emptyList(); // 권한 체계 없음
	}

	@Override
	public String getPassword() {
		return null; // JWT 인증에서는 불필요
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
