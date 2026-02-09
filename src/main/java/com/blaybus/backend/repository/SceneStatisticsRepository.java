package com.blaybus.backend.repository;

import com.blaybus.backend.domain.scene.SceneCategory;
import com.blaybus.backend.domain.scene.SceneInformation;
import com.blaybus.backend.domain.scene.SceneStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SceneStatisticsRepository extends JpaRepository<SceneStatistics, Long> {

        @Query("SELECT s FROM SceneStatistics s " +
                        "JOIN FETCH s.scene " +
                        "WHERE s.aggregatedTime = :aggregatedTime " +
                        "AND (:category IS NULL OR s.scene.category = :category) " +
                        "ORDER BY s.rank ASC " +
                        "LIMIT 5")
        List<SceneStatistics> findTop5ByAggregatedTimeAndCategory(
                        @Param("aggregatedTime") LocalDateTime aggregatedTime,
                        @Param("category") SceneCategory category);

        boolean existsBySceneAndAggregatedTime(SceneInformation scene, LocalDateTime aggregatedTime);
}
