#!/usr/bin/env bash
# Deploy Care Companion API to a remote Ubuntu host over SSH.
#
# PREREQS (your laptop): rsync, ssh
# PREREQS (remote): Docker + Compose plugin, directory writable by DEPLOY_USER
#
# FIRST TIME ON THE SERVER (SSH in manually):
#   mkdir -p /home/ubuntu/care-companion-api
#   cd /home/ubuntu/care-companion-api
#   cp .env.example .env   # then edit .env — JWT_SECRET, Twilio (or TWILIO_MOCK=true)
# Point Caddy at 127.0.0.1:3000 — see scripts/caddy-api-reverse-proxy.snippet
#
# EVERY DEPLOY FROM YOUR LAPTOP (run inside this repo's server/ folder):
#   chmod +x scripts/deploy-via-ssh.sh   # once
#   DEPLOY_HOST=YOUR_EC2_PUBLIC_IP_OR_DNS \
#   DEPLOY_USER=ubuntu \
#   DEPLOY_KEY=/path/to/your.pem \
#   ./scripts/deploy-via-ssh.sh
#
# Notes:
# - .env is NEVER rsynced (excluded) so secrets stay only on the server.
# - node_modules and dist are excluded; the remote image builds fresh.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${SERVER_ROOT}"

: "${DEPLOY_HOST:?Set DEPLOY_HOST (Elastic IP or hostname)}"
DEPLOY_USER="${DEPLOY_USER:-ubuntu}"
DEPLOY_KEY="${DEPLOY_KEY:-}"
REMOTE_DIR="${REMOTE_DIR:-/home/${DEPLOY_USER}/care-companion-api}"

SSH_ARGS=( -o StrictHostKeyChecking=accept-new )
if [[ -n "${DEPLOY_KEY}" ]]; then
  SSH_ARGS+=( -i "${DEPLOY_KEY}" )
fi

TARGET="${DEPLOY_USER}@${DEPLOY_HOST}"

if [[ -n "${DEPLOY_KEY}" ]]; then
  RSYNC_RSH="ssh -i ${DEPLOY_KEY} -o StrictHostKeyChecking=accept-new"
else
  RSYNC_RSH="ssh -o StrictHostKeyChecking=accept-new"
fi

echo "==> Ensuring remote dir exists: ${REMOTE_DIR}"
ssh "${SSH_ARGS[@]}" "${TARGET}" "mkdir -p '${REMOTE_DIR}'"

echo "==> Rsync ${SERVER_ROOT}/ → ${TARGET}:${REMOTE_DIR}/"
rsync -az --delete --human-readable \
  -e "${RSYNC_RSH}" \
  --filter 'P .env' \
  --exclude node_modules \
  --exclude dist \
  --exclude .git \
  --exclude .env \
  ./ "${TARGET}:${REMOTE_DIR}/"

echo "==> docker compose up -d --build (remote)"
ssh "${SSH_ARGS[@]}" "${TARGET}" "cd '${REMOTE_DIR}' && docker compose up -d --build"

echo "==> Done. Quick check (from laptop):"
echo "    curl -sS \"https://YOUR_API_DOMAIN/health\""
echo "Remote logs:"
echo "    ssh ... '${TARGET}' \"cd '${REMOTE_DIR}' && docker compose logs -f --tail=80 api\""
