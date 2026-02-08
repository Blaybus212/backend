package com.blaybus.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.blaybus.backend.domain.Component;

public interface ComponentRepository extends JpaRepository<Component, Long> {

	List<Component> findByIdIn(List<Long> ids);
}
