#!/bin/bash

echo "========================================"
echo "Advanced Minecraft Plugin - Database Setup"
echo "========================================"
echo ""
echo "This script will set up MongoDB and Redis using Docker."
echo "Make sure Docker is installed and running!"
echo ""

# Check if Docker is running
if ! docker version >/dev/null 2>&1; then
    echo "ERROR: Docker is not running or not installed!"
    echo "Please install Docker and start it, then run this script again."
    echo ""
    echo "Installation guides:"
    echo "- Ubuntu/Debian: https://docs.docker.com/engine/install/ubuntu/"
    echo "- macOS: https://docs.docker.com/desktop/install/mac-install/"
    exit 1
fi

echo "Docker is running. Starting databases..."
echo ""

# Stop and remove existing containers if they exist
echo "Stopping existing containers..."
docker stop mongodb redis 2>/dev/null
docker rm mongodb redis 2>/dev/null

# Start MongoDB
echo "Starting MongoDB..."
if ! docker run -d --name mongodb -p 27017:27017 mongo:latest; then
    echo "ERROR: Failed to start MongoDB!"
    exit 1
fi

# Start Redis
echo "Starting Redis..."
if ! docker run -d --name redis -p 6379:6379 redis:latest; then
    echo "ERROR: Failed to start Redis!"
    echo "Stopping MongoDB..."
    docker stop mongodb
    docker rm mongodb
    exit 1
fi

echo ""
echo "========================================"
echo "SUCCESS! Databases are now running:"
echo "========================================"
echo "MongoDB: localhost:27017"
echo "Redis: localhost:6379"
echo ""
echo "You can now start your Minecraft server with the plugin!"
echo ""
echo "To stop the databases later, run:"
echo "docker stop mongodb redis"
echo ""
echo "To start them again, run this script again."
echo "" 