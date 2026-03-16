package io.wanjune.zagent.mapper;

import io.wanjune.zagent.model.entity.AiClientApi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * API端点配置 Mapper, 操作表: ai_client_api
 */
@Mapper
public interface AiClientApiMapper {

    /**
     * 根据API配置ID查询
     *
     * @param apiId API配置ID
     * @return API端点配置信息
     */
    AiClientApi selectByApiId(@Param("apiId") String apiId);

}
