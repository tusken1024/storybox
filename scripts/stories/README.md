# scripts/stories/ — Génération de texte (`gen-stories.sh`)

Génère le **texte** des histoires de Storybox (avant leur
synthèse audio), via un LLM local (Ollama) ou l'API Thaura. 

```bash
./scripts/stories/gen-stories.sh src/main/resources/stories.json stories-src
```

Le script lit `.stories[]` dans `stories.json` — exactement comme
`gen-audio.sh` — résout les labels de chaque axe (`Héros`, `Compagnon`,
`Lieu`, `Objet`), appelle le LLM avec le même prompt que
`StoryGenerator.java`, et écrit un `.txt` par histoire sous `stories-src/`.

**Il ne génère que les combinaisons que tu as explicitement spécifié dans
`stories.json`** — pas toutes les combinaisons possibles des 4 axes. Si tu
veux du texte pour davantage de combinaisons, ajoute-les d'abord à
`stories.json`.

---

## Workflow complet

```
1. Tu ajoutes une entrée dans stories.json
   (selection HERO/COMPANION/PLACE/OBJECT + title + audio path — pas de texte)
                    │
                    ▼
2. ./scripts/story/gen-stories.sh
   → appelle le LLM, écrit stories-src/<key>.txt
                    │
                    ▼
3. (optionnel) tu relis / corriges le .txt à la main
                    │
                    ▼
4. ./scripts/tts/gen-audio.sh
   → synthétise audio/stories/<key>.mp3 à partir du .txt
```

Le `<key>` est identique à celui que produit `StorySelection.key()`
(`chevalier-dragon-chateau-epee`), donc les deux scripts s'enchaînent sans
rien à renommer à la main.

---

## Structure du dossier

```
scripts/stories/
├── README.md
└── gen-stories.sh
```

Les deux backends, Ollama et Thaura, sont de simples appels HTTP.

---

## Choisir le moteur LLM

Sélection via la variable d'env `STORY_ENGINE` (défaut : `ollama`) :

| Moteur     | `STORY_ENGINE=` | Où ça tourne         | Hors ligne ? | Clé API requise |
|------------|-----------------|----------------------|--------------|-----------------|
| **Ollama** | `ollama`        | Laptop ou Pi (local) | Oui          | Non             |
| **Thaura** | `thaura`        | Cloud (Thaura)       | Non          | Oui             |

```bash
STORY_ENGINE=ollama ./scripts/story/gen-stories.sh
STORY_ENGINE=thaura THAURA_API_KEY=sk-xxx ./scripts/story/gen-stories.sh
```

---

## Prérequis communs

```bash
sudo apt update
sudo apt install -y jq curl
```

---

## 1. Ollama (défaut, hors ligne)

Nécessite une instance `ollama serve` accessible et le modèle déjà pullé :

```bash
ollama pull qwen2.5:7b        # modèle de dev par défaut
./scripts/stories/gen-stories.sh
```

Variables d'env :

| Variable       | Défaut                   | Rôle                |
|----------------|--------------------------|---------------------|
| `OLLAMA_HOST`  | `http://localhost:11434` | URL de l'API Ollama |
| `OLLAMA_MODEL` | `qwen2.5:7b`             | Modèle à utiliser   |

Pour valider la qualité et la latence du modèle réellement embarqué sur le
Pi **avant** de déployer, pointe simplement sur ce modèle depuis le laptop :

```bash
OLLAMA_MODEL="LiquidAI/lfm2.5-1.2b-instruct:q4_0" ./scripts/story/gen-stories.sh
```

---

## 2. Thaura (cloud, nécessite une clé API)

```bash
STORY_ENGINE=thaura THAURA_API_KEY=sk-xxx ./scripts/story/gen-stories.sh
```

Variables d'env :

| Variable          | Défaut                         | Rôle                             |
|-------------------|--------------------------------|----------------------------------|
| `THAURA_BASE_URL` | `https://backend.thaura.ai/v1` | Base URL de l'API (style OpenAI) |
| `THAURA_MODEL`    | `thaura`                       | Modèle à utiliser                |
| `THAURA_API_KEY`  | *(aucun — obligatoire)*        | Ta clé API personnelle           |

Casse la propriété « tout hors ligne » du projet le temps de cette seule
étape (génération de texte) — pratique sur un laptop sans GPU ou pour
comparer la qualité contre Ollama, mais l'audio et le device final restent
100% offline une fois les `.txt`/`.mp3` générés.

✅ Testé et validé avec les deux moteurs (Ollama et Thaura).

---

## Options communes aux deux moteurs

| Variable | Défaut | Rôle                                                                   |
|----------|--------|------------------------------------------------------------------------|
| `FORCE`  | `0`    | `FORCE=1` régénère un `.txt` même s'il existe déjà (sinon il est skip) |

```bash
FORCE=1 ./scripts/story/gen-stories.sh   # réécrit tous les textes authored
```

---

## Dépannage rapide

| Symptôme                                            | Cause probable                                               | Fix                                                               |
|-----------------------------------------------------|--------------------------------------------------------------|-------------------------------------------------------------------|
| `ERROR: Ollama request failed`                      | `ollama serve` pas lancé, ou modèle pas pullé                | `ollama serve` (autre terminal) puis `ollama pull <model>`        |
| `ERROR: unexpected Ollama response: ...`            | Réponse JSON inattendue (souvent une erreur Ollama en texte) | Regarde le message brut affiché ; vérifie `OLLAMA_MODEL`          |
| `THAURA_API_KEY: parameter null or not set`         | Variable d'env non exportée                                  | `export THAURA_API_KEY=sk-xxx` ou passe-la inline sur la commande |
| `ERROR: Thaura request failed`                      | Mauvaise base URL, clé invalide, ou pas de réseau            | Vérifie `THAURA_BASE_URL` / `THAURA_API_KEY` et ta connexion      |
| Toutes les entrées sont `(already exists, skipped)` | Comportement normal — cache déjà rempli                      | `FORCE=1` pour régénérer                                          |
| `Story pack not found: ...`                         | Mauvais chemin vers `stories.json`                           | Passe le bon chemin en 1er argument                               |

---

## Récap : quel moteur pour quel besoin

- **Développement quotidien, tout hors ligne** → `ollama` (défaut)
- **Valider le modèle exact du Pi avant déploiement** → `ollama` +
  `OLLAMA_MODEL=LiquidAI/lfm2.5-1.2b-instruct:q4_0`
- **Comparer la qualité contre un modèle cloud plus costaud, ou dépanner sans
  GPU local** → `thaura`