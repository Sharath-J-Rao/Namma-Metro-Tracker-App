@echo off
setlocal
cd /d %~dp0
if not exist .venv (
  echo Backend is not installed. Run setup_windows.bat first.
  pause
  exit /b 1
)
call .venv\Scripts\activate.bat
set PYTHONPATH=%cd%
echo Starting Namma Metro Admin at http://0.0.0.0:8000
uvicorn app:app --host 0.0.0.0 --port 8000
