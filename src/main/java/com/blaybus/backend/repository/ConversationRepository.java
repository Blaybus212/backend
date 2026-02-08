package com.blaybus.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blaybus.backend.domain.Conversation;
import com.blaybus.backend.domain.user.User;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

	Optional<Conversation> findByUserAndSceneId(User user, Long sceneId);
}
