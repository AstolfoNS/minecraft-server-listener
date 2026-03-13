#!/bin/bash

# --- 路径配置 ---
BASE_DIR="/www/minecraft/listener"

YAML_PATH="${BASE_DIR}/server/config/application.yaml"
SECRET_FILE="${BASE_DIR}/secret/hmac_secret"

IP="127.0.0.1"

# 检查文件是否存在，防止 Python 报错
if [ ! -f "$YAML_PATH" ]; then
    echo "Error: YAML file not found at $YAML_PATH"
    exit 1
fi

if [ ! -f "$SECRET_FILE" ]; then
    echo "Error: Secret file not found at $SECRET_FILE"
    exit 1
fi

echo "Reading configuration from $YAML_PATH..."

# 提取端口，增加简单的异常处理
PORT=$(python3 -c "import yaml;
try:
    with open('$YAML_PATH') as f:
        print(yaml.safe_load(f)['server'].get('port', 8081))
except Exception:
    print(8081)" )

# 提取 Context Path
CONTEXT_PATH=$(python3 -c "import yaml;
try:
    with open('$YAML_PATH') as f:
        print(yaml.safe_load(f)['server']['servlet'].get('context-path', ''))
except Exception:
    print('')" )

if [ -z "$PORT" ]; then
    echo "Error: Could not extract PORT from YAML."
    exit 1
fi

# --- 签名与请求逻辑 ---

URL="http://${IP}:${PORT}${CONTEXT_PATH}/actuator/refresh"
SIGN_PATH="${CONTEXT_PATH}/actuator/refresh"
HMAC_SECRET=$(cat "$SECRET_FILE")
TIMESTAMP=$(date +%s)
# 修正 nonce 获取方式，确保兼容性
NONCE=$(head /dev/urandom | tr -dc A-Za-z0-9 | head -c 16)
METHOD="POST"

# 构造待签名字符串 (\n 会被 printf %b 正确转义)
CANONICAL_STR="${METHOD}\n${SIGN_PATH}\n${TIMESTAMP}\n${NONCE}"

# 签名计算 (修复 SC2059)
SIGNATURE=$(printf "%b" "$CANONICAL_STR" | openssl dgst -sha256 -hmac "$HMAC_SECRET" -binary | base64 | tr -d '\n')

echo "--------------------------------------"
echo "Config: Port=$PORT, Path=$CONTEXT_PATH"
echo "Target: $URL"
echo "--------------------------------------"

# 发送请求 (修复 -w 输出内容，使其只包含数字)
HTTP_CODE=$(curl -X POST "$URL" \
     -H "X-TS: $TIMESTAMP" \
     -H "X-NONCE: $NONCE" \
     -H "X-SIGN: $SIGNATURE" \
     -H "Content-Type: application/json" \
     -s -o /dev/null -w "%{http_code}")

if [ "$HTTP_CODE" -eq 200 ]; then
    echo "Success: Refresh request sent successfully (200)."
else
    echo "Failed: Server returned HTTP $HTTP_CODE"
    exit 1
fi
