package com.workcheck.domain.assignment.repository;

import com.workcheck.domain.assignment.entity.Assignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @Query("select distinct a from Assignment a left join fetch a.rules order by a.id desc")
    List<Assignment> findAllWithRules();

    @Query("select a from Assignment a left join fetch a.rules where a.id = :assignmentId")
    Optional<Assignment> findByIdWithRules(@Param("assignmentId") Long assignmentId);
}
