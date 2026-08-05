# FitBuddy model benchmark review — Ollama Cloud (free)

**Status:** awaiting your approval (Ollama only; Gemini / OpenRouter not run yet)  
**Date:** 2026-08-05  
**Raw dump:** `tools/benchmark_results/ollama_raw_outputs.json`  
**How scored:** manually by reviewing each model’s macros (not the app’s static point bands).  
**Do not ship rankings into the app until this doc is approved.**

---

## Scope

| Item | Value |
|------|--------|
| Provider | Ollama Cloud (`https://ollama.com`) |
| Catalog listed | 18 models |
| Free on this key | 7 models (11 returned HTTP 403 “requires a subscription”) |
| Probes | 6 text meal-JSON estimates (3 USDA staples + 3 Indian plates) |
| Prompt | JSON-only nutrition estimate (Indian-diet aware); `temperature=0` |

### Free models benchmarked

`gemma4:31b`, `gpt-oss:20b`, `gpt-oss:120b`, `minimax-m3`, `nemotron-3-nano:30b`, `nemotron-3-super`, `nemotron-3-ultra`

### Paid / skipped (403)

`deepseek-v4-flash`, `deepseek-v4-flash:0731`, `deepseek-v4-pro`, `glm-5.1`, `glm-5.2`, `kimi-k2.6`, `kimi-k2.7-code`, `kimi-k3`, `minimax-m2.7`, `mistral-large-3:675b`, `qwen3.5:397b`

---

## Probes & expected ballpark (my reference, not scored by code)

| Probe | Food | Ballpark I used |
|-------|------|-----------------|
| egg | 1 large boiled egg | ~78 kcal / 6P / 1C / 5F |
| rice | 100g cooked white rice | ~130 / 3 / 28 / 0 |
| banana | 1 medium banana | ~105 / 1 / 27 / 0 |
| roti_dal | 2 medium whole-wheat roti + 150g dal tadka | ~400–450 / 16–20P / 55–70C / 8–14F |
| biryani | restaurant chicken biryani ~400g | ~550–700 / 22–32P / 60–80C / 18–28F |
| dosa | 1 masala dosa + coconut chutney | ~280–380 / 6–9P / 40–55C / 8–14F |

Staples (egg/rice/banana) almost all models nailed. **Differentiation is almost entirely on the three Indian plates.**

---

## Stored outputs (parsed macros)

Format: `kcal / protein_g / carbs_g / fats_g` · latency is wall time for that probe.

### gemma4:31b — total **26.2 s**

| Probe | Macros | ms | My note |
|-------|--------|-----|---------|
| egg | 78/6/1/5 | 5699 | Exact |
| rice | 130/3/28/0 | 1941 | Exact |
| banana | 105/1/27/0 | 5790 | Exact |
| roti_dal | 385/18/56/11 | 3461 | Slightly light on kcal; otherwise solid |
| biryani | 650/32/68/28 | 2384 | Cal ok; fat a bit rich |
| dosa | 450/8/62/20 | 6951 | **High** kcal/fat/carbs for one dosa+chutney |

### gpt-oss:20b — total **29.9 s**

| Probe | Macros | ms | My note |
|-------|--------|-----|---------|
| egg | 78/6/1/5 | 2287 | Exact |
| rice | 130/3/28/0 | 3621 | Exact |
| banana | 105/1/27/0 | 2465 | Exact |
| roti_dal | 440/18/77/9 | 4909 | Cal ok; **carbs high** |
| biryani | 720/37/60/34 | 7204 | **Cal/protein/fat high** |
| dosa | 160/2/18/8 | 9378 | **Far too low** (under-counts masala+chutney) |

### gpt-oss:120b — total **47.3 s**

| Probe | Macros | ms | My note |
|-------|--------|-----|---------|
| egg | 78/6/1/5 | 5334 | Exact |
| rice | 130/2/28/0 | 4990 | Protein −1; fine |
| banana | 105/1/27/0 | 6152 | Exact |
| roti_dal | 508/24/84/7 | 9844 | **Cal/carbs high** |
| biryani | 820/27/72/45 | 13872 | **Cal/fat too high** |
| dosa | 270/6/43/10 | 7157 | A bit low but usable |

### minimax-m3 — total **41.6 s**  ← **champion (accuracy)**

| Probe | Macros | ms | My note |
|-------|--------|-----|---------|
| egg | 78/6/1/5 | 4338 | Exact |
| rice | 130/3/28/0 | 4232 | Exact |
| banana | 105/1/27/0 | 5697 | Exact |
| roti_dal | 435/17/61/12 | 8107 | Best of the set |
| biryani | 620/26/65/22 | 8671 | Best of the set |
| dosa | 320/7/43/13 | 10542 | Best of the set |

### nemotron-3-nano:30b — total **55.4 s**

| Probe | Macros | ms | My note |
|-------|--------|-----|---------|
| egg | 70/6/1/5 | 5834 | Slightly low kcal |
| rice | 130/2/28/0 | 9545 | Fine |
| banana | 105/1/27/0 | 4043 | Exact |
| roti_dal | 416/22/73/8 | 15267 | Cal ok; carbs/protein a bit high |
| biryani | 520/28/80/22 | 5216 | A bit light on kcal for ~400g restaurant plate |
| dosa | 300/7/55/9 | 15446 | Good |

