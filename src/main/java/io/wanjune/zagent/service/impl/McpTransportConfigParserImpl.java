package io.wanjune.zagent.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.wanjune.zagent.model.dto.SseTransportConfig;
import io.wanjune.zagent.model.dto.StdioTransportConfig;
import io.wanjune.zagent.service.McpTransportConfigParser;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class McpTransportConfigParserImpl implements McpTransportConfigParser {

    @Override
    public SseTransportConfig parseSse(String transportConfig) {
        JSONObject config = JSON.parseObject(transportConfig);
        return new SseTransportConfig(
                normalizeSseBaseUri(config.getString("baseUri")),
                normalizeSseEndpoint(config.getString("sseEndpoint"))
        );
    }

    @Override
    public StdioTransportConfig parseStdio(String transportConfig) {
        JSONObject config = JSON.parseObject(transportConfig);
        String toolName = config.keySet().iterator().next();
        JSONObject toolConfig = config.getJSONObject(toolName);

        String command = toolConfig.getString("command");
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("stdio command must not be blank");
        }

        List<String> args = toolConfig.getJSONArray("args") != null
                ? toolConfig.getJSONArray("args").toJavaList(String.class)
                : Collections.emptyList();

        Map<String, String> env = new LinkedHashMap<>();
        JSONObject envConfig = toolConfig.getJSONObject("env");
        if (envConfig != null) {
            for (String key : envConfig.keySet()) {
                Object value = envConfig.get(key);
                if (value != null) {
                    env.put(key, String.valueOf(value));
                }
            }
        }

        return new StdioTransportConfig(toolName, command, List.copyOf(args), Map.copyOf(env));
    }

    @Override
    public ServerParameters toServerParameters(StdioTransportConfig config) {
        ServerParameters.Builder builder = ServerParameters.builder(config.command())
                .args(config.args());
        if (!config.env().isEmpty()) {
            builder.env(config.env());
        }
        return builder.build();
    }

    static String normalizeSseBaseUri(String baseUri) {
        if (baseUri == null || baseUri.isBlank()) {
            throw new IllegalArgumentException("baseUri must not be blank");
        }
        String normalized = baseUri.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    static String normalizeSseEndpoint(String sseEndpoint) {
        if (sseEndpoint == null || sseEndpoint.isBlank()) {
            return "/sse";
        }
        String normalized = sseEndpoint.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
