package com.zhizhi.ai.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private final Cache<String, AtomicLong> requestCounts = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    @Around("@annotation(com.zhizhi.ai.common.RateLimit)")
    public Object handleRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        String key = buildKey(joinPoint, rateLimit.windowSeconds());
        AtomicLong counter = requestCounts.get(key, k -> new AtomicLong(0));

        long currentCount = counter.incrementAndGet();
        if (currentCount > rateLimit.maxRequests()) {
            log.warn("接口限流: key={}, count={}, limit={}", key, currentCount, rateLimit.maxRequests());
            throw BusinessException.quotaExceeded(
                    "请求过于频繁，每" + rateLimit.windowSeconds() + "秒最多" + rateLimit.maxRequests() + "次请求");
        }

        return joinPoint.proceed();
    }

    private String buildKey(ProceedingJoinPoint joinPoint, int windowSeconds) {
        String methodName = joinPoint.getSignature().toShortString();
        String userId = extractUserId(joinPoint);
        long window = System.currentTimeMillis() / (windowSeconds * 1000L);
        return methodName + ":" + userId + ":" + window;
    }

    private String extractUserId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof Authentication auth && auth.getDetails() instanceof Long userId) {
                return String.valueOf(userId);
            }
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Long userId) {
            return String.valueOf(userId);
        }
        return "anonymous";
    }
}
