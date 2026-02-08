package com.blaybus.backend.repository;

import com.blaybus.backend.domain.scene.UserScene;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSceneRepository extends JpaRepository<UserScene, Long> {

    @Query("SELECT us FROM UserScene us JOIN FETCH us.scene WHERE us.user.id = :userId ORDER BY us.lastAccessedAt DESC")
    List<UserScene> findTop3ByUserIdOrderByLastAccessedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
