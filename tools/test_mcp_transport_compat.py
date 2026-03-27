from __future__ import annotations

import asyncio
import logging
import unittest

from tools.mcp_transport_compat import McpSseCompatibilityMiddleware, UvicornUpgradeNoiseFilter


class _DummyApp:
    def __init__(self) -> None:
        self.called = False
        self.scopes = []
        self.bodies = []

    async def __call__(self, scope, receive, send):
        self.called = True
        self.scopes.append(scope)

        chunks = []
        while True:
            message = await receive()
            if message["type"] != "http.request":
                continue
            chunks.append(message.get("body", b""))
            if not message.get("more_body", False):
                break
        self.bodies.append(b"".join(chunks))

        await send({"type": "http.response.start", "status": 200, "headers": []})
        await send({"type": "http.response.body", "body": b"ok"})


class McpSseCompatibilityMiddlewareTest(unittest.TestCase):

    def test_injects_content_type_for_message_post(self) -> None:
        app = _DummyApp()
        middleware = McpSseCompatibilityMiddleware(app, "/messages/", logging.getLogger("test-mcp"))

        async def run_case():
            scope = {
                "type": "http",
                "method": "POST",
                "path": "/messages/",
                "headers": [],
            }
            events = [{"type": "http.request", "body": b'{"jsonrpc":"2.0"}', "more_body": False}]
            sent = []

            async def receive():
                return events.pop(0)

            async def send(message):
                sent.append(message)

            await middleware(scope, receive, send)
            return sent

        asyncio.run(run_case())

        self.assertTrue(app.called)
        headers = dict(app.scopes[0]["headers"])
        self.assertEqual(headers.get(b"content-type"), b"application/json")
        self.assertEqual(app.bodies[0], b'{"jsonrpc":"2.0"}')

    def test_ignores_empty_message_post_and_returns_accepted(self) -> None:
        app = _DummyApp()
        middleware = McpSseCompatibilityMiddleware(app, "/messages/", logging.getLogger("test-mcp"))

        async def run_case():
            scope = {
                "type": "http",
                "method": "POST",
                "path": "/messages/",
                "headers": [(b"content-type", b"application/json")],
            }
            events = [{"type": "http.request", "body": b"", "more_body": False}]
            sent = []

            async def receive():
                return events.pop(0)

            async def send(message):
                sent.append(message)

            await middleware(scope, receive, send)
            return sent

        sent = asyncio.run(run_case())

        self.assertFalse(app.called)
        self.assertEqual(sent[0]["type"], "http.response.start")
        self.assertEqual(sent[0]["status"], 202)

    def test_uvicorn_upgrade_noise_filter_suppresses_known_messages(self) -> None:
        noise_filter = UvicornUpgradeNoiseFilter()

        record = logging.LogRecord(
            name="uvicorn.error",
            level=logging.WARNING,
            pathname=__file__,
            lineno=1,
            msg="Unsupported upgrade request.",
            args=(),
            exc_info=None,
        )
        self.assertFalse(noise_filter.filter(record))

        normal_record = logging.LogRecord(
            name="uvicorn.error",
            level=logging.WARNING,
            pathname=__file__,
            lineno=1,
            msg="Regular warning",
            args=(),
            exc_info=None,
        )
        self.assertTrue(noise_filter.filter(normal_record))


if __name__ == "__main__":
    unittest.main()
