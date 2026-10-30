// Produit mes_donnees.json : toutes les données perso de la version actuelle (à importer dans l'appli)
const {chromium}=require('playwright');const fs=require('fs');
const [,,srcPath,persoPath,outPath]=process.argv;
if(!outPath){console.log('Usage : node tools/build_data.js <source.html> <perso_html.json> <mes_donnees.json>');process.exit(1);}
(async()=>{const b=await chromium.launch();const p=await b.newPage();
await p.route(/fonts\.(googleapis|gstatic)/,r=>r.abort());
await p.goto('file://'+require('path').resolve(srcPath),{waitUntil:'domcontentloaded'});await p.waitForTimeout(800);
const D=await p.evaluate(()=>{ const x=JSON.parse(JSON.stringify(D)); x._persoJs=PERSO_HTML; return x; });
const perso=JSON.parse(fs.readFileSync(persoPath,'utf8'));
D.perso={...perso,...D._persoJs}; delete D._persoJs;
D._export={app:'Suivi Santé',date:new Date().toISOString()};
fs.writeFileSync(outPath,JSON.stringify(D,null,1));
console.log('données :',Object.entries(D).map(([k,v])=>k+'='+(Array.isArray(v)?v.length:typeof v)).join(' '));
await b.close();})();
