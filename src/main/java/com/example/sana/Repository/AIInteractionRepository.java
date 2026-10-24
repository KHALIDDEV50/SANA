package com.example.sana.Repository;

import com.example.sana.Model.AIInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface AIInteractionRepository extends JpaRepository<AIInteraction, Integer> {

    Optional<AIInteraction> findByRequestIdAndProject_Id(String requestId, Integer projectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM AIInteraction r WHERE r.id = :id")
    Optional<AIInteraction> findByIdForUpdate(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM AIInteraction r WHERE r.requestId = :requestId")
    Optional<AIInteraction> findByRequestIdForUpdate(@Param("requestId") String requestId);

    @Query("SELECT r.id FROM AIInteraction r WHERE r.requestId IS NOT NULL AND r.status = com.example.sana.Enums.ExecutionStatus.PENDING AND r.dispatchStatus = com.example.sana.Enums.AIDispatchStatus.QUEUED AND r.nextDispatchAt <= :now ORDER BY r.id")
    List<Integer> findDispatchable(@Param("now") LocalDateTime now, Pageable page);

    @Query("SELECT r.id FROM AIInteraction r WHERE r.requestId IS NOT NULL AND r.status IN (com.example.sana.Enums.ExecutionStatus.PENDING, com.example.sana.Enums.ExecutionStatus.RUNNING) AND (r.deadlineAt <= :now OR (r.dispatchStatus = com.example.sana.Enums.AIDispatchStatus.IN_FLIGHT AND r.dispatchLeaseUntil <= :now)) ORDER BY r.id")
    List<Integer> findExpired(@Param("now") LocalDateTime now, Pageable page);

    AIInteraction findAIInteractionById(Integer id);
}
