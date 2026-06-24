/* Good Drivers Defender — microsite interactions */
(function () {
  "use strict";

  /* ---- starfield ---- */
  var stars = document.getElementById("stars");
  if (stars) {
    var n = window.innerWidth < 700 ? 40 : 90;
    var frag = document.createDocumentFragment();
    for (var i = 0; i < n; i++) {
      var s = document.createElement("i");
      s.style.left = Math.random() * 100 + "%";
      s.style.top = Math.random() * 100 + "%";
      s.style.animationDelay = (Math.random() * 4).toFixed(2) + "s";
      s.style.transform = "scale(" + (0.5 + Math.random() * 1.5).toFixed(2) + ")";
      frag.appendChild(s);
    }
    stars.appendChild(frag);
  }

  /* ---- year ---- */
  var yr = document.getElementById("yr");
  if (yr) yr.textContent = new Date().getFullYear();

  /* ---- parallax background on scroll ---- */
  var layers = [].slice.call(document.querySelectorAll("[data-parallax]"));
  var ticking = false;
  function onScroll() {
    if (ticking) return;
    ticking = true;
    requestAnimationFrame(function () {
      var y = window.pageYOffset;
      layers.forEach(function (el) {
        var sp = parseFloat(el.getAttribute("data-parallax")) || 0;
        el.style.transform = "translate3d(0," + (y * sp).toFixed(1) + "px,0)";
      });
      ticking = false;
    });
  }
  window.addEventListener("scroll", onScroll, { passive: true });

  /* ---- emulator screen switching ---- */
  var screens = [].slice.call(document.querySelectorAll(".screen"));
  var navBtns = [].slice.call(document.querySelectorAll("#emuNav button"));
  function show(name) {
    screens.forEach(function (s) { s.classList.toggle("active", s.getAttribute("data-screen") === name); });
    navBtns.forEach(function (b) { b.classList.toggle("on", b.getAttribute("data-go") === name); });
  }
  navBtns.forEach(function (b) { b.addEventListener("click", function () { show(b.getAttribute("data-go")); }); });
  // tiles inside the dashboard that jump to a screen
  [].slice.call(document.querySelectorAll("[data-go].tool, .tool[data-go]")).forEach(function (t) {
    t.addEventListener("click", function () { show(t.getAttribute("data-go")); });
  });

  /* ---- live speed flicker on the dashboard ---- */
  var spd = document.getElementById("emuSpeed");
  if (spd) {
    var base = 63;
    setInterval(function () {
      var v = base + Math.round((Math.random() - 0.5) * 6);
      spd.textContent = Math.max(0, v);
    }, 1400);
  }

  /* ---- animate the score gauge when it first appears ---- */
  var arc = document.getElementById("gaugeArc");
  if (arc) {
    arc.style.transition = "stroke-dashoffset 1.6s cubic-bezier(.22,1,.36,1)";
    arc.style.strokeDashoffset = "377"; // start empty
    var armed = false;
    var armGauge = function () {
      if (armed) return; armed = true;
      setTimeout(function () { arc.style.strokeDashoffset = "68"; }, 250); // 82% -> 377*(1-0.82)≈68
    };
    var scoreBtn = document.querySelector('#emuNav [data-go="score"]');
    if (scoreBtn) scoreBtn.addEventListener("click", armGauge);
    [].slice.call(document.querySelectorAll('[data-go="score"]')).forEach(function (e) { e.addEventListener("click", armGauge); });
  }

  /* ---- reveal-on-scroll ---- */
  var reveals = [].slice.call(document.querySelectorAll(".reveal"));
  if ("IntersectionObserver" in window) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); }
      });
    }, { threshold: 0.12 });
    reveals.forEach(function (el) { io.observe(el); });
  } else {
    reveals.forEach(function (el) { el.classList.add("in"); });
  }

  /* ---- pointer glow + subtle tilt on feature cards ---- */
  [].slice.call(document.querySelectorAll(".fcard")).forEach(function (card) {
    card.addEventListener("pointermove", function (ev) {
      var r = card.getBoundingClientRect();
      var mx = ((ev.clientX - r.left) / r.width) * 100;
      var my = ((ev.clientY - r.top) / r.height) * 100;
      card.style.setProperty("--mx", mx + "%");
      card.style.setProperty("--my", my + "%");
    });
  });

  /* ---- gentle 3D tilt of the phone toward the cursor ---- */
  var phone = document.getElementById("phone");
  if (phone && window.matchMedia("(pointer:fine)").matches) {
    var hero = phone.closest(".hero");
    hero.addEventListener("pointermove", function (ev) {
      var r = hero.getBoundingClientRect();
      var dx = (ev.clientX - r.left) / r.width - 0.5;
      var dy = (ev.clientY - r.top) / r.height - 0.5;
      phone.style.transform = "perspective(1100px) rotateY(" + (dx * 8).toFixed(2) + "deg) rotateX(" + (-dy * 8).toFixed(2) + "deg)";
    });
    hero.addEventListener("pointerleave", function () {
      phone.style.transform = "perspective(1100px) rotateY(0) rotateX(0)";
    });
  }

  /* ---- scroll progress bar ---- */
  var prog = document.getElementById("progress");
  function updateProgress() {
    var h = document.documentElement;
    var max = h.scrollHeight - h.clientHeight;
    var p = max > 0 ? (window.pageYOffset || h.scrollTop) / max : 0;
    if (prog) prog.style.width = (Math.min(1, Math.max(0, p)) * 100).toFixed(2) + "%";
  }
  window.addEventListener("scroll", updateProgress, { passive: true });
  window.addEventListener("resize", updateProgress, { passive: true });
  updateProgress();

  /* ---- active nav-link highlighting ---- */
  var navMap = {};
  [].slice.call(document.querySelectorAll('.nav-links a[href^="#"]')).forEach(function (a) {
    navMap[a.getAttribute("href").slice(1)] = a;
  });
  if ("IntersectionObserver" in window) {
    var navIO = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        var a = navMap[e.target.id];
        if (a && e.isIntersecting) {
          Object.keys(navMap).forEach(function (k) { navMap[k].classList.remove("active"); });
          a.classList.add("active");
        }
      });
    }, { rootMargin: "-45% 0px -50% 0px" });
    ["demo", "intel", "features", "pricing"].forEach(function (id) {
      var el = document.getElementById(id);
      if (el) navIO.observe(el);
    });
  }
})();
