(() => {
  /**
   * Public supporter trading cards — mirrors app rules in DonorBadgeRules / DonorLore.
   * Amounts are used only to derive card chrome; never rendered.
   */
  const DONORS_URL =
    "https://raw.githubusercontent.com/anantdark/FitBuddy/main/config/donors.json";

  const TWEMOJI = "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72";

  const PALETTES = {
    OG: {
      frame: ["#8D6E63", "#FFCC80", "#5D4037", "#BCAAA4"],
      shellTop: "#6D4C41",
      shellBottom: "#3E2723",
      inner: "#FFF3E0",
      ink: "#3E2723",
      accent: "#FFAB40",
      badgeLabel: "OG SUPPORTER",
      subtitle: "FitBuddy · Origin circle",
    },
    GENEROUS: {
      frame: ["#00C853", "#B9F6CA", "#00BFA5", "#69F0AE"],
      shellTop: "#1B5E20",
      shellBottom: "#0A2F12",
      inner: "#E8F5E9",
      ink: "#1B5E20",
      accent: "#00E676",
      badgeLabel: "GENEROUS",
      subtitle: "FitBuddy · Warm boost",
    },
    LEGENDARY: {
      frame: ["#7C4DFF", "#18FFFF", "#E040FB", "#536DFE"],
      shellTop: "#311B92",
      shellBottom: "#12005E",
      inner: "#1A237E",
      ink: "#E8EAF6",
      accent: "#18FFFF",
      badgeLabel: "LEGENDARY",
      subtitle: "FitBuddy · Rare aura",
    },
    GODLIKE: {
      frame: ["#FFD700", "#FF1744", "#FFEA00", "#FF6D00", "#FFD700"],
      shellTop: "#4A0000",
      shellBottom: "#0A0000",
      inner: "#1A0500",
      ink: "#FFF8E1",
      accent: "#FFD700",
      badgeLabel: "GODLIKE",
      subtitle: "FitBuddy · Mythic seal",
    },
    STANDARD: {
      frame: ["#78909C", "#ECEFF1", "#546E7A"],
      shellTop: "#455A64",
      shellBottom: "#263238",
      inner: "#CFD8DC",
      ink: "#263238",
      accent: "#90A4AE",
      badgeLabel: "SUPPORTER",
      subtitle: "FitBuddy · Community",
    },
  };

  const LORE = {
    OG: [
      "Present at the dawn of FitBuddy — when the first meal log was still a dare.",
      "Helped light the first campfire. Every free feature still warms by that spark.",
      "Signed the imaginary founding scroll. Chai stains optional, conviction required.",
      "One of the earliest believers. The roadmap still nods in their direction.",
      "Held the door open while FitBuddy walked into the world. Never asked for a throne.",
      "Origin-circle energy: quiet, stubborn, and allergic to paywalls.",
      "Their tip landed before the hype. That's how legends get a seat up front.",
      "Witness to version zero vibes. Still cheering like it's day one.",
      "Carved their name into the cornerstone — metaphorically, and with excellent macros.",
      "OG status isn't bought twice. It was earned when the app was still finding its voice.",
    ],
    GENEROUS: [
      "A warm tip that keeps the kettle on and the commits landing.",
      "Proof that kindness scales: one chai at a time, features unlock for everyone.",
      "Shows up with generosity and zero guilt trips. FitBuddy's favorite kind of hero.",
      "Turned spare change into open-source momentum. The community felt it.",
      "A soft boost with hard impact — servers hum a little happier.",
      "Believes free fitness tracking should stay free. Backs that belief.",
      "Their support smells like fresh chai and merged pull requests.",
      "Didn't need a parade. Just sent fuel and let the app cook.",
      "The kind of tip that makes late-night bugfixes feel worth it.",
      "Generosity logged. Gratitude returned — forever, and in public.",
    ],
    LEGENDARY: [
      "Rare aura detected. Roadmaps subtly rearrange themselves in respect.",
      "When they tip, the changelog grows a little braver.",
      'Legendary backing: the sort that turns "someday" into "shipped".',
      "Walks softly, funds loudly. FitBuddy's lore books took notes.",
      "A presence you feel in the release notes before you see the name.",
      "Elevated the floor for everyone else. That's legendary manners.",
      "Their support has main-character energy without the ego DLC.",
      "Rumored to make CI pass on the first try. Unconfirmed. Respected.",
      "Left a mark in the supporter vault that still faintly glows cyan.",
      "Not loud. Not flashy. Just legendary — and deeply appreciated.",
    ],
    GODLIKE: [
      "Mythic backing. The kind that bends roadmaps and steadies the night builds.",
      "When gods tip, open source answers. FitBuddy heard them clearly.",
      "A seal of belief so strong the paywall never stood a chance.",
      "Godlike generosity: features arrive faster, guilt stays at zero.",
      "Whispers say the coffee machine bows. The changelog definitely does.",
      "Rewrote the odds for a free app. Mortals call that godlike.",
      "Their name in the vault reads like a final boss — on our side.",
      "Funded the impossible quietly. The app got louder with gratitude.",
      "A tip that felt like armor for every unpaid evening of craft.",
      "Ascended the supporter ladder and still kept the vibe humble. Iconic.",
    ],
    STANDARD: [
      "A valued FitBuddy supporter keeping the app free and open.",
      "Part of the circle that chooses community over paywalls.",
      "Logged kindness the same way others log protein — consistently.",
      "Their support is a quiet yes to home kitchens and honest tracking.",
      "Standing with FitBuddy so everyone can keep logging without guilt.",
      "A tip, a thank-you, a little more runway for the next feature.",
      "Proof the community shows up for tools that respect them.",
      "Helping keep the lights on and the macros honest.",
      'One of the folks who make "free and open" actually sustainable.',
      "Appreciated deeply — on the dashboard and behind the scenes.",
    ],
  };

  const BADGE_META = {
    og: {
      title: "OG Supporter",
      emoji: "2694",
      kanji: "始",
      rankEn: "ORIGIN",
    },
    generous: {
      title: "Generous Supporter",
      emoji: "1f338",
      kanji: "慈",
      rankEn: "BLESS",
    },
    legendary: {
      title: "Legendary Supporter",
      emoji: "1f409",
      kanji: "龍",
      rankEn: "MYTHIC",
    },
    godlike: {
      title: "Godlike Supporter",
      emoji: "1f525",
      kanji: "神",
      rankEn: "DIVINE",
    },
  };

  function moneyBadge(lifetimeUsd) {
    if (lifetimeUsd > 10) return "GODLIKE";
    if (lifetimeUsd > 5) return "LEGENDARY";
    if (lifetimeUsd > 3) return "GENEROUS";
    return null;
  }

  function cardStyle(donor) {
    const lifetime = (donor.donations || []).reduce(
      (sum, d) => sum + Math.max(0, Number(d.amountUsd) || 0),
      0,
    );
    const money = moneyBadge(lifetime);
    if (money) return money;
    const tags = (donor.tags || []).map((t) => String(t).trim().toLowerCase());
    if (tags.includes("og")) return "OG";
    return "STANDARD";
  }

  function badges(donor) {
    const out = [];
    const lifetime = (donor.donations || []).reduce(
      (sum, d) => sum + Math.max(0, Number(d.amountUsd) || 0),
      0,
    );
    const money = moneyBadge(lifetime);
    if (money === "GODLIKE") out.push("godlike");
    else if (money === "LEGENDARY") out.push("legendary");
    else if (money === "GENEROUS") out.push("generous");
    const reserved = new Set(["generous", "legendary", "godlike"]);
    for (const tag of (donor.tags || []).map((t) => String(t).trim().toLowerCase())) {
      if (!tag) continue;
      if (tag === "og") out.push("og");
      else if (!reserved.has(tag)) out.push(tag);
    }
    return out;
  }

  /** Match Kotlin DonorLore.stableSeed (FNV-1a 64-bit) — keep BigInt to avoid precision loss. */
  function stableSeed(hash, styleName) {
    let h = 0xcbf29ce484222325n;
    const key = `${hash}|${styleName}`;
    for (const ch of key) {
      h ^= BigInt(ch.codePointAt(0));
      h = (h * 0x100000001b3n) & 0xffffffffffffffffn;
    }
    return h & 0x7fffffffffffffffn;
  }

  function loreFor(hash, style) {
    const pool = LORE[style] || LORE.STANDARD;
    const seed = stableSeed(hash, style);
    let index = Number(seed % BigInt(pool.length));
    if (index < 0) index += pool.length;
    return pool[index];
  }

  function formatMonthYear(iso) {
    const parts = String(iso || "").trim().split("-");
    if (parts.length < 2) return "—";
    const months = [
      "Jan", "Feb", "Mar", "Apr", "May", "Jun",
      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    ];
    const mon = months[Number(parts[1]) - 1];
    if (!mon) return "—";
    return `${mon} '${parts[0].slice(-2)}`;
  }

  function profileHome(url) {
    if (!url) return "Private";
    try {
      const host = new URL(url).hostname.toLowerCase();
      if (host === "github.com" || host.endsWith(".github.com")) return "GitHub";
      if (host.includes("instagram.com")) return "Instagram";
      return "Web";
    } catch {
      return "Web";
    }
  }

  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function crestHtml(id) {
    const meta = BADGE_META[id] || {
      title: id.replace(/[-_]/g, " ").replace(/\b\w/g, (c) => c.toUpperCase()),
      emoji: "2728",
      kanji: "印",
      rankEn: "CREST",
    };
    const emoji = BADGE_META[id] ? BADGE_META[id].emoji : "2728";
    return `
      <div class="supporter-crest" title="${escapeHtml(meta.title)}">
        <span class="supporter-crest-kanji" aria-hidden="true">${escapeHtml(meta.kanji)}</span>
        <img src="${TWEMOJI}/${emoji}.png" alt="" width="28" height="28" loading="lazy" />
        <span class="supporter-crest-rank">${escapeHtml(meta.rankEn)}</span>
        <span class="supporter-crest-title">${escapeHtml(meta.title)}</span>
      </div>`;
  }

  function cardHtml(donor, rosterIndex) {
    const name = String(donor.name || "").trim();
    const hash = String(donor.hash || "").trim().toLowerCase();
    const style = cardStyle(donor);
    const palette = PALETTES[style];
    const number = 1001 + rosterIndex;
    const lore = loreFor(hash, style);
    const badgeIds = badges(donor);
    const photo = String(donor.photoUrl || "").trim();
    const link = String(donor.linkUrl || "").trim();
    const dates = (donor.donations || [])
      .map((d) => String(d.at || "").trim())
      .filter(Boolean)
      .sort();
    const since = dates.length ? formatMonthYear(dates[0]) : "—";
    const tips = (donor.donations || []).length;
    const tipsLabel = tips === 0 ? "—" : tips === 1 ? "1×" : `${tips}×`;
    const home = profileHome(link || null);
    const letter = name.slice(0, 1).toUpperCase() || "?";
    const frame = palette.frame.join(", ");
    const intense = style === "GODLIKE" || style === "LEGENDARY";

    const photoBlock = photo
      ? `<img class="supporter-photo" src="${escapeHtml(photo)}" alt="${escapeHtml(name)}" loading="lazy" />`
      : `<div class="supporter-letter" aria-hidden="true">${escapeHtml(letter)}</div>`;

    const crests = badgeIds.map(crestHtml).join("");
    const profileBtn = link
      ? `<a class="supporter-profile" href="${escapeHtml(link)}" rel="noopener" target="_blank">Open profile</a>`
      : "";

    return `
      <article class="supporter-card${intense ? " is-intense" : ""}" data-style="${style}" style="--shell-top:${palette.shellTop};--shell-bottom:${palette.shellBottom};--inner:${palette.inner};--ink:${palette.ink};--accent:${palette.accent};--frame:${frame}">
        <button type="button" class="supporter-flip" aria-pressed="false" aria-label="Flip ${escapeHtml(name)} supporter card">
          <div class="supporter-inner">
            <div class="supporter-face supporter-front">
              <div class="supporter-shell">
                <div class="supporter-banner">
                  <span aria-hidden="true">★</span>
                  <span>No. ${number}  ·  ${escapeHtml(palette.badgeLabel)}</span>
                </div>
                <div class="supporter-art">${photoBlock}</div>
                <div class="supporter-meta">
                  <h3>${escapeHtml(name.toUpperCase())}</h3>
                  <p>${escapeHtml(palette.subtitle)}</p>
                </div>
                ${badgeIds.length ? `<p class="supporter-crests-label">Rank crests</p><div class="supporter-crests">${crests}</div>` : ""}
                <dl class="supporter-insights">
                  <div><dt>Since</dt><dd>${escapeHtml(since)}</dd></div>
                  <div><dt>Tips</dt><dd>${escapeHtml(tipsLabel)}</dd></div>
                  <div><dt>Home</dt><dd>${escapeHtml(home)}</dd></div>
                </dl>
              </div>
            </div>
            <div class="supporter-face supporter-back" aria-hidden="true">
              <p class="supporter-lore-kicker">Official lore</p>
              <p class="supporter-lore-name">${escapeHtml(name)}</p>
              <p class="supporter-lore">${escapeHtml(lore)}</p>
              ${badgeIds.length ? `<div class="supporter-crests">${crests}</div>` : ""}
              ${profileBtn}
              <p class="supporter-series">FitBuddy Supporter Series</p>
            </div>
          </div>
        </button>
        <p class="supporter-hint">Tap card to flip</p>
      </article>`;
  }

  function bindFlips(root) {
    root.querySelectorAll(".supporter-flip").forEach((btn) => {
      btn.addEventListener("click", (event) => {
        if (event.target.closest("a")) return;
        const card = btn.closest(".supporter-card");
        if (!card) return;
        const flipped = card.classList.toggle("is-flipped");
        btn.setAttribute("aria-pressed", String(flipped));
        const back = btn.querySelector(".supporter-back");
        const front = btn.querySelector(".supporter-front");
        if (back) back.setAttribute("aria-hidden", flipped ? "false" : "true");
        if (front) front.setAttribute("aria-hidden", flipped ? "true" : "false");
      });
    });
  }

  async function mountSupporters() {
    const root = document.getElementById("supporters-gallery");
    if (!root) return;

    root.innerHTML = `<p class="fine">Loading supporters…</p>`;

    try {
      const res = await fetch(DONORS_URL, { headers: { Accept: "application/json" } });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const file = await res.json();
      const donors = Array.isArray(file.donors) ? file.donors : [];
      const cards = [];
      donors.forEach((donor, index) => {
        const name = String(donor?.name || "").trim();
        const hash = String(donor?.hash || "").trim();
        if (!name || !hash) return;
        cards.push(cardHtml(donor, index));
      });

      if (!cards.length) {
        root.innerHTML = `<p class="fine">No public supporter cards yet — be the first.</p>`;
        return;
      }

      root.innerHTML = `<div class="supporter-grid">${cards.join("")}</div>`;
      bindFlips(root);
      const updated = String(file.updatedAt || "").trim();
      const stamp = document.getElementById("supporters-updated");
      if (stamp && updated) stamp.textContent = `Roster updated ${updated}`;
    } catch {
      root.innerHTML = `
        <p class="fine">
          Couldn’t load the live roster.
          <a href="https://github.com/anantdark/FitBuddy/blob/main/config/donors.json">View donors.json</a>
          or support via
          <a href="https://github.com/sponsors/anantdark">GitHub Sponsors</a>.
        </p>`;
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", mountSupporters);
  } else {
    mountSupporters();
  }
})();
