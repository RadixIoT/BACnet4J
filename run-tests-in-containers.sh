#!/bin/bash

# Ensure that the working directory is the script directory
cd "$(dirname "$0")"

# Build the assets needed for testing, but don't run the tests.
mvn clean verify -DskipTests

# Start the containers to run the tests.
docker-compose -f src/test/resources/docker-compose.yml -p bacnet4j up --build --abort-on-container-exit
