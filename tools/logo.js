// Logo Suivi Santé — dessiné pour rester net de 16 px à 1024 px
module.exports = (id='lg') => `<defs><linearGradient id="${id}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ef5b4a"/><stop offset="1" stop-color="#b8322a"/></linearGradient></defs>
<path d="M19 2.6 32.9 10.6v16.8L19 35.4 5.1 27.4V10.6Z" fill="url(#${id})" stroke="url(#${id})" stroke-width="2.4" stroke-linejoin="round"/>
<path d="M19 28.2c-4.8-3.2-8.4-6.4-8.4-10.4 0-2.7 2-4.7 4.4-4.7 1.7 0 3.1.9 4 2.4.9-1.5 2.3-2.4 4-2.4 2.4 0 4.4 2 4.4 4.7 0 4-3.6 7.2-8.4 10.4Z" fill="none" stroke="#fff" stroke-width="2.1" stroke-linejoin="round"/>
<path d="M8.4 19.4h5l1.6-3.4 2.4 6.6 2.2-4.8 1.2 1.6h8.8" fill="none" stroke="#fff" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round"/>`;
