package io.wanjune.zagent.mapper;

import io.wanjune.zagent.model.entity.AiClientConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 客户端统一关联配置 Mapper, 操作表: ai_client_config
 */
@Mapper
public interface AiClientConfigMapper {

    /**
     * 根据源类型和源ID查询所有关联配置
     *
     * @param sourceType 源类型
     * @param sourceId 源ID
     * @return 关联配置列表
     */
    List<AiClientConfig> selectBySource(@Param("sourceType") String sourceType, @Param("sourceId") String sourceId);

    /**
     * 根据源和目标类型查询关联配置
     *
     * @param sourceType 源类型
     * @param sourceId 源ID
     * @param targetType 目标类型
     * @return 符合条件的关联配置列表
     */
    List<AiClientConfig> selectBySourceAndTargetType(@Param("sourceType") String sourceType,
                                                      @Param("sourceId") String sourceId,
                                                      @Param("targetType") String targetType);

}
