package com.zhizhi.ai.service;

/**
 * 原始文件存储抽象。当前实现存 PostgreSQL（bytea），
 * 后续可无缝替换为本地磁盘 / MinIO / S3 等实现。
 */
public interface FileStorageService {

    /** 保存原始文件字节 */
    void store(Long documentId, Long tenantId, byte[] data);

    /** 读取原始文件字节，不存在返回 null */
    byte[] load(Long documentId);

    /** 删除原始文件 */
    void delete(Long documentId);
}
