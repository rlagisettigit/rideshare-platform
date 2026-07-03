package com.rideshare.platform.audit.aspect;

import com.rideshare.platform.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * FR: Section 18 Audit - "Audit every: Login, Logout, Ride Publish, Booking, Payment,
 * Refund, Admin Action, Configuration Change." Cross-cutting via @Audited-annotated methods
 * so services don't hand-roll audit calls at every call site.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object result = joinPoint.proceed();
        // userId resolution and entityId extraction from `result`/args is call-site specific;
        // wire it here once the surrounding SecurityContext -> internal user id lookup is in place.
        auditService.record(null, audited.action(), audited.entityType(), null, null);
        return result;
    }
}
