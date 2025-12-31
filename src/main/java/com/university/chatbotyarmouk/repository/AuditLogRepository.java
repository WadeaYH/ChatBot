package com.university.chatbotyarmouk.repository;


import com.university.chatbotyarmouk.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
