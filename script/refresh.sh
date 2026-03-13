#!/bin/bash

BASE_DIR="/www/minecraft/listener"
YAML_FILE="${BASE_DIR}/server/config/application.yaml"

# 使用 Python 提取变量
S_PORT=$(python3 -c "import yaml; print(yaml.safe_load(open('$YAML_FILE'))['server']['port'])" 2>/dev/null || echo 8081)
C_PATH=$(python3 -c "import yaml; print(yaml.safe_load(open('$YAML_FILE'))['server']['servlet']['context-path'])" 2>/dev/null || echo "")

URL="http://localhost:${S_PORT}${C_PATH}/actuator/refresh"

echo "Using Python to parse YAML..."
echo "Target URL: $URL"
curl -X POST "$URL"
