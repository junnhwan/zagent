"""
Git Repository MCP Server (SSE)

Provides tools for analyzing git repositories:
  - get_recent_commits   : list recent commits with filters
  - get_commit_diff      : full diff for a specific commit
  - get_commit_detail    : structured metadata for a commit
  - list_changed_files   : files changed since a given date
  - read_file_at_commit  : file content at a specific revision
  - get_repo_summary     : high-level repo activity summary
  - ping                 : health check

Environment variables:
  GIT_MCP_HOST          (default 127.0.0.1)
  GIT_MCP_PORT          (default 18082)
  GIT_MCP_SSE_PATH      (default /sse)
  GIT_MCP_MESSAGE_PATH  (default /messages/)
  GIT_DEFAULT_REPO      (default "")
  GIT_ALLOWED_PATHS     (comma-separated, default "" = allow all)
"""
from __future__ import annotations

import os
import re
import logging
import subprocess
from pathlib import Path
from collections import Counter

from mcp.server.fastmcp import FastMCP

from tools.mcp_transport_compat import McpSseCompatibilityMiddleware, install_uvicorn_upgrade_noise_filter


# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
MCP_HOST = os.getenv("GIT_MCP_HOST", "127.0.0.1")
MCP_PORT = int(os.getenv("GIT_MCP_PORT", "18082"))
MCP_SSE_PATH = os.getenv("GIT_MCP_SSE_PATH", "/sse")
MCP_MESSAGE_PATH = os.getenv("GIT_MCP_MESSAGE_PATH", "/messages/")

GIT_DEFAULT_REPO = os.getenv("GIT_DEFAULT_REPO", "")
GIT_ALLOWED_PATHS = os.getenv("GIT_ALLOWED_PATHS", "")

MAX_OUTPUT_CHARS = 10_000
GIT_TIMEOUT = 30

mcp = FastMCP("zagent-git-repo")
logger = logging.getLogger("zagent-git-repo")
install_uvicorn_upgrade_noise_filter()


# ---------------------------------------------------------------------------
# ASGI middleware (same pattern as amap_sse_mcp.py)
# ---------------------------------------------------------------------------



# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
_UNSAFE_PATTERN = re.compile(r"[;&|`$(){}!\n\r]")


def _sanitize(value: str) -> str:
    """Reject values containing shell meta-characters."""
    if _UNSAFE_PATTERN.search(value):
        raise ValueError(f"Potentially unsafe characters in argument: {value!r}")
    return value


def _resolve_repo(repo_path: str) -> str:
    """Resolve and validate the repository path."""
    path = repo_path.strip() or GIT_DEFAULT_REPO
    if not path:
        raise ValueError("repo_path is required (or set GIT_DEFAULT_REPO)")

    resolved = str(Path(path).resolve())

    # Allowed-paths check
    if GIT_ALLOWED_PATHS:
        allowed = [str(Path(p.strip()).resolve()) for p in GIT_ALLOWED_PATHS.split(",") if p.strip()]
        if allowed and not any(resolved.startswith(a) for a in allowed):
            raise ValueError(f"Repository path is not under any allowed directory: {resolved}")

    # Must be a git repo
    git_dir = Path(resolved) / ".git"
    if not git_dir.exists():
        raise ValueError(f"Not a git repository (no .git directory): {resolved}")

    return resolved


def _run_git(repo_path: str, args: list[str]) -> subprocess.CompletedProcess:
    """Run a git command inside *repo_path* and return the result."""
    cmd = ["git", "-C", repo_path] + args
    return subprocess.run(
        cmd,
        capture_output=True,
        text=True,
        timeout=GIT_TIMEOUT,
    )


def _git_output(repo_path: str, args: list[str], max_chars: int = 0) -> str:
    """Run git, return stdout on success or an error string."""
    result = _run_git(repo_path, args)
    if result.returncode != 0:
        err = result.stderr.strip() or f"git exited with code {result.returncode}"
        return f"[ERROR] {err}"
    output = result.stdout
    if max_chars and len(output) > max_chars:
        output = output[:max_chars] + f"\n\n... (truncated at {max_chars} characters)"
    return output


