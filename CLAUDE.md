# Health K' (ex-Suivi Santé) — consignes de maintenance

Deux versions vivent en parallèle et doivent **toujours** être livrées ensemble à chaque modification :

1. **Le fichier HTML perso** `sante_tracker_desktop.html` — c'est la **source** (il contient les données
   personnelles pré-remplies). Il n'est **jamais** dans ce dépôt : l'utilisateur le joint à la conversation
   et le reçoit en retour (SendUserFile) après chaque modification.
2. **L'appli (PWA)** publiée ici sur GitHub Pages (`https://kaddraxler24.github.io/Health-Track/`), générée
   à partir de la source, **sans aucune donnée personnelle**.

## Règles de confidentialité (dépôt public)

- Ne jamais committer la source HTML, `mes_donnees.json`, ni aucune donnée médicale, date, nom de labo ou de médecin.
- Toute donnée perso dans la source doit être dans une région marquée :
  - JS : `//<PERSO:nom>` … `//</PERSO:nom>` → remplacée par le stub `STUBS[nom]` de `tools/build_app.py`
  - HTML : conteneur `data-perso="cle"` + `<!--PERSO:cle-->…<!--/PERSO:cle-->` → remplacé par `EMPTY[cle]`
- Nouvelle région perso ⇒ ajouter son stub / contenu vide dans `tools/build_app.py`.
- `build_app.py` échoue si une date `AAAA-MM-JJ` (hors liste blanche) se retrouve dans la version publique.

## Sécurité du code (à respecter)

- Toute donnée venant de l'extérieur (import `.json`, OCR, IA, fichiers montre) passe par `sanitizeData()` ;
  le HTML du dossier perso passe par `sanitizeHtml()` (seuls quelques `onclick` connus sont gardés, voir `PERSO_HANDLERS`).
- Politique de sécurité (balise `Content-Security-Policy` dans la source) : toute nouvelle adresse externe
  (bibliothèque, API) doit y être ajoutée, sinon elle sera bloquée.
- Dates : toujours `toISODay(new Date())` (heure locale), jamais `toISOString()` pour une date du jour.
- Le service worker n'intercepte pas les fichiers `.apk`, `.json`, `.pdf`.

## Procédure à chaque modification

1. Modifier la source (`sante_tracker_desktop.html`, jointe par l'utilisateur).
2. Générer l'appli : `python3 tools/build_app.py <source.html> <tmp>/perso_html.json`
   (icônes : `node tools/build_icons.js` si le logo `tools/logo.js` change).
3. Tester (Playwright) **les deux** versions : la source en `file://`, et l'appli servie en local
   (`python3 -m http.server`) — appli vierge sans données perso, import de `mes_donnees.json`, mobile 390 px.
4. Committer/pousser `index.html` + `sw.js` (et fichiers modifiés) sur `main` → l'appli installée se met à jour seule.
5. Renvoyer à l'utilisateur la source HTML mise à jour.
6. Si de nouvelles données pré-remplies ont été ajoutées dans la source :
   `node tools/build_data.js <source.html> <tmp>/perso_html.json mes_donnees.json` et l'envoyer (à importer dans l'appli).

## Appli Android (`android-app/`) — synchro Galaxy Watch via Santé Connect

- Projet Capacitor qui **affiche la version en ligne** (`server.url` = GitHub Pages) : toute modification web
  publiée arrive aussi dans l'appli Android, **sans nouvel APK**.
- Le code web détecte l'appli Android avec `IS_NATIVE` et appelle les modules natifs par
  `Capacitor.nativePromise('Health' | 'Filesystem' | 'Share', méthode, options)`.
  Synchro : `hcSync()` (sommeil, FC, pas, SpO2 → `D.wearable` source « Santé Connect » ; poids et masse grasse → `D.weights`).
  Fichiers (export JSON, PDF) : toujours passer par `saveFile(blob, nom)` (les téléchargements ne marchent pas dans l'appli Android).
- Un **nouvel APK** n'est nécessaire que si `android-app/` change (permissions, modules, icônes, réglages natifs).
  Le workflow `.github/workflows/android.yml` le construit et le publie à
  `https://github.com/Kaddraxler24/Health-Track/releases/download/android/health-k.apk`.
  En cas d'échec, le journal Gradle est poussé sur la branche `build-logs` (`git fetch origin build-logs`).
- La clé de signature `android-app/health-k-release.keystore` ne doit **jamais** changer, sinon l'APK ne s'installe plus par-dessus l'ancien.
- Modules natifs : Health (Santé Connect), LocalNotifications (rappels, boutons d'action), Filesystem + Share (fichiers, sauvegarde auto),
  App (liens `healthk://express|morning|temp|injection|note` du widget et des raccourcis), NativeBiometric (verrouillage),
  et `HealthKWidget` (plugin maison `android-app/android/app/src/main/java/.../WidgetPlugin.java` + `HealthKWidget.java`, widget d'écran d'accueil).
  Le code web tolère un ancien APK (module absent → la fonction est simplement ignorée, jamais de blocage).
- Icônes Android : `node tools/build_android_icons.js`. Tests : simuler `window.Capacitor` (isNativePlatform + nativePromise) dans Playwright.
