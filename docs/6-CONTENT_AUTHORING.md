# ✍️ Content Authoring Workflow

Creating and deploying new stories is a modular three-step process:

```text
1. Define story entry in stories.json (Hero, Companion, Place, Object, title, audio path)
                            │
                            ▼
2. ./scripts/stories/gen-stories.sh
   → Generates draft text in stories-src/<key>.txt using Ollama / Thaura
                            │
                            ▼
3. (Optional) Review, refine, and edit stories-src/<key>.txt
                            │
                            ▼
4. ./scripts/tts/gen-audio.sh
   → Synthesizes audio/stories/<key>.mp3 and audio/cues/*.mp3
                            │
                            ▼
5. ./scripts/deploy.sh
   → Deploys updated audio assets and application to Raspberry Pi
```

### Text Generation Examples
```bash
# Generate using default local Ollama (qwen2.5:7b)
./scripts/stories/gen-stories.sh

# Test with the lightweight model intended for the Pi
OLLAMA_MODEL="LiquidAI/lfm2.5-1.2b-instruct:q4_0" ./scripts/stories/gen-stories.sh

# Generate using Thaura AI Cloud
STORY_ENGINE=thaura THAURA_API_KEY=sk-xxx ./scripts/stories/gen-stories.sh

# Force re-generation of all existing text files
FORCE=1 ./scripts/stories/gen-stories.sh
```
> 📖 See [`scripts/stories/README.md`](../scripts/stories/README.md) for full text generation details.

### Audio Synthesis Examples
```bash
# Generate cues and stories with Kokoro TTS (default)
./scripts/tts/gen-audio.sh src/main/resources/stories.json audio

# Generate with Chatterbox and custom reference voice cloning
TTS_ENGINE=chatterbox CHATTERBOX_VOICE=sample.wav ./scripts/tts/gen-audio.sh src/main/resources/stories.json audio

# Generate with Piper ONNX neural engine
TTS_ENGINE=piper PIPER_MODEL=~/piper-voices/fr_FR-siwis-high.onnx ./scripts/tts/gen-audio.sh src/main/resources/stories.json audio

# Fast technical verification with eSpeak
TTS_ENGINE=espeak ./scripts/tts/gen-audio.sh src/main/resources/stories.json audio
```
> 📖 See [`scripts/tts/README.md`](../scripts/tts/README.md) for full audio generation details.
