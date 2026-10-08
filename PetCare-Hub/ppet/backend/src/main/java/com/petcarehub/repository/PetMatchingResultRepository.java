package com.petcarehub.repository;

import com.petcarehub.entity.PetMatchingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PetMatchingResultRepository extends JpaRepository<PetMatchingResult, Long> {
    List<PetMatchingResult> findByQuizId(Long quizId);
}
