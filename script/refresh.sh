#!/bin/bash

# --- 路径配置 ---
# 假设配置文件在脚本目录的上一级或特定位置，请根据实际情况调整
BASE_DIR="/www/minecraft/listener"

YAML_PATH="{$BASE_DIR}/server/config/application.yaml"
SECRET_FILE="/${BASE_DIR}/secret/hmac_secret"

IP="127.0.0.1"

echo "Reading configuration from $YAML_PATH..."
# 提取端口，默认为 8081
PORT=$(python3 -c "import yaml; print(yaml.safe_load(open('$YAML_PATH'))['server'].get('port', 8081))")
# 提取 Context Path，确保处理首尾斜杠
CONTEXT_PATH=$(python3 -c "import yaml; print(yaml.safe_load(open('$YAML_PATH'))['server']['servlet'].get('context-path', ''))")

if [ -z "$PORT" ] || [ -z "$CONTEXT_PATH" ]; then
    echo "Error: Could not extract PORT or CONTEXT_PATH from YAML."
    exit 1
fi

# --- 签名与请求逻辑 ---

# 准备基础信息
URL="http://${IP}:${PORT}${CONTEXT_PATH}/actuator/refresh"
SIGN_PATH="${CONTEXT_PATH}/actuator/refresh"
HMAC_SECRET=$(cat "$SECRET_FILE")
TIMESTAMP=$(date +%s)
NONCE=$(head /dev/urandom | tr -dc A-Za-z0-9 | head -c 16)
METHOD="POST"

CANONICAL_STR="${METHOD}\n${SIGN_PATH}\n${TIMESTAMP}\n${NONCE}"
SIGNATURE=$(printf "%b" "$CANONICAL_STR" | openssl dgst -sha256 -hmac "$HMAC_SECRET" -binary | base64 | tr -d '\n')

echo "--------------------------------------"
echo "Config: Port=$PORT, Path=$CONTEXT_PATH"
echo "Target: $URL"
echo "--------------------------------------"

# 发送请求
HTTP_CODE=$(curl -X POST "$URL" \
     -H "X-TS: $TIMESTAMP" \
     -H "X-NONCE: $NONCE" \
     -H "X-SIGN: $SIGNATURE" \
     -H "Content-Type: application/json" \
     -s -o /dev/null -w "Response Code: %{http_code}")

if [ "$HTTP_CODE" -eq 200 ]; then
    echo "Refresh request sent successfully."
else
    echo "Failed to send refresh request."
fi
