#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="/www/listener"
SERVER_DIR="${BASE_DIR}/server"
CONFIG_DIR="${SERVER_DIR}/config"
CONFIG_FILE="${CONFIG_DIR}/application.yaml"
JAR_FILE="${SERVER_DIR}/minecraft-0.0.1-SNAPSHOT.jar"
LOG_DIR="${BASE_DIR}/logs"
PID_FILE="${BASE_DIR}/listener.pid"
SECRET_FILE="${BASE_DIR}/secret/hmac_secret"

# 确保日志目录存在
mkdir -p "${LOG_DIR}"

# 检查 Jar 和配置文件是否存在
check_file_exists() {
  if [[ ! -f "$1" ]]; then
    echo "ERROR: $2 not found: $1"
    exit 1
  fi
}

check_file_exists "${JAR_FILE}" "Jar"
check_file_exists "${CONFIG_FILE}" "Config"
check_file_exists "${SECRET_FILE}" "Secret file"

HMAC_SECRET="$(tr -d '\r\n' < "${SECRET_FILE}")"
if [[ -z "${HMAC_SECRET}" ]]; then
  echo "ERROR: Secret file is empty: ${SECRET_FILE}"
  exit 1
fi
export HMAC_SECRET

# 检查是否有已运行的进程
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
echo "  Jar:        ${JAR_FILE}"
echo "  Config dir: ${CONFIG_DIR}"
echo "  Secret:     ${SECRET_FILE}"
echo "  Log:        ${LOG_FILE}"

JAVA_BIN="${JAVA_BIN:-java}"
JAVA_OPTS="${JAVA_OPTS:--Xms128m -Xmx512m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai}"

nohup "${JAVA_BIN}" "${JAVA_OPTS}" -jar "${JAR_FILE}" \
  --spring.config.additional-location="file:${CONFIG_DIR}/" \
  > "${LOG_FILE}" 2>&1 &

NEW_PID=$!
echo "${NEW_PID}" > "${PID_FILE}"

# 等待进程启动
sleep 1
if kill -0 "${NEW_PID}" 2>/dev/null; then
  echo "Started successfully (pid=${NEW_PID})"
else
  echo "Failed to start. Check log: ${LOG_FILE}"
  rm -f "${PID_FILE}"
  exit 1
fi
