from __future__ import annotations

import json
import os
from typing import Any
from urllib.parse import urlencode
from urllib.request import urlopen

from mcp.server.fastmcp import FastMCP


AMAP_WEB_API_KEY = os.getenv("AMAP_WEB_API_KEY", "")
AMAP_BASE_URL = os.getenv("AMAP_BASE_URL", "https://restapi.amap.com")

MCP_HOST = os.getenv("AMAP_MCP_HOST", "127.0.0.1")
MCP_PORT = int(os.getenv("AMAP_MCP_PORT", "18081"))
MCP_SSE_PATH = os.getenv("AMAP_MCP_SSE_PATH", "/sse")
MCP_MESSAGE_PATH = os.getenv("AMAP_MCP_MESSAGE_PATH", "/messages/")

mcp = FastMCP("zagent-amap-sse")


def require_api_key(api_key: str) -> str:
    if not api_key or not api_key.strip():
        raise ValueError("缺少环境变量 AMAP_WEB_API_KEY")
    return api_key.strip()


def call_amap_api(path: str, params: dict[str, Any]) -> dict[str, Any]:
    query = dict(params)
    query["key"] = require_api_key(AMAP_WEB_API_KEY)
    query.setdefault("output", "JSON")
    url = f"{AMAP_BASE_URL}{path}?{urlencode(query)}"

    with urlopen(url, timeout=15) as response:
        payload = json.loads(response.read().decode("utf-8"))

    if payload.get("status") != "1":
        info = payload.get("info") or "unknown error"
        infocode = payload.get("infocode") or ""
        raise RuntimeError(f"Amap API 调用失败: {info} {infocode}".strip())

    return payload


def extract_weather_result(payload: dict[str, Any]) -> dict[str, str]:
    lives = payload.get("lives") or []
    if not lives:
        raise ValueError("天气查询结果为空")

    weather = lives[0]
    return {
        "province": weather.get("province", ""),
        "city": weather.get("city", ""),
        "adcode": weather.get("adcode", ""),
        "weather": weather.get("weather", ""),
        "temperature": f"{weather.get('temperature', '')}℃",
        "wind": f"{weather.get('winddirection', '')}风 {weather.get('windpower', '')}级".strip(),
        "humidity": f"{weather.get('humidity', '')}%",
        "report_time": weather.get("reporttime", ""),
    }


def extract_poi_results(payload: dict[str, Any], limit: int = 5) -> list[dict[str, str]]:
    pois = payload.get("pois") or []
    results: list[dict[str, str]] = []
    for poi in pois[:limit]:
        item = {
            "name": poi.get("name", ""),
            "address": poi.get("address", "") or poi.get("adname", ""),
            "type": poi.get("type", ""),
            "location": poi.get("location", ""),
        }
        distance = poi.get("distance")
        if distance:
            item["distance"] = f"{distance}米"
        results.append(item)

    return results


@mcp.tool()
def ping() -> str:
    return "pong-from-zagent-amap-sse"


@mcp.tool()
def amap_weather(city_or_adcode: str) -> dict[str, str]:
    payload = call_amap_api(
        "/v3/weather/weatherInfo",
        {
            "city": city_or_adcode,
            "extensions": "base",
        },
    )
    return extract_weather_result(payload)


@mcp.tool()
def amap_search_poi(
    keyword: str,
    city: str = "",
    location: str = "",
    radius: int = 3000,
    limit: int = 5,
) -> list[dict[str, str]]:
    if location.strip():
        payload = call_amap_api(
            "/v3/place/around",
            {
                "keywords": keyword,
                "location": location,
                "radius": radius,
                "offset": limit,
                "page": 1,
                "sortrule": "distance",
            },
        )
    else:
        payload = call_amap_api(
            "/v3/place/text",
            {
                "keywords": keyword,
                "city": city,
                "citylimit": "true" if city else "false",
                "offset": limit,
                "page": 1,
                "extensions": "base",
            },
        )

    return extract_poi_results(payload, limit=limit)


mcp.settings.sse_path = MCP_SSE_PATH
mcp.settings.message_path = MCP_MESSAGE_PATH
app = mcp.sse_app()


if __name__ == "__main__":
    import uvicorn

    print("[zagent-amap-sse] Amap SSE MCP server starting")
    print(f"[zagent-amap-sse] SSE endpoint: http://{MCP_HOST}:{MCP_PORT}{MCP_SSE_PATH}")
    print(f"[zagent-amap-sse] Message endpoint: http://{MCP_HOST}:{MCP_PORT}{MCP_MESSAGE_PATH}")
    uvicorn.run(app, host=MCP_HOST, port=MCP_PORT)