def _safe_call(fn):
    """Decorator that catches exceptions and returns an error dict/string."""
    def wrapper(*args, **kwargs):
        try:
            return fn(*args, **kwargs)
        except Exception as exc:
            return {"error": str(exc)}
    wrapper.__name__ = fn.__name__
    wrapper.__doc__ = fn.__doc__
    wrapper.__annotations__ = fn.__annotations__
    return wrapper


# ---------------------------------------------------------------------------
# Tools
# ---------------------------------------------------------------------------
@mcp.tool()
@_safe_call
def ping() -> str:
    """Health check for the git-repo MCP server."""
    return "pong-from-zagent-git-repo"


@mcp.tool()
@_safe_call
def get_recent_commits(
    repo_path: str,
    since: str = "1 day ago",
    max_count: int = 20,
) -> list[dict]:
    """Get recent git commits from a repository.

    Args:
        repo_path:  Absolute path to the git repository.
        since:      Only commits more recent than this date (e.g. "1 day ago", "2024-01-01").
        max_count:  Maximum number of commits to return (default 20).

    Returns:
        A list of dicts with keys: hash, author, date, message.
    """
    repo = _resolve_repo(repo_path)
    _sanitize(since)
    fmt = "%H%n%an%n%ai%n%s%n---"
    output = _git_output(repo, [
        "log",
        f"--since={since}",
        f"--max-count={max_count}",
        f"--format={fmt}",
    ])
    if output.startswith("[ERROR]"):
        return [{"error": output}]

    commits: list[dict] = []
    blocks = [b.strip() for b in output.split("---") if b.strip()]
    for block in blocks:
        lines = block.splitlines()
        if len(lines) >= 4:
            commits.append({
                "hash": lines[0],
                "author": lines[1],
                "date": lines[2],
                "message": lines[3],
            })
    return commits


@mcp.tool()
@_safe_call
def get_commit_diff(repo_path: str, commit_hash: str) -> str:
    """Get the full diff for a specific commit.

    Args:
        repo_path:    Absolute path to the git repository.
        commit_hash:  The commit SHA (full or abbreviated).

    Returns:
        A string containing the stat summary followed by the full diff
        (truncated to 10 000 characters).
    """
    repo = _resolve_repo(repo_path)
    _sanitize(commit_hash)

    stat = _git_output(repo, ["show", "--stat", commit_hash])
    diff = _git_output(repo, ["show", commit_hash], max_chars=MAX_OUTPUT_CHARS)

    if stat.startswith("[ERROR]"):
        return stat
    if diff.startswith("[ERROR]"):
        return diff

    return f"=== STAT ===\n{stat}\n=== FULL DIFF ===\n{diff}"


@mcp.tool()
@_safe_call
def get_commit_detail(repo_path: str, commit_hash: str) -> dict:
    """Get detailed metadata about a specific commit.

    Args:
        repo_path:    Absolute path to the git repository.
        commit_hash:  The commit SHA (full or abbreviated).

    Returns:
        A dict with: hash, author, date, message, files_changed, insertions, deletions.
    """
    repo = _resolve_repo(repo_path)
    _sanitize(commit_hash)

    # Basic metadata
    fmt = "%H%n%an%n%ai%n%B"
    meta = _git_output(repo, ["show", "-s", f"--format={fmt}", commit_hash])
    if meta.startswith("[ERROR]"):
        return {"error": meta}

    lines = meta.strip().splitlines()
    if len(lines) < 4:
        return {"error": "Unexpected git output format"}

    commit_info: dict = {
        "hash": lines[0],
        "author": lines[1],
        "date": lines[2],
        "message": "\n".join(lines[3:]).strip(),
    }

    # Numstat for insertions / deletions
    numstat = _git_output(repo, ["show", "--numstat", "--format=", commit_hash])
    files_changed = 0
    insertions = 0
    deletions = 0
    if not numstat.startswith("[ERROR]"):
        for line in numstat.strip().splitlines():
            parts = line.split("\t")
            if len(parts) >= 3:
                files_changed += 1
                try:
                    insertions += int(parts[0])
                except ValueError:
                    pass
                try:
                    deletions += int(parts[1])
                except ValueError:
                    pass

    commit_info["files_changed"] = files_changed
    commit_info["insertions"] = insertions
    commit_info["deletions"] = deletions
    return commit_info


