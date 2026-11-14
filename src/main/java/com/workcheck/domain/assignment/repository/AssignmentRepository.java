package com.workcheck.domain.assignment.repository;

import com.workcheck.domain.assignment.entity.Assignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @Query("select a from Assignment a left join fetch a.rules where a.member.id = :memberId order by a.id desc")
    List<Assignment> findAllWithRules(@Param("memberId") Long memberId);

    @Query("select a from Assignment a left join fetch a.rules where a.id = :assignmentId and a.member.id = :memberId")
    Optional<Assignment> findByIdWithRules(@Param("assignmentId") Long assignmentId, @Param("memberId") Long memberId);
}
