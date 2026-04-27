"""Database engine and session management."""

import logging
import threading
from collections.abc import Generator
from contextlib import contextmanager
from typing import ClassVar

from sqlalchemy import Engine, create_engine
from sqlalchemy.engine import URL
from sqlalchemy.orm import Session, sessionmaker

from config import Config


class DbAccess:
    """Singleton database access.

    Subsequent ``DbAccess()`` calls return the same instance.
    """

    _instance: ClassVar["DbAccess | None"] = None
    _initialized: bool = False

    def __new__(cls) -> "DbAccess":
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
        self._lock = threading.Lock()
        self._engine: Engine | None = None
        self._session_local: sessionmaker[Session] | None = None

    def get_engine(self) -> Engine:
        """Return the shared SQLAlchemy engine, creating it on first call."""
        if self._engine is not None:
            return self._engine
        with self._lock:
            if self._engine is None:
                cfg = Config()
                url = URL.create(
                    drivername="postgresql+psycopg",
                    username=cfg.api_db_username,
                    password=cfg.api_db_password,
                    host=cfg.api_db_hostname,
                    port=cfg.api_db_port,
                    database=cfg.api_db_database,
                )
                self._log.info(
                    "Creating engine for %s:%s/%s",
                    cfg.api_db_hostname,
                    cfg.api_db_port,
                    cfg.api_db_database,
                )
                self._engine = create_engine(
                    url,
                    connect_args={"connect_timeout": 10},
                    pool_pre_ping=True,
                )
        return self._engine

    def get_session(self) -> Session:
        """Return a new SQLAlchemy session."""
        if self._session_local is None:
            engine = self.get_engine()
            with self._lock:
                if self._session_local is None:
                    self._session_local = sessionmaker(bind=engine)
        return self._session_local()

    @contextmanager
    def session_scope(self) -> Generator[Session]:
        """Provide a transactional scope around a series of operations."""
        session = self.get_session()
        try:
            yield session
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    @classmethod
    def reset(cls) -> None:
        """Reset the singleton instance (intended for use in tests)."""
        if cls._instance is not None and cls._instance._engine is not None:
            cls._instance._engine.dispose()
        cls._instance = None
