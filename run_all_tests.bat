@echo off
setlocal
where mvn >nul 2>nul || (echo Maven 3.9+ with Java 17 is required. See docs\HOW_TO_RUN.md. & exit /b 127)
call :java auth-service || exit /b 1
call :java document-service || exit /b 1
call :java query-service || exit /b 1
call :java api-gateway || exit /b 1
python -m pip install -q -e .\python-common[test] requests || exit /b 1
pushd python-common && set PYTHONPATH=. && python -m pytest -q tests || exit /b 1
popd
pushd ai-ingestion-service && set PYTHONPATH=..\python-common && python -m pytest -q tests || exit /b 1
popd
pushd ai-rag-service && set PYTHONPATH=..\python-common && python -m pytest -q tests || exit /b 1
popd
python -m pytest -q tests\integration || exit /b 1
pushd frontend && call npm ci && call npm test || exit /b 1
popd
echo All test suites passed.
exit /b 0
:java
pushd %1
call mvn -q test
set RESULT=%ERRORLEVEL%
popd
exit /b %RESULT%
