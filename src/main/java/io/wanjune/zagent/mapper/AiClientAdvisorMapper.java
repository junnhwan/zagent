package io.wanjune.zagent.mapper;

import io.wanjune.zagent.model.entity.AiClientAdvisor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * Advisor配置 Mapper, 操作表: ai_client_advisor
 */
@Mapper
public interface AiClientAdvisorMapper {

    /**
     * 根据顾问ID查询
     *
     * @param advisorId 顾问ID
     * @return Advisor配置信息
     */
    AiClientAdvisor selectByAdvisorId(@Param("advisorId") String advisorId);

    /**
     * 根据顾问ID列表批量查询（按order_num升序）
     *
     * @param advisorIds 顾问ID列表
     * @return 按order_num升序排列的Advisor配置列表
     */
    List<AiClientAdvisor> selectByAdvisorIds(@Param("advisorIds") List<String> advisorIds);

}
