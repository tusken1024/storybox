# CLAUDE.md

> **Scope note:** v1.0's runtime is static MP3 playback only — no network
> or LLM calls happen during actual play. The AI backends documented below
> (`spring.ai.ollama.*`, `StoryGenerator`) power two offline-only paths:
> the `scripts/stories/` + `scripts/tts/` authoring pipeline, and the
> `StoryGeneratorController` HTTP POC. 
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build and run (dev profile, no GPIO)
./mvnw spring-boot:run

# Run on Pi (GPIO enabled)
./mvnw spring-boot:run -Dspring-active-profiles=pi

# Run tests (always uses dev profile — no hardware needed)
./mvnw test

# Build fat JAR
./mvnw package -DskipTests

# Deploy to Pi (build + copy + restart)
./scripts/deploy.sh
./scripts/deploy.sh --logs   # + tail logs

# Generate story texts from stories.json (default: Ollama, qwen2.5:7b)
./scripts/stories/gen-stories.sh src/main/resources/stories.json stories-src

# Generate story texts with specific engine or model
STORY_ENGINE=ollama OLLAMA_MODEL="LiquidAI/lfm2.5-1.2b-instruct:q4_0" ./scripts/stories/gen-stories.sh
STORY_ENGINE=thaura THAURA_API_KEY=sk-xxx ./scripts/stories/gen-stories.sh
FORCE=1 ./scripts/stories/gen-stories.sh   # re-generate existing .txt files

# Generate audio assets from stories-src/ and stories.json (default: Kokoro)
./scripts/tts/gen-audio.sh src/main/resources/stories.json audio

# Generate audio assets with specific TTS engine (kokoro, chatterbox, piper, espeak)
TTS_ENGINE=chatterbox ./scripts/tts/gen-audio.sh src/main/resources/stories.json audio
TTS_ENGINE=piper      ./scripts/tts/gen-audio.sh src/main/resources/stories.json audio
TTS_ENGINE=espeak     ./scripts/tts/gen-audio.sh src/main/resources/stories.json audio
```

## Architecture

Storybox is a Spring Boot app for a physical Raspberry Pi storyteller. The core user interaction is a KY-040 rotary encoder: turn to browse, press to select.

### Request flow

```
KY-040 hardware
    └─> RotaryEncoderService  (Pi4J, only when storybox.gpio.enabled=true)
            └─> publishes RotaryEvent (CW / CCW / PRESS)
                    └─> StoryEngineBridge  (Spring @EventListener)
                                └─> StoryEngine (state machine)
                                        └─> AudioPlayer (mpv subprocess)

HTTP (dev/testing)
    └─> StoryController  ─> StoryEngine
    └─> AudioController  ─> AudioPlayer
    └─> StoryGeneratorController  ─> StoryGenerator
```

### StoryEngine state machine

`StoryEngine` is the heart of the app. States: `IDLE → CHOOSING → PLAYING`.

- **IDLE → CHOOSING**: first press; cycles through 4 axes in order: `HERO → COMPANION → PLACE → OBJECT`
- **CHOOSING**: turn cycles options within the active axis (plays an audio cue per option); press validates the choice and advances to the next axis
- **CHOOSING(OBJECT) + press**: all 4 axes resolved → resolves the story selection via `StoryLibrary.resolve()` and transitions to **PLAYING** (plays the authored story MP3 or fallback)
- **PLAYING → press → IDLE**: stops playback and resets back to IDLE

All state transitions are `synchronized`.

### Story data

`StoryLibrary` loads `src/main/resources/stories.json` (or the path in `storybox.story.pack`). The JSON defines:
- `axes`: a map of `StoryAxis → List<StoryChoice>`, each choice has an `id`, `label`, and `cue` (path to an MP3 announcement)
- `stories`: authored `Story` entries keyed by `HERO|COMPANION|PLACE|OBJECT` id strings
- `fallback`: played when no authored story matches the selection

### AI backends

Configured via `spring.ai.ollama.*` and `spring.ai.openai.*` (Thaura):
- `StoryGenerator` builds chat clients for both Ollama and Thaura (default: Ollama).
- **dev**: Ollama at `localhost:11434`, model `qwen2.5:7b`
- **pi**: Ollama, model `LiquidAI/lfm2.5-1.2b-instruct:q4_0`, with Pi-specific tuning (num_threads=4, keep_alive=-1, num-predict=450)
- **Thaura**: set `THAURA_API_KEY` env var (`spring.ai.openai.base-url=https://backend.thaura.ai`, model `thaura`)

### Profiles

| Profile | GPIO | Audio dir           | AI model                             |
|---------|------|---------------------|--------------------------------------|
| `dev`   | off  | `./audio`           | `qwen2.5:7b`                         |
| `pi`    | on   | `/home/mejdi/audio` | `LiquidAI/lfm2.5-1.2b-instruct:q4_0` |

### Key configuration properties (`storybox.*`)

Bound via `StoryboxProperties` (`@ConfigurationProperties(prefix = "storybox")`):
- `storybox.audio.library-dir` — root directory for all audio files (cues, generated, authored MP3s)
- `storybox.audio.player` — external player command (default: `mpv`)
- `storybox.audio.default-track` — fallback track used mainly for test (default: `fallback.mp3`)
- `storybox.gpio.enabled` / `*.clk-pin` / `*.dt-pin` / `*.sw-pin` — Pi4J GPIO configuration (BCM pin numbers)

Bound via `@Value`:
- `storybox.story.pack` — path/classpath to `stories.json` (default: `classpath:stories.json`)

