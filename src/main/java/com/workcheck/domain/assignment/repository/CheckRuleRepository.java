package com.workcheck.domain.assignment.repository;

import com.workcheck.domain.assignment.entity.CheckRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckRuleRepository extends JpaRepository<CheckRule, Long> {
}
