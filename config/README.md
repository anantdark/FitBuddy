# Config

## Failover ladder config

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

## Donors list (`donors.json`)

Public list of Support ID hashes for people who donated. Fetched at runtime from:

`https://raw.githubusercontent.com/anantdark/FitBuddy/main/config/donors.json`

Pushing an update to `main` is enough; no app release required.

### Schema

```json
{
  "updatedAt": "2026-03-21",
  "donors": [
    {
      "hash": "<sha256 hex>",
      "name": "Ada",
      "photoUrl": "https://example.com/ada.jpg",
      "linkUrl": "https://example.com/ada"
    },
    {
      "hash": "<sha256 hex>"
    }
  ]
}
```

| Field | Required | Notes |
|-------|----------|-------|
| `hash` | yes | SHA-256 of the **canonical** Support ID (see below) |
| `name` | no | Shown in the public thank-you dialog; omit for hash-only (reminder off, no thank-you) |
| `photoUrl` | no | Optional avatar URL (Coil). If missing or load fails, the first letter of the name is shown |
| `linkUrl` | no | Optional profile / website URL; tapping the donor row opens it |

### Example entry (dummy)

Illustrative only. Do not use this hash for a real donor. The hash below is the
canonical SHA-256 of Support ID `550e8400-e29b-41d4-a716-446655440000`.

```json
{
  "updatedAt": "2026-03-21",
  "donors": [
    {
      "hash": "140f39b05a2d9de451b9b7ad2d1f4a26b16fb5e5c8b7cbde6154679102614882",
      "name": "Ada Example",
      "photoUrl": "https://avatars.githubusercontent.com/u/9919?s=128",
      "linkUrl": "https://en.wikipedia.org/wiki/Ada_Lovelace"
    },
    {
      "hash": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
  ]
}
```

The first row shows a named donor with photo and profile link (thank-you +
reminder off). The second is hash-only (reminder off, skipped in thank-you).

### Canonical hash (must match the app)

1. Trim whitespace  
2. Lowercase  
3. Remove all `-`  
4. SHA-256 of the UTF-8 bytes  
5. Lowercase hex (64 chars)

```bash
ID='550e8400-e29b-41d4-a716-446655440000'
canon=$(printf '%s' "$ID" | tr '[:upper:]' '[:lower:]' | tr -d '-')
printf '%s' "$canon" | shasum -a 256
```

Never commit raw Support IDs; they also unlock cloud backups.
