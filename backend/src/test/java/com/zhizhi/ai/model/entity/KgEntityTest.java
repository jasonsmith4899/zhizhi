package com.zhizhi.ai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KgEntity 实体单元测试")
class KgEntityTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        KgEntity entity = new KgEntity();
        entity.setId(1L);
        entity.setTenantId(10L);
        entity.setKnowledgeBaseId(20L);
        entity.setName("张三");
        entity.setType("人物");
        entity.setDescription("CEO");
        entity.setNormName("张三");
        entity.setMentionCount(5);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);

        assertEquals(1L, entity.getId());
        assertEquals(10L, entity.getTenantId());
        assertEquals(20L, entity.getKnowledgeBaseId());
        assertEquals("张三", entity.getName());
        assertEquals("人物", entity.getType());
        assertEquals("CEO", entity.getDescription());
        assertEquals("张三", entity.getNormName());
        assertEquals(5, entity.getMentionCount());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        KgEntity entity = new KgEntity(1L, 10L, 20L, "公司", "组织",
                "一家公司", "公司", 3, now);

        assertEquals(1L, entity.getId());
        assertEquals("公司", entity.getName());
        assertEquals("组织", entity.getType());
    }

    @Test
    @DisplayName("Builder 验证默认值 mentionCount=1")
    void builderDefaults() {
        KgEntity entity = KgEntity.builder().build();
        assertEquals(1, entity.getMentionCount());
    }

    @Test
    @DisplayName("@PrePersist 设置 createdAt")
    void prePersist() {
        KgEntity entity = KgEntity.builder().build();
        entity.onCreate();
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        KgEntity e1 = KgEntity.builder().id(1L).name("A").build();
        KgEntity e2 = KgEntity.builder().id(1L).name("A").build();
        KgEntity e3 = KgEntity.builder().id(2L).name("B").build();

        assertEquals(e1, e2);
        assertNotEquals(e1, e3);
    }
}
