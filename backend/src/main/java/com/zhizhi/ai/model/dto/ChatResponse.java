package com.zhizhi.ai.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

    private String reply;
    private String sessionId;
    private String messageId;
    private List<SourceReference> sources;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceReference {
        private Long documentId;
        private String documentName;
        private String content;
        private Double score;
        private Integer chunkIndex;
    }
}
