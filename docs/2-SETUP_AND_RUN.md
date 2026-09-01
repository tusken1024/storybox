# 🚀 Setup & Run Commands

### 1. Application Entry Points

| Execution Mode           | Command / Entry Point                                       | Profile Active | Description                                                                          |
|--------------------------|-------------------------------------------------------------|----------------|--------------------------------------------------------------------------------------|
| **Local Development**    | `./mvnw spring-boot:run`                                    | `dev`          | Runs Spring Boot locally on port 8080. GPIO disabled. Audio path: `./audio`.         |
| **Systemd Service (Pi)** | `systemctl --user start storybox`                           | `pi`           | Managed background auto-start user unit on Raspberry Pi.                             |
| **Text Generator**       | `./scripts/stories/gen-stories.sh`                          | —              | Batch CLI to generate story texts via LLM.                                           |
| **TTS Generator**        | `./scripts/tts/gen-audio.sh`                                | —              | Batch CLI to synthesize audio via TTS.                                               |

---

### 2. Local Development (Laptop)

```bash
# 1. Clone the repository
git clone https://github.com/tusken1024/storybox.git
cd storybox

# 2. Generate audio assets if starting from scratch
./scripts/tts/gen-audio.sh src/main/resources/stories.json audio

# 3. Launch the Spring Boot application (dev profile is active by default)
./mvnw spring-boot:run
```

#### HTTP Control & Test Harness
When running locally without physical hardware, use `curl` to drive the state machine, test audio playback, or verify AI text generation:

```bash
# --- 1. Drive the Story State Machine ---
# Start session: IDLE -> CHOOSING (Hero axis)
curl -X POST "http://localhost:8080/story/press"

# Rotate encoder knob: next / previous choice on current axis
curl -X POST "http://localhost:8080/story/turn?dir=cw"
curl -X POST "http://localhost:8080/story/turn?dir=ccw"

# Select and advance through axes (Hero -> Companion -> Place -> Object -> PLAYING)
curl -X POST "http://localhost:8080/story/press"
curl -X POST "http://localhost:8080/story/press"
curl -X POST "http://localhost:8080/story/press"
curl -X POST "http://localhost:8080/story/press"

# Query current state machine status
curl -X GET "http://localhost:8080/story/status"

# Reset state machine back to IDLE
curl -X POST "http://localhost:8080/story/reset"


# --- 2. Direct Audio Player Verification ---
# Play audio file from audio library
curl -X POST "http://localhost:8080/audio/play?file=stories/fallback.mp3"

# Check playback status
curl -X GET "http://localhost:8080/audio/status"

# Stop playback
curl -X POST "http://localhost:8080/audio/stop"


# --- 3. Health & Observability ---
curl -X GET "http://localhost:8080/actuator/health"


# --- 4. AI Story Generation (Developer POC) ---
curl -X POST "http://localhost:8080/generator/text?hero=chevalier&companion=dragon&place=chateau&object=epee"
```

---

### 3. Raspberry Pi Setup & Deployment

#### Step A: One-Time Raspberry Pi Setup
SSH into your Raspberry Pi and execute `setup-pi.sh` to install SDKMAN, Java 21, `mpv`, `libgpiod`, configure system permissions, and enable systemd user lingering:

```bash
# On the Raspberry Pi:
bash scripts/setup-pi.sh

# Reboot to apply group permissions:
sudo reboot
```

#### Step B: Build & Deploy from Laptop
From your development workstation, deploy the application, audio files, and systemd service to the Pi in a single command:

```bash
# Full build + sync audio + deploy JAR + restart systemd service
./scripts/deploy.sh

# Deploy without rebuilding the JAR
./scripts/deploy.sh --skip-build

# Deploy without syncing audio files (faster code-only update)
./scripts/deploy.sh --skip-audio

# Deploy and automatically stream live logs from the Pi
./scripts/deploy.sh --logs
```

#### Step C: Service Management on the Pi
The application runs as a systemd user service (`storybox.service`) that starts automatically on boot:

```bash
# Check service status
systemctl --user status storybox

# View live application logs
journalctl --user -u storybox -f

# Restart or stop the service
systemctl --user restart storybox
systemctl --user stop storybox
```
