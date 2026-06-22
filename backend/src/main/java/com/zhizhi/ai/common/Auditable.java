package com.zhizhi.ai.common;

import java.lang.annotation.*;

/**
 * 标注需要记录审计日志的方法。
 * 由 {@link AuditAspect} 拦截，在方法执行后异步记录。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    /** 操作类型，如 CREATE / DELETE / ROLLBACK */
    String action();

    /** 目标类型，如 knowledge_base / document / tag */
    String targetType() default "";
}
