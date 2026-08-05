---
name: benchmark-free-models
description: >-
  Benchmark FitBuddy free AI text models (Ollama Cloud, Gemini Flash, OpenRouter
  :free), manually score outputs, and propose updates to
  config/failover_ladders.json. Use when free model catalogs change, user asks to
  re-benchmark, refresh AI ladders, or update config/failover_ladders.json.
disable-model-invocation: true
---

# Benchmark free models (FitBuddy)

Re-run when Ollama / Gemini / OpenRouter **free** catalogs change. Do **not** restore
catalog “intelligence” ranking or an in-app developer benchmark UI — Auto failover
loads order from [`config/failover_ladders.json`](../../config/failover_ladders.json)
via [`FailoverLadders`](../../app/src/main/java/com/anant/fitbuddy/data/settings/FailoverLadders.kt).

## Prerequisites

- API keys in repo-root `./apis` (gitignored): `openrouter=…`, `ollama=…`, `gemini=…`
- Collector: `tools/collect_model_outputs.py`
- Local notes/dumps only (gitignored): `tools/benchmark/` · `tools/benchmark_results/`
  — **do not commit** review markdown or raw model outputs

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

Pick a **champion** per provider. Keep review notes under gitignored `tools/benchmark/` if useful (never commit them).

### 3. Present for user approval

Show champions + proposed ladders. **Wait for approval** before editing app code.

After approval — update **`config/failover_ladders.json`** (not Kotlin ranking code):

1. Edit `text` / `photo` arrays per provider (`OPENROUTER`, `GEMINI`, `OLLAMA`, `OPENAI`).
2. Bump `updated` (and `source` if useful). Defaults (dropdown seeds) are ladder heads — no separate AppSettings constants to sync.
3. Keep failover logic in `FailoverLadders.kt`: **selected model first**, then ladder ∩ catalog, then leftovers A–Z.
4. Do **not** reintroduce catalog intelligence ranking or an in-app Settings benchmark UI.
5. Run `FailoverLaddersTest`; update `AGENTS.md` if the story drifts.

## Output template

```
Champions: Ollama=… · Gemini=… · OpenRouter=…
Accuracy ladders: …
Speed ladders: …
Suggested Auto (balanced): …
Approve to update config/failover_ladders.json?
```

## Constraints

- Never commit `./apis`, `tools/benchmark/`, or `tools/benchmark_results/`.
- Never `adb uninstall` the release app; device tests use debug only.
- Photo ladders may follow multimodal cousins of text champions when vision was not re-benchmarked — say so when proposing.
