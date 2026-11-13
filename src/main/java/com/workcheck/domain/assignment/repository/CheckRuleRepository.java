package com.workcheck.domain.assignment.repository;

import com.workcheck.domain.assignment.entity.CheckRule;
import com.workcheck.global.data.FileType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckRuleRepository extends JpaRepository<CheckRule, Long> {

    List<CheckRule> findByAssignment_IdAndFileType(Long assignmentId, FileType fileType);
}
