package com.workcheck.domain.check.repository;

import com.workcheck.domain.check.entity.CheckHistory;
import com.workcheck.global.data.CheckStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckHistoryRepository extends JpaRepository<CheckHistory, Long> {

    List<CheckHistory> findByAssignment_IdOrderByCreatedAtDescIdDesc(Long assignmentId);

    List<CheckHistory> findByAssignment_IdAndStatusOrderByCreatedAtDescIdDesc(Long assignmentId, CheckStatus status);

    @Query("select h from CheckHistory h left join fetch h.results where h.id = :checkId and h.assignment.id = :assignmentId")
    Optional<CheckHistory> findByIdWithResults(@Param("checkId") Long checkId, @Param("assignmentId") Long assignmentId);
}
