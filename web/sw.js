// Minimal offline shell cache — deliberately narrow. Only ever serves the
// app's own static files; every other request (Gemini API calls,
// tel:/sms:/wa.me links, YouTube) always goes straight to the network.
//
// Network-first, not cache-first: a returning visitor should always see
// the latest deployed UI while online, and only fall back to the cached
// shell when actually offline. (An earlier cache-first version of this
// file meant every UI update was invisible to returning visitors until
// the cache name was bumped by hand — bumping it here once to clear that
// stale cache, but the strategy change below is what actually fixes it
// going forward, not the version bump.)
const CACHE_NAME = 'lifeos-shell-v2';
const SHELL_FILES = [
  './',
  './index.html',
  './manifest.json',
  './css/style.css',
  './js/app.js',
  './icons/icon.svg',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(SHELL_FILES)).catch(() => {}),
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))),
    ),
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  // Only handle same-origin GET requests for the app shell — never touch
  // cross-origin calls (Gemini's API, WhatsApp/YouTube links, etc.).
  if (event.request.method !== 'GET' || url.origin !== self.location.origin) return;

  event.respondWith(
    fetch(event.request)
      .then((fresh) => {
        const copy = fresh.clone();
        caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy)).catch(() => {});
        return fresh;
      })
      .catch(() => caches.match(event.request)),
  );
});
