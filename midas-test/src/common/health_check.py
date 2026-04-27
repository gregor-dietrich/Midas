
from typing import Any

from common.base_step import BaseStep


class HealthCheck(BaseStep):
    """Test step that verifies the API health endpoint.

    Hits ``/q/health`` and asserts that the overall status and every
    individual check report ``UP``.
    """

    def _run(self) -> None:
        """Assert the API health endpoint returns a healthy response."""
        payload: bytes | None = self.request("/q/health", 200)
        assert payload is not None

        content: dict[str, Any] = self.deserialize_json(payload)

        assert "status" in content
        assert content["status"] == "UP"

        assert "checks" in content
        checks: list[dict[str, Any]] = content["checks"]
        for check in checks:
            assert "name" in check
            assert "status" in check
            assert check["status"] == "UP"
            if "data" in check:
                assert check["data"]["<default>"] == "UP"

        print(content)