@mcp.tool()
@_safe_call
def list_changed_files(
    repo_path: str,
    since: str = "1 day ago",
) -> list[dict]:
    """List all files changed since a given date.

    Args:
        repo_path:  Absolute path to the git repository.
        since:      Only include changes more recent than this date.

    Returns:
        A list of dicts with keys: filename, status, change_count.
    """
    repo = _resolve_repo(repo_path)
    _sanitize(since)

    output = _git_output(repo, [
        "log",
        f"--since={since}",
        "--name-status",
        "--format=",
    ])
    if output.startswith("[ERROR]"):
        return [{"error": output}]

    file_status: dict[str, str] = {}
    file_count: Counter = Counter()

    for line in output.strip().splitlines():
        line = line.strip()
        if not line:
            continue
        parts = line.split("\t", 1)
        if len(parts) == 2:
            status_code, filename = parts
            status_map = {"A": "added", "M": "modified", "D": "deleted"}
            status_label = status_map.get(status_code[0], status_code)
            file_status[filename] = status_label
            file_count[filename] += 1

    return [
        {"filename": fn, "status": file_status[fn], "change_count": file_count[fn]}
        for fn in sorted(file_count, key=file_count.get, reverse=True)
    ]


@mcp.tool()
@_safe_call
def read_file_at_commit(
    repo_path: str,
    commit_hash: str,
    file_path: str,
) -> str:
    """Read a file's content at a specific commit.

    Args:
        repo_path:    Absolute path to the git repository.
        commit_hash:  The commit SHA (full or abbreviated).
        file_path:    Relative path of the file inside the repository.

    Returns:
        The file content (truncated to 10 000 characters).
    """
    repo = _resolve_repo(repo_path)
    _sanitize(commit_hash)
    _sanitize(file_path)

    return _git_output(repo, ["show", f"{commit_hash}:{file_path}"], max_chars=MAX_OUTPUT_CHARS)


@mcp.tool()
@_safe_call
def get_repo_summary(
    repo_path: str,
    since: str = "7 days ago",
) -> dict:
    """Get a high-level activity summary for a repository.

    Args:
        repo_path:  Absolute path to the git repository.
        since:      Time window for the summary (default "7 days ago").

    Returns:
        A dict with: total_commits, unique_authors, authors,
        files_changed, top_changed_files (up to 10).
    """
    repo = _resolve_repo(repo_path)
    _sanitize(since)

    # Commit count and authors
    log_output = _git_output(repo, [
        "log",
        f"--since={since}",
        "--format=%an",
    ])
    if log_output.startswith("[ERROR]"):
        return {"error": log_output}

    authors = [a.strip() for a in log_output.strip().splitlines() if a.strip()]
    author_counts = Counter(authors)

    # Changed files
    files_output = _git_output(repo, [
        "log",
        f"--since={since}",
        "--name-only",
        "--format=",
    ])
    file_counts: Counter = Counter()
    if not files_output.startswith("[ERROR]"):
        for line in files_output.strip().splitlines():
            line = line.strip()
            if line:
                file_counts[line] += 1

    top_files = [
        {"file": f, "changes": c}
        for f, c in file_counts.most_common(10)
    ]

    return {
        "total_commits": len(authors),
        "unique_authors": len(author_counts),
        "authors": dict(author_counts),
        "files_changed": len(file_counts),
        "top_changed_files": top_files,
    }


# ---------------------------------------------------------------------------
# Server setup
# ---------------------------------------------------------------------------
mcp.settings.sse_path = MCP_SSE_PATH
mcp.settings.message_path = MCP_MESSAGE_PATH
app = mcp.sse_app()
app = McpSseCompatibilityMiddleware(app, MCP_MESSAGE_PATH, logger)


if __name__ == "__main__":
    import uvicorn

    print("[zagent-git-repo] Git Repository SSE MCP server starting")
    print(f"[zagent-git-repo] SSE endpoint: http://{MCP_HOST}:{MCP_PORT}{MCP_SSE_PATH}")
    print(f"[zagent-git-repo] Message endpoint: http://{MCP_HOST}:{MCP_PORT}{MCP_MESSAGE_PATH}")
    if GIT_DEFAULT_REPO:
        print(f"[zagent-git-repo] Default repo: {GIT_DEFAULT_REPO}")
    if GIT_ALLOWED_PATHS:
        print(f"[zagent-git-repo] Allowed paths: {GIT_ALLOWED_PATHS}")
    uvicorn.run(app, host=MCP_HOST, port=MCP_PORT)