### GPIO guard

Every GPIO-touching service **must** carry `@ConditionalOnProperty(name = "storybox.gpio.enabled", havingValue = "true")`.

### Audio

`AudioPlayer` uses `ProcessBuilder` to invoke `mpv` — barge-in semantics: calling `play()` while already playing kills the previous process. `AudioController` exposes `/audio/play?file=`, `/audio/stop`, `/audio/status`.

### Testing

Use `@ActiveProfiles("dev")` on full-context tests. For controller tests, use slice testing with `@WebMvcTest(controllers = ...)` and `@MockitoBean` (see `.junie/AGENTS.md` for an example). GPIO is never initialized in tests.

### HTTP test surface (dev)

```bash
curl -X POST "http://localhost:8080/story/press"          # IDLE -> CHOOSING(HERO)
curl -X POST "http://localhost:8080/story/turn?dir=cw"    # next option
curl -X POST "http://localhost:8080/story/turn?dir=ccw"   # previous option
curl -X GET  "http://localhost:8080/story/status"
curl -X POST "http://localhost:8080/story/reset"
curl -X POST "http://localhost:8080/audio/play?file=test.mp3"
curl -X GET  "http://localhost:8080/audio/status"
curl -X POST "http://localhost:8080/audio/stop"
curl -X POST "http://localhost:8080/generator/text?hero=princesse&companion=renard&place=foret&object=lanterne"
curl         "http://localhost:8080/actuator/health"
```

### Story Text Generation (`scripts/stories/`)

Offline story text files are pre-generated from `stories.json` using `scripts/stories/gen-stories.sh`. It writes `.txt` files to `stories-src/<key>.txt` matching the `StorySelection.key()` format.

#### Directory layout
```
scripts/stories/
├── README.md                  # Text generation guide & LLM backends
└── gen-stories.sh             # Batch LLM text generator (curl + jq)
```

#### Available LLM backends (`STORY_ENGINE=`)
- `ollama` (default): Local/offline generation using Ollama (`OLLAMA_HOST=http://localhost:11434`, `OLLAMA_MODEL=qwen2.5:7b` or Pi model `LiquidAI/lfm2.5-1.2b-instruct:q4_0`).
- `thaura`: Cloud generation via ThauraAI OpenAI-compatible API (`THAURA_BASE_URL=https://backend.thaura.ai/v1`, `THAURA_MODEL=thaura`, `THAURA_API_KEY=...`).
- `FORCE=1`: Overwrites existing `.txt` files instead of skipping.

### TTS & Audio Generation (`scripts/tts/`)

Offline audio assets (axis option cues and static stories) are generated from `stories.json` and `stories-src/*.txt` using `scripts/tts/gen-audio.sh`.

#### Directory layout
```
scripts/tts/
├── gen-audio.sh               # Multi-engine batch synthesis orchestrator
├── kokoro/                    # Kokoro TTS isolated project
│   ├── pyproject.toml         # Locked dependencies (kokoro==0.9.4, torch==2.6.0, soundfile)
│   ├── kokoro_tts.py          # Python synthesis script (default voice: ff_siwis)
│   └── uv.lock                # Pinned dependency lockfile
└── chatterbox/                # Chatterbox TTS isolated project
    ├── pyproject.toml         # Locked dependencies (chatterbox-tts==0.1.7, torch, cu124)
    ├── chatterbox_tts.py      # Python script (voice cloning, auto-chunking ~250 chars)
    └── uv.lock                # Pinned dependency lockfile
```

`kokoro` and `chatterbox` are isolated Python projects managed via [Astral uv](https://astral.sh/uv/) (`uv run --project <dir>` handles virtualenvs and dependencies automatically).

#### Available TTS engines (`TTS_ENGINE=`)
- `kokoro` (default): Fast, natural French voice (`ff_siwis`), runs on CPU/GPU.
- `chatterbox`: Highly expressive voice, supports voice cloning (`CHATTERBOX_VOICE=ref.wav`), emotion exaggeration (`CHATTERBOX_EXAG=0.7`, default 0.7), and CUDA GPU acceleration (`CHATTERBOX_DEVICE=cuda`). Text is chunked to ~250 chars to avoid CUDA OOM.
- `piper`: Standalone ONNX runtime, lightweight (suited for on-device live synthesis on Pi). Requires `PIPER_MODEL` pointing to the `.onnx` voice model (e.g. `fr_FR-siwis-high.onnx`).
- `espeak`: Zero-setup synthetic fallback for fast pipeline verification.

#### Standalone Python CLI calls (via uv)
```bash
# Kokoro
uv run --project scripts/tts/kokoro python scripts/tts/kokoro/kokoro_tts.py "Bonjour" out.wav [voice]

# Chatterbox
uv run --project scripts/tts/chatterbox python scripts/tts/chatterbox/chatterbox_tts.py "Bonjour" out.wav [--voice ref.wav] [--exaggeration 0.7] [--device cuda]
```

### Story authoring & asset pipeline

1. **Define combinations**: Add entries to `src/main/resources/stories.json` (`selection`, `title`, `audio` file path).
2. **Generate story texts**: Run `./scripts/stories/gen-stories.sh` to generate `stories-src/<key>.txt` via Ollama or Thaura.
3. **(Optional) Review text**: Edit or refine texts in `stories-src/` manually.
4. **Synthesize audio**: Run `./scripts/tts/gen-audio.sh src/main/resources/stories.json audio` to batch-synthesize option cues into `audio/cues/` and static story MP3s into `audio/stories/`.
