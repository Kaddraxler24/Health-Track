// Health K' — service worker (version 3b8fe51727)
// Réseau d'abord : chaque ouverture en ligne charge la dernière version publiée ;
// hors connexion, la dernière version téléchargée est utilisée.
const CACHE = 'suivi-sante-3b8fe51727';
const ASSETS = ['./', './index.html', './manifest.webmanifest', './icon-192.png', './icon-512.png', './icon-maskable-512.png', './apple-touch-icon.png', './favicon-32.png', './favicon.svg'];
self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ASSETS)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys()
    .then(ks => Promise.all(ks.filter(k => k !== CACHE && k !== 'suivi-sante-lib').map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
});
self.addEventListener('fetch', e => {
  const r = e.request;
  if (r.method !== 'GET') return;
  const u = new URL(r.url);
  // Bibliothèques (PDF, lecture d'image) : gardées en cache pour fonctionner hors connexion
  if (u.hostname === 'cdnjs.cloudflare.com') {
    e.respondWith(caches.open('suivi-sante-lib').then(c => c.match(r).then(m => m || fetch(r).then(res => { if (res.ok) c.put(r, res.clone()); return res; }))));
    return;
  }
  if (u.origin !== location.origin) return;
  // Fichiers à télécharger (appli Android) : jamais interceptés ni mis en cache
  if (/\.(apk|json|pdf)$/i.test(u.pathname)) return;
  // Appli Android : l'en-tête Accept des pages permet d'y injecter le pont vers Santé Connect
  const init = { cache: 'no-cache' };
  if (r.mode === 'navigate') init.headers = { 'Accept': r.headers.get('Accept') || 'text/html' };
  e.respondWith(
    fetch(r.url, init)
      .then(res => { if (res.ok && res.type === 'basic') { const cp = res.clone(); caches.open(CACHE).then(c => c.put(r, cp)); } return res; })
      .catch(() => caches.match(r, { ignoreSearch: true }).then(m => m || caches.match('./index.html')))
  );
});
