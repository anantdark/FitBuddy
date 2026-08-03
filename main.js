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
      ".strip, .features, .steps, .compare, .feature-list li, .step-list li"
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

})();
