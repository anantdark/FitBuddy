# FitBuddy model benchmark review — Gemini (free Flash tier)

**Status:** awaiting your approval  
**Date:** 2026-08-05  
**Raw dump:** `tools/benchmark_results/gemini_raw_outputs.json`  
**How scored:** manually from stored macros (not app static bands).  
**Filter:** matches app `GeminiModelDto.isFreeTier` (id contains `flash`, excludes pro/ultra/image).

---

## Scope

| Item | Value |
|------|--------|
| Provider | Google Gemini OpenAI-compat chat |
| Free catalog attempted | 12 Flash / Flash-Lite ids |
| Usable (6/6 probes) | 8 models |
| Partial | `gemini-3.5-flash` (5/6 — banana 503) |
| No data | `gemini-2.0-flash`, `gemini-2.0-flash-lite` (429 quota), `gemini-2.5-flash-lite` (404 retired) |

---

## Stored outputs (parsed macros)

Format: `kcal / P / C / F`

### gemini-3.5-flash-lite — **9.2 s** total

| Probe | Macros | Note |
|-------|--------|------|
| egg | 78/6/1/5 | Exact |
| rice | 130/3/28/0 | Exact |
| banana | 105/1/27/0 | Exact |
| roti_dal | 370/13/60/9 | Slightly light protein; good overall |
| biryani | 680/32/75/26 | Strong |
| dosa | 310/6/48/11 | Strong |

### gemini-flash-lite-latest — **9.2 s** (alias; nearly same as 3.5-lite)

| Probe | Macros | Note |
|-------|--------|------|
| egg | 72/6/0/5 | Tiny kcal miss |
| rice | 130/3/28/0 | Exact |
| banana | 105/1/27/0 | Exact |
| roti_dal | 360/14/54/11 | Good |
| biryani | 680/32/75/26 | Strong |
| dosa | 310/6/48/11 | Strong |

### gemini-3.1-flash-lite — **9.7 s**

| Probe | Macros | Note |
|-------|--------|------|
| egg/rice/banana | exact | |
| roti_dal | 365/14/58/9 | Good |
| biryani | 680/32/72/28 | Strong (fat a touch rich) |
| dosa | 380/8/52/16 | A bit high kcal/fat |

### gemini-3.1-flash-lite-preview — **10.6 s**

Identical macros to `gemini-3.1-flash-lite` (treat as same model for ranking).

### gemini-3.6-flash — **33.5 s**

| Probe | Macros | Note |
|-------|--------|------|
| staples | exact | |
| roti_dal | 320/13/53/6 | **Light** on kcal/fat for 2 roti+dal |
| biryani | 780/36/92/30 | **High** cal/carbs/protein |
| dosa | 360/7/53/13 | Good |

### gemini-3-flash-preview — **46.6 s**

| Probe | Macros | Note |
|-------|--------|------|
| staples | exact | |
| roti_dal | 470/16/68/15 | Upper end but plausible |
| biryani | 760/32/88/31 | High cal/carbs |
| dosa | 385/8/52/16 | Slightly high |

### gemini-2.5-flash — **50.7 s**

| Probe | Macros | Note |
|-------|--------|------|
| staples | exact | |
| roti_dal | 357/13/57/9 | A bit light |
| biryani | 790/33/78/38 | **Fat/cal high** |
| dosa | 470/9/58/26 | **Too high** |

### gemini-flash-latest — **62.2 s**

| Probe | Macros | Note |
|-------|--------|------|
| staples | exact | |
| roti_dal | 330/12/52/8 | Light |
| biryani | 786/40/80/34 | High protein/fat |
| dosa | 400/7/51/19 | High |

### gemini-3.5-flash — **~48 s** (incomplete)

Missing banana (503). Other probes: roti 315 (light), biryani 802 (high), dosa 355 (ok). Ranked below complete models.

---

## Manual scores (/10 per probe → /60)

Champion for Gemini free: **`gemini-3.5-flash-lite`** — best balance on Indian plates + exact staples + fastest among strong answers.

| Model | egg | rice | ban | roti | biryani | dosa | **Tot** | **%** |
|-------|-----|------|-----|------|---------|------|---------|-------|
| **gemini-3.5-flash-lite** | 10 | 10 | 10 | 9 | 9 | 10 | **58** | **97%** |
| gemini-flash-lite-latest | 9 | 10 | 10 | 9 | 9 | 10 | **57** | **95%** |
| gemini-3.1-flash-lite | 10 | 10 | 10 | 9 | 8 | 8 | **55** | **92%** |
| gemini-3.1-flash-lite-preview | 10 | 10 | 10 | 9 | 8 | 8 | **55** | **92%** |
| gemini-3-flash-preview | 10 | 10 | 10 | 8 | 7 | 8 | **53** | **88%** |
| gemini-3.6-flash | 10 | 10 | 10 | 6 | 6 | 9 | **51** | **85%** |
| gemini-flash-latest | 10 | 10 | 10 | 7 | 6 | 7 | **50** | **83%** |
| gemini-2.5-flash | 10 | 10 | 10 | 8 | 5 | 5 | **48** | **80%** |
| gemini-3.5-flash (partial) | 9 | 10 | 0* | 6 | 5 | 8 | **38** | **63%** |

\*banana failed (503) → 0 for that probe.

---

## Proposed rankings — Gemini free

### Accuracy

1. `gemini-3.5-flash-lite`  
2. `gemini-flash-lite-latest` (alias of lite — optional collapse with #1)  
3. `gemini-3.1-flash-lite`  
4. `gemini-3.1-flash-lite-preview` (duplicate of #3 — optional collapse)  
5. `gemini-3-flash-preview`  
6. `gemini-3.6-flash`  
7. `gemini-flash-latest`  
8. `gemini-2.5-flash`  
9. `gemini-3.5-flash` (partial — retry later)

### Speed

1. `gemini-flash-lite-latest` — 9.2 s  
2. `gemini-3.5-flash-lite` — 9.2 s  
3. `gemini-3.1-flash-lite` — 9.7 s  
4. `gemini-3.1-flash-lite-preview` — 10.6 s  
5. `gemini-3.6-flash` — 33.5 s  
6. `gemini-3-flash-preview` — 46.6 s  
7. `gemini-2.5-flash` — 50.7 s  
8. `gemini-flash-latest` — 62.2 s  

**Suggested default ladder:** `gemini-3.5-flash-lite` → `gemini-3.1-flash-lite` → `gemini-3.6-flash` → `gemini-3-flash-preview` (drop duplicate aliases).

---

## Notes

- `gemini-2.0-*` hit free-tier quota (429) on this key — not ranked.  
- `gemini-2.5-flash-lite` is retired (404).  
- Alias pairs (`*-latest`, `*-preview`) often return identical macros; collapsing them in the app ladder is recommended.
