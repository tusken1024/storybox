#!/usr/bin/env bash
# One-shot setup script to run on a fresh Raspberry Pi OS Lite installation.
# Installs Java 21, mpv, and required packages; creates the audio library
# directory; prepares the storybox user systemd service (installed/started
# by deploy.sh, not here).
#
# Run this ONCE, from an interactive SSH session on the Pi itself
# (sudo needs a real TTY to prompt for your password):
#   ssh mejdi@storybox
#   bash setup-pi.sh

set -euo pipefail

echo "▶ Updating system packages…"
sudo apt-get update -qq

echo "▶ Installing system dependencies (mpv, libgpiod, git, curl)…"
sudo apt-get install -y -qq \
  mpv \
  libgpiod-dev gpiod \
  git curl wget \
  apt-transport-https gpg

echo "▶ Installing SDKMAN! and Java 21…"
export SDKMAN_DIR="$HOME/.sdkman"
if [[ ! -s "$HOME/.sdkman/bin/sdkman-init.sh" ]]; then
  curl -s "https://get.sdkman.io?rcupdate=false" | bash
fi

set +u
# shellcheck source=/dev/null
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.2-tem || true
sdk default java 21.0.2-tem
set -u

echo "▶ Granting GPIO + audio group access to user '$USER'…"
sudo usermod -aG gpio,dialout,audio,systemd-journal "$USER"

echo "▶ Enabling lingering (lets storybox start without a login session)…"
sudo loginctl enable-linger "$USER"
mkdir -p "$HOME/.config/systemd/user"

echo "▶ Creating audio library directory…"
mkdir -p "$HOME/audio"

echo "✅ Pi setup complete!"
echo ""
echo "Versions installed:"
set +u
source "$HOME/.sdkman/bin/sdkman-init.sh"
java -version 2>&1 | head -1 | sed 's/^/   /'
set -u
mpv --version 2>&1 | head -1 | sed 's/^/   /'
echo ""
echo "Next steps:"
echo "  1. Add a test track:   Drop an mp3 file (e.g., test.mp3) into ~/audio/"
echo "  2. Test audio chain:   mpv --no-video --really-quiet ~/audio/<your-file>.mp3"
echo "  3. Verify GPIO access: gpioinfo | head"
echo "  4. Deploy from laptop: ./scripts/deploy.sh"
echo "     (this installs the storybox.service unit and starts it — nothing"
echo "      runs yet on the Pi until this first deploy)"
echo ""
echo "⚠ You may need to reboot for the gpio/audio group membership to take effect:"
echo "   sudo reboot"