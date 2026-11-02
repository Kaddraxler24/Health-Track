// Icônes de l'appli montre (Wear OS) à partir du logo tools/logo.js
// Usage : node tools/build_wear_icons.js
const {chromium}=require('playwright');const path=require('path');const fs=require('fs');
const RES=path.resolve(__dirname,'..','wear-app','app','src','main','res');
const LOGO=require('./logo.js');
const BG='radial-gradient(circle at 50% 50%, #13505A 0%, #0E3B43 60%)';
const D={mdpi:1,hdpi:1.5,xhdpi:2,xxhdpi:3};
(async()=>{const b=await chromium.launch();const p=await b.newPage();
const shot=async(file,size,scale)=>{ fs.mkdirSync(path.dirname(file),{recursive:true});
  await p.setViewportSize({width:size,height:size});
  await p.setContent(`<html><body style="margin:0;background:transparent"><div style="width:${size}px;height:${size}px;background:${BG};border-radius:50%;display:flex;align-items:center;justify-content:center"><svg width="${Math.round(size*scale)}" height="${Math.round(size*scale)}" viewBox="0 0 38 38">${LOGO('g')}</svg></div></body></html>`);
  await p.screenshot({path:file,omitBackground:true}); };
for(const [d,k] of Object.entries(D)) await shot(path.join(RES,'mipmap-'+d,'ic_launcher.png'),48*k,0.74);
// Aperçu de la tuile (liste des tuiles de la montre)
await p.setViewportSize({width:300,height:300});
await p.setContent(`<html><body style="margin:0;background:transparent"><div style="width:300px;height:300px;border-radius:50%;background:#000;color:#fff;font-family:sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:4px">
<div style="color:#34D3C3;font-weight:700;font-size:20px">Health K'</div><div style="color:#A9BDBA;font-size:17px">BASDAI du jour</div><div style="color:#22C17A;font-weight:700;font-size:52px;line-height:1">3,2</div><div style="color:#B7A8FF;font-size:17px">IMRALDI J-3</div>
<div style="margin-top:10px;background:#0F9F9A;border-radius:30px;padding:12px 26px;font-weight:700;font-size:18px">Faire le BASDAI</div></div></body></html>`);
await p.screenshot({path:path.join(RES,'drawable','tile_preview.png'),omitBackground:true});
await b.close();console.log('icônes montre OK');})();
