-- ============================================================
-- 种子脚本：mason 管理账号
-- ============================================================
-- 用途：在新环境快速初始化默认登录账号 mason。
-- 幂等：可重复执行，已存在则只更新密码/邮箱，不会重复插入。
--
-- 账号：mason
-- 密码：Rj@4899520   （BCrypt $2a$10$，与 Spring BCryptPasswordEncoder 兼容）
--
-- 执行方式（在已应用全部 Flyway 迁移的库上手动运行）：
--   docker exec -i zhizhi-postgres psql -U postgres -d zhizhi < seed_mason.sql
--
-- 注意：这不是 Flyway 迁移脚本，不放在 db/migration 下，
--       以免改变迁移链 checksum。仅供初始化/重置使用。
-- ============================================================

-- 新增或更新 mason 用户（username 唯一）
INSERT INTO users (username, email, password, plan, refresh_token_version, daily_queries_used, created_at, updated_at)
VALUES (
    'mason',
    'mason@zhizhi.ai',
    '$2a$10$fbBY.JNgMj/mAvQuatq7e.hOweFzS2a7NW3vWnXE0ILSzV1G.4CD2',
    'pro',
    0,
    0,
    NOW(),
    NOW()
)
ON CONFLICT (username) DO UPDATE
    SET password               = EXCLUDED.password,
        plan                   = EXCLUDED.plan,
        refresh_token_version  = COALESCE(users.refresh_token_version, 0) + 1,  -- 使旧 token 失效
        updated_at             = NOW();

-- 验证结果
SELECT id, username, email, plan, refresh_token_version, LEFT(password, 7) AS pw_prefix
FROM users
WHERE username = 'mason';
