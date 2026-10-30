// Icônes PWA à partir du logo de l'appli
const {chromium}=require('playwright');const path=require('path');const ROOT=path.resolve(__dirname,'..');
const LOGO=`<polygon points="19,2 34,10.5 34,27.5 19,36 4,27.5 4,10.5" fill="#c0392b" stroke="#e74c3c" stroke-width="1"/><path d="M11 19 C11 15, 15 12, 19 17 C23 12, 27 15, 27 19 C27 23, 23 26, 19 29 C15 26, 11 23, 11 19Z" fill="none" stroke="white" stroke-width="1.6" stroke-linejoin="round"/><polyline points="8,19 12,19 14,14 16,24 18,17 20,21 22,19 30,19" stroke="white" stroke-width="1.4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>`;
(async()=>{const b=await chromium.launch();const p=await b.newPage();
for(const [name,size,scale] of [['icon-192.png',192,0.78],['icon-512.png',512,0.78],['icon-maskable-512.png',512,0.56]]){
 const lg=Math.round(size*scale);
 await p.setViewportSize({width:size,height:size});
 await p.setContent(`<html><body style="margin:0;width:${size}px;height:${size}px;background:#1c1f2e;display:flex;align-items:center;justify-content:center;"><svg width="${lg}" height="${lg}" viewBox="0 0 38 38">${LOGO}</svg></body></html>`);
 await p.screenshot({path:path.join(ROOT,name)});}
await b.close();console.log('icônes OK');})();
