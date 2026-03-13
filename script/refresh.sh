#!/bin/bash

BASE_DIR="/www/minecraft/listener"
YAML_FILE="${BASE_DIR}/server/config/application.yaml"

PORT=$(yq '.server.port // 8080' $YAML_FILE)
CONTEXT_PATH=$(yq '.server.servlet.context-path // ""' $YAML_FILE)

REFRESH_URL="http://localhost:${PORT}${CONTEXT_PATH}/actuator/refresh"

echo "从配置文件读取到端口: $PORT"
echo "正在刷新: $REFRESH_URL"
curl -X POST "$REFRESH_URL"