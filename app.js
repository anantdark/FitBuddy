(function () {
  const STORAGE_KEY = "fitbuddy-theme";

  function systemTheme() {
    return window.matchMedia("(prefers-color-scheme: dark)").matches
      ? "dark"
      : "light";
  }

  function currentTheme() {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === "light" || stored === "dark") return stored;
    return systemTheme();
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    const toggle = document.getElementById("theme-toggle");
    if (toggle) {
      const next = theme === "dark" ? "light" : "dark";
      toggle.setAttribute(
        "aria-label",
        next === "dark" ? "Switch to dark mode" : "Switch to light mode"
      );
      toggle.setAttribute("aria-pressed", theme === "dark" ? "true" : "false");
    }
  }

  function initThemeToggle() {
    applyTheme(currentTheme());
    const toggle = document.getElementById("theme-toggle");
    if (!toggle) return;

    toggle.addEventListener("click", () => {
      const next =
        document.documentElement.getAttribute("data-theme") === "dark"
          ? "light"
          : "dark";
      localStorage.setItem(STORAGE_KEY, next);
      applyTheme(next);
    });

    window
      .matchMedia("(prefers-color-scheme: dark)")
      .addEventListener("change", () => {
        if (!localStorage.getItem(STORAGE_KEY)) applyTheme(systemTheme());
      });
  }

  initThemeToggle();

  function initNavToggle() {
    const header = document.querySelector(".site-header");
    const toggle = document.getElementById("nav-toggle");
    const nav = document.getElementById("site-nav");
    if (!header || !toggle || !nav) return;

    function setOpen(open) {
      header.classList.toggle("is-nav-open", open);
      toggle.setAttribute("aria-expanded", open ? "true" : "false");
      toggle.setAttribute("aria-label", open ? "Close menu" : "Open menu");
    }

    toggle.addEventListener("click", () => {
      setOpen(!header.classList.contains("is-nav-open"));
    });

    nav.querySelectorAll("a").forEach((link) => {
      link.addEventListener("click", () => setOpen(false));
    });

    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape") setOpen(false);
    });

    window.matchMedia("(min-width: 901px)").addEventListener("change", (event) => {
      if (event.matches) setOpen(false);
    });
  }

  initNavToggle();

  function initPhoneCarousel() {
    const root = document.getElementById("phone-carousel");
    if (!root) return;

    const slides = [
      {
        src: "screens/onboarding-ai.png",
        alt: "FitBuddy onboarding — connect an AI provider",
        caption:
          "Step 1 — connect Gemini, OpenRouter, or Ollama. Keys stay on your phone.",
      },
      {
        src: "screens/onboarding-profile.png",
        alt: "FitBuddy onboarding — about you profile form",
        caption:
          "Step 2 — age, height, and weight so targets fit your body.",
      },
      {
        src: "screens/onboarding-goals.png",
        alt: "FitBuddy onboarding — activity level and goal",
        caption:
          "Step 3 — activity and goal; AI sets calorie and macro targets.",
      },
      {
        src: "screens/today.png",
        alt: "FitBuddy Dashboard with calorie ring and meal log",
        caption:
          "Dashboard — pick any day in the week strip; ring, macros, and log follow.",
      },
      {
        src: "screens/log-hub.png",
        alt: "FitBuddy log sheet with photo, text, and workout options",
        caption:
          "Log hub — photo, gallery, freeform text, meals, or a workout (onto the selected day).",
      },
      {
        src: "screens/food-review.png",
        alt: "FitBuddy food review with live macro scaling",
        caption:
          "Review food — change total weight; ingredients and macros rescale live.",
      },
      {
        src: "screens/progress.png",
        alt: "FitBuddy Progress charts for weight and calories",
        caption:
          "Progress — Weekly or Monthly charts, month navigator, and Progress Coach.",
      },
      {
        src: "screens/body.png",
        alt: "FitBuddy Body tab with composition readings",
        caption:
          "Body — composition readings, goals, and saved foods for meals.",
      },
      {
        src: "screens/settings.png",
        alt: "FitBuddy Settings with Gemini AI provider selected",
        caption:
          "Settings — pick a provider, rotate keys, and turn on auto failover.",
      },
    ];

    const img = document.getElementById("carousel-image");
    const caption = document.getElementById("carousel-caption");
    const dots = document.getElementById("carousel-dots");
    const prev = document.getElementById("carousel-prev");
    const next = document.getElementById("carousel-next");
    if (!img || !caption || !dots || !prev || !next) return;

    let index = 3; // start on Today dashboard
    let timer = null;
    const INTERVAL_MS = 4500;

    slides.forEach((slide, i) => {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "carousel-dot";
      btn.setAttribute("role", "tab");
      btn.setAttribute("aria-label", `Show screen ${i + 1}`);
      btn.addEventListener("click", () => goTo(i, true));
      dots.appendChild(btn);
    });

    function render(fade) {
      const slide = slides[index];
      const apply = () => {
        img.src = slide.src;
        img.alt = slide.alt;
        caption.textContent = slide.caption;
        dots.querySelectorAll(".carousel-dot").forEach((dot, i) => {
          dot.setAttribute("aria-selected", i === index ? "true" : "false");
        });
        img.classList.remove("is-fading");
      };

      if (fade) {
        img.classList.add("is-fading");
        window.setTimeout(apply, 180);
      } else {
        apply();
      }
    }

    function goTo(i, userDriven) {
      index = (i + slides.length) % slides.length;
      render(true);
      if (userDriven) restart();
    }

    function restart() {
      if (timer) window.clearInterval(timer);
      timer = window.setInterval(() => goTo(index + 1, false), INTERVAL_MS);
    }

    prev.addEventListener("click", () => goTo(index - 1, true));
    next.addEventListener("click", () => goTo(index + 1, true));

    root.addEventListener("mouseenter", () => {
      if (timer) window.clearInterval(timer);
      timer = null;
    });
    root.addEventListener("mouseleave", restart);
    root.addEventListener("focusin", () => {
      if (timer) window.clearInterval(timer);
      timer = null;
    });
    root.addEventListener("focusout", (event) => {
      if (!root.contains(event.relatedTarget)) restart();
    });

    render(false);
    restart();
  }

  initPhoneCarousel();

  const REPO = "anantdark/FitBuddy";
  const RELEASES_PAGE = `https://github.com/${REPO}/releases/latest`;
  // Same-origin launcher resolves the real asset URL and forces a file download.
  // Never point primary CTAs at /releases/latest (that is the HTML releases page).
  const DOWNLOAD_PAGE = "get-apk.html";
  const STABLE_APK =
    `https://github.com/${REPO}/releases/latest/download/FitBuddy-latest.apk`;

  const buttons = [
    document.getElementById("download-btn"),
    document.getElementById("download-btn-secondary"),
  ].filter(Boolean);
  const versionLine = document.getElementById("version-line");

  function setDownload(url) {
    buttons.forEach((btn) => {
      btn.href = url;
      btn.removeAttribute("download");
    });
  }

  function setLabel(text) {
    buttons.forEach((btn) => {
      btn.textContent = text;
    });
  }

  function wireDirectDownload(apkUrl) {
    buttons.forEach((btn) => {
      btn.href = apkUrl;
      btn.onclick = (event) => {
        // Force a top-level navigation so GitHub soft-nav / in-app browsers
        // cannot keep you on the releases HTML page.
        event.preventDefault();
        window.location.assign(apkUrl);
      };
    });
  }

  if (!buttons.length && !versionLine) return;

  // Safe default: same-origin bounce page (works even if the API call fails).
  setDownload(DOWNLOAD_PAGE);

  fetch(`https://api.github.com/repos/${REPO}/releases/latest?_=${Date.now()}`, {
    cache: "no-store",
    headers: { Accept: "application/vnd.github+json" },
  })
    .then((res) => {
      if (!res.ok) throw new Error("release fetch failed");
      return res.json();
    })
    .then((release) => {
      const assets = release.assets || [];
      const stable = assets.find((a) => a.name === "FitBuddy-latest.apk");
      const versioned = assets.find(
        (a) =>
          /^FitBuddy-.*\.apk$/i.test(a.name || "") &&
          a.name !== "FitBuddy-latest.apk"
      );
      const apk = stable || versioned;
      const apkUrl = (apk && apk.browser_download_url) || STABLE_APK;
      wireDirectDownload(apkUrl);

      const label = release.name || release.tag_name;
      if (label) {
        const short =
          (release.tag_name && release.tag_name.replace(/^v/, "")) || label;
        setLabel(`Download ${short}`);
        if (versionLine) {
          versionLine.hidden = false;
          versionLine.textContent = `Latest: ${label}`;
        }
      }
    })
    .catch(() => {
      wireDirectDownload(STABLE_APK);
      if (versionLine) {
        versionLine.hidden = false;
        versionLine.innerHTML =
          `Latest APK · <a href="${RELEASES_PAGE}">all releases</a>`;
      }
    });
})();
