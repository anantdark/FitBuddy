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

Public supporter roster. Fetched at runtime from:

`https://raw.githubusercontent.com/anantdark/FitBuddy/main/config/donors.json`

Pushing an update to `main` is enough; no app release required.

**Never show donation amounts or tier thresholds in the app UI.** Amounts exist only so
the app can derive badges / card styles on device.

### Schema

```json
{
  "updatedAt": "2026-10-09",
  "donors": [
    {
      "hash": "<sha256 hex>",
      "name": "Ada",
      "photoUrl": "https://example.com/ada.jpg",
      "linkUrl": "https://github.com/ada",
      "tags": ["og"],
      "donations": [
        { "id": "optional-stable-id", "amountUsd": 12.0, "at": "2026-10-01" },
        { "id": "recurring-2", "amountUsd": 5.0, "at": "2026-10-15" }
      ]
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
| `name` | no | Public thank-you / gallery; omit for hash-only (reminder off, no public card) |
| `photoUrl` | no | Avatar URL (Coil). Missing/fail → letter avatar. Row tap opens trading card. Optional portraits under `config/donors/` via raw GitHub URL |
| `linkUrl` | no | Profile URL; opened only from the **card flip-side** (styled GitHub / Instagram / site panel) |
| `tags` | no | Non-amount badges. Known: `"og"` (OG Supporter). Future badges = new tag strings |
| `donations` | no | History of gifts. Sum `amountUsd` (USD) drives money badge; `at` drives thank-you |

### Money badges (derived, exclusive — highest only)

Computed from **lifetime** `sum(donations.amountUsd)`. Do not document thresholds in UI copy.

| Badge | Lifetime USD |
|-------|----------------|
| Generous Supporter | `> 3` |
| Legendary Supporter | `> 5` |
| Godlike Supporter | `> 10` |

Card chrome follows the money badge when present; otherwise `"og"` → OG card; else a
simple supporter card. Medals can stack (e.g. Godlike + OG).

**Supporter number:** `1001 + (0-based index in the `donors` array)`. First entry = **No. 1001**.

**Lore:** each tier has 10 back-of-card lines. The app picks one deterministically per
supporter + current card tier. Upgrading the money medal (or moving to OG-only vs money)
changes the pool — only **one** lore line shows at a time.

### Thank-you / recurring donations

The app stores a **last-seen donation date** (`yyyy-MM-dd`) locally (not in cloud backup).

- Named donors with any donation `at` **strictly after** that date appear in the thank-you dialog.
- On dismiss (or when there is nothing new), last-seen advances to the latest `at` in the file.
- Add another `donations[]` row with a newer `at` to resurface the same person after a recurring gift.

### Display order

List / thank-you / gallery preserve **array order** in this file.

Demo data: `DemoDonors` in `app/.../data/donors/DonorModels.kt`.

### Example entry (dummy)

Illustrative only. Do not use this hash for a real donor. The hash below is the
canonical SHA-256 of Support ID `550e8400-e29b-41d4-a716-446655440000`.

```json
{
  "updatedAt": "2026-10-09",
  "donors": [
    {
      "hash": "140f39b05a2d9de451b9b7ad2d1f4a26b16fb5e5c8b7cbde6154679102614882",
      "name": "Ada Example",
      "photoUrl": "https://avatars.githubusercontent.com/u/9919?s=128",
      "linkUrl": "https://github.com/torvalds",
      "tags": ["og"],
      "donations": [
        { "amountUsd": 12.0, "at": "2026-10-01" }
      ]
    },
    {
      "hash": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      "donations": [
        { "amountUsd": 5.0, "at": "2026-10-02" }
      ]
    }
  ]
}
```

### Canonical hash (must match the app)

**Source of truth:** `SupportIdHasher` in
`app/src/main/java/com/anant/fitbuddy/data/donors/SupportIdHasher.kt`
(unit tests lock the golden vectors).

1. Trim whitespace  
2. Lowercase  
3. Remove all `-`  
4. SHA-256 of the UTF-8 bytes  
5. Lowercase hex (64 chars)

Offline helper (same algorithm):

```bash
./config/hash_support_id.sh '550e8400-e29b-41d4-a716-446655440000'
# → 140f39b05a2d9de451b9b7ad2d1f4a26b16fb5e5c8b7cbde6154679102614882
```

Never commit raw Support IDs; they also unlock cloud backups.
