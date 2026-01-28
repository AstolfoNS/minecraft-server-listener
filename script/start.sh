#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="/www/listener"
SERVER_DIR="${BASE_DIR}/server"
CONFIG_FILE="${SERVER_DIR}/config/application.yaml"
JAR_FILE="${SERVER_DIR}/minecraft-0.0.1-SNAPSHOT.jar"
LOG_DIR="${BASE_DIR}/logs"
PID_FILE="${BASE_DIR}/listener.pid"

mkdir -p "${LOG_DIR}"

if [[ ! -f "${JAR_FILE}" ]]; then
  echo "ERROR: Jar not found: ${JAR_FILE}"
  exit 1
fi

if [[ ! -f "${CONFIG_FILE}" ]]; then
  echo "ERROR: Config not found: ${CONFIG_FILE}"
  exit 1
fi

# 已有进程在跑，直接退出
if [[ -f "${PID_FILE}" ]]; then
  OLD_PID="$(cat "${PID_FILE}" || true)"
  if [[ -n "${OLD_PID}" ]] && kill -0 "${OLD_PID}" 2>/dev/null; then
    echo "Listener already running (pid=${OLD_PID})"
    exit 0
  else
    rm -f "${PID_FILE}"
  fi
fi

cd "${SERVER_DIR}"

TS="$(date +'%F_%H-%M-%S')"
LOG_FILE="${LOG_DIR}/listener_${TS}.log"

echo "Starting minecraft-listener..."
echo "  Jar:    ${JAR_FILE}"
echo "  Config: ${CONFIG_FILE}"
echo "  Log:    ${LOG_FILE}"

# 如果你使用环境变量方式注入密钥（推荐）
# export SECURITY_API_KEY="change-me-to-a-long-random-string"

nohup java -jar "${JAR_FILE}" \
  --spring.config.location="file:${CONFIG_FILE}" \
  > "${LOG_FILE}" 2>&1 &

NEW_PID=$!
echo "${NEW_PID}" > "${PID_FILE}"

sleep 1
if kill -0 "${NEW_PID}" 2>/dev/null; then
  echo "Started successfully (pid=${NEW_PID})"
else
  echo "Failed to start. Check log: ${LOG_FILE}"
  rm -f "${PID_FILE}"
  exit 1
fi
