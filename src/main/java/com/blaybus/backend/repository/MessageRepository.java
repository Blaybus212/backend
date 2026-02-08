package com.blaybus.backend.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.blaybus.backend.domain.Conversation;
import com.blaybus.backend.domain.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

	@Query("SELECT m FROM Message m WHERE m.conversation = :conversation AND m.id < :cursor ORDER BY m.id DESC")
	Slice<Message> findByConversationAndIdLessThanOrderByIdDesc(
		@Param("conversation")
		Conversation conversation,
		@Param("cursor")
		Long cursor,
		Pageable pageable);

	@Query("SELECT m FROM Message m WHERE m.conversation = :conversation ORDER BY m.id DESC")
	Slice<Message> findByConversationOrderByIdDesc(
		@Param("conversation")
		Conversation conversation,
		Pageable pageable);

	List<Message> findByConversationOrderByPostedAtAsc(Conversation conversation);
}
