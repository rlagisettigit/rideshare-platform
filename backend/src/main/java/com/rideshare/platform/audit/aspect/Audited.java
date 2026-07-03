package com.rideshare.platform.audit.aspect;

import java.lang.annotation.*;

/** Marks a service method whose invocation must be recorded per Section 18 Audit. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    String action();
    String entityType() default "";
}
