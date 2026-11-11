# Translating FitBuddy

FitBuddy UI strings live in Android `strings.xml` files and are translated on
**[Hosted Weblate](https://hosted.weblate.org/projects/fitbuddy/fitbuddy/)**.

| | |
| --- | --- |
| Project | [Fitbuddy](https://hosted.weblate.org/projects/fitbuddy/) |
| Component | [FitBuddy](https://hosted.weblate.org/projects/fitbuddy/fitbuddy/) |
| Source language | English (`app/src/main/res/values/strings.xml`) |
| Translations | `app/src/main/res/values-*/strings.xml` |
| License | GPL-3.0-only |

![Translation status](https://hosted.weblate.org/widget/fitbuddy/fitbuddy/horizontal-auto.svg)

## For translators

1. Open the [FitBuddy Weblate component](https://hosted.weblate.org/projects/fitbuddy/fitbuddy/).
2. Sign in (GitHub / other Weblate login is fine).
3. Pick your language, or **Start new translation** if it is missing.
4. Translate in the Weblate editor. Suggestions are welcome if you are unsure.
5. You do **not** need to open a GitHub PR yourself — Weblate pushes a
   `chore(l10n): …` pull request when changes are ready.

### Style guidelines

- Keep the meaning and tone of the English source (direct, short, helpful).
- Prefer natural phrasing in the target language over word-for-word calques.
- Preserve placeholders exactly: `%1$s`, `%2$d`, `%s`, etc. Do not reorder
  numbers unless the language grammar requires it — then keep every index.
- Preserve XML entities (`&amp;`, `&lt;`, `\'`) and typographic characters
  that appear in the source when they matter for layout.
- Do not translate brand names: **FitBuddy**, provider names (OpenRouter,
  Gemini, Ollama, OpenAI), or file/format tokens the app shows as-is.
- Leave technical identifiers untranslated when they are clearly machine
  values (model ids, URLs, Support ID labels when shown as codes).
- If a string is unclear, leave a Weblate comment asking for context rather
  than guessing.

### What not to translate by hand in Git

Please do **not** edit `app/src/main/res/values-*/strings.xml` in a normal
feature PR unless you are fixing a Weblate merge conflict. Source English
changes belong in `values/strings.xml`; translations flow through Weblate.

## For developers (source English)

1. Add or change user-visible copy only in
   `app/src/main/res/values/strings.xml`.
2. Use stable, descriptive key names (`food_review_…`, `settings_…`).
3. Prefer numbered placeholders (`%1$s`) so translators can reorder safely.
4. Mark non-UI / debug-only strings with `translatable="false"` when needed.
5. After merging English string changes to `main`, Weblate pulls them
   automatically (GitHub App hooks). New keys show up as untranslated.
6. Review and merge Weblate PRs labeled `translations` when CI is green.

Debug-only overrides under `app/src/debug/res/` are **not** part of the
Weblate component.

## Maintainer checklist (Hosted Weblate)

The GitHub ↔ Weblate link is already in place (`vcs: github-app`, component
file mask / template set for Android). Confirm these in the Weblate UI:

### Project ([Fitbuddy settings](https://hosted.weblate.org/settings/fitbuddy/))

- [ ] **Translation instructions** — paste the short blurb below (or link to this file).
- [ ] **Language code style** — **Android style** (`pt-rBR`, `zh-rCN`, …).
- [ ] **Adding new translation** — allow translators to add languages.
- [ ] **Enable hooks** — on (keeps Weblate in sync with GitHub pushes).

### Component ([FitBuddy settings](https://hosted.weblate.org/settings/fitbuddy/fitbuddy/))

| Setting | Expected value |
| --- | --- |
| File format | Android String Resource |
| File mask | `app/src/main/res/values-*/strings.xml` |
| Monolingual base language file | `app/src/main/res/values/strings.xml` |
| Template for new translations | *(empty)* |
| Repository branch | `main` |
| Version control system | GitHub (App) |
| Merge requests | Create pull requests (label `translations`) |

Optional but useful:

- [ ] **Screenshot file mask**:
  `fastlane/metadata/android/en-US/images/phoneScreenshots/*.png`
  then assign screenshots to strings in Weblate’s screenshot UI.
- [ ] Remove accidental / test locales before they grow (e.g. empty stubs).

### Suggested project instructions (paste into Weblate)

```text
FitBuddy is an AI-powered Android health tracker (GPL-3.0).

Translate UI strings only. Keep placeholders like %1$s unchanged.
Do not translate the brand name “FitBuddy” or AI provider names
(OpenRouter, Gemini, Ollama, OpenAI). Prefer natural, concise wording.

Full guide: https://github.com/anantdark/FitBuddy/blob/main/docs/TRANSLATING.md
Component: https://hosted.weblate.org/projects/fitbuddy/fitbuddy/
```

## How updates land on GitHub

1. Translators save work on Weblate.
2. After Weblate’s commit interval, it opens a PR from a `weblate-…` branch
   (see [PR #119](https://github.com/anantdark/FitBuddy/pull/119) as an example).
3. Maintainers review, wait for CI, and merge into `main`.
4. The next app release / F-Droid build picks up the new `values-*` folders.

Empty new-language files (`<resources></resources>`) are normal until strings
are actually translated; Android still falls back to English.

## Local `wlc` (optional)

A [`.weblate`](../.weblate) file is in the repo root for the
[Weblate client](https://docs.weblate.org/en/latest/wlc.html):

```bash
pip install wlc
# configure ~/.config/weblate with your API key, then:
wlc list-languages
wlc pull
```

## F-Droid / store listing (not in Weblate yet)

Play/F-Droid metadata under `fastlane/metadata/android/` is English-only today.
A second Weblate component can be added later if store listing translations
are desired.
