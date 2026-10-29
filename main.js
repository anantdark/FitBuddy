(() => {
  const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  // Copy buttons on terminal blocks
  document.querySelectorAll("[data-copy]").forEach((block) => {
    const btn = block.querySelector(".copy-btn");
    const code = block.querySelector("code");
    if (!btn || !code) return;
    btn.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(code.textContent || "");
        btn.textContent = "Copied";
        btn.classList.add("copied");
        setTimeout(() => {
          btn.textContent = "Copy";
          btn.classList.remove("copied");
        }, 1600);
      } catch {
        btn.textContent = "Select & copy";
      }
    });
  });

  // Scroll reveal for major sections
  if (!reduce) {
    const targets = document.querySelectorAll(
      ".strip, .features, .steps, .compare, .feature-list li, .step-list li, .supporter-grid"
    );
    targets.forEach((el) => el.classList.add("reveal"));
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("in");
            io.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: "0px 0px -8% 0px" }
    );
    targets.forEach((el) => io.observe(el));
  }

  // Soft header contrast after leaving hero
  const header = document.querySelector(".site-header");
  const hero = document.querySelector(".hero");
  if (header && hero) {
    const onScroll = () => {
      const past = window.scrollY > hero.offsetHeight * 0.55;
      header.style.background = past
        ? "rgba(10, 14, 26, 0.92)"
        : "linear-gradient(to bottom, rgba(10, 14, 26, 0.92), rgba(10, 14, 26, 0))";
      header.style.backdropFilter = past ? "blur(10px)" : "none";
    };
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
  }

  // Mobile nav
  const toggle = document.querySelector(".nav-toggle");
  const nav = document.querySelector(".site-header nav");
  if (toggle && nav) {
    toggle.addEventListener("click", () => {
      const open = toggle.getAttribute("aria-expanded") === "true";
      toggle.setAttribute("aria-expanded", String(!open));
      nav.classList.toggle("is-open", !open);
    });
    nav.querySelectorAll("a").forEach((a) => {
      a.addEventListener("click", () => {
        toggle.setAttribute("aria-expanded", "false");
        nav.classList.remove("is-open");
      });
    });
    window.addEventListener("resize", () => {
      if (window.innerWidth > 640) {
        toggle.setAttribute("aria-expanded", "false");
        nav.classList.remove("is-open");
      }
    });
  }

  // Infinite screenshot marquee
  document.querySelectorAll("[data-carousel]").forEach((root) => {
    const track = root.querySelector(".shot-carousel-track");
    const viewport = root.querySelector(".shot-carousel-viewport");
    const prev = root.querySelector("[data-carousel-prev]");
    const next = root.querySelector("[data-carousel-next]");
    const playBtn = root.querySelector("[data-carousel-toggle]");
    if (!track || !viewport) return;

    const originals = Array.from(track.children);
    if (!originals.length) return;
    originals.forEach((node) => track.appendChild(node.cloneNode(true)));

    const measure = () => {
      const half = track.scrollWidth / 2;
      root.style.setProperty("--carousel-shift", `${half}px`);
      root.style.setProperty("--carousel-duration", `${Math.max(28, half / 42)}s`);
    };
    measure();
    window.addEventListener("resize", measure);
    // Remeasure after images load
    track.querySelectorAll("img").forEach((img) => {
      if (!img.complete) img.addEventListener("load", measure, { once: true });
    });

    if (!reduce) root.classList.add("is-animating");

    let paused = reduce;
    const setPaused = (value) => {
      paused = value;
      root.classList.toggle("is-paused", paused);
      if (playBtn) {
        playBtn.setAttribute("aria-pressed", String(paused));
        playBtn.setAttribute("aria-label", paused ? "Resume auto-scroll" : "Pause auto-scroll");
        playBtn.textContent = paused ? "▶" : "❚❚";
      }
    };
    setPaused(paused);

    playBtn?.addEventListener("click", () => {
      if (reduce) return;
      setPaused(!paused);
    });

    const step = () => (originals[0]?.getBoundingClientRect().width || 220) + 16;
    const scrollBy = (dir) => {
      setPaused(true);
      viewport.scrollBy({ left: dir * step(), behavior: "smooth" });
    };
    prev?.addEventListener("click", () => scrollBy(-1));
    next?.addEventListener("click", () => scrollBy(1));
  });

})();
