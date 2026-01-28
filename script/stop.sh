#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="/www/listener"
PID_FILE="${BASE_DIR}/listener.pid"

if [[ ! -f "${PID_FILE}" ]]; then
  echo "No PID file found: ${PID_FILE}"
  echo "Listener may not be running."
  exit 0
fi

PID="$(cat "${PID_FILE}" || true)"

if [[ -z "${PID}" ]]; then
  echo "PID file is empty, removing: ${PID_FILE}"
  rm -f "${PID_FILE}"
  exit 0
fi

if ! kill -0 "${PID}" 2>/dev/null; then
  echo "Process not running (pid=${PID}), removing PID file."
  rm -f "${PID_FILE}"
  exit 0
fi

echo "Stopping listener (pid=${PID}) ..."

# 优雅停止（SIGTERM）
kill "${PID}"

# 等待最多 30 秒
TIMEOUT=30
for ((i=1; i<=TIMEOUT; i++)); do
  if ! kill -0 "${PID}" 2>/dev/null; then
    echo "Stopped successfully."
    rm -f "${PID_FILE}"
    exit 0
  fi
  sleep 1
done

echo "Process did not stop within ${TIMEOUT}s, killing forcibly..."
kill -9 "${PID}" 2>/dev/null || true

# 再等一下确认
sleep 1
if kill -0 "${PID}" 2>/dev/null; then
  echo "ERROR: Failed to kill process (pid=${PID})"
  exit 1
fi

echo "Killed successfully."
rm -f "${PID_FILE}"