### nemotron-3-super — total **63.7 s** (retry after one timeout)

| Probe | Macros | ms | My note |
|-------|--------|-----|---------|
| egg | 78/6/1/5 | 16973 | Exact (slow) |
| rice | 130/2/28/0 | 3599 | Fine |
| banana | 105/1/27/0 | 5530 | Exact |
| roti_dal | 300/14/44/8 | 11162 | **Too low** for 2 roti + 150g dal |
| biryani | 650/36/72/24 | 14683 | Cal ok; protein high |
| dosa | 205/4/29/7 | 11704 | **Too low** |

### nemotron-3-ultra — total **89.9 s**

| Probe | Macros | ms | My note |
|-------|--------|-----|---------|
| egg | 78/6/1/5 | 11789 | Exact (slow) |
| rice | 130/3/28/0 | 14355 | Exact (slow) |
| banana | 105/1/27/0 | 10562 | Exact |
| roti_dal | 350/14/62/6 | 18963 | Light on kcal/fat |
| biryani | 650/22/85/24 | 16580 | Cal ok; carbs high / protein light |
| dosa | 345/7/54/12 | 17609 | Strong |

---

## Manual scoring method

I scored each probe **0–10 by hand**:

- Staples: full marks if within ~5–10% of USDA; small deductions for tiny misses.
- Indian plates: judged against the ballparks above **and** relative consistency with the best answer in the set (`minimax-m3`).
- JSON always parsed for these 7 free models — no format failures.

**Champion = `minimax-m3`**: closest overall to realistic Indian-plate macros; staples perfect. All accuracy ranks below are “how close to this champion / to my ballpark,” not a coded rubric.

### Per-probe manual scores ( /10 )

| Model | egg | rice | banana | roti_dal | biryani | dosa | **Total /60** | **Acc %** |
|-------|-----|------|--------|----------|---------|------|---------------|-----------|
| **minimax-m3** | 10 | 10 | 10 | 10 | 10 | 10 | **60** | **100%** |
| nemotron-3-ultra | 10 | 10 | 10 | 7 | 8 | 9 | **54** | **90%** |
| nemotron-3-nano:30b | 9 | 9 | 10 | 8 | 8 | 9 | **53** | **88%** |
| gemma4:31b | 10 | 10 | 10 | 8 | 8 | 6 | **52** | **87%** |
| gpt-oss:120b | 10 | 9 | 10 | 6 | 5 | 8 | **48** | **80%** |
| nemotron-3-super | 10 | 9 | 10 | 5 | 8 | 5 | **47** | **78%** |
| gpt-oss:20b | 10 | 10 | 10 | 7 | 6 | 3 | **46** | **77%** |

---

## Proposed rankings (Ollama free) — **please approve or edit**

### Accuracy (failover order if user picks “by accuracy”)

1. `minimax-m3` — champion  
2. `nemotron-3-ultra`  
3. `nemotron-3-nano:30b`  
4. `gemma4:31b`  
5. `gpt-oss:120b`  
6. `nemotron-3-super`  
7. `gpt-oss:20b`

### Speed (sum of 6 probe wall times; failover order if user picks “by speed”)

1. `gemma4:31b` — **26.2 s** (87% acc)  
2. `gpt-oss:20b` — **29.9 s** (77% — fast but weakest Indian plates)  
3. `minimax-m3` — **41.6 s** (100%)  
4. `gpt-oss:120b` — **47.3 s** (80%)  
5. `nemotron-3-nano:30b` — **55.4 s** (88%)  
6. `nemotron-3-super` — **63.7 s** (78%)  
7. `nemotron-3-ultra` — **89.9 s** (90% — accurate but slowest)

### Practical recommendation for default Auto failover

If we ship one default ladder after approval:

- Prefer **accuracy ladder** with `minimax-m3` first, then `nemotron-3-nano:30b` / `gemma4:31b` (good accuracy without ultra’s latency).  
- Or a **balanced** ladder: `gemma4:31b` → `minimax-m3` → `nemotron-3-nano:30b` → … (fast first, then champion).

Your call — approve/edit the accuracy list, speed list, and which becomes the app default.

---

## What is intentionally *not* done yet

- No Gemini / OpenRouter runs yet (you said start with Ollama).  
- Rankings are **not** written into app DataStore / failover order UI yet.  
- App static benchmark scorer may exist in the tree from earlier WIP — **these rankings do not depend on it**; this document is the source of truth until you approve.

---

## Approval checklist

Reply with edits or “approved” on:

- [ ] Accuracy order (1–7)  
- [ ] Speed order (1–7)  
- [ ] Which order Auto failover should use by default (accuracy / speed / balanced)  
- [ ] Whether to drop weak models from the failover ladder (e.g. `gpt-oss:20b` dosa miss)

After approval: persist ranking in the app + Settings UI to order preferred models by speed or accuracy.
