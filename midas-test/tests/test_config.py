"""Unit tests for :class:`config.Config`."""

import sys
from pathlib import Path
from unittest.mock import mock_open, patch

import pytest

from config import Config

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "src"))


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

_FULL_INI = """\
API_DB_DATABASE=midas
API_DB_HOSTNAME=localhost
API_DB_PASSWORD=changeit
API_DB_PORT=5432
API_DB_USERNAME=midas
API_PORT=8080
API_SCHEMA=http
API_URL=localhost
"""


def _make_config(content: str) -> Config:
    """Reset the singleton and construct a fresh :class:`Config` from *content*."""
    Config.reset()
    with patch("builtins.open", mock_open(read_data=content)):
        return Config()


# ---------------------------------------------------------------------------
# Singleton behaviour
# ---------------------------------------------------------------------------


class TestSingleton:
    """Config is a singleton — same instance across calls."""

    def setup_method(self) -> None:
        Config.reset()

    def teardown_method(self) -> None:
        Config.reset()

    def test_returns_same_instance(self) -> None:
        c1 = _make_config(_FULL_INI)
        c2 = Config()
        assert c1 is c2

    def test_reset_clears_instance(self) -> None:
        c1 = _make_config(_FULL_INI)
        Config.reset()
        c2 = _make_config(_FULL_INI)
        assert c1 is not c2


# ---------------------------------------------------------------------------
# Parsing
# ---------------------------------------------------------------------------


class TestParsing:
    """_read() correctly parses config.ini content."""

    def setup_method(self) -> None:
        Config.reset()

    def teardown_method(self) -> None:
        Config.reset()

    def test_reads_all_required_keys(self) -> None:
        cfg = _make_config(_FULL_INI)
        assert cfg.api_db_database == "midas"
        assert cfg.api_db_hostname == "localhost"
        assert cfg.api_db_password == "changeit"
        assert cfg.api_db_port == 5432
        assert cfg.api_db_username == "midas"
        assert cfg.api_port == "8080"
        assert cfg.api_schema == "http"
        assert cfg.api_url == "localhost"

    def test_skips_comment_lines(self) -> None:
        content = _FULL_INI + "# this is a comment\n"
        cfg = _make_config(content)
        assert cfg.api_db_database == "midas"

    def test_skips_unknown_keys(self) -> None:
        content = _FULL_INI + "UNKNOWN_KEY=value\n"
        cfg = _make_config(content)
        assert cfg.validate() is True

    def test_skips_malformed_lines(self) -> None:
        content = _FULL_INI + "NOEQUALSSIGN\n"
        cfg = _make_config(content)
        assert cfg.validate() is True

    def test_value_with_equals_sign_preserved(self) -> None:
        content = _FULL_INI.replace(
            "API_DB_PASSWORD=changeit", "API_DB_PASSWORD=pass=word")
        cfg = _make_config(content)
        assert cfg.api_db_password == "pass=word"


# ---------------------------------------------------------------------------
# Transitive value
# ---------------------------------------------------------------------------


class TestTransitives:
    """_set_transitives() derives API_ENDPOINT correctly."""

    def setup_method(self) -> None:
        Config.reset()

    def teardown_method(self) -> None:
        Config.reset()

    def test_api_endpoint_derived(self) -> None:
        cfg = _make_config(_FULL_INI)
        assert cfg.api_endpoint == "http://localhost:8080"


# ---------------------------------------------------------------------------
# api_db_port
# ---------------------------------------------------------------------------


