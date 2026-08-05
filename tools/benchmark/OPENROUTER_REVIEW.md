# FitBuddy model benchmark review — OpenRouter (free)

**Status:** awaiting your approval  
**Date:** 2026-08-05  
**Raw dump:** `tools/benchmark_results/openrouter_raw_outputs.json`  
**How scored:** manually from stored macros (not app static bands).

---

## Scope

| Item | Value |
|------|--------|
| Provider | OpenRouter (`:free` catalog) |
| Attempted | 10 chat-capable free models |
| Skipped up front | `cohere/north-mini-code` (5+ min/probe), `poolside/laguna-s` (hangs), `nemotron-nano-12b-v2-vl` (timeouts / bad staples), `content-safety` |
| Usable (6/6) | 9 models |
| No data | `google/gemma-4-31b-it:free` (429 rate-limited entire run) |

---

## Stored outputs (parsed macros)

Format: `kcal / P / C / F` · ballparks same as Ollama/Gemini reviews.

### inclusionai/ling-3.0-flash:free — **31.0 s** ← champion

| Probe | Macros | Note |
|-------|--------|------|
| egg | 78/6/1/5 | Exact |
| rice | 130/3/28/0 | Exact |
| banana | 105/1/27/0 | Exact |
| roti_dal | 380/15/59/10 | Slightly light; solid |
| biryani | 680/30/68/28 | Strong |
| dosa | 350/7/45/14 | Best of OpenRouter set |

### google/gemma-4-26b-a4b-it:free — **62.7 s**

| Probe | Macros | Note |
|-------|--------|------|
| egg | 78/6/1/5 | Exact |
| rice | 130/2/28/0 | Protein −1 |
| banana | 105/1/27/0 | Exact |
| roti_dal | 415/16/62/13 | Excellent |
| biryani | 720/32/84/28 | Cal/carbs high |
| dosa | 385/7/54/16 | Slightly high |

### nvidia/nemotron-3-ultra-550b-a55b:free — **149 s**

| Probe | Macros | Note |
|-------|--------|------|
| staples | exact | |
| roti_dal | 457/21/80/8 | Carbs high |
| biryani | 560/28/70/22 | Slightly light kcal |
| dosa | 308/7/40/14 | Good |

### poolside/laguna-xs-2.1:free — **82.1 s**

| Probe | Macros | Note |
|-------|--------|------|
| egg | 72/6/0/5 | Tiny miss |
| rice | 130/2/28/0 | Fine |
| banana | exact | |
| roti_dal | 465/19/65/7 | Upper cal; fat light |
| biryani | 650/28/70/25 | Good |
| dosa | 380/8/47/17 | Slightly high fat |

### nvidia/nemotron-3-super-120b-a12b:free — **66.7 s**

| Probe | Macros | Note |
|-------|--------|------|
| staples | good | |
| roti_dal | 325/15/47/7 | **Too light** |
| biryani | 644/30/95/16 | Carbs high / fat light |
| dosa | 375/7/47/15 | Good |

### nvidia/nemotron-3-nano-30b-a3b:free — **69.5 s**

| Probe | Macros | Note |
|-------|--------|------|
| egg | 70/6/0/5 | Slight miss |
| roti_dal | 420/18/70/7 | Carbs high |
| biryani | 720/24/120/20 | **Carbs absurd** |
| dosa | 200/4/29/9 | **Too low** |

### openai/gpt-oss-20b:free — **345 s**

| Probe | Macros | Note |
|-------|--------|------|
| staples | good | |
| roti_dal | 506/19/83/12 | High |
| biryani | 750/35/95/25 | High |
| dosa | 155/3/16/6 | **Far too low** |

### nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free — **543 s**

| Probe | Macros | Note |
|-------|--------|------|
| roti_dal | 641/22/83/30 | Way high (also 7+ min for one probe) |
| biryani | 522/56/49/20 | Protein absurd |
| dosa | 230/5/35/10 | Low |
| — | — | Unusable for failover |

### nvidia/nemotron-nano-9b-v2:free — **214 s**

| Probe | Macros | Note |
|-------|--------|------|
| rice | 115/3/24/0 | Wrong staple |
| roti_dal | 620/25/100/15 | Way high |
| dosa | 570/14/60/27 | Absurd |
| — | — | Weak |

### google/gemma-4-31b-it:free — no data (429)

Retry later; Ollama Cloud `gemma4:31b` was strong in the Ollama review.

---

## Manual scores (/10 → /60)

Champion: **`inclusionai/ling-3.0-flash:free`**.

| Model | egg | rice | ban | roti | biryani | dosa | **Tot** | **%** |
|-------|-----|------|-----|------|---------|------|---------|-------|
| **ling-3.0-flash** | 10 | 10 | 10 | 9 | 9 | 10 | **58** | **97%** |
| nemotron-3-ultra | 10 | 10 | 10 | 7 | 9 | 9 | **55** | **92%** |
| gemma-4-26b-a4b-it | 10 | 9 | 10 | 10 | 7 | 8 | **54** | **90%** |
| laguna-xs-2.1 | 9 | 9 | 10 | 8 | 9 | 8 | **53** | **88%** |
| nemotron-3-super | 10 | 9 | 10 | 6 | 7 | 8 | **50** | **83%** |
| nemotron-3-nano-30b | 9 | 9 | 10 | 8 | 5 | 4 | **45** | **75%** |
| gpt-oss-20b | 10 | 9 | 10 | 6 | 6 | 2 | **43** | **72%** |
| nemotron-omni-reasoning | 9 | 10 | 10 | 3 | 4 | 5 | **41** | **68%** |
| nemotron-nano-9b-v2 | 9 | 7 | 10 | 2 | 6 | 2 | **36** | **60%** |
| gemma-4-31b-it | — | — | — | — | — | — | **n/a** | rate-limited |

---

## Proposed rankings — OpenRouter free

### Accuracy

1. `inclusionai/ling-3.0-flash:free`  
2. `nvidia/nemotron-3-ultra-550b-a55b:free`  
3. `google/gemma-4-26b-a4b-it:free`  
4. `poolside/laguna-xs-2.1:free`  
5. `nvidia/nemotron-3-super-120b-a12b:free`  
6. `nvidia/nemotron-3-nano-30b-a3b:free`  
7. `openai/gpt-oss-20b:free`  
8. `nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free` *(optional drop)*  
9. `nvidia/nemotron-nano-9b-v2:free` *(optional drop)*

### Speed

1. `inclusionai/ling-3.0-flash:free` — **31 s**  
2. `google/gemma-4-26b-a4b-it:free` — **63 s**  
3. `nvidia/nemotron-3-super-120b-a12b:free` — **67 s**  
4. `nvidia/nemotron-3-nano-30b-a3b:free` — **70 s**  
5. `poolside/laguna-xs-2.1:free` — **82 s**  
6. `nvidia/nemotron-3-ultra-550b-a55b:free` — **149 s**  
7. `nvidia/nemotron-nano-9b-v2:free` — **214 s**  
8. `openai/gpt-oss-20b:free` — **345 s**  
9. `nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free` — **543 s**

**Suggested default ladder:** `ling-3.0-flash` → `gemma-4-26b-a4b-it` → `nemotron-3-super` → `laguna-xs` → `nemotron-3-ultra` (drop omni / nano-9b / gpt-oss-20b from Auto).

---

## Notes

- Free-tier rate limits hit `gemma-4-31b-it` hard; re-probe before shipping it in the ladder.  
- Reasoning/omni variants are too slow and noisy for meal JSON.  
- Same probes/prompt as Ollama + Gemini reviews for cross-platform comparison.
