#!/usr/bin/env python3
"""
chatterbox_tts.py — Génération TTS français avec Chatterbox Multilingual

Équivalent fonctionnel de kokoro_tts.py, adapté à Chatterbox.

Différence clé avec Kokoro :
    Kokoro   -> voix nommées pré-entraînées (ex: ff_siwis)
    Chatterbox -> clonage de voix à partir d'un échantillon audio de référence
                  (10-15s minimum). Sans échantillon fourni, une voix par
                  défaut générique est utilisée.

Usage :
    # Voix par défaut (générique, sans clonage)
    python3 chatterbox_tts.py "Il était une fois, dans une forêt lointaine..." sortie.wav

    # Voix clonée (recommandé pour un ton "conte" chaleureux/expressif)
    python3 chatterbox_tts.py "Il était une fois..." sortie.wav --voice reference_voix.wav
"""

import argparse
import re
import sys
from pathlib import Path

import torch
import torchaudio as ta
from chatterbox.mtl_tts import ChatterboxMultilingualTTS

LANGUAGE_ID = "fr"

# Longueur max par segment envoyé au modèle. Chatterbox utilise de
# l'attention "eager" (pas de Flash/SDPA optimisée) -> la mémoire GPU
# explose de façon quadratique avec la longueur du texte. Un paragraphe
# entier peut faire sauter une 8GB VRAM alors qu'un court label passe
# sans souci. On découpe donc systématiquement en segments courts,
# quelle que soit la longueur du texte d'entrée.
MAX_CHARS_PER_CHUNK = 250


def split_into_chunks(text: str, max_chars: int = MAX_CHARS_PER_CHUNK) -> list[str]:
    """Découpe le texte en segments <= max_chars, sur des frontières de phrases.

    Ne coupe jamais au milieu d'une phrase si possible ; si une phrase
    seule dépasse max_chars, elle est coupée sur les virgules/espaces
    en dernier recours.
    """
    sentences = re.split(r"(?<=[.!?])\s+", text.strip())
    chunks: list[str] = []
    current = ""

    for sentence in sentences:
        candidate = f"{current} {sentence}".strip() if current else sentence
        if len(candidate) <= max_chars:
            current = candidate
            continue

        if current:
            chunks.append(current)
            current = ""

        if len(sentence) <= max_chars:
            current = sentence
        else:
            # Phrase trop longue à elle seule : découpe sur les virgules.
            parts = re.split(r"(?<=,)\s+", sentence)
            sub = ""
            for part in parts:
                sub_candidate = f"{sub} {part}".strip() if sub else part
                if len(sub_candidate) <= max_chars:
                    sub = sub_candidate
                else:
                    if sub:
                        chunks.append(sub)
                    sub = part
            if sub:
                current = sub

    if current:
        chunks.append(current)

    return chunks


def load_model(device: str = "cpu") -> ChatterboxMultilingualTTS:
    """Charge le modèle Chatterbox Multilingual.

    Note perf : sur laptop CPU-only, le chargement + génération sont
    nettement plus lents que Kokoro. Prévoir cette étape pour la
    pré-génération (comme Kokoro), pas pour du live sur le Pi 4.
    """
    print(f"[chatterbox] Chargement du modèle sur '{device}'...", file=sys.stderr)
    model = ChatterboxMultilingualTTS.from_pretrained(device=device)
    print("[chatterbox] Modèle chargé.", file=sys.stderr)
    return model


