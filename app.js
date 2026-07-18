(function () {
  const REPO = "anantdark/FitBuddy";
  const FALLBACK = `https://github.com/${REPO}/releases/latest`;
  const buttons = [
    document.getElementById("download-btn"),
    document.getElementById("download-btn-secondary"),
  ].filter(Boolean);
  const versionLine = document.getElementById("version-line");

  function setHref(url) {
    buttons.forEach((btn) => {
      btn.href = url;
    });
  }

  fetch(`https://api.github.com/repos/${REPO}/releases/latest`)
    .then((res) => {
      if (!res.ok) throw new Error("release fetch failed");
      return res.json();
    })
    .then((release) => {
      const apk = (release.assets || []).find((a) =>
        /\.apk$/i.test(a.name || "")
      );
      if (apk && apk.browser_download_url) {
        setHref(apk.browser_download_url);
      } else {
        setHref(release.html_url || FALLBACK);
      }
      if (versionLine && release.tag_name) {
        const label = release.name || release.tag_name;
        versionLine.hidden = false;
        versionLine.textContent = `Latest: ${label}`;
      }
    })
    .catch(() => {
      setHref(FALLBACK);
    });
})();
