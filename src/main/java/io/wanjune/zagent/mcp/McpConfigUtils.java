package io.wanjune.zagent.mcp;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * MCP配置文件读写的共享工具方法。
 */
public final class McpConfigUtils {

    private McpConfigUtils() {}

    /**
     * 去除 UTF-8 BOM 头 (\uFEFF)。
     */
    public static String stripUtf8Bom(String content) {
        if (content != null && !content.isEmpty() && content.charAt(0) == '\uFEFF') {
            return content.substring(1);
        }
        return content;
    }

    /**
     * 根据 configLocation 解析可写的配置文件路径。
     * <p>支持 file: 和 classpath: 前缀。找不到时返回 null，不抛异常。</p>
     */
    public static Path tryResolveWritableConfigPath(String configLocation) {
        if (StringUtils.startsWith(configLocation, "file:")) {
            return Paths.get(URI.create(configLocation));
        }
        if (StringUtils.startsWith(configLocation, "classpath:")) {
            String relativePath = StringUtils.removeStart(configLocation, "classpath:");
            Path sourcePath = Paths.get("src", "main", "resources", relativePath);
            if (Files.exists(sourcePath)) {
                return sourcePath;
            }
        }
        return null;
    }
}
