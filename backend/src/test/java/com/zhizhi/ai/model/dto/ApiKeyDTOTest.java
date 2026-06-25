package com.zhizhi.ai.model.dto;

import com.zhizhi.ai.model.entity.ApiKey;
import com.zhizhi.ai.model.entity.KnowledgeBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiKeyDTO 单元测试")
class ApiKeyDTOTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        ApiKeyDTO dto = new ApiKeyDTO();
        dto.setId(1L);
        dto.setName("key1");
        dto.setDescription("desc");
        dto.setKeyValue("sk-abc123");
        dto.setAssistantPersona("persona");
        dto.setMerchantBackground("bg");
        dto.setAnswerRules("rules");
        LocalDateTime now = LocalDateTime.now();
        dto.setCreatedAt(now);
        dto.setKnowledgeBaseIds(Set.of(10L, 20L));

        assertEquals(1L, dto.getId());
        assertEquals("key1", dto.getName());
        assertEquals("desc", dto.getDescription());
        assertEquals("sk-abc123", dto.getKeyValue());
        assertEquals("persona", dto.getAssistantPersona());
        assertEquals("bg", dto.getMerchantBackground());
        assertEquals("rules", dto.getAnswerRules());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(2, dto.getKnowledgeBaseIds().size());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Set<Long> kbIds = Set.of(1L);
        ApiKeyDTO dto = new ApiKeyDTO(1L, "name", "desc", "key",
                "persona", "bg", "rules", now, kbIds);

        assertEquals(1L, dto.getId());
        assertEquals("name", dto.getName());
    }

    @Test
    @DisplayName("Builder")
    void builder() {
        ApiKeyDTO dto = ApiKeyDTO.builder().id(1L).name("test").build();
        assertEquals(1L, dto.getId());
        assertEquals("test", dto.getName());
    }

    @Test
    @DisplayName("fromEntity 正确映射所有字段")
    void fromEntity() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(100L);

        ApiKey entity = ApiKey.builder()
                .id(1L)
                .userId(10L)
                .keyValue("sk-abcdef123456")
                .name("test-key")
                .description("desc")
                .assistantPersona("persona")
                .merchantBackground("bg")
                .answerRules("rules")
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .knowledgeBases(new HashSet<>(Set.of(kb)))
                .build();

        ApiKeyDTO dto = ApiKeyDTO.fromEntity(entity);

        assertEquals(1L, dto.getId());
        assertEquals("test-key", dto.getName());
        assertEquals("desc", dto.getDescription());
        assertEquals("sk-abcdef123456", dto.getKeyValue());
        assertEquals("persona", dto.getAssistantPersona());
        assertEquals("bg", dto.getMerchantBackground());
        assertEquals("rules", dto.getAnswerRules());
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), dto.getCreatedAt());
        assertTrue(dto.getKnowledgeBaseIds().contains(100L));
    }

    @Test
    @DisplayName("fromEntityMasked 掩码中间部分")
    void fromEntityMasked() {
        ApiKey entity = ApiKey.builder()
                .id(1L)
                .keyValue("sk-abcdef123456")
                .name("test")
                .knowledgeBases(new HashSet<>())
                .build();

        ApiKeyDTO dto = ApiKeyDTO.fromEntityMasked(entity);

        assertEquals("sk-a****3456", dto.getKeyValue());
    }

    @Test
    @DisplayName("fromEntityMasked 短 key 不掩码")
    void fromEntityMasked_shortKey() {
        ApiKey entity = ApiKey.builder()
                .id(1L)
                .keyValue("short")
                .name("test")
                .knowledgeBases(new HashSet<>())
                .build();

        ApiKeyDTO dto = ApiKeyDTO.fromEntityMasked(entity);
        assertEquals("short", dto.getKeyValue());
    }

    @Test
    @DisplayName("fromEntityMasked key 为 null 不报错")
    void fromEntityMasked_nullKey() {
        ApiKey entity = ApiKey.builder()
                .id(1L)
                .keyValue(null)
                .name("test")
                .knowledgeBases(new HashSet<>())
                .build();

        ApiKeyDTO dto = ApiKeyDTO.fromEntityMasked(entity);
        assertNull(dto.getKeyValue());
    }

    @Test
    @DisplayName("equals/hashCode")
    void equalsHashCode() {
        ApiKeyDTO d1 = ApiKeyDTO.builder().id(1L).name("k1").build();
        ApiKeyDTO d2 = ApiKeyDTO.builder().id(1L).name("k1").build();
        ApiKeyDTO d3 = ApiKeyDTO.builder().id(2L).name("k2").build();

        assertEquals(d1, d2);
        assertNotEquals(d1, d3);
    }
}
