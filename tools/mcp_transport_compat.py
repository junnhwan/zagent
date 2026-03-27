from __future__ import annotations

import logging
from starlette.responses import Response


class UvicornUpgradeNoiseFilter(logging.Filter):
    NOISE_KEYWORDS = (
        'Unsupported upgrade request',
        'No supported WebSocket library detected',
    )

    def filter(self, record: logging.LogRecord) -> bool:
        message = record.getMessage()
        return not any(keyword in message for keyword in self.NOISE_KEYWORDS)


def install_uvicorn_upgrade_noise_filter() -> None:
    for logger_name in ('uvicorn.error', 'uvicorn.server'):
        current_logger = logging.getLogger(logger_name)
        if any(isinstance(existing_filter, UvicornUpgradeNoiseFilter) for existing_filter in current_logger.filters):
            continue
        current_logger.addFilter(UvicornUpgradeNoiseFilter())


class McpSseCompatibilityMiddleware:
    def __init__(self, app, message_path: str, logger: logging.Logger | None = None):
        self.app = app
        self.message_path = (message_path or '/messages/').rstrip('/')
        self.logger = logger or logging.getLogger('mcp-sse-compat')

    async def __call__(self, scope, receive, send):
        if not self._should_intercept(scope):
            await self.app(scope, receive, send)
            return

        headers = list(scope.get('headers', []))
        has_content_type = any(key.lower() == b'content-type' for key, _ in headers)
        if not has_content_type:
            headers.append((b'content-type', b'application/json'))
            self.logger.debug('MCP POST 缺少 Content-Type，已回退为 application/json')

        body = await self._read_body(receive)
        if not body.strip():
            self.logger.debug('忽略空的 MCP POST 请求: path=%s', scope.get('path'))
            response = Response('Accepted', status_code=202)
            await response(scope, self._empty_receive, send)
            return

        forwarded_scope = dict(scope)
        forwarded_scope['headers'] = headers
        await self.app(forwarded_scope, self._replay_receive(body), send)

    def _should_intercept(self, scope) -> bool:
        if scope.get('type') != 'http':
            return False
        if scope.get('method', '').upper() != 'POST':
            return False
        return (scope.get('path') or '').rstrip('/') == self.message_path

    async def _read_body(self, receive) -> bytes:
        chunks = []
        while True:
            message = await receive()
            if message['type'] != 'http.request':
                continue
            chunks.append(message.get('body', b''))
            if not message.get('more_body', False):
                break
        return b''.join(chunks)

    @staticmethod
    async def _empty_receive():
        return {'type': 'http.request', 'body': b'', 'more_body': False}

    @staticmethod
    def _replay_receive(body: bytes):
        sent = False

        async def receive():
            nonlocal sent
            if sent:
                return {'type': 'http.request', 'body': b'', 'more_body': False}
            sent = True
            return {'type': 'http.request', 'body': body, 'more_body': False}

        return receive
