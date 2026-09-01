#!/usr/bin/env bash
#
# gen-audio.sh — generate the project audio assets (cues + static stories) with a
# local TTS engine. No cloud, no API. Run it on your laptop, then deploy the
# resulting MP3s alongside the app (same audio dir your AudioPlayer reads from).
#
# It generates:
#   - one short "cue" MP3 per axis choice (the spoken label), parsed from stories.json
#   - one story MP3 per authored story, read from a matching .txt you write
#
# TTS engine (choose with TTS_ENGINE, default: kokoro):
#   kokoro     — decent voice quality, native French, runs on CPU (GPU if available).
#                Isolated project in tts/kokoro/ (pyproject.toml + uv).
#                Default FR voice: ff_siwis.
#   piper      — more robotic, very lightweight. set PIPER_MODEL=/path/fr_FR-xxx.onnx
#                https://github.com/rhasspy/piper  (grab a French voice from its releases)
#   espeak     — zero-setup, very robotic, just to validate the pipeline.
#   chatterbox — most expressive voice, laptop/GPU only (pre-generation,
#                never live on Pi). Isolated project in tts/chatterbox/
#                (pyproject.toml + uv, torch CUDA via dedicated index).
#                CHATTERBOX_VOICE=/path/reference.wav (optional, voice cloning)
#                CHATTERBOX_DEVICE=cuda|cpu (default: cuda)
#                CHATTERBOX_EXAG=0.7 (expressive intensity, default: 0.7)
#
# Kokoro and Chatterbox each run in their own isolated Python project
# (tts/kokoro/, tts/chatterbox/), managed by `uv` — no manual venv activation,
# `uv run` resolves and installs dependencies on first invocation.
#
# Deps: jq, ffmpeg, uv (https://docs.astral.sh/uv/), and an active TTS engine
# based on TTS_ENGINE.
#
# Usage:
#   ./scripts/tts/gen-audio.sh path/to/stories.json path/to/audio-output-dir
#
set -euo pipefail

PACK="${1:-src/main/resources/stories.json}"
OUT="${2:-audio}"
SRC_STORIES="${OUT}/../stories-src"   # where you write the story texts (.txt)

command -v jq     >/dev/null || { echo "Missing: jq";     exit 1; }
command -v ffmpeg >/dev/null || { echo "Missing: ffmpeg"; exit 1; }

# --- pick a TTS backend ------------------------------------------------------
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

tts() {  # tts "text" out.wav
  local text="$1" wav="$2"
  case "${TTS_ENGINE:-kokoro}" in
    chatterbox)
      command -v uv >/dev/null || { echo "Missing: uv (https://docs.astral.sh/uv/)" >&2; exit 1; }
      local cb_args=(--device "${CHATTERBOX_DEVICE:-cuda}" --exaggeration "${CHATTERBOX_EXAG:-0.7}")
      if [[ -n "${CHATTERBOX_VOICE:-}" ]]; then
        cb_args+=(--voice "${CHATTERBOX_VOICE}")
      fi
      uv run --project "${HERE}/chatterbox" python "${HERE}/chatterbox/chatterbox_tts.py" "$text" "$wav" "${cb_args[@]}"
      ;;
    kokoro)
      command -v uv >/dev/null || { echo "Missing: uv (https://docs.astral.sh/uv/)" >&2; exit 1; }
      uv run --project "${HERE}/kokoro" python "${HERE}/kokoro/kokoro_tts.py" "$text" "$wav" "${KOKORO_VOICE:-ff_siwis}"
      ;;
    piper)
      [[ -n "${PIPER_MODEL:-}" ]] || { echo "Set PIPER_MODEL=/path/to/voice.onnx" >&2; exit 1; }
      echo "$text" | piper --model "$PIPER_MODEL" --output_file "$wav" >/dev/null 2>&1
      ;;
    espeak)
      espeak-ng -v fr+f3 -s 137 -p 53 "$text" -w "$wav"
      ;;
    *)
      echo "Unknown TTS_ENGINE='${TTS_ENGINE}' (use kokoro|piper|espeak|chatterbox)" >&2
      exit 1
      ;;
  esac
}

say_to_mp3() {  # say_to_mp3 "text" relative/path.mp3
  local text="$1" rel="$2" wav mp3
  mp3="${OUT}/${rel}"
  wav="$(mktemp --suffix=.wav)"
  mkdir -p "$(dirname "$mp3")"
  tts "$text" "$wav"
  ffmpeg -y -nostdin -loglevel error -i "$wav" -codec:a libmp3lame -q:a 4 "$mp3"
  rm -f "$wav"
  echo "  ✓ $rel"
}

echo "== Cues (axis choices) =="
# For every axis, every choice: speak the label into its cue file.
jq -r '.axes | to_entries[] | .value[] | "\(.label)\t\(.cue)"' "$PACK" \
| while IFS=$'\t' read -r label cue; do
    say_to_mp3 "$label" "$cue"
  done

echo "== Stories =="
# For each authored story, read ${SRC_STORIES}/<audio-basename>.txt.
# Write those text files yourself (or paste an LLM draft).
jq -r '(.stories[]), .fallback | "\(.title)\t\(.audio)"' "$PACK" \
| while IFS=$'\t' read -r title audio; do
    base="$(basename "${audio%.mp3}")"
    txt="${SRC_STORIES}/${base}.txt"
    if [[ -f "$txt" ]]; then
      say_to_mp3 "$(cat "$txt")" "$audio"
    else
      echo "  ⚠ missing text: $txt  (write the story for \"$title\" there, then re-run)"
    fi
  done

echo "Done. Audio written under: $OUT"