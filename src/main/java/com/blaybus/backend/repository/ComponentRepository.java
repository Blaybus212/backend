package com.blaybus.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.blaybus.backend.domain.alignment.Component;

@Repository
public interface ComponentRepository extends JpaRepository<Component, Long> {
	Optional<Component> findByName(String name);

	List<Component> findByIdIn(List<Long> ids);
}
