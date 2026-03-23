package io.wanjune.zagent.mapper;

import io.wanjune.zagent.model.entity.AiClientSystemPrompt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 系统提示词 Mapper, 操作表: ai_client_system_prompt
 */
@Mapper
public interface AiClientSystemPromptMapper {

    /**
     * 根据提示词ID查询
     *
     * @param promptId 提示词ID
     * @return 系统提示词配置信息
     */
    AiClientSystemPrompt selectByPromptId(@Param("promptId") String promptId);

    /**
     * 根据提示词ID列表批量查询
     *
     * @param promptIds 提示词ID列表
     * @return 系统提示词配置列表
     */
    List<AiClientSystemPrompt> selectByPromptIds(@Param("promptIds") List<String> promptIds);

    List<AiClientSystemPrompt> selectAll();

    int insert(AiClientSystemPrompt prompt);

    int update(AiClientSystemPrompt prompt);

    int deleteByPromptId(@Param("promptId") String promptId);

}
