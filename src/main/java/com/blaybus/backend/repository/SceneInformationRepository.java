package com.blaybus.backend.repository;

import java.util.Optional;

import com.blaybus.backend.domain.scene.SceneCategory;
import com.blaybus.backend.domain.scene.SceneInformation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.blaybus.backend.domain.scene.SceneInformation;

@Repository
public interface SceneInformationRepository extends JpaRepository<SceneInformation, Long> {

	Optional<SceneInformation> findByEngTitle(String engTitle);
  Optional<SceneInformation> findByTitle(String title);
  
  @Query("SELECT s FROM SceneInformation s " +
         "WHERE (:category IS NULL OR s.category = :category) " +
         "AND (:query IS NULL OR s.title LIKE CONCAT('%', :query, '%') OR s.engTitle LIKE CONCAT('%', :query, '%'))")
  Page<SceneInformation> findByCategoryAndQuery(
    @Param("category") SceneCategory category,
    @Param("query") String query,
    Pageable pageable);
}
