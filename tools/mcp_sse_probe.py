from __future__ import annotations

import os
from pathlib import Path

from mcp.server.fastmcp import FastMCP
ROOT_DIR = Path(__file__).resolve().parents[1]
DOCS_DIR = ROOT_DIR / "docs"
PROBE_FILE = DOCS_DIR / "mcp_probe.txt"

MCP_HOST = os.getenv("MCP_HOST", "127.0.0.1")
MCP_PORT = int(os.getenv("MCP_PORT", "18080"))
MCP_SSE_PATH = os.getenv("MCP_SSE_PATH", "/sse")
MCP_MESSAGE_PATH = os.getenv("MCP_MESSAGE_PATH", "/messages/")


mcp = FastMCP("zagent-sse-probe")


def _ensure_docs_path(file_name: str) -> Path:
    candidate = (DOCS_DIR / file_name).resolve()
    docs_root = DOCS_DIR.resolve()
    if docs_root not in candidate.parents and candidate != docs_root:
        raise ValueError("仅允许访问 docs/ 目录下的文件")
    return candidate


@mcp.tool()
def ping() -> str:
    return "pong-from-zagent-sse-probe"


@mcp.tool()
def read_probe() -> str:
    if not PROBE_FILE.exists():
        raise FileNotFoundError(f"未找到探针文件: {PROBE_FILE}")
    return PROBE_FILE.read_text(encoding="utf-8")


@mcp.tool()
def read_docs_file(file_name: str) -> str:
    target = _ensure_docs_path(file_name)
    if not target.exists():
        raise FileNotFoundError(f"文件不存在: {target.name}")
    if not target.is_file():
        raise ValueError(f"目标不是文件: {target.name}")
    return target.read_text(encoding="utf-8")


@mcp.tool()
def list_docs_files() -> list[str]:
    return sorted([path.name for path in DOCS_DIR.iterdir() if path.is_file()])


mcp.settings.sse_path = MCP_SSE_PATH
mcp.settings.message_path = MCP_MESSAGE_PATH
app = mcp.sse_app()


if __name__ == "__main__":
    import uvicorn

    print(f"[zagent-sse-probe] docs dir: {DOCS_DIR}")
    print(f"[zagent-sse-probe] SSE endpoint: http://{MCP_HOST}:{MCP_PORT}{MCP_SSE_PATH}")
    print(f"[zagent-sse-probe] Message endpoint: http://{MCP_HOST}:{MCP_PORT}{MCP_MESSAGE_PATH}")
    uvicorn.run(app, host=MCP_HOST, port=MCP_PORT)
