import json
from abc import ABC, abstractmethod
from typing import Any

import requests

from config import Config


class BaseStep(ABC):
    """Abstract base class for all test steps.

    Subclasses implement ``_run`` with the step-specific assertions.
    The ``run`` method orchestrates setup, execution, and teardown.
    """

    def __init__(self) -> None:
        """Initialize the step and load shared configuration."""
        super().__init__()
        self._cfg: Config = Config()

    def _setup(self) -> None:
        """Validate configuration before the step runs."""
        Config().validate()

    def _teardown(self) -> None:
        """Reset the configuration singleton after the step completes."""
        Config().reset()

    @abstractmethod
    def _run(self) -> None:
        """Execute the step-specific logic. Must be implemented by subclasses."""

    def run(self) -> None:
        """Run the step: setup → _run → teardown."""
        self._setup()
        self._run()
        self._teardown()

    @staticmethod
    def deserialize_json(payload: bytes) -> dict[str, Any]:
        """Deserialize *payload* from JSON.

        Raises :class:`AssertionError` if the bytes are not valid JSON.
        """
        try:
            return json.loads(payload)
        except json.decoder.JSONDecodeError as e:
            raise AssertionError("Failed to deserialize JSON") from e

    def request(
            self, endpoint: str, expected_status: int, empty_body: bool = False
    ) -> bytes | None:
        """Send a GET request to *endpoint* and assert the response status.

        Args:
            endpoint: Path appended to the configured API endpoint.
            expected_status: HTTP status code the response must match.
            empty_body: When ``True``, assert the response body is empty
                and return ``None``.

        Returns:
            Raw response bytes, or ``None`` when *empty_body* is ``True``.
        """
        response = requests.get(f"{self._cfg.api_endpoint}{endpoint}")
        assert response.status_code == expected_status

        payload: bytes | None = response.content

        if empty_body:
            assert payload is None or len(payload) <= 0
            return None

        assert payload is not None
        assert len(payload) > 0

        return payload
