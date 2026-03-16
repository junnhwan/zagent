package io.wanjune.zagent.mapper;

import io.wanjune.zagent.model.entity.AiClientModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 聊天模型配置 Mapper, 操作表: ai_client_model
 */
@Mapper
public interface AiClientModelMapper {

    /**
     * 根据模型ID查询
     *
     * @param modelId 模型ID
     * @return 聊天模型配置信息
     */
    AiClientModel selectByModelId(@Param("modelId") String modelId);

}
