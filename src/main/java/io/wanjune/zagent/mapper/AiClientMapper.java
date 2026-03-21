package io.wanjune.zagent.mapper;

import io.wanjune.zagent.model.entity.AiClient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * AI客户端 Mapper, 操作表: ai_client
 */
@Mapper
public interface AiClientMapper {

    AiClient selectByClientId(@Param("clientId") String clientId);

    /** 查询所有客户端（管理用） */
    List<AiClient> selectAll();

}