def generate_audio(
        model: ChatterboxMultilingualTTS,
        text: str,
        output_path: str,
        voice_reference: str | None = None,
        exaggeration: float = 0.5,
        max_chars_per_chunk: int = MAX_CHARS_PER_CHUNK,
        silence_ms: int = 300,
) -> None:
    """Génère un fichier audio à partir du texte, en découpant en segments.

    Le texte est toujours découpé en segments courts avant génération (même
    un texte court ne fait qu'un seul segment) pour éviter les OOM GPU sur
    les longs paragraphes. Chaque segment est généré séparément, le cache
    CUDA est vidé entre chaque appel, puis les segments sont concaténés
    avec un court silence entre eux.

    Args:
        text: Texte français à synthétiser.
        output_path: Chemin du .wav de sortie.
        voice_reference: Chemin vers un échantillon audio (10-15s+) pour
            cloner une voix spécifique. None = voix par défaut.
        exaggeration: Intensité expressive (0.0 = neutre/plat,
            1.0 = très expressif). Pour un conte, 0.6-0.8 donne un ton
            plus vivant et théâtral qu'un 0.5 par défaut.
        max_chars_per_chunk: Taille max d'un segment envoyé au modèle.
            Réduire si l'OOM persiste (ex: 150), augmenter si ta VRAM
            le permet et que tu veux moins de segments (plus rapide).
        silence_ms: Silence inséré entre les segments concaténés, pour
            un rendu naturel façon pause de narration.
    """
    chunks = split_into_chunks(text, max_chars=max_chars_per_chunk)
    print(f"[chatterbox] Texte découpé en {len(chunks)} segment(s).", file=sys.stderr)

    ref_path = None
    if voice_reference:
        ref_path = Path(voice_reference)
        if not ref_path.exists():
            raise FileNotFoundError(
                f"Fichier de référence vocale introuvable : {voice_reference}"
            )
        print(f"[chatterbox] Clonage voix depuis : {voice_reference}", file=sys.stderr)
    else:
        print("[chatterbox] Voix par défaut (pas de référence fournie).", file=sys.stderr)

    waveforms = []
    sample_rate = model.sr
    silence = torch.zeros((1, int(sample_rate * silence_ms / 1000)))

    for i, chunk in enumerate(chunks, start=1):
        print(f"[chatterbox]   segment {i}/{len(chunks)} ({len(chunk)} car.)...", file=sys.stderr)
        kwargs = {
            "text": chunk,
            "language_id": LANGUAGE_ID,
            "exaggeration": exaggeration,
        }
        if ref_path:
            kwargs["audio_prompt_path"] = str(ref_path)

        try:
            wav = model.generate(**kwargs)
        except torch.cuda.OutOfMemoryError:
            print(
                f"[chatterbox] OOM sur le segment {i} malgré le découpage "
                f"({len(chunk)} caractères). Réessaie avec "
                f"--max-chars plus petit (ex: 150).",
                file=sys.stderr,
            )
            raise

        waveforms.append(wav)
        if i < len(chunks):
            waveforms.append(silence)

        # Libère la mémoire GPU entre chaque segment : chaque génération
        # laisse des tenseurs intermédiaires en cache CUDA tant que le
        # process reste vivant, ce qui accumule sur les textes longs.
        if torch.cuda.is_available():
            torch.cuda.empty_cache()

    full_wav = torch.cat(waveforms, dim=1)
    ta.save(output_path, full_wav, sample_rate)
    print(f"[chatterbox] Audio généré : {output_path}", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description="Génération TTS français via Chatterbox")
    parser.add_argument("text", help="Texte français à synthétiser")
    parser.add_argument("output", help="Chemin du fichier .wav de sortie")
    parser.add_argument(
        "--voice",
        default=None,
        help="Chemin vers un échantillon audio de référence (10-15s+) pour cloner une voix",
    )
    parser.add_argument(
        "--exaggeration",
        type=float,
        default=0.7,
        help="Intensité expressive 0.0-1.0 (défaut: 0.7, adapté au ton conte)",
    )
    parser.add_argument(
        "--device",
        default="cpu",
        help="Device torch: 'cpu' ou 'cuda' si GPU disponible (défaut: cpu)",
    )
    parser.add_argument(
        "--max-chars",
        type=int,
        default=MAX_CHARS_PER_CHUNK,
        help=(
            "Taille max (caractères) par segment envoyé au modèle "
            f"(défaut: {MAX_CHARS_PER_CHUNK}). Réduis si tu as encore un "
            "OutOfMemoryError CUDA."
        ),
    )
    args = parser.parse_args()

    model = load_model(device=args.device)
    generate_audio(
        model=model,
        text=args.text,
        output_path=args.output,
        voice_reference=args.voice,
        exaggeration=args.exaggeration,
        max_chars_per_chunk=args.max_chars,
    )


if __name__ == "__main__":
    main()