package com.judgment.query.repository;

import com.judgment.query.entity.AuditLog;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {}
