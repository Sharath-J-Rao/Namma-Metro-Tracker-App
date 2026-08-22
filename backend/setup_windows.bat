@echo off
setlocal
cd /d %~dp0
py -m venv .venv
call .venv\Scripts\activate.bat
python -m pip install --upgrade pip
pip install -r requirements.txt
if not exist .env copy .env.example .env
echo.
echo Backend setup complete.
echo Edit backend\.env and set ADMIN_PASSWORD to a strong password.
pause
