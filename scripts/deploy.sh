#!/usr/bin/env bash
# Build the Spring Boot jar locally, push it to the Pi, and run it.
#
# Usage:
#   ./scripts/deploy.sh                       # full build + copy audio files + deploy + run
#   ./scripts/deploy.sh --skip-build          # deploy only (re-push current jar)
#   ./scripts/deploy.sh --skip-audio          # do not copy audio files
#   ./scripts/deploy.sh --logs                # tail the logs after deploy
#
# Configuration via environment variables (with sensible defaults):
#   PI_HOST       — SSH host alias or IP (default: storybox)
#   PI_USER       — SSH username        (default: mejdi)
#   PI_KEY        — SSH private key     (default: ~/.ssh/storybox)
#   REMOTE_DIR    — Target directory    (default: /home/mejdi)

set -euo pipefail

PI_HOST="${PI_HOST:-storybox}"
PI_USER="${PI_USER:-mejdi}"
PI_KEY="${PI_KEY:-$HOME/.ssh/storybox}"
REMOTE_DIR="${REMOTE_DIR:-/home/$PI_USER}"
JAR_NAME="storybox.jar"
LOCAL_JAR="target/${JAR_NAME}"
AUDIO_DIR="audio"

SKIP_BUILD=false
SKIP_AUDIO=false
TAIL_LOGS=false
for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=true ;;
    --skip-audio) SKIP_AUDIO=true ;;
    --logs)       TAIL_LOGS=true ;;
    *) echo "Unknown arg: $arg" >&2; exit 1 ;;
  esac
done

cd "$(dirname "$0")/.."

if ! $SKIP_BUILD; then
  echo "▶ Building (this can take ~30s)…"
  ./mvnw -q -DskipTests package
fi

if [ ! -f "$LOCAL_JAR" ]; then
  echo "❌ $LOCAL_JAR not found. Run without --skip-build first." >&2
  exit 1
fi

SSH_OPTS=(-i "$PI_KEY" -o ConnectTimeout=5 -o StrictHostKeyChecking=accept-new)

if ! $SKIP_AUDIO; then
  echo "▶ Copying audio files..."
  scp "${SSH_OPTS[@]}" -r ${AUDIO_DIR} "${PI_USER}@${PI_HOST}:${REMOTE_DIR}"
fi

echo "▶ Copying jar to ${PI_USER}@${PI_HOST}:${REMOTE_DIR}/${JAR_NAME}…"
scp "${SSH_OPTS[@]}" "$LOCAL_JAR" "${PI_USER}@${PI_HOST}:${REMOTE_DIR}/${JAR_NAME}"

echo "▶ Installing/updating the systemd unit…"
ssh "${SSH_OPTS[@]}" "${PI_USER}@${PI_HOST}" "mkdir -p ${REMOTE_DIR}/.config/systemd/user"
scp "${SSH_OPTS[@]}" scripts/storybox.service "${PI_USER}@${PI_HOST}:${REMOTE_DIR}/.config/systemd/user/storybox.service"

echo "▶ Restarting the storybox service…"
ssh "${SSH_OPTS[@]}" "${PI_USER}@${PI_HOST}" \
  "export XDG_RUNTIME_DIR=/run/user/\$(id -u) && \
   systemctl --user daemon-reload && \
   systemctl --user enable storybox && \
   systemctl --user restart storybox"

echo "✅ Deployed. Try:"
echo "   curl http://${PI_HOST}:8080/actuator/health"
echo "   curl -X POST 'http://${PI_HOST}:8080/audio/play?file=test.mp3'"

if $TAIL_LOGS; then
  ssh "${SSH_OPTS[@]}" "${PI_USER}@${PI_HOST}" \
    "journalctl --user-unit=storybox -f"
fi