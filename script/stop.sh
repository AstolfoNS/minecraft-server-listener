#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="/www/listener"
PID_FILE="${BASE_DIR}/listener.pid"

if [[ ! -f "${PID_FILE}" ]]; then
  echo "No pid file found. Is listener running?"
  exit 0
fi

PID="$(cat "${PID_FILE}" || true)"
if [[ -z "${PID}" ]]; then
  echo "Empty pid file."
  rm -f "${PID_FILE}"
  exit 0
fi

if kill -0 "${PID}" 2>/dev/null; then
  echo "Stopping listener (pid=${PID})..."
  kill "${PID}"

  # 等待最多 20 秒优雅退出
  for i in {1..20}; do
    if kill -0 "${PID}" 2>/dev/null; then
      sleep 1
    else
      break
    fi
  done

  # 还没退出就强杀
  if kill -0 "${PID}" 2>/dev/null; then
    echo "Still running, killing forcibly..."
    kill -9 "${PID}" || true
  fi

  echo "Stopped."
else
  echo "Process not running, cleaning pid file."
fi

rm -f "${PID_FILE}"