class TestApiDbPort:
    """api_db_port validates and converts the port string."""

    def setup_method(self) -> None:
        Config.reset()

    def teardown_method(self) -> None:
        Config.reset()

    def test_returns_integer(self) -> None:
        cfg = _make_config(_FULL_INI)
        assert cfg.api_db_port == 5432
        assert isinstance(cfg.api_db_port, int)

    def test_raises_on_non_numeric_port(self) -> None:
        content = _FULL_INI.replace("API_DB_PORT=5432", "API_DB_PORT=notaport")
        cfg = _make_config(content)
        with pytest.raises(RuntimeError, match="must be numeric"):
            _ = cfg.api_db_port

    def test_raises_on_port_zero(self) -> None:
        content = _FULL_INI.replace("API_DB_PORT=5432", "API_DB_PORT=0")
        cfg = _make_config(content)
        with pytest.raises(RuntimeError, match="1-65535"):
            _ = cfg.api_db_port

    def test_raises_on_port_too_large(self) -> None:
        content = _FULL_INI.replace("API_DB_PORT=5432", "API_DB_PORT=65536")
        cfg = _make_config(content)
        with pytest.raises(RuntimeError, match="1-65535"):
            _ = cfg.api_db_port

    def test_boundary_port_1(self) -> None:
        content = _FULL_INI.replace("API_DB_PORT=5432", "API_DB_PORT=1")
        cfg = _make_config(content)
        assert cfg.api_db_port == 1

    def test_boundary_port_65535(self) -> None:
        content = _FULL_INI.replace("API_DB_PORT=5432", "API_DB_PORT=65535")
        cfg = _make_config(content)
        assert cfg.api_db_port == 65535


# ---------------------------------------------------------------------------
# Log defaults
# ---------------------------------------------------------------------------


class TestLogDefaults:
    """Optional log keys fall back to built-in defaults when absent."""

    def setup_method(self) -> None:
        Config.reset()

    def teardown_method(self) -> None:
        Config.reset()

    def test_log_level_default(self) -> None:
        cfg = _make_config(_FULL_INI)
        assert cfg.log_level == "INFO"

    def test_log_level_from_config(self) -> None:
        content = _FULL_INI + "LOG_LEVEL=DEBUG\n"
        cfg = _make_config(content)
        assert cfg.log_level == "DEBUG"

    def test_log_format_default_not_empty(self) -> None:
        cfg = _make_config(_FULL_INI)
        assert cfg.log_format != ""

    def test_log_date_format_default_not_empty(self) -> None:
        cfg = _make_config(_FULL_INI)
        assert cfg.log_date_format != ""


# ---------------------------------------------------------------------------
# validate()
# ---------------------------------------------------------------------------


class TestValidate:
    """validate() raises when required keys are empty."""

    def setup_method(self) -> None:
        Config.reset()

    def teardown_method(self) -> None:
        Config.reset()

    def test_returns_true_when_all_keys_set(self) -> None:
        cfg = _make_config(_FULL_INI)
        assert cfg.validate() is True

    # Keys safe to omit at construction time — _set_transitives() doesn't
    # reference them, so Config() succeeds but validate() raises ValueError.
    @pytest.mark.parametrize("key", [
        "API_DB_DATABASE",
        "API_DB_HOSTNAME",
        "API_DB_PASSWORD",
        "API_DB_PORT",
        "API_DB_USERNAME",
    ])
    def test_raises_value_error_when_required_key_empty(self, key: str) -> None:
        lines = [line for line in _FULL_INI.splitlines()
                 if not line.startswith(key)]
        content = "\n".join(lines)
        cfg = _make_config(content)
        with pytest.raises(ValueError, match=key):
            cfg.validate()

    # Keys consumed by _set_transitives() during __init__ — missing values
    # raise RuntimeError at construction time before validate() is reached.
    @pytest.mark.parametrize("key", ["API_PORT", "API_SCHEMA", "API_URL"])
    def test_raises_runtime_error_on_construction_when_transitive_key_empty(
        self, key: str
    ) -> None:
        lines = [line for line in _FULL_INI.splitlines()
                 if not line.startswith(key)]
        content = "\n".join(lines)
        Config.reset()
        with pytest.raises(RuntimeError, match=key), patch(
            "builtins.open", mock_open(read_data=content)
        ):
            Config()
