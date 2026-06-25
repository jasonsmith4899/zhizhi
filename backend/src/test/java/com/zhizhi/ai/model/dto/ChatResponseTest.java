package com.zhizhi.ai.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChatResponse 单元测试")
class ChatResponseTest {

    @Test
    @DisplayName("NoArgsConstructor + Getter/Setter")
    void getterSetter() {
        ChatResponse resp = new ChatResponse();
        resp.setReply("回答内容");
        resp.setSessionId("sess-1");
        resp.setMessageId("msg-1");
        resp.setSources(List.of());

        assertEquals("回答内容", resp.getReply());
        assertEquals("sess-1", resp.getSessionId());
        assertEquals("msg-1", resp.getMessageId());
        assertNotNull(resp.getSources());
        assertTrue(resp.getSources().isEmpty());
    }

    @Test
    @DisplayName("AllArgsConstructor")
    void allArgsConstructor() {
        List<ChatResponse.SourceReference> sources = List.of();
        ChatResponse resp = new ChatResponse("reply", "sid", "mid", sources);

        assertEquals("reply", resp.getReply());
        assertEquals("sid", resp.getSessionId());
        assertEquals("mid", resp.getMessageId());
    }

    @Test
    @DisplayName("Builder")
    void builder() {
        ChatResponse resp = ChatResponse.builder()
                .reply("test").sessionId("s1").build();
        assertEquals("test", resp.getReply());
        assertEquals("s1", resp.getSessionId());
    }

    @Test
    @DisplayName("SourceReference Getter/Setter")
    void sourceReferenceGetterSetter() {
        ChatResponse.SourceReference ref = new ChatResponse.SourceReference();
        ref.setDocumentId(1L);
        ref.setDocumentName("doc.pdf");
        ref.setContent("相关内容");
        ref.setScore(0.95);
        ref.setChunkIndex(3);

        assertEquals(1L, ref.getDocumentId());
        assertEquals("doc.pdf", ref.getDocumentName());
        assertEquals("相关内容", ref.getContent());
        assertEquals(0.95, ref.getScore());
        assertEquals(3, ref.getChunkIndex());
    }

    @Test
    @DisplayName("SourceReference AllArgsConstructor")
    void sourceReferenceAllArgsConstructor() {
        ChatResponse.SourceReference ref = new ChatResponse.SourceReference(
                1L, "doc.pdf", "content", 0.8, 2);

        assertEquals(1L, ref.getDocumentId());
        assertEquals("doc.pdf", ref.getDocumentName());
        assertEquals(0.8, ref.getScore());
    }

    @Test
    @DisplayName("SourceReference Builder")
    void sourceReferenceBuilder() {
        ChatResponse.SourceReference ref = ChatResponse.SourceReference.builder()
                .documentId(1L).documentName("test").score(0.5).build();

        assertEquals(1L, ref.getDocumentId());
        assertEquals(0.5, ref.getScore());
    }

    @Test
    @DisplayName("SourceReference equals/hashCode")
    void sourceReferenceEqualsHashCode() {
        ChatResponse.SourceReference r1 = ChatResponse.SourceReference.builder()
                .documentId(1L).documentName("d").content("c").score(0.5).chunkIndex(0).build();
        ChatResponse.SourceReference r2 = ChatResponse.SourceReference.builder()
                .documentId(1L).documentName("d").content("c").score(0.5).chunkIndex(0).build();
        ChatResponse.SourceReference r3 = ChatResponse.SourceReference.builder()
                .documentId(2L).documentName("d").content("c").score(0.5).chunkIndex(0).build();

        assertEquals(r1, r2);
        assertNotEquals(r1, r3);
    }
}
