---
name: benchmark-free-models
description: >-
  Benchmark FitBuddy free AI text models (Ollama Cloud, Gemini Flash, OpenRouter
  :free), manually score outputs, update review docs, and propose FailoverLadders
  defaults. Use when free model catalogs change, user asks to re-benchmark, refresh
  AI ladders, or update FailoverLadders.kt / tools/benchmark/*.md.
disable-model-invocation: true
---

# Benchmark free models (FitBuddy)

Re-run when Ollama / Gemini / OpenRouter **free** catalogs change. Do **not** restore
catalog “intelligence” ranking or an in-app developer benchmark UI — Auto failover
keeps using hardcoded
[`FailoverLadders`](../../app/src/main/java/com/anant/fitbuddy/data/settings/FailoverLadders.kt).

## Prerequisites

- API keys in repo-root `./apis` (gitignored): `openrouter=…`, `ollama=…`, `gemini=…`
- Collector: `tools/collect_model_outputs.py`
- Committed reviews: `tools/benchmark/*_REVIEW.md` / `CATALOG_REVIEW.md`
- Raw dumps (gitignored): `tools/benchmark_results/`

## Workflow

### 1. Collect raw outputs (do not auto-score as truth)

```bash
python3 tools/collect_model_outputs.py --provider ollama --cooldown 0.5
python3 tools/collect_model_outputs.py --provider gemini --cooldown 1.0
python3 tools/collect_model_outputs.py --provider openrouter --cooldown 2.0
```

Writes `tools/benchmark_results/{provider}_raw_outputs.json`. Checkpoint after each model.

**Filters (match the app):**

| Provider | Free set |
|----------|----------|
| Ollama Cloud | Probe chat; skip HTTP 403 “requires a subscription” |
| Gemini | `isFreeTier`: id contains `flash`, not pro/ultra/image |
| OpenRouter | id ends with `:free`; exclude safety/guard/embed/media |

Same **6 probes** as the collector (egg, rice, banana, roti+dal, biryani, dosa). Prompt = Indian-diet JSON nutrition estimate, `temperature=0`.

### 2. You score manually

Open each raw JSON. Score probes yourself (0–10) against USDA/IFCT ballparks — **do not** treat any static point bands as ranking truth.

Pick a **champion** per provider. Update committed reviews under `tools/benchmark/`:

- `OLLAMA_REVIEW.md` / `GEMINI_REVIEW.md` / `OPENROUTER_REVIEW.md`
- `CATALOG_REVIEW.md` (combined accuracy + speed + suggested Auto ladders)

### 3. Present for user approval

Show champions + proposed ladders. **Wait for approval** before editing app code.

### 4. After approval — update the app

1. Edit `FailoverLadders.kt` `TEXT` / `PHOTO` lists.
2. Align `AppSettings.DEFAULT_*_MODEL` / `DEFAULT_*_TEXT_MODEL` with ladder heads.
3. Keep failover: **selected model first**, then ladder ∩ live catalog, then leftover catalog A–Z.
4. Do **not** reintroduce catalog intelligence ranking or Settings “Run text-model benchmark”.
5. Update `FailoverLaddersTest.kt` and `AGENTS.md` if defaults drift.

## Output template

```
Champions: Ollama=… · Gemini=… · OpenRouter=…
Accuracy ladders: …
Speed ladders: …
Suggested Auto (balanced): …
Approve to update FailoverLadders.kt?
```

## Constraints

- Never commit `./apis` or raw dumps with secrets.
- Never `adb uninstall` the release app; device tests use debug only.
- Photo ladders may follow multimodal cousins of text champions when vision was not re-benchmarked — say so in the review.
