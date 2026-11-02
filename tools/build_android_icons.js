// Icônes de l'appli Android (Health K') à partir du logo vectoriel tools/logo.js
// Usage : node tools/build_android_icons.js
const {chromium}=require('playwright');const path=require('path');
const RES=path.resolve(__dirname,'..','android-app','android','app','src','main','res');
const LOGO=require('./logo.js');
const BG='radial-gradient(circle at 50% 50%, #13505A 0%, #0E3B43 55%)';
const D={mdpi:1,hdpi:1.5,xhdpi:2,xxhdpi:3,xxxhdpi:4};
(async()=>{const b=await chromium.launch();const p=await b.newPage();
const shot=async(file,size,scale,bg,round)=>{
  await p.setViewportSize({width:size,height:size});
  await p.setContent(`<html><body style="margin:0;width:${size}px;height:${size}px;background:transparent;"><div style="width:${size}px;height:${size}px;background:${bg};border-radius:${round?'50%':'0'};display:flex;align-items:center;justify-content:center;overflow:hidden"><svg width="${Math.round(size*scale)}" height="${Math.round(size*scale)}" viewBox="0 0 38 38">${LOGO('g')}</svg></div></body></html>`);
  await p.screenshot({path:file,omitBackground:true});
};
for(const [d,k] of Object.entries(D)){
  const dir=path.join(RES,'mipmap-'+d);
  await shot(path.join(dir,'ic_launcher.png'),48*k,0.84,BG,false);
  await shot(path.join(dir,'ic_launcher_round.png'),48*k,0.80,BG,true);
  await shot(path.join(dir,'ic_launcher_foreground.png'),108*k,0.56,'transparent',false); // zone de sécurité 66/108
}
await b.close();console.log('icônes Android OK');})();
