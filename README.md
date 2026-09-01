# Storybox

> Open-source, screenless, privacy-friendly storytelling box inspired by Lunii.  
> Built with Spring Boot for Raspberry Pi and controlled entirely by a physical rotary encoder.
> AI tools generate story text and narration offline, ahead of time.

[![Java](https://img.shields.io/badge/java-21-orange.svg)](https://adoptium.net/)
[![Python](https://img.shields.io/badge/python-3.12-blue.svg)](https://www.python.org/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.4.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/spring--ai-1.1.5-blue.svg)](https://spring.io/projects/spring-ai)
[![Pi4J](https://img.shields.io/badge/pi4j-2.7.0-red.svg)](https://pi4j.com/)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

---

## 🌟 Overview

**Storybox** is an open-source, audio-first physical storytelling box — no screen required. A KY-040 rotary encoder and push button, wired to the Raspberry Pi's GPIO pins, let you browse and pick story ingredients across four axes:

**Hero × Companion × Place × Object**

### State Machine Lifecycle

```text
       ┌──────────┐
       │   IDLE   │ ◄──────────────────────────────┐
       └────┬─────┘                                │
            │ Button press                         │
            ▼                                      │
 ┌──────────────────────┐                          │
 │       CHOOSING       │                          │
 │  1. Hero             │                          │
 │  2. Companion        │ Rotate knob: next/prev   │ Reset / Finished
 │  3. Place            │ Button press: validate   │
 │  4. Object           │                          │
 └──────────┬───────────┘                          │
            │ Final choice validated               │
            ▼                                      │
       ┌──────────┐                                │
       │ PLAYING  │ ───────────────────────────────┘
       └──────────┘
```

### Core Features (v1.0)
- **Screenless Audio UI**: audio guides navigation through every axis.
- **Hardware Integration**: Driven by a KY-040 rotary encoder on Raspberry Pi GPIO.
- **Robust Audio Management**: Process-based `mpv` playback.
- **Multi-Profile Configuration**: 
  - laptop development (`dev` profile, GPIO disabled, mockable HTTP harness) 
  - Raspberry Pi deployment (`pi` profile, hardware GPIO active, auto-starting via systemd)
- **HTTP Test Harness & Observability**: 
  - Full REST API for controlling audio and the state machine without hardware
  - Spring Boot Actuator health and metrics endpoints
- **Automated Content Pipeline**: 
  - Batch text generation via Ollama/Thaura
  - Batch neural TTS synthesis via Kokoro/Chatterbox/Piper

---

## 🛠 Tech Stack

- **Languages**: 
  - **Java 21** (Eclipse Temurin / OpenJDK 21) — Core application runtime
  - **Python 3.12+** — Offline TTS synthesis scripts
  - **Bash** — Automation, deployment, and batch authoring scripts
- **Frameworks & Libraries**:
  - [Spring Boot 3.4.0](https://spring.io/projects/spring-boot) (`spring-boot-starter-web`, `spring-boot-starter-actuator`)
  - [Spring AI 1.1.5](https://spring.io/projects/spring-ai) (`spring-ai-starter-model-ollama`, `spring-ai-starter-model-openai`)
  - [Pi4J v2.7.0](https://pi4j.com/) (`pi4j-core`, `pi4j-plugin-raspberrypi`, `pi4j-plugin-gpiod`)
- **Audio Subsystem**:
  - `mpv` media player (controlled via `ProcessBuilder` subprocess management)
- **Story Generation (LLM Backends)**:
  - [Ollama](https://ollama.com/) (Local/offline: `qwen2.5:7b` for dev, `LiquidAI/lfm2.5-1.2b-instruct:q4_0` for embedded)
  - [Thaura AI](https://thaura.ai/) (Cloud OpenAI-compatible endpoint)
- **TTS Synthesis Engines**:
  - [Kokoro TTS](https://github.com/hexgrad/kokoro) (`kokoro==0.9.4`, French voice `ff_siwis`)
  - [Chatterbox](https://github.com/resemble-ai/chatterbox) (`chatterbox-tts==0.1.7`, zero-shot voice cloning)
  - [Piper](https://github.com/rhasspy/piper) (Local ONNX neural TTS)
  - [eSpeak-ng](https://github.com/espeak-ng/espeak-ng) (Lightweight fast fallback)
- **Package & Dependency Managers**:
  - [Maven](https://maven.apache.org/) (bundled via `./mvnw` wrapper)
  - [Astral uv](https://astral.sh/uv/) (Isolated virtual environment & lockfile management for Python TTS tools)
  - [SDKMAN!](https://sdkman.io/) (JDK version management)
  - [APT](https://wiki.debian.org/Apt) (system package installation)
---

## 📋 Requirements

### Development (Laptop / Local Workstation)
- **Java 21 JDK** (e.g. Eclipse Temurin 21)
- **mpv**: Required for local audio playback
  - macOS: `brew install mpv`
  - Linux (Debian/Ubuntu): `sudo apt update && sudo apt install -y mpv`
- **System Utilities**: `jq`, `ffmpeg`, `curl`
  - Linux: `sudo apt install -y jq ffmpeg curl`
  - macOS: `brew install jq ffmpeg curl`
- **(Required) Audio Generation**:
  - [Astral uv](https://docs.astral.sh/uv/) for Python TTS projects:
    ```bash
    curl -LsSf https://astral.sh/uv/install.sh | sh
    ```
  - Optional NVIDIA GPU with CUDA for accelerated TTS synthesis with Kokoro/Chatterbox.
- **(Optional) Story Generation**:
  - [Ollama](https://ollama.com/) running locally with model pulled (`ollama pull qwen2.5:7b`) OR a [Thaura](https://thaura.ai/) API key.

### Deployment (Raspberry Pi Target)
- **Board**: Raspberry Pi 3, 4, 5, or Zero 2W (Development and tests have been done on a Pi 4 with 4GB RAM).
- **Operating System**: Raspberry Pi OS Lite (64-bit Debian Bookworm) with no desktop.
- **Hardware Module**: KY-040 rotary encoder module with integrated push button and 5-pin header.
- **Audio Output**: 3.5mm headphone jack, USB DAC, or I2S speaker (e.g. MAX98357A).
- **System Dependencies**: Installed automatically via `scripts/setup-pi.sh` (Java 21 via SDKMAN, `mpv`, `libgpiod-dev`, `gpiod`).

---

## ⚡ Quick Startup on dev machine

### 1. Setup and Run

```bash
# 1. Clone the repository
git clone https://github.com/tusken1024/storybox.git
cd storybox

# 2. Generate audio assets
./scripts/tts/gen-audio.sh src/main/resources/stories.json audio

# 3. Launch the Spring Boot application (dev profile is active by default)
./mvnw spring-boot:run
```

### 2. HTTP Control & Test
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

# --- 2. Health & Observability ---
curl -X GET "http://localhost:8080/actuator/health"

# --- 3. AI Story Generation (ollama required) ---
curl -X POST "http://localhost:8080/generator/text?hero=chevalier&companion=dragon&place=chateau&object=epee"
```
---

## 🍓 Raspberry Pi Deployment

For flashing the SD card, running `setup-pi.sh`, and deploying with
`deploy.sh` (including auto-start on boot), see
[`docs/2-SETUP_AND_RUN.md`](docs/2-SETUP_AND_RUN.md).

---

## 📁 Project Structure

For a complete directory overview and file breakdown, see [`docs/1-PROJECT_STRUCTURE.md`](docs/1-PROJECT_STRUCTURE.md).

---

## 🔌 Hardware & Wiring (KY-040 Rotary Encoder)

For pinout, connection schematics, and hardware safety notes, see [`docs/3-HARDWARE_AND_WIRING.md`](docs/3-HARDWARE_AND_WIRING.md).

---

## 📜 Automation Scripts

For a breakdown of deployment, story text generation, and TTS audio synthesis scripts, see [`docs/4-AUTOMATION_SCRIPTS.md`](docs/4-AUTOMATION_SCRIPTS.md).

---

## ⚙️ Environment Variables & Configuration

For Spring Boot application properties, deployment variables, and script configuration parameters, see [`docs/5-CONFIGURATION.md`](docs/5-CONFIGURATION.md).

---

## ✍️ Content Authoring Workflow

For the complete multi-step story text generation and audio synthesis workflow, see [`docs/6-CONTENT_AUTHORING.md`](docs/6-CONTENT_AUTHORING.md).

---

## 🧪 Tests

For test suite architecture and running unit/integration tests, see [`docs/7-TESTS.md`](docs/7-TESTS.md).

---

## 📄 License

This project is licensed under the **Apache License 2.0**. See the [`LICENSE`](LICENSE) file for details.

Copyright (c) 2026 Mejdi Ounissi.