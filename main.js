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

  // Infinite screenshot marquee (JS-driven — no CSS animation / hover traps)
  document.querySelectorAll("[data-carousel]").forEach((root) => {
    const track = root.querySelector(".shot-carousel-track");
    const viewport = root.querySelector(".shot-carousel-viewport");
    const prev = root.querySelector("[data-carousel-prev]");
    const next = root.querySelector("[data-carousel-next]");
    const playBtn = root.querySelector("[data-carousel-toggle]");
    if (!track || !viewport) return;

    const originals = Array.from(track.children);
    if (!originals.length) return;
    // Duplicate set for seamless loop
    originals.forEach((node) => track.appendChild(node.cloneNode(true)));

    let offset = 0;
    let loopWidth = 0;
    let playing = !reduce;
    let hovering = false;
    let raf = 0;
    let lastTs = 0;
    const speed = 42; // px/sec

    const measure = () => {
      // First half of the duplicated track
      loopWidth = track.scrollWidth / 2;
      if (loopWidth > 0) offset = ((offset % loopWidth) + loopWidth) % loopWidth;
      track.style.transform = `translate3d(${-offset}px, 0, 0)`;
    };

    const paint = (ts) => {
      if (!lastTs) lastTs = ts;
      const dt = Math.min(0.064, (ts - lastTs) / 1000);
      lastTs = ts;
      if (playing && !hovering && loopWidth > 0) {
        offset += speed * dt;
        if (offset >= loopWidth) offset -= loopWidth;
        track.style.transform = `translate3d(${-offset}px, 0, 0)`;
      }
      raf = requestAnimationFrame(paint);
    };

    const syncPlayUi = () => {
      if (!playBtn) return;
      playBtn.classList.toggle("is-playing", playing);
      playBtn.setAttribute("aria-pressed", String(!playing));
      playBtn.setAttribute("aria-label", playing ? "Pause auto-scroll" : "Resume auto-scroll");
      playBtn.textContent = playing ? "❚❚" : "▶";
    };

    const setPlaying = (value) => {
      if (reduce) {
        playing = false;
      } else {
        playing = value;
      }
      lastTs = 0;
      syncPlayUi();
    };

    const stepPx = () => {
      const first = originals[0];
      return (first ? first.getBoundingClientRect().width : 220) + 16;
    };

    const nudge = (dir) => {
      if (loopWidth <= 0) return;
      offset += dir * stepPx();
      offset = ((offset % loopWidth) + loopWidth) % loopWidth;
      track.style.transform = `translate3d(${-offset}px, 0, 0)`;
    };

    measure();
    syncPlayUi();
    window.addEventListener("resize", measure);
    track.querySelectorAll("img").forEach((img) => {
      if (!img.complete) img.addEventListener("load", measure, { once: true });
      else measure();
    });

    // Soft-pause while pointer is over the image strip only (not the buttons)
    viewport.addEventListener("pointerenter", () => {
      hovering = true;
    });
    viewport.addEventListener("pointerleave", () => {
      hovering = false;
      lastTs = 0;
    });

    playBtn?.addEventListener("click", () => {
      if (reduce) return;
      setPlaying(!playing);
    });
    prev?.addEventListener("click", () => nudge(-1));
    next?.addEventListener("click", () => nudge(1));

    if (!reduce) raf = requestAnimationFrame(paint);
    // If reduced motion, keep track static but still allow manual nudges via overflow scroll fallback
  });

  // Screenshot lightbox / fullscreen zoom
  (() => {
    const lightbox = document.querySelector("[data-lightbox]");
    if (!lightbox) return;
    const img = lightbox.querySelector("[data-lightbox-img]");
    const caption = lightbox.querySelector("[data-lightbox-caption]");
    const btnClose = lightbox.querySelector("[data-lightbox-close]");
    const btnPrev = lightbox.querySelector("[data-lightbox-prev]");
    const btnNext = lightbox.querySelector("[data-lightbox-next]");

    // Only original (non-cloned) hits at init time — re-query live for clones after carousel dup
    const collect = () =>
      Array.from(document.querySelectorAll(".shot-carousel-track .shot-hit")).filter(
        (el, idx, arr) => {
          // De-dupe by src so clones don't double the gallery
          const src = el.getAttribute("data-lightbox-src");
          return arr.findIndex((x) => x.getAttribute("data-lightbox-src") === src) === idx;
        }
      );

    let index = 0;
    let lastFocus = null;

    const openAt = (i) => {
      const items = collect();
      if (!items.length) return;
      index = ((i % items.length) + items.length) % items.length;
      const el = items[index];
      const src = el.getAttribute("data-lightbox-src");
      const alt = el.getAttribute("data-lightbox-alt") || "";
      if (img) {
        img.src = src;
        img.alt = alt;
      }
      if (caption) caption.textContent = alt;
      lightbox.hidden = false;
      lightbox.classList.add("is-open");
      document.body.classList.add("lightbox-open");
      lastFocus = document.activeElement;
      btnClose?.focus();
    };

    const close = () => {
      lightbox.classList.remove("is-open");
      document.body.classList.remove("lightbox-open");
      window.setTimeout(() => {
        lightbox.hidden = true;
        if (img) img.removeAttribute("src");
      }, 180);
      if (lastFocus && lastFocus.focus) lastFocus.focus();
    };

    document.addEventListener("click", (e) => {
      const hit = e.target.closest?.(".shot-hit");
      if (!hit) return;
      e.preventDefault();
      const items = collect();
      const src = hit.getAttribute("data-lightbox-src");
      const i = items.findIndex((x) => x.getAttribute("data-lightbox-src") === src);
      openAt(i >= 0 ? i : 0);
    });

    btnClose?.addEventListener("click", close);
    btnPrev?.addEventListener("click", () => openAt(index - 1));
    btnNext?.addEventListener("click", () => openAt(index + 1));
    img?.addEventListener("click", close);
    lightbox.addEventListener("click", (e) => {
      if (e.target === lightbox || e.target.classList.contains("shot-lightbox-stage")) close();
    });

    window.addEventListener("keydown", (e) => {
      if (!lightbox.classList.contains("is-open")) return;
      if (e.key === "Escape") close();
      if (e.key === "ArrowLeft") openAt(index - 1);
      if (e.key === "ArrowRight") openAt(index + 1);
    });
  })();

})();
