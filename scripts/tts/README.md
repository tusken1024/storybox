# scripts/tts/ — Génération audio (`gen-audio.sh`)

Génère tous les **assets** audio de Storybox (cues des axes + histoires statiques)
à partir de `stories.json`, en local, hors ligne. Pas de cloud, pas d'API.

```bash
./scripts/tts/gen-audio.sh src/main/resources/stories.json audio
```

Le script lit `stories.json`, génère un MP3 par cue (le label parlé de chaque
choix) et un MP3 par histoire (texte lu depuis `stories-src/<nom>.txt`), puis
les écrit sous `audio/cues/` et `audio/stories/`.

---

## Structure du dossier

```
scripts/tts/
├── README.md
├── gen-audio.sh
├── kokoro/
│   ├── pyproject.toml     # dépendances verrouillées (kokoro, torch, soundfile)
│   ├── kokoro_tts.py
│   └── uv.lock            # généré au premier `uv run` — à committer
└── chatterbox/
    ├── pyproject.toml     # dépendances verrouillées (chatterbox-tts, torch+cu124)
    ├── chatterbox_tts.py
    └── uv.lock            # généré au premier `uv run` — à committer
```

Kokoro et Chatterbox sont chacun un **projet Python isolé** (son propre
`pyproject.toml`, son propre `.venv` géré par `uv`) — l'équivalent de deux
modules Maven séparés, chacun avec son `pom.xml`. Pas de venv à activer à la
main : `uv run --project <dossier>` s'en charge, comme `./mvnw` résout le
classpath sans qu'on y pense.

---

## Choisir le moteur TTS

Quatre moteurs disponibles, sélectionnés via la variable d'env `TTS_ENGINE`
(défaut : `kokoro`) :

| Moteur         | `TTS_ENGINE=` | Usage prévu                         | Qualité voix                           |
|----------------|---------------|-------------------------------------|----------------------------------------|
| **Kokoro**     | `kokoro`      | Pré-génération laptop (par défaut)  | Bonne, voix FR native (`ff_siwis`)     |
| **Chatterbox** | `chatterbox`  | Pré-génération laptop, GPU requis   | La meilleure, clonage de voix possible |
| **Piper**      | `piper`       | Génération live sur le Pi 4 (léger) | Correcte, un cran sous Kokoro          |
| **espeak-ng**  | `espeak`      | Validation de la chaîne uniquement  | Robotique — jamais pour le rendu final |

```bash
TTS_ENGINE=chatterbox ./scripts/tts/gen-audio.sh src/main/resources/stories.json audio
TTS_ENGINE=piper      ./scripts/tts/gen-audio.sh src/main/resources/stories.json audio
```

---

## Prérequis communs (tous moteurs)

```bash
sudo apt update
sudo apt install -y jq ffmpeg
```

