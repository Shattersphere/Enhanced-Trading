@echo off
rem Creates a compact code-review archive under zips\ without source edits.
setlocal
set "ROOT=%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT%scripts\create_review_code_zip.ps1"
exit /b %ERRORLEVEL%
