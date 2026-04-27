import sys
from pathlib import Path

from app import App  # type: ignore[import]

# Ensure the src directory is on the import path so the package is
# importable even when running via `streamlit run main.py` without
# installing the project.
sys.path.insert(0, str(Path(__file__).resolve().parent / "src"))


def main() -> None:
    app: App = App()
    app.run()


main()
