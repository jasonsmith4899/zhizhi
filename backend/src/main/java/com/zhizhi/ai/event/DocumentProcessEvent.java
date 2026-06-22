package com.zhizhi.ai.event;

/**
 * 文档处理事件：上传或重新向量化后发布，由监听器在事务提交后异步消费。
 * 这是应用内的轻量"事件驱动"解耦（无需引入 MQ 中间件），
 * 后续如需削峰/跨服务可平滑替换为 RabbitMQ / Redis Stream。
 */
public record DocumentProcessEvent(
        Long documentId,
        byte[] contentBytes,
        String filename,
        Long knowledgeBaseId
) {}
