#!/usr/bin/env python3
"""
kokoro_tts.py — synthétise une phrase en WAV avec Kokoro TTS (français).

Usage:
    python3 kokoro_tts.py "Le chevalier" out.wav [voix]

Voix par défaut : ff_siwis (français). Sortie : WAV mono 24 kHz.

Device : auto-détecté par KPipeline (cuda si torch.cuda.is_available(),
sinon cpu). Rien à configurer — installe torch en version CUDA dans
pyproject.toml pour que ça bascule automatiquement sur GPU.
"""
import sys

import numpy as np
import soundfile as sf
import torch
from kokoro import KPipeline


def main() -> None:
    if len(sys.argv) < 3:
        sys.exit("usage: kokoro_tts.py <texte> <out.wav> [voix]")
    text, out = sys.argv[1], sys.argv[2]
    voice = sys.argv[3] if len(sys.argv) > 3 else "ff_siwis"

    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"[kokoro] Device : {device}", file=sys.stderr)

    pipeline = KPipeline(lang_code="f")
    chunks = [audio for _, _, audio in pipeline(text, voice=voice)]
    if not chunks:
        sys.exit(f"Kokoro n'a produit aucun audio pour : {text!r}")

    sf.write(out, np.concatenate(chunks), 24000)


if __name__ == "__main__":
    main()
