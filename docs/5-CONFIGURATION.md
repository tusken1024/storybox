# ⚙️ Environment Variables & Configuration

### 1. Spring Boot Application Properties

Configured in `application.properties`, `application-dev.properties`, and `application-pi.properties`:

| Property                              | Default (`dev`)             | Default (`pi`)                       | Description                                             |
|---------------------------------------|-----------------------------|--------------------------------------|---------------------------------------------------------|
| `storybox.audio.library-dir`          | `./audio`                   | `/home/mejdi/audio`                  | Root directory containing audio cues and story MP3s     |
| `storybox.audio.player`               | `mpv`                       | `mpv`                                | Executable used for audio playback                      |
| `storybox.audio.default-track`        | `stories/fallback.mp3`      | `stories/fallback.mp3`               | Fallback track when a combination has no authored audio |
| `storybox.gpio.enabled`               | `false`                     | `true`                               | Enables or disables Pi4J hardware GPIO listener         |
| `storybox.gpio.clk-pin`               | `22`                        | `22`                                 | BCM GPIO pin for rotary encoder CLK                     |
| `storybox.gpio.dt-pin`                | `27`                        | `27`                                 | BCM GPIO pin for rotary encoder DT                      |
| `storybox.gpio.sw-pin`                | `17`                        | `17`                                 | BCM GPIO pin for rotary encoder push button             |
| `storybox.story.pack`                 | `classpath:stories.json`    | `classpath:stories.json`             | Resource path to story pack JSON                        |
| `spring.profiles.default`             | `dev`                       | —                                    | Default active profile                                  |
| `spring.ai.ollama.base-url`           | `http://localhost:11434`    | `http://localhost:11434`             | Local Ollama API host                                   |
| `spring.ai.ollama.chat.options.model` | `qwen2.5:7b`                | `LiquidAI/lfm2.5-1.2b-instruct:q4_0` | Model used for Ollama generation                        |
| `spring.ai.openai.base-url`           | `https://backend.thaura.ai` | `https://backend.thaura.ai`          | Base URL for Thaura cloud API                           |
| `spring.ai.openai.api-key`            | `${THAURA_API_KEY:none}`    | `${THAURA_API_KEY:none}`             | API key for Thaura cloud API                            |
| `server.port`                         | `8080`                      | `8080`                               | Embedded HTTP server port                               |

---

### 2. Deployment Script Variables (`scripts/deploy.sh`)

| Variable     | Default Value     | Description                                           |
|--------------|-------------------|-------------------------------------------------------|
| `PI_HOST`    | `storybox`        | SSH hostname or IP address of the target Raspberry Pi |
| `PI_USER`    | `mejdi`           | Remote SSH user on the Raspberry Pi                   |
| `PI_KEY`     | `~/.ssh/storybox` | Path to the private SSH key used for authentication   |
| `REMOTE_DIR` | `/home/$PI_USER`  | Target deployment directory on the Raspberry Pi       |

Example:
```bash
PI_HOST="192.168.1.50" PI_USER="pi" PI_KEY="~/.ssh/id_rsa" ./scripts/deploy.sh
```

---

### 3. Story Text Generation Variables (`scripts/stories/gen-stories.sh`)

| Variable          | Default Value                  | Allowed Values     | Description                                                         |
|-------------------|--------------------------------|--------------------|---------------------------------------------------------------------|
| `STORY_ENGINE`    | `ollama`                       | `ollama`, `thaura` | LLM backend for story text generation                               |
| `OLLAMA_HOST`     | `http://localhost:11434`       | URL                | Endpoint of local Ollama server                                     |
| `OLLAMA_MODEL`    | `qwen2.5:7b`                   | Model tag          | Ollama model identifier (e.g. `LiquidAI/lfm2.5-1.2b-instruct:q4_0`) |
| `THAURA_BASE_URL` | `https://backend.thaura.ai/v1` | URL                | Base URL for Thaura cloud API                                       |
| `THAURA_MODEL`    | `thaura`                       | Model tag          | Model identifier for Thaura AI                                      |
| `THAURA_API_KEY`  | *(none)*                       | String             | Required API key when `STORY_ENGINE=thaura`                         |
| `FORCE`           | `0`                            | `0`, `1`           | Set `FORCE=1` to overwrite existing `.txt` files in `stories-src/`  |

---

### 4. Audio Synthesis Variables (`scripts/tts/gen-audio.sh`)

| Variable            | Default Value | Allowed Values                            | Description                                                   |
|---------------------|---------------|-------------------------------------------|---------------------------------------------------------------|
| `TTS_ENGINE`        | `kokoro`      | `kokoro`, `chatterbox`, `piper`, `espeak` | TTS synthesis engine backend                                  |
| `KOKORO_VOICE`      | `ff_siwis`    | Voice ID                                  | Voice name for Kokoro TTS                                     |
| `CHATTERBOX_VOICE`  | *(none)*      | File path                                 | Reference audio file (`.wav`) for Chatterbox voice cloning    |
| `CHATTERBOX_DEVICE` | `cuda`        | `cuda`, `cpu`                             | Processing device for Chatterbox                              |
| `CHATTERBOX_EXAG`   | `0.7`         | Float `0.0` - `1.0`                       | Emotion exaggeration parameter for Chatterbox                 |
| `PIPER_MODEL`       | *(none)*      | File path                                 | Path to Piper ONNX voice model (e.g. `fr_FR-siwis-high.onnx`) |
