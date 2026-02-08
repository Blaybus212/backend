package com.blaybus.backend.repository;

import com.blaybus.backend.domain.user.User;
import com.blaybus.backend.domain.user.UserGrass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserGrassRepository extends JpaRepository<UserGrass, Long> {
    List<UserGrass> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);

    Optional<UserGrass> findByUserAndDate(User user, LocalDate date);

    Optional<UserGrass> findFirstByUserAndDateLessThanEqualOrderByDateDesc(User user, LocalDate date);
}
