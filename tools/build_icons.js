// Icônes de l'appli (écran d'accueil, onglet, iPhone) à partir du logo vectoriel tools/logo.js
// Usage : node tools/build_icons.js
const {chromium}=require('playwright');const path=require('path');const fs=require('fs');
const ROOT=path.resolve(__dirname,'..');const LOGO=require('./logo.js');
const BG='radial-gradient(circle at 50% 28%, #2e3456 0%, #1c1f2e 58%, #141622 100%)';
const ICONS=[ // [fichier, taille, part du logo, fond]
  ['icon-192.png',192,0.84,BG], ['icon-512.png',512,0.84,BG],
  ['icon-maskable-512.png',512,0.62,BG],      // zone de sécurité Android (cercle 80 %)
  ['apple-touch-icon.png',180,0.80,BG],       // iPhone / iPad
  ['favicon-32.png',32,1,'transparent'],
];
(async()=>{const b=await chromium.launch();const p=await b.newPage();
for(const [name,size,scale,bg] of ICONS){
  const lg=Math.round(size*scale);
  await p.setViewportSize({width:size,height:size});
  await p.setContent(`<html><body style="margin:0;width:${size}px;height:${size}px;background:${bg};display:flex;align-items:center;justify-content:center;"><svg width="${lg}" height="${lg}" viewBox="0 0 38 38" shape-rendering="geometricPrecision">${LOGO('g')}</svg></body></html>`);
  await p.screenshot({path:path.join(ROOT,name),omitBackground:bg==='transparent'});
}
fs.writeFileSync(path.join(ROOT,'favicon.svg'),`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 38 38">${LOGO('g')}</svg>\n`);
await b.close();console.log('icônes OK');})();