Pour Kokoro et Chatterbox, il faut aussi `uv` (gestionnaire de projet Python,
l'équivalent Maven — gère venv + dépendances + lockfile) :
```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
```

---

## Installation par moteur, sur une machine neuve

### 1. Kokoro (défaut)

```bash
cd scripts/tts/kokoro
uv run python kokoro_tts.py "Bonjour" test.wav   # installe tout automatiquement au premier appel
```

C'est tout — `uv` lit `pyproject.toml`, crée un `.venv` local à ce dossier,
installe les versions verrouillées (`kokoro==0.9.4`, `torch==2.6.0`,
`soundfile==0.13.1`), et génère `uv.lock`. Premier run Kokoro : télécharge
aussi ~350 Mo de poids du modèle, puis fonctionne hors ligne.

**GPU :** `torch` est installé en version CUDA (`cu124`) via l'index dédié
dans `pyproject.toml`. `KPipeline` (dans `kokoro_tts.py`) détecte le GPU
automatiquement — rien à configurer. Le script logue le device utilisé :
`[kokoro] Device : cuda`. Si ta version CUDA diffère de `cu124`
(vérifiable avec `nvidia-smi` puis `python3 -c "import torch; print(torch.version.cuda)"`
une fois installé), adapte l'URL de l'index dans `pyproject.toml`.

Utilisation via `gen-audio.sh` (pas besoin d'appeler `kokoro_tts.py` à la main) :
```bash
TTS_ENGINE=kokoro ./scripts/tts/gen-audio.sh src/main/resources/stories.json audio
```

Voix par défaut : `KOKORO_VOICE=ff_siwis` (seule voix FR du modèle).

---

### 2. Chatterbox

Même principe, projet isolé dans `scripts/tts/chatterbox/` :
```bash
cd scripts/tts/chatterbox
uv run python chatterbox_tts.py "Bonjour" test.wav --device cuda
```

Versions verrouillées : `chatterbox-tts==0.1.7`, `torch==2.6.0`,
`torchaudio==2.6.0` (CUDA `cu124`), `setuptools==80.10.2` (pin volontaire —
voir Dépannage).

**Variables d'env disponibles (via `gen-audio.sh`) :**
- `CHATTERBOX_DEVICE=cuda|cpu` (défaut `cuda`)
- `CHATTERBOX_EXAG=0.7` — intensité expressive 0.0-1.0 (défaut `0.7`, adapté au conte)
- `CHATTERBOX_VOICE=/chemin/reference.wav` — clonage de voix à partir d'un
  échantillon audio de 10-15s+ (optionnel ; sans ça, voix par défaut générique)

```bash
TTS_ENGINE=chatterbox ./scripts/tts/gen-audio.sh src/main/resources/stories.json audio
```

**Voix de référence libre de droits :** un extrait LibriVox (domaine public,
FR, ton conte) fonctionne très bien. Isoler 15-20s propres avec `sox` :
```bash
sox source_librivox.mp3 reference_voix.wav trim 30 20
```

**Note perf/mémoire :** le script découpe automatiquement les textes longs en
segments (`~250` caractères) avant génération, pour éviter les
`OutOfMemoryError` CUDA sur les GPU à VRAM limitée (8GB type RTX 2080).
Ajustable via `chatterbox_tts.py --max-chars` si l'OOM persiste (essayer `150`).

**Répétitions occasionnelles de mots :** Chatterbox a un garde-fou interne
anti-répétition, mais il agit *après coup* — le mot dupliqué peut donc
apparaître dans l'audio malgré le warning `forcing EOS token` dans les logs.
Plus fréquent sur les textes très courts (les cues d'un mot). C'est un
comportement probabiliste du modèle, pas un bug du script : en cas de
répétition sur un fichier précis, le plus simple est de le régénérer
(le sampling change à chaque appel).

---

### 3. Piper

Pas de Python, pas de venv — binaire précompilé + modèle `.onnx`. C'est
aussi la méthode qui sera utilisée en prod sur le Pi 4 pour la génération live.

```bash
# Binaire
mkdir -p ~/tools/piper && cd ~/tools/piper
wget https://github.com/rhasspy/piper/releases/latest/download/piper_linux_x86_64.tar.gz
tar -xzf piper_linux_x86_64.tar.gz
chmod +x piper   # requis après extraction

# Modèle de voix FR (Siwis — même timbre que Kokoro ff_siwis)
mkdir -p ~/piper-voices && cd ~/piper-voices
wget https://huggingface.co/rhasspy/piper-voices/resolve/main/fr/fr_FR/siwis/high/fr_FR-siwis-high.onnx
wget https://huggingface.co/rhasspy/piper-voices/resolve/main/fr/fr_FR/siwis/high/fr_FR-siwis-high.onnx.json

# Phonémiseur requis par Piper (souvent déjà embarqué dans le tarball,
# mais si le binaire réclame la lib système au démarrage) :
sudo apt install -y espeak-ng
```

**Ajoute au `~/.zshrc`** pour ne pas répéter à chaque session :
```bash
echo 'export PATH="$HOME/tools/piper:$PATH"' >> ~/.zshrc
echo 'export PIPER_MODEL="$HOME/piper-voices/fr_FR-siwis-high.onnx"' >> ~/.zshrc
source ~/.zshrc
```

```bash
TTS_ENGINE=piper ./scripts/tts/gen-audio.sh src/main/resources/stories.json audio
```

Utilise `medium` au lieu de `high` si tu veux tester la version plus légère
(qualité un cran en dessous, taille de fichier plus petite — pertinent pour
un vrai déploiement embarqué sur le Pi, moins pour le laptop).

---

### 4. espeak-ng

Fallback zéro-setup, uniquement pour valider que la chaîne technique
(texte → audio → mp3) fonctionne. Voix robotique par nature (synthèse par
formants, pas de réseau neuronal) — **jamais pour le rendu final**.

```bash
sudo apt install -y espeak-ng
```

```bash
TTS_ENGINE=espeak ./scripts/tts/gen-audio.sh src/main/resources/stories.json audio
```

---

## Récap : quel moteur pour quel besoin

- **Écrire/valider `stories.json` et le pipeline** → `espeak` (rapide, zéro dépendance)
- **Générer les assets audio finaux (V1.0)** → `kokoro` ou `chatterbox` (laptop)
- **Génération audio en live sur le Pi 4 (à terme)** → `piper` (seul assez léger)

## Versions verrouillées

Les `pyproject.toml` de `kokoro/` et `chatterbox/` fixent des versions
exactes (pas de plages, pas de `LATEST`) — équivalent d'un `<version>`
explicite dans un `pom.xml`. Une fois `uv run` lancé une première fois,
`uv.lock` fige aussi les dépendances transitives : **committe ce fichier
dans git**, il garantit qu'une réinstallation sur une autre machine retombe
sur exactement les mêmes versions, pas les dernières du moment.

Pour mettre à jour une version en connaissance de cause (jamais en silence) :
```bash
cd scripts/tts/kokoro   # ou chatterbox
# éditer la version dans pyproject.toml, puis :
uv lock --upgrade-package kokoro
uv run python kokoro_tts.py "test" test.wav   # vérifier que ça fonctionne toujours
```

## Dépannage rapide

| Symptôme                                                            | Cause probable                                                                     | Fix                                                                                                                                                 |
|---------------------------------------------------------------------|------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `numpy` échoue à la compilation (`meson`/`cc` introuvable)          | Pas de compilateur système, ou Python trop récent (3.13+)                          | `sudo apt install build-essential` ; `uv` gère la version Python via `requires-python` dans `pyproject.toml`, pas besoin de deadsnakes manuellement |
| `ImportError: cannot import name 'ChatterboxMultilingualTTS'`       | Mauvais chemin d'import                                                            | `from chatterbox.mtl_tts import ...` (pas `chatterbox.tts`) — déjà corrigé dans le script                                                           |
| `ModuleNotFoundError: No module named 'pkg_resources'` (Chatterbox) | `setuptools` ≥ 81 a supprimé `pkg_resources`, requis par Perth (watermarking)      | Déjà pinné à `setuptools==80.10.2` dans `pyproject.toml`                                                                                            |
| `torch.OutOfMemoryError: CUDA out of memory` (Chatterbox)           | Texte trop long envoyé en un seul bloc                                             | Déjà géré par le découpage auto ; sinon `--max-chars 150`                                                                                           |
| `🚨 Detected 2x repetition of token` + mot dupliqué dans l'audio    | Garde-fou anti-répétition de Chatterbox agit après coup, surtout sur textes courts | Comportement probabiliste normal ; régénérer le fichier suffit en général                                                                           |
| `zsh: permission denied: piper`                                     | Bit exécutable manquant après extraction                                           | `chmod +x ~/tools/piper/piper`                                                                                                                      |
| `Set PIPER_MODEL=/path/to/voice.onnx`                               | Variable d'env non définie                                                         | Voir section Piper ci-dessus                                                                                                                        |
| `uv: command not found`                                             | `uv` non installé                                                                  | `curl -LsSf https://astral.sh/uv/install.sh \| sh`                                                                                                  |