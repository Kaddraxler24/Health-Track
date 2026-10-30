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
