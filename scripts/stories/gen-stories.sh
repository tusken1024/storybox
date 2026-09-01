#!/usr/bin/env bash
#
# gen-stories.sh — pre-generate the story *texts* (not audio) for every
# combination already in stories.json, using a local Ollama model
# or a Thaura API key. No cloud dependency required if you stick to Ollama.
#
# This script only ever touches combinations you've explicitly listed in
# stories.json's `stories[]` array — it does not try every possible
# Hero x Companion x Place x Object combination.
#
# LLM backend (choose with STORY_ENGINE, default: ollama):
#   ollama  — local, offline, no API key. Talks to $OLLAMA_HOST/api/chat.
#             OLLAMA_HOST=http://localhost:11434 (default)
#             OLLAMA_MODEL=qwen2.5:7b            (default; use the Pi model to
#                                                  sanity-check quality/latency
#                                                  before deploying, e.g.
#                                                  LiquidAI/lfm2.5-1.2b-instruct:q4_0)
#   thaura  — THAURA_BASE_URL=https://backend.thaura.ai/v1  (default)
#             THAURA_MODEL=thaura                           (default)
#             THAURA_API_KEY=...                            (required, use your own)
#
# FORCE=1  regenerate even if the .txt already exists (default: skip, same
#          "cache" philosophy as gen-audio.sh and GenerativeStorySource).
#
# Usage:
#   ./scripts/stories/gen-stories.sh [path/to/stories.json] [stories-src-dir]
#
# Deps: jq, curl. For 'ollama', a running `ollama serve` with the model pulled
# (`ollama pull qwen2.5:7b`). For 'thaura', just curl + a valid API key.
#
set -euo pipefail

PACK="${1:-src/main/resources/stories.json}"
OUT="${2:-stories-src}"

command -v jq   >/dev/null || { echo "Missing: jq";   exit 1; }
command -v curl >/dev/null || { echo "Missing: curl"; exit 1; }
[[ -f "$PACK" ]] || { echo "Story pack not found: $PACK"; exit 1; }

mkdir -p "$OUT"

# --- keep this in sync with StoryGenerator.java's defaultSystem() -----------
SYSTEM_PROMPT="$(cat <<'EOF'
Tu es un conteur pour enfants de 4 à 7 ans.
Tu écris une courte histoire du soir en français, douce et rassurante.
Règles :
- 200 à 300 mots maximum.
- Phrases courtes et simples.
- Aucune violence, aucune peur durable, rien d'effrayant.
- Une fin apaisante qui invite au sommeil.
- Termine par une phrase douce, par exemple « Bonne nuit ».
- Réponds UNIQUEMENT avec le texte de l'histoire, sans titre ni commentaire.
EOF
)"

# --- LLM backends -------------------------------------------------------

ollama_generate() {  # ollama_generate "user prompt" -> stdout: story text
  local user="$1"
  local host="${OLLAMA_HOST:-http://localhost:11434}"
  local model="${OLLAMA_MODEL:-qwen2.5:7b}"

  local payload response
  payload="$(jq -n \
    --arg model "$model" \
    --arg system "$SYSTEM_PROMPT" \
    --arg user "$user" \
    '{model: $model, stream: false,
      messages: [{role:"system", content:$system}, {role:"user", content:$user}],
      options: {temperature: 0.8, num_predict: 450}}')"

  response="$(curl -sS --fail "${host}/api/chat" \
    -H 'Content-Type: application/json' \
    -d "$payload")" || { echo "ERROR: Ollama request failed (is 'ollama serve' running and is '$model' pulled?)" >&2; return 1; }

  jq -er '.message.content' <<<"$response" 2>/dev/null \
    || { echo "ERROR: unexpected Ollama response: $response" >&2; return 1; }
}

thaura_generate() {  # thaura_generate "user prompt" -> stdout: story text
  local user="$1"
  local base="${THAURA_BASE_URL:-https://backend.thaura.ai/v1}"
  local model="${THAURA_MODEL:-thaura}"
  local key="${THAURA_API_KEY:?Set THAURA_API_KEY}"

  local payload response
  payload="$(jq -n \
    --arg model "$model" \
    --arg system "$SYSTEM_PROMPT" \
    --arg user "$user" \
    '{model: $model,
      messages: [{role:"system", content:$system}, {role:"user", content:$user}],
      temperature: 0.8, max_tokens: 450}')"

  response="$(curl -sS --fail "${base}/chat/completions" \
    -H "Authorization: Bearer ${key}" \
    -H 'Content-Type: application/json' \
    -d "$payload")" || { echo "ERROR: Thaura request failed — check THAURA_BASE_URL/THAURA_API_KEY" >&2; return 1; }

  jq -er '.choices[0].message.content' <<<"$response" 2>/dev/null \
    || { echo "ERROR: unexpected Thaura response: $response" >&2; return 1; }
}

generate() {  # generate "user prompt"
  case "${STORY_ENGINE:-ollama}" in
    ollama) ollama_generate "$1" ;;
    thaura) thaura_generate "$1" ;;
    *) echo "Unknown STORY_ENGINE='${STORY_ENGINE:-}' (use ollama|thaura)" >&2; exit 1 ;;
  esac
}

# --- read the combinations straight from stories.json --------------
# Same idea as gen-audio.sh's `.stories[]` loop, but resolving each axis id
# to its human-readable label (needed for the prompt) via the pack's own
# axes definitions.

echo "== Stories (from ${PACK}) — engine: ${STORY_ENGINE:-ollama} =="
total=0 done_count=0 skipped=0 failed=0

jq -r '
  . as $root
  | ($root.axes.HERO      | map({(.id): .label}) | add) as $heroLbl
  | ($root.axes.COMPANION | map({(.id): .label}) | add) as $compLbl
  | ($root.axes.PLACE     | map({(.id): .label}) | add) as $placeLbl
  | ($root.axes.OBJECT    | map({(.id): .label}) | add) as $objLbl
  | $root.stories[]
  | [.title,
     .selection.HERO, .selection.COMPANION, .selection.PLACE, .selection.OBJECT,
     $heroLbl[.selection.HERO], $compLbl[.selection.COMPANION],
     $placeLbl[.selection.PLACE], $objLbl[.selection.OBJECT]]
  | @tsv
' "$PACK" \
| while IFS=$'\t' read -r title hero_id comp_id place_id obj_id hero_lbl comp_lbl place_lbl obj_lbl; do
    total=$((total + 1))
    key="${hero_id}-${comp_id}-${place_id}-${obj_id}"
    out_file="${OUT}/${key}.txt"

    if [[ -f "$out_file" && "${FORCE:-0}" != "1" ]]; then
      skipped=$((skipped + 1))
      echo "  ~ $key — \"$title\" (already exists, skipped)"
      continue
    fi

    user_prompt="Écris l'histoire avec ces ingrédients :
- Héros : ${hero_lbl}
- Compagnon : ${comp_lbl}
- Lieu : ${place_lbl}
- Objet : ${obj_lbl}"

    if text="$(generate "$user_prompt")"; then
      printf '%s\n' "$text" > "$out_file"
      done_count=$((done_count + 1))
      echo "  ✓ $key — \"$title\" ($(wc -w < "$out_file" | tr -d ' ') words)"
    else
      failed=$((failed + 1))
      echo "  ✗ $key — \"$title\" (generation failed, see error above)"
    fi
  done

echo ""
echo "Done. Texts written under: $OUT"
echo "Next: ./scripts/tts/gen-audio.sh $PACK audio   # to synthesize the missing MP3s"