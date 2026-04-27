import logging
from typing import ClassVar

import streamlit as st

from common.base_step import BaseStep
from common.health_check import HealthCheck
from config import Config


class App:
    """Singleton Streamlit application.

    Only one instance is ever created; subsequent calls return the
    cached instance.
    """

    _instance: ClassVar["App | None"] = None
    _initialized: bool = False

    def __new__(cls) -> "App":
        if cls._instance is None:
            instance = super().__new__(cls)
            instance._initialized = False
            cls._instance = instance
        return cls._instance

    def __init__(self) -> None:
        if self._initialized:
            return
        self._initialized = True
        self._log: logging.Logger = logging.getLogger(__class__.__name__)
        self._cfg: Config = Config()
        self._steps: list[BaseStep] = [HealthCheck()]
        self._configure_logging()
        self._log.info("App initialized")

    @classmethod
    def reset(cls) -> None:
        """Reset the singleton instance.

        Intended for use in tests where a clean state is required between
        test cases.
        """
        cls._instance = None

    def _configure_logging(self) -> None:
        """Configure root logging from the active :class:`Config`."""
        logging.basicConfig(
            level=getattr(logging, self._cfg.log_level.upper(), logging.INFO),
            format=self._cfg.log_format,
            datefmt=self._cfg.log_date_format,
        )

    def run(self) -> None:
        """Render the Streamlit application.

        Validates configuration, then iterates over all registered
        :class:`~common.base_step.BaseStep` instances and runs each one.
        Stops rendering with a user-visible error message if the
        configuration is invalid.
        """
        self._log.debug("Entering run()")
        st.write("Hello Mars!")

        try:
            Config().validate()
        except RuntimeError as e:
            self._log.critical("Configuration error: %s", e)
            st.error(
                "Required configuration is missing. "
                "Please check server logs for details."
            )
            st.stop()

        st.set_page_config(
            # layout="wide",
            page_title="Midas Test",
            page_icon=":material/experiment:",
        )

        for step in self._steps:
            step.run()
