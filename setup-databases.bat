@echo off
echo ========================================
echo Advanced Minecraft Plugin - Database Setup
echo ========================================
echo.
echo This script will set up MongoDB and Redis using Docker.
echo Make sure Docker Desktop is installed and running!
echo.

REM Check if Docker is running
docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not running or not installed!
    echo Please install Docker Desktop from https://www.docker.com/products/docker-desktop/
    echo Then start Docker Desktop and run this script again.
    pause
    exit /b 1
)

echo Docker is running. Starting databases...
echo.

REM Stop and remove existing containers if they exist
echo Stopping existing containers...
docker stop mongodb redis 2>nul
docker rm mongodb redis 2>nul

REM Start MongoDB
echo Starting MongoDB...
docker run -d --name mongodb -p 27017:27017 mongo:latest
if %errorlevel% neq 0 (
    echo ERROR: Failed to start MongoDB!
    pause
    exit /b 1
)

REM Start Redis
echo Starting Redis...
docker run -d --name redis -p 6379:6379 redis:latest
if %errorlevel% neq 0 (
    echo ERROR: Failed to start Redis!
    echo Stopping MongoDB...
    docker stop mongodb
    docker rm mongodb
    pause
    exit /b 1
)

echo.
echo ========================================
echo SUCCESS! Databases are now running:
echo ========================================
echo MongoDB: localhost:27017
echo Redis: localhost:6379
echo.
echo You can now start your Minecraft server with the plugin!
echo.
echo To stop the databases later, run:
echo docker stop mongodb redis
echo.
echo To start them again, run this script again.
echo.
pause 