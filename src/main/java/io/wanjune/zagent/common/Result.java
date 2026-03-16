package io.wanjune.zagent.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应包装类, code=0000表示成功, code=0001表示失败
 *
 * @param <T> 响应数据类型
 * @author zagent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 响应编码: 0000=成功, 0001=失败 */
    private String code;

    /** 响应信息描述 */
    private String info;

    /** 响应数据 */
    private T data;

    /**
     * 构建成功响应（携带数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code("0000")
                .info("success")
                .data(data)
                .build();
    }

    /**
     * 构建成功响应（不携带数据）
     *
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 构建失败响应
     *
     * @param info 失败信息描述
     * @param <T>  数据类型
     * @return 失败响应
     */
    public static <T> Result<T> fail(String info) {
        return Result.<T>builder()
                .code("0001")
                .info(info)
                .build();
    }

}
