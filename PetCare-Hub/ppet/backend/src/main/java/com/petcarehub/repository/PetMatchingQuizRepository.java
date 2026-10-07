package com.petcarehub.repository;

import com.petcarehub.entity.PetMatchingQuiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PetMatchingQuizRepository extends JpaRepository<PetMatchingQuiz, Long> {
    Page<PetMatchingQuiz> findByCustomerId(Long customerId, Pageable pageable);
}
