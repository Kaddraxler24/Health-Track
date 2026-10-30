#!/usr/bin/env python3
"""Construit la version publique (sans données personnelles) de Suivi Santé.

Usage : python3 tools/build_app.py <source.html> <perso_html.json>
  source.html     : la version perso (sante_tracker_desktop.html, AVEC données)
  perso_html.json : fichier temporaire où sont extraits les blocs perso (hors dépôt)
Sortie : la racine du dépôt (index.html, manifest.webmanifest, sw.js).
Les données perso (mes_donnees.json) sont produites par build_data.js — JAMAIS dans le dépôt.
"""
import hashlib, json, re, sys
from pathlib import Path

if len(sys.argv) != 3: sys.exit(__doc__)
SRC = Path(sys.argv[1])
OUT = Path(__file__).resolve().parent.parent
PERSO_OUT = Path(sys.argv[2])

s = SRC.read_text(encoding='utf-8')

# ── 1. Régions JS personnelles → versions vides ──
STUBS = {
    'anchor':  "const INJ_DEFAULT_ANCHOR = null;",
    'imraldi': "const PRESC = null;\nconst CFG_DEFAULT = {};\nconst PERSO_HTML = {};\n"
               "function injLog() { if(!Array.isArray(D.injLog)) D.injLog=[]; return D.injLog; }\n"
               "function seedImraldi() {}",
    'fixed':   "const STATIC_FIXED_ROWS = {};",
    'seeds':   "function seedData() {}",
}
found = re.findall(r'//<PERSO:(\w+)>', s)
assert sorted(found) == sorted(STUBS), f'régions JS inattendues : {found}'
for name, stub in STUBS.items():
    s, n = re.subn(rf'//<PERSO:{name}>.*?//</PERSO:{name}>', lambda m: stub, s, flags=re.S)
    assert n == 1, name

# ── 2. Blocs HTML personnels → contenu générique ──
EMPTY = {
    'calpCard':      '<div class="stat-label" style="font-size:9px;">Calprotectine</div><div class="stat-value sv-green" style="font-size:18px;">—</div>',
    'bioRemarks':    '<div style="font-size:12px;color:var(--t3);">Aucune remarque pour le moment.</div>',
    'calpRowStatic': '',
    'fixedRows':     '',
    'consultNext':   '',
    'history':       '<div class="card"><div class="empty-state"><div class="empty-title">Profil et chronologie</div>'
                     '<div style="font-size:12px;color:var(--t3);">Importe ton fichier de données (Plus → Importer) pour les afficher.</div></div></div>',
    'weightAdvice':  '<div style="font-size:12px;">Objectif à définir avec ton médecin.</div>',
}
perso = {}
def _cut(m):
    key, inner = m.group(1), m.group(2)
    perso[key] = inner
    return f'<!--PERSO:{key}-->{EMPTY[key]}<!--/PERSO:{key}-->'
s = re.sub(r'<!--PERSO:(\w+)-->(.*?)<!--/PERSO:\1-->', _cut, s, flags=re.S)
assert sorted(perso) == sorted(EMPTY), f'blocs HTML inattendus : {sorted(perso)}'
for key in perso:
    s, n = re.subn(rf'data-perso="{key}"', f'data-perso="{key}" data-perso-public', s)
    assert n == 1, key

# ── 3. PWA : manifeste, icônes, service worker ──
HEAD = '''<link rel="manifest" href="manifest.webmanifest">
<meta name="theme-color" content="#1c1f2e">
<meta name="mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
<link rel="icon" type="image/svg+xml" href="favicon.svg">
<link rel="icon" type="image/png" sizes="32x32" href="favicon-32.png">
<link rel="apple-touch-icon" sizes="180x180" href="apple-touch-icon.png">
</head>'''
assert s.count('</head>') == 1
s = s.replace('</head>', HEAD, 1)
SW_REG = '''<script>
if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
  window.addEventListener('load', () => navigator.serviceWorker.register('sw.js').catch(() => {}));
}
</script>
</body>'''
assert s.count('</body>') == 1
s = s.replace('</body>', SW_REG, 1)

# ── Garde-fou confidentialité : aucune trace de région perso ni de date (hors liste blanche) ──
assert '<PERSO:' not in s.replace('<!--PERSO:', ''), 'marqueur de région perso restant'
ALLOWED_DATES = {'2023-06-01'}   # version d'API Anthropic
leak = sorted(set(re.findall(r"20[0-9]{2}-[01][0-9]-[0-3][0-9]", s)) - ALLOWED_DATES)
assert not leak, f'dates présentes dans la version publique : {leak}'

OUT.mkdir(parents=True, exist_ok=True)
(OUT / 'index.html').write_text(s, encoding='utf-8')

ver = hashlib.sha1(s.encode()).hexdigest()[:10]
(OUT / 'sw.js').write_text(f'''// Suivi Santé — service worker (version {ver})
// Réseau d'abord : chaque ouverture en ligne charge la dernière version publiée ;
// hors connexion, la dernière version téléchargée est utilisée.
const CACHE = 'suivi-sante-{ver}';
const ASSETS = ['./', './index.html', './manifest.webmanifest', './icon-192.png', './icon-512.png', './icon-maskable-512.png', './apple-touch-icon.png', './favicon-32.png', './favicon.svg'];
self.addEventListener('install', e => {{
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ASSETS)).then(() => self.skipWaiting()));
}});
self.addEventListener('activate', e => {{
  e.waitUntil(caches.keys()
    .then(ks => Promise.all(ks.filter(k => k !== CACHE && k !== 'suivi-sante-lib').map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
}});
self.addEventListener('fetch', e => {{
  const r = e.request;
  if (r.method !== 'GET') return;
  const u = new URL(r.url);
  // Bibliothèques (PDF, lecture d'image) : gardées en cache pour fonctionner hors connexion
  if (u.hostname === 'cdnjs.cloudflare.com') {{
    e.respondWith(caches.open('suivi-sante-lib').then(c => c.match(r).then(m => m || fetch(r).then(res => {{ if (res.ok) c.put(r, res.clone()); return res; }}))));
    return;
  }}
  if (u.origin !== location.origin) return;
  e.respondWith(
    fetch(r.url, {{ cache: 'no-cache' }})
      .then(res => {{ if (res.ok) {{ const cp = res.clone(); caches.open(CACHE).then(c => c.put(r, cp)); }} return res; }})
      .catch(() => caches.match(r, {{ ignoreSearch: true }}).then(m => m || caches.match('./index.html')))
  );
}});
''', encoding='utf-8')

(OUT / 'manifest.webmanifest').write_text(json.dumps({
    "name": "Suivi Santé", "short_name": "Suivi Santé", "lang": "fr",
    "start_url": "./", "scope": "./", "display": "standalone", "orientation": "portrait",
    "background_color": "#f7f5f0", "theme_color": "#1c1f2e",
    "icons": [
        {"src": "icon-192.png", "sizes": "192x192", "type": "image/png", "purpose": "any"},
        {"src": "icon-512.png", "sizes": "512x512", "type": "image/png", "purpose": "any"},
        {"src": "icon-maskable-512.png", "sizes": "512x512", "type": "image/png", "purpose": "maskable"},
    ],
}, ensure_ascii=False, indent=2), encoding='utf-8')

PERSO_OUT.write_text(json.dumps(perso, ensure_ascii=False), encoding='utf-8')
print(f'OK — index.html {len(s)//1024} Ko, version {ver}, {len(perso)} blocs perso extraits')
