package com.rideshare.platform.audit.service;

import com.rideshare.platform.audit.entity.AuditLog;
import com.rideshare.platform.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * FR: Section 18 Audit. Records are append-only (no update/delete repository methods exposed) -
 * this satisfies the "Immutable audit records" requirement at the application layer.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void record(Long userId, String action, String entityType, String entityId, String detailsJson) {
        AuditLog log = new AuditLog();
        log.setCorrelationId(MDC.get("correlationId"));
        log.setUserId(userId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetailsJson(detailsJson);
        auditLogRepository.save(log);
    }
}
