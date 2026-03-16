package io.wanjune.zagent.mapper;

import io.wanjune.zagent.model.entity.AiClient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AI客户端 Mapper, 操作表: ai_client
 */
@Mapper
public interface AiClientMapper {

    /**
     * 根据客户端ID查询（仅启用状态）
     *
     * @param clientId 客户端ID
     * @return 启用状态的客户端信息
     */
    AiClient selectByClientId(@Param("clientId") String clientId);

}
