# Failover ladder config

Edit **`failover_ladders.json`** when free-model catalogs change (after running
`skills/benchmark-free-models` and getting approval).

| Field | Meaning |
|-------|---------|
| `text` / `photo` | Per-provider ordered model ids for Auto failover |
| Provider keys | `OPENROUTER`, `GEMINI`, `OLLAMA`, `OPENAI` (must match `AiProvider.name`) |

**App behavior:** selected model first → listed ladder ∩ live catalog → other catalog
models last. Do not reintroduce dynamic “intelligence” ranking in Kotlin.

The app loads this file from the classpath (`app` `resources` includes this `config/`
directory via `app/build.gradle.kts`).
