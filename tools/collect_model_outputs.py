#!/usr/bin/env python3
"""Collect raw chat outputs for free models. Does NOT score — review docs score manually.

  python3 tools/collect_model_outputs.py --provider openrouter
  python3 tools/collect_model_outputs.py --provider gemini
  python3 tools/collect_model_outputs.py --provider ollama
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
APIS_FILE = ROOT / "apis"
OUT_DIR = Path(__file__).resolve().parent / "benchmark_results"

PROBES = [
    {"id": "egg", "user_food": "1 large boiled egg"},
    {"id": "rice", "user_food": "100g cooked white rice"},
    {"id": "banana", "user_food": "1 medium banana"},
    {
        "id": "roti_dal",
        "user_food": "2 medium whole-wheat roti with 1 katori (150g) dal tadka",
    },
    {
        "id": "biryani",
        "user_food": "1 plate chicken biryani (restaurant style, ~400g)",
    },
    {
        "id": "dosa",
        "user_food": "1 plain masala dosa with coconut chutney",
    },
]


def prompt_for(probe: dict[str, Any]) -> str:
    return f"""You are a nutrition estimator for a calorie-tracking app optimised for Indian diets.
Estimate macros for the food below. Be precise and realistic (use standard
USDA / IFCT-style values). Reply with ONLY one JSON object —
no markdown fences, no commentary:
{{
  "status": "SUCCESS",
  "clarification_message": null,
  "food_analysis": {{
    "dish_name": "short dish name",
    "macros": {{
      "calories": <integer kcal for the full portion>,
      "protein_g": <integer grams>,
      "carbs_g": <integer grams>,
      "fats_g": <integer grams>
    }},
    "ingredients": []
  }},
  "exercise_analysis": null
}}
Food: {probe["user_food"]}"""


def load_apis() -> dict[str, str]:
    out: dict[str, str] = {}
    if APIS_FILE.exists():
        for line in APIS_FILE.read_text().splitlines():
            line = line.strip()
            if not line or "=" not in line or line.startswith("#"):
                continue
            k, v = line.split("=", 1)
            out[k.strip()] = v.strip()
    return out


def http_json(
    url: str,
    method: str = "GET",
    body: dict | None = None,
    auth: str | None = None,
    timeout: int = 120,
    extra_headers: dict[str, str] | None = None,
) -> Any:
    data = None if body is None else json.dumps(body).encode()
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if auth:
        headers["Authorization"] = auth
    if extra_headers:
        headers.update(extra_headers)
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        err_body = e.read().decode(errors="replace")[:800]
        raise RuntimeError(f"HTTP {e.code}: {err_body}") from e


def strip_code_fences(raw: str) -> str:
    s = raw.strip()
    if s.startswith("```"):
        s = s[3:].lstrip()
        if s.lower().startswith("json"):
            s = s[4:].lstrip()
        end = s.rfind("```")
        if end >= 0:
            s = s[:end]
    start = s.find("{")
    end = s.rfind("}")
    if start >= 0 and end > start:
        s = s[start : end + 1]
    return s.strip()


def parse_macros(raw: str | None) -> dict[str, Any] | None:
    if not raw or not str(raw).strip():
        return None
    try:
        data = json.loads(strip_code_fences(str(raw)))
    except json.JSONDecodeError:
        return None
    if str(data.get("status", "")).upper() != "SUCCESS":
        return None
    food = data.get("food_analysis") or {}
    macros = food.get("macros") or {}
    try:
        return {
            "dish_name": str(food.get("dish_name") or ""),
            "calories": int(macros["calories"]),
            "protein_g": int(macros["protein_g"]),
            "carbs_g": int(macros["carbs_g"]),
            "fats_g": int(macros["fats_g"]),
        }
    except (KeyError, TypeError, ValueError):
        return None


def provider_cfg(name: str, apis: dict[str, str]) -> dict[str, Any]:
    if name == "ollama":
        key = apis.get("ollama") or os.environ.get("OLLAMA_API_KEY", "")
        if not key:
            raise SystemExit("Missing ollama key")
        return {
            "name": "ollama",
            "chat_url": "https://ollama.com/v1/chat/completions",
            "auth": f"Bearer {key}",
            "extra_headers": {},
        }
    if name == "openrouter":
        key = apis.get("openrouter") or os.environ.get("OPENROUTER_API_KEY", "")
        if not key:
            raise SystemExit("Missing openrouter key")
        return {
            "name": "openrouter",
            "chat_url": "https://openrouter.ai/api/v1/chat/completions",
            "auth": f"Bearer {key}",
            "extra_headers": {
                "HTTP-Referer": "https://github.com/anantdark/FitBuddy",
                "X-Title": "FitBuddy-benchmark",
            },
        }
    if name == "gemini":
        key = apis.get("gemini") or os.environ.get("GEMINI_API_KEY", "")
        if not key:
            raise SystemExit("Missing gemini key")
        return {
            "name": "gemini",
            "chat_url": "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
            "auth": f"Bearer {key}",
            "extra_headers": {},
            "api_key": key,
        }
    raise SystemExit(f"Unknown provider {name}")


def list_models(cfg: dict[str, Any]) -> list[str]:
    name = cfg["name"]
    if name == "ollama":
        raw = http_json("https://ollama.com/v1/models", auth=cfg["auth"])
        return sorted({m["id"] for m in raw.get("data") or [] if m.get("id")})
    if name == "openrouter":
        raw = http_json("https://openrouter.ai/api/v1/models", auth=cfg["auth"])
        skip_sub = ("content-safety",)
        out = []
        for m in raw.get("data") or []:
            mid = m.get("id") or ""
            if not mid.endswith(":free"):
                continue
            if any(s in mid for s in skip_sub):
                continue
            out.append(mid)
        return sorted(set(out))
    if name == "gemini":
        key = cfg["api_key"]
        raw = http_json(
            f"https://generativelanguage.googleapis.com/v1beta/models?key={key}"
        )
        # Match app GeminiModelDto.isFreeTier: flash in id, not pro/ultra/computer-use.
        out = []
        for m in raw.get("models") or []:
            mid = (m.get("name") or "").removeprefix("models/")
            methods = m.get("supportedGenerationMethods") or []
            if "generateContent" not in methods:
                continue
            low = mid.lower()
            if "pro" in low or "ultra" in low:
                continue
            if "computer-use" in low or "computer_use" in low or "robotics" in low:
                continue
            if any(
                x in low
                for x in (
                    "embedding",
                    "imagen",
                    "image",
                    "lyria",
                    "aqa",
                    "tts",
                    "omni",
                    "nano-banana",
                )
            ):
                continue
            if "flash" not in low:
                continue
            out.append(mid)
        # Prefer canonical aliases over -001 duplicates
        preferred = []
        seen_base = set()
        for mid in sorted(out, key=lambda s: (s.endswith("-001"), s)):
            base = mid.removesuffix("-001")
            if base in seen_base and mid.endswith("-001"):
                continue
            seen_base.add(base)
            preferred.append(mid)
        return preferred
    return []


def is_subscription_error(err: str) -> bool:
    low = err.lower()
    return (
        "requires a subscription" in low
        or "upgrade for access" in low
        or "quota" in low and "exceeded" in low
    )


def chat(cfg: dict[str, Any], model: str, prompt: str) -> tuple[str, int]:
    started = time.time()
    body = {
        "model": model,
        "temperature": 0,
        "messages": [{"role": "user", "content": prompt}],
    }
    raw = http_json(
        cfg["chat_url"],
        method="POST",
        body=body,
        auth=cfg["auth"],
        extra_headers=cfg.get("extra_headers") or {},
    )
    ms = int((time.time() - started) * 1000)
    if raw.get("error"):
        raise RuntimeError(str(raw["error"])[:400])
    choices = raw.get("choices") or []
    if not choices:
        raise RuntimeError("Empty choices")
    msg = choices[0].get("message") or {}
    content = msg.get("content")
    if isinstance(content, list):
        content = "".join(
            p.get("text", "") if isinstance(p, dict) else str(p) for p in content
        )
    if not content or not str(content).strip():
        raise RuntimeError("Empty content")
    return str(content), ms


def probe_accessible(cfg: dict[str, Any], model: str) -> str | None:
    try:
        chat(cfg, model, "Reply with the single word OK.")
        return None
    except Exception as e:  # noqa: BLE001
        return str(e)[:300]


def collect(provider: str, model_filter: list[str] | None, cooldown: float) -> Path:
    apis = load_apis()
    cfg = provider_cfg(provider, apis)
    models = list_models(cfg)
    if model_filter:
        want = set(model_filter)
        models = [m for m in models if m in want]
        missing = sorted(want - set(models))
        if missing:
            print(f"warn: not in catalog: {missing}", file=sys.stderr)

    skipped: list[dict[str, str]] = []
    if provider == "ollama" and not model_filter:
        accessible = []
        print(f"probing free access ({len(models)} models)…", flush=True)
        for mid in models:
            err = probe_accessible(cfg, mid)
            if err:
                skipped.append({"model_id": mid, "reason": err[:200]})
                print(f"  skip {mid}: {err[:80]}", flush=True)
            else:
                accessible.append(mid)
                print(f"  free {mid}", flush=True)
            time.sleep(min(cooldown, 0.3))
        models = accessible

    if not models:
        raise SystemExit("No models to collect")

    results: dict[str, list[dict[str, Any]]] = {}
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    path = OUT_DIR / f"{provider}_raw_outputs.json"
    payload: dict[str, Any] = {
        "provider": provider,
        "collected_at_epoch_ms": int(time.time() * 1000),
        "probes": [{"id": p["id"], "food": p["user_food"]} for p in PROBES],
        "skipped": skipped,
        "models": results,
    }

    print(f"collect provider={provider} models={len(models)} probes={len(PROBES)}", flush=True)
    total = len(models) * len(PROBES)
    n = 0
    for i, mid in enumerate(models):
        print(f"[{i+1}/{len(models)}] {mid}", flush=True)
        rows: list[dict[str, Any]] = []
        abort_model = False
        for probe in PROBES:
            n += 1
            if abort_model:
                rows.append({"probe": probe["id"], "error": "skipped after model abort"})
                continue
            try:
                raw, ms = chat(cfg, mid, prompt_for(probe))
                parsed = parse_macros(raw)
                rows.append(
                    {
                        "probe": probe["id"],
                        "latency_ms": ms,
                        "parsed": parsed,
                        "raw": raw,
                    }
                )
                cal = parsed["calories"] if parsed else "-"
                print(
                    f"  {probe['id']}: {'ok' if parsed else 'unparsed'} {ms}ms cal={cal}",
                    flush=True,
                )
            except Exception as e:  # noqa: BLE001
                err = str(e)[:400]
                rows.append({"probe": probe["id"], "error": err})
                print(f"  {probe['id']}: ERROR {err[:120]}", flush=True)
                if is_subscription_error(err) or "HTTP 401" in err or "HTTP 403" in err:
                    abort_model = True
                    skipped.append({"model_id": mid, "reason": err[:200]})
            if n < total and cooldown > 0:
                time.sleep(cooldown)
        results[mid] = rows
        # Checkpoint after each model so hung later models don't lose earlier work.
        payload["collected_at_epoch_ms"] = int(time.time() * 1000)
        payload["models"] = results
        payload["skipped"] = skipped
        path.write_text(json.dumps(payload, indent=2))
        print(f"  checkpoint → {path.name} ({len(results)} models)", flush=True)

    print(f"wrote {path}", flush=True)
    return path


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--provider", choices=["ollama", "openrouter", "gemini"], required=True)
    p.add_argument("--models", default="")
    p.add_argument("--cooldown", type=float, default=1.0)
    args = p.parse_args()
    models = [m.strip() for m in args.models.split(",") if m.strip()] or None
    collect(args.provider, models, args.cooldown)


if __name__ == "__main__":
    main()
