import logging
from typing import ClassVar

_LOG_DATE_FORMAT_DEFAULT = "%d-%m-%Y %H:%M:%S"
_LOG_FORMAT_DEFAULT = "%(asctime)s [%(levelname)s] %(name)s: %(message)s"
_LOG_LEVEL_DEFAULT = "INFO"

_REQUIRED_KEYS: list[str] = [
    "API_DB_DATABASE",
    "API_DB_HOSTNAME",
    "API_DB_PASSWORD",
    "API_DB_PORT",
    "API_DB_USERNAME",
    "API_PORT",
    "API_SCHEMA",
    "API_URL",
]

_OPTIONAL_KEYS: list[str] = [
    "LOG_DATE_FORMAT",
    "LOG_FORMAT",
    "LOG_LEVEL",
]

_ALL_KEYS: list[str] = _REQUIRED_KEYS + _OPTIONAL_KEYS


class Config:
    """Singleton configuration.

    Subsequent ``Config()`` calls return the same instance.
    """

    _instance: ClassVar["Config | None"] = None
    _initialized: bool = False

    def __new__(cls) -> "Config":
        if cls._instance is None:
            instance = super().__new__(cls)
            instance._initialized = False
            cls._instance = instance
        return cls._instance

    def __init__(self) -> None:
        if self._initialized:
            return
        self._initialized = True
        self._log = logging.getLogger(__class__.__name__)
        self._values: dict[str, str] = {key: "" for key in _ALL_KEYS}
        self._read()
        self._set_transitives()
        self._log.debug("Config initialized with keys: %s",
                        list(self._values.keys()))

    def _read(self) -> None:
        """Parse ``config.ini`` and populate ``_values`` with recognised keys."""
        read_values: dict[str, str] = {}
        with open("config.ini") as file:
            for line in file:
                line = line.strip()
                if line.startswith("#"):
                    continue
                line_split = line.split("=", 1)
                if len(line_split) != 2:
                    self._log.warning(f"Skipping line '{line}'")
                    continue
                read_values[line_split[0]] = line_split[1]
        for key in read_values:
            if key not in _ALL_KEYS:
                self._log.warning(f"Skipping unknown key '{key}'")
                continue
            self._values[key] = read_values[key]

    def _set_transitives(self) -> None:
        """Derive ``API_ENDPOINT`` from the other API settings."""
        self._values["API_ENDPOINT"] = f"{self.api_schema}://{self.api_url}:{self.api_port}"

    def _get(self, key: str) -> str:
        """Return the config value for *key*, or ``""`` if absent."""
        self._log.debug("Config.get(%s)", key)
        return self._values.get(key, "")

    def _get_or_raise(self, key: str) -> str:
        val = self._get(key)
        if val:
            return val
        raise RuntimeError(f"{key} must not be empty")

    @property
    def log_level(self) -> str:
        return self._get("LOG_LEVEL") or _LOG_LEVEL_DEFAULT

    @property
    def log_format(self) -> str:
        return self._get("LOG_FORMAT") or _LOG_FORMAT_DEFAULT

    @property
    def log_date_format(self) -> str:
        return self._get("LOG_DATE_FORMAT") or _LOG_DATE_FORMAT_DEFAULT

    @property
    def api_db_database(self) -> str:
        return self._get_or_raise("API_DB_DATABASE")

    @property
    def api_db_password(self) -> str:
        return self._get_or_raise("API_DB_PASSWORD")

    @property
    def api_db_hostname(self) -> str:
        return self._get_or_raise("API_DB_HOSTNAME")

    @property
    def api_db_port(self) -> int:
        """Return the database port as an integer.

        Raises :class:`RuntimeError` if the value is not numeric.
        """
        val = self._get_or_raise("API_DB_PORT")
        try:
            port = int(val)
        except ValueError as e:
            raise RuntimeError(
                f"API_DB_PORT must be numeric, got: {val!r}") from e
        if not 1 <= port <= 65535:
            raise RuntimeError(f"API_DB_PORT must be 1-65535, got: {port}")
        return port

    @property
    def api_db_username(self) -> str:
        return self._get_or_raise("API_DB_USERNAME")

    @property
    def api_endpoint(self) -> str:
        return self._get_or_raise("API_ENDPOINT")

    @property
    def api_port(self) -> str:
        return self._get_or_raise("API_PORT")

    @property
    def api_schema(self) -> str:
        return self._get_or_raise("API_SCHEMA")

    @property
    def api_url(self) -> str:
        return self._get_or_raise("API_URL")

    @classmethod
    def reset(cls) -> None:
        """Reset the singleton instance (intended for use in tests)."""
        cls._instance = None

    def validate(self) -> bool:
        for key in _REQUIRED_KEYS:
            if key not in self._values:
                raise KeyError(f"Required key '{key}' not found")
            if not self._get(key):
                raise ValueError(f"Required key '{key}' has no value")
        return True
