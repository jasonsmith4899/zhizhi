package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiKey 实体单元测试")
class ApiKeyTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        ApiKey key = new ApiKey();
        key.setId(1L);
        key.setUserId(10L);
        key.setTenantId(20L);
        key.setKeyValue("sk-abc123");
        key.setName("测试Key");
        key.setDescription("desc");
        key.setAssistantPersona("你是一个助手");
        key.setMerchantBackground("商家背景");
        key.setAnswerRules("回答规则");
        LocalDateTime now = LocalDateTime.now();
        key.setCreatedAt(now);
        key.setUpdatedAt(now);

        assertEquals(1L, key.getId());
        assertEquals(10L, key.getUserId());
        assertEquals(20L, key.getTenantId());
        assertEquals("sk-abc123", key.getKeyValue());
        assertEquals("测试Key", key.getName());
        assertEquals("desc", key.getDescription());
        assertEquals("你是一个助手", key.getAssistantPersona());
        assertEquals("商家背景", key.getMerchantBackground());
        assertEquals("回答规则", key.getAnswerRules());
        assertEquals(now, key.getCreatedAt());
        assertEquals(now, key.getUpdatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor 构造所有字段")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Set<KnowledgeBase> kbs = new HashSet<>();
        ApiKey key = new ApiKey(1L, 10L, 20L, "sk-abc", "name", "desc",
                "persona", "bg", "rules", now, now, kbs);

        assertEquals(1L, key.getId());
        assertEquals(10L, key.getUserId());
        assertEquals("sk-abc", key.getKeyValue());
        assertEquals(kbs, key.getKnowledgeBases());
    }

    @Test
    @DisplayName("Builder 构建并验证默认值")
    void builderDefaults() {
        ApiKey key = ApiKey.builder().build();

        assertEquals("默认Key", key.getName());
        assertNotNull(key.getKnowledgeBases());
        assertTrue(key.getKnowledgeBases().isEmpty());
    }

    @Test
    @DisplayName("Builder 构建自定义值")
    void builderCustom() {
        ApiKey key = ApiKey.builder()
                .id(1L)
                .userId(10L)
                .keyValue("sk-xyz")
                .name("自定义")
                .build();

        assertEquals(1L, key.getId());
        assertEquals("sk-xyz", key.getKeyValue());
        assertEquals("自定义", key.getName());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt 和 updatedAt")
    void prePersist() {
        ApiKey key = ApiKey.builder().build();
        key.onCreate();

        assertNotNull(key.getCreatedAt());
        assertNotNull(key.getUpdatedAt());
    }

    @Test
    @DisplayName("@PreUpdate 更新 updatedAt")
    void preUpdate() {
        ApiKey key = ApiKey.builder().build();
        key.onCreate();
        LocalDateTime original = key.getCreatedAt();

        key.onUpdate();
        assertNotNull(key.getUpdatedAt());
        // createdAt 不变
        assertEquals(original, key.getCreatedAt());
    }

    @Test
    @DisplayName("getKnowledgeBaseIds 返回关联知识库 ID 集合")
    void getKnowledgeBaseIds() {
        KnowledgeBase kb1 = new KnowledgeBase();
        kb1.setId(100L);
        KnowledgeBase kb2 = new KnowledgeBase();
        kb2.setId(200L);

        ApiKey key = ApiKey.builder()
                .knowledgeBases(new HashSet<>(Set.of(kb1, kb2)))
                .build();

        Set<Long> ids = key.getKnowledgeBaseIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains(100L));
        assertTrue(ids.contains(200L));
    }

    @Test
    @DisplayName("getKnowledgeBaseIds 在 knowledgeBases 为 null 时返回空集合")
    void getKnowledgeBaseIds_null() {
        ApiKey key = new ApiKey();
        key.setKnowledgeBases(null);

        Set<Long> ids = key.getKnowledgeBaseIds();
        assertNotNull(ids);
        assertTrue(ids.isEmpty());
    }

    @Test
    @DisplayName("canAccessKnowledgeBase 返回 true 当有权限")
    void canAccessKnowledgeBase_true() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(50L);

        ApiKey key = ApiKey.builder()
                .knowledgeBases(new HashSet<>(Set.of(kb)))
                .build();

        assertTrue(key.canAccessKnowledgeBase(50L));
    }

    @Test
    @DisplayName("canAccessKnowledgeBase 返回 false 当无权限")
    void canAccessKnowledgeBase_false() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(50L);

        ApiKey key = ApiKey.builder()
                .knowledgeBases(new HashSet<>(Set.of(kb)))
                .build();

        assertFalse(key.canAccessKnowledgeBase(999L));
    }

    @Test
    @DisplayName("canAccessKnowledgeBase 在 knowledgeBases 为空时返回 false")
    void canAccessKnowledgeBase_empty() {
        ApiKey key = ApiKey.builder()
                .knowledgeBases(new HashSet<>())
                .build();

        assertFalse(key.canAccessKnowledgeBase(50L));
    }

    @Test
    @DisplayName("canAccessKnowledgeBase 在 knowledgeBases 为 null 时返回 false")
    void canAccessKnowledgeBase_null() {
        ApiKey key = new ApiKey();
        key.setKnowledgeBases(null);

        assertFalse(key.canAccessKnowledgeBase(50L));
    }

    @Test
    @DisplayName("equals/hashCode 基于 @Data 自动生成")
    void equalsHashCode() {
        ApiKey key1 = ApiKey.builder().id(1L).userId(10L).keyValue("sk-abc").name("k1").build();
        ApiKey key2 = ApiKey.builder().id(1L).userId(10L).keyValue("sk-abc").name("k1").build();
        ApiKey key3 = ApiKey.builder().id(2L).userId(10L).keyValue("sk-xyz").name("k2").build();

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        assertNotEquals(key1, key3);
    }

    @Test
    @DisplayName("toString 包含关键字段")
    void toStringContainsFields() {
        ApiKey key = ApiKey.builder().id(1L).name("test").build();
        String str = key.toString();
        assertTrue(str.contains("1"));
        assertTrue(str.contains("test"));
    }
}
