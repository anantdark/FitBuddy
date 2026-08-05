# FitBuddy free-model catalog — combined review

**Status:** ladders applied via `config/failover_ladders.json` — 2026-08-05  
**Date:** 2026-08-05  
**Scoring:** manual review of stored API outputs — not the app’s static benchmark scorer.

| Platform | Review | Raw JSON (gitignored) |
|----------|--------|----------|
| Ollama Cloud | [`OLLAMA_REVIEW.md`](./OLLAMA_REVIEW.md) | `../benchmark_results/ollama_raw_outputs.json` |
| Gemini Flash free | [`GEMINI_REVIEW.md`](./GEMINI_REVIEW.md) | `../benchmark_results/gemini_raw_outputs.json` |
| OpenRouter `:free` | [`OPENROUTER_REVIEW.md`](./OPENROUTER_REVIEW.md) | `../benchmark_results/openrouter_raw_outputs.json` |

Same 6 probes everywhere (egg, rice, banana, roti+dal, biryani, dosa). Champion per platform = best overall Indian-plate realism + staple accuracy.

---

## Champions (accuracy)

| Platform | Champion | Acc (manual) | Speed (6 probes) |
|----------|----------|--------------|------------------|
| **Ollama** | `minimax-m3` | 100% | 41.6 s |
| **Gemini** | `gemini-3.5-flash-lite` | 97% | 9.2 s |
| **OpenRouter** | `inclusionai/ling-3.0-flash:free` | 97% | 31.0 s |

---

## Per-platform ladders (proposed)

### Ollama — accuracy

1. `minimax-m3`  
2. `nemotron-3-ultra`  
3. `nemotron-3-nano:30b`  
4. `gemma4:31b`  
5. `gpt-oss:120b`  
6. `nemotron-3-super`  
7. `gpt-oss:20b`

### Ollama — speed

1. `gemma4:31b` (26 s, 87%)  
2. `gpt-oss:20b` (30 s, 77%)  
3. `minimax-m3` (42 s, 100%)  
4. `gpt-oss:120b` → `nemotron-3-nano:30b` → `nemotron-3-super` → `nemotron-3-ultra`

### Gemini — accuracy *(collapse aliases in app)*

1. `gemini-3.5-flash-lite`  
2. `gemini-flash-lite-latest` *(≈ same as #1)*  
3. `gemini-3.1-flash-lite`  
4. `gemini-3.1-flash-lite-preview` *(≈ same as #3)*  
5. `gemini-3-flash-preview`  
6. `gemini-3.6-flash`  
7. `gemini-flash-latest`  
8. `gemini-2.5-flash`

### Gemini — speed

1. `gemini-flash-lite-latest` / `gemini-3.5-flash-lite` (~9 s)  
2. `gemini-3.1-flash-lite` (~10 s)  
3. `gemini-3.6-flash` (34 s)  
4. heavier Flash variants (46–62 s)

### OpenRouter — accuracy

1. `inclusionai/ling-3.0-flash:free`  
2. `nvidia/nemotron-3-ultra-550b-a55b:free`  
3. `google/gemma-4-26b-a4b-it:free`  
4. `poolside/laguna-xs-2.1:free`  
5. `nvidia/nemotron-3-super-120b-a12b:free`  
6. `nvidia/nemotron-3-nano-30b-a3b:free`  
7. `openai/gpt-oss-20b:free`  
8–9. omni-reasoning / nano-9b *(recommend drop from Auto)*

### OpenRouter — speed

1. `ling-3.0-flash` (31 s)  
2. `gemma-4-26b-a4b-it` (63 s)  
3. `nemotron-3-super` (67 s)  
4. `nemotron-3-nano-30b` (70 s)  
5. `laguna-xs` (82 s)  
6. `nemotron-3-ultra` (149 s) …

---

## Suggested default Auto ladders (if you approve without edits)

**Text (accuracy-leaning):**

**Ollama:** `minimax-m3` → `gemma4:31b` → `nemotron-3-nano:30b` → `gpt-oss:120b`  
**Gemini:** `gemini-3.5-flash-lite` → `gemini-3.1-flash-lite` → `gemini-3.6-flash` → `gemini-3-flash-preview`  
**OpenRouter:** `inclusionai/ling-3.0-flash:free` → `google/gemma-4-26b-a4b-it:free` → `nvidia/nemotron-3-super-120b-a12b:free` → `poolside/laguna-xs-2.1:free`

**Photo / vision (shipped in `config/failover_ladders.json`):**

| Platform | Order (default first) |
|----------|------------------------|
| **OpenRouter** | `google/gemma-4-26b-a4b-it:free` → `ling-3.0-flash` → `nemotron-3-super` → `nemotron-3-nano` → `nemotron-3-ultra` |
| **Gemini** | `gemini-flash-latest` → `3.6-flash` → `3.5-flash` → `3-flash-preview` → `2.5-flash` → then Flash-Lite aliases |
| **Ollama** | `gemma4:31b` → `minimax-m3` → `nemotron-3-nano:30b` → `gpt-oss:120b` → `nemotron-3-super` → `nemotron-3-ultra` |
| **OpenAI** | `gpt-4o` → `gpt-4o-mini` → `gpt-4.1` → `gpt-4.1-mini` |

Photo defaults: Gemma on OpenRouter/Ollama; `gemini-flash-latest` on Gemini (full Flash before Lite).

User-facing Settings toggle later: order preferred models **by accuracy** or **by speed** from the approved lists.

---

## Gaps / caveats

| Issue | Impact |
|-------|--------|
| Ollama: 11/18 catalog models need paid subscription | Free ladder is 7 models only |
| Gemini: `2.0-*` 429 quota; `2.5-flash-lite` retired; `3.5-flash` missing banana (503) | Ranked only complete free Flash |
| OpenRouter: `gemma-4-31b-it:free` fully 429 | Not ranked; retry before including |
| OpenRouter: cohere / laguna-s / VL skipped | Too slow or unstable for meal logging |
| Latencies are wall-clock from this machine/API path | Relative order matters more than absolute ms |

---

## Approval checklist

Reply with edits or “approved” on:

- [ ] Ollama accuracy + speed orders  
- [ ] Gemini accuracy + speed orders (and whether to collapse `*-latest` / `*-preview` aliases)  
- [ ] OpenRouter accuracy + speed orders (and which weak models to drop)  
- [ ] Default Auto mode per platform: **accuracy** / **speed** / **balanced** (suggested balanced ladders above)

After approval: persist rankings in the app + Settings UI to order failover by speed or accuracy.
