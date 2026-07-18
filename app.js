(function () {
  const REPO = "anantdark/FitBuddy";
  const RELEASES_PAGE = `https://github.com/${REPO}/releases/latest`;
  // Stable alias uploaded by CI on each release (see release.yml).
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
    });
  }

  function setLabel(text) {
    buttons.forEach((btn) => {
      btn.textContent = text;
    });
  }

  if (!buttons.length && !versionLine) return;

  // Bust caches so each visit sees the current GitHub "latest" release.
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
      if (apk && apk.browser_download_url) {
        setDownload(apk.browser_download_url);
      } else if (stable || assets.some((a) => a.name === "FitBuddy-latest.apk")) {
        setDownload(STABLE_APK);
      } else {
        setDownload(release.html_url || RELEASES_PAGE);
      }

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
      // Prefer stable alias if CI has published it; otherwise the releases page.
      fetch(STABLE_APK, { method: "HEAD", redirect: "follow", cache: "no-store" })
        .then((res) => {
          setDownload(res.ok ? STABLE_APK : RELEASES_PAGE);
        })
        .catch(() => setDownload(RELEASES_PAGE));
      if (versionLine) {
        versionLine.hidden = false;
        versionLine.innerHTML =
          `See <a href="${RELEASES_PAGE}">GitHub Releases</a> for the newest build.`;
      }
    });
})();
