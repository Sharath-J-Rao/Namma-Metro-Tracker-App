import json
import os
from pathlib import Path

from fastapi import APIRouter, Depends, HTTPException, Request
from fastapi.responses import HTMLResponse
from fastapi.security import HTTPBasic, HTTPBasicCredentials

from .store import load_config, save_config

router = APIRouter()
security = HTTPBasic()

ADMIN_USER = os.getenv('ADMIN_USER', 'admin')
ADMIN_PASSWORD = os.getenv('ADMIN_PASSWORD', 'change-me')


def require_admin(credentials: HTTPBasicCredentials = Depends(security)):
    if credentials.username != ADMIN_USER or credentials.password != ADMIN_PASSWORD:
        raise HTTPException(status_code=401, detail='Invalid admin credentials', headers={'WWW-Authenticate': 'Basic'})
    return True


@router.get('/admin', response_class=HTMLResponse)
async def admin_page(_: bool = Depends(require_admin)) -> str:
    return '''<!doctype html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Namma Metro Admin</title>
<style>body{font-family:Arial,sans-serif;max-width:900px;margin:30px auto;padding:0 16px;background:#f5f6f8;color:#18202a}section{background:#fff;border-radius:16px;padding:18px;margin:14px 0;box-shadow:0 2px 12px #00000010}input,textarea{width:100%;box-sizing:border-box;padding:10px;border:1px solid #ccd2da;border-radius:10px;margin:6px 0 12px}button{padding:11px 16px;border:0;border-radius:10px;cursor:pointer;margin-right:8px}.primary{background:#1268d0;color:white}.muted{color:#697586}.row{display:grid;grid-template-columns:1fr 1fr;gap:14px}@media(max-width:650px){.row{grid-template-columns:1fr}}label{font-weight:700;font-size:12px}</style></head>
<body><h1>Namma Metro Admin</h1><p class="muted">Edit the configuration once; Android clients download the new version automatically.</p>
<div id="app">Loading…</div>
<script>
let cfg;
const esc=s=>String(s??'').replace(/[&<>\"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;'}[c]));
async function getCfg(){const r=await fetch('/api/config');cfg=await r.json();render();}
function render(){let h='<section><h2>Service notice</h2><label>NOTICE</label><textarea id="notice">'+esc(cfg.notice||'')+'</textarea><button class="primary" onclick="save()">SAVE ALL CHANGES</button><button onclick="location.reload()">RESET</button></section>';
h+='<section><h2>Lines & timings</h2>';
for(const [key,line] of Object.entries(cfg.lines||{})){h+='<div class="row"><div><label>'+esc(line.name)+' FIRST TRAIN</label><input id="first_'+key+'" value="'+esc(line.first_train)+'"></div><div><label>'+esc(line.name)+' LAST TRAIN</label><input id="last_'+key+'" value="'+esc(line.last_train)+'"></div></div>'}
h+='</section><section><h2>Fare slabs</h2><p class="muted">Enter distance in km and fare in INR.</p>';for(let i=0;i<(cfg.fares||[]).length;i++){const f=cfg.fares[i];h+='<div class="row"><div><label>MAX KM</label><input id="km_'+i+'" type="number" step="0.1" value="'+esc(f.max_km)+'"></div><div><label>FARE ₹</label><input id="fare_'+i+'" type="number" step="1" value="'+esc(f.fare)+'"></div></div>'}h+='</section><section><h2>App version</h2><p>Configuration version: <b>'+esc(cfg.version)+'</b> • Updated: '+esc(cfg.updated_at)+'</p></section>';document.getElementById('app').innerHTML=h;}
async function save(){for(const [key,line] of Object.entries(cfg.lines)){line.first_train=document.getElementById('first_'+key).value;line.last_train=document.getElementById('last_'+key).value}for(let i=0;i<cfg.fares.length;i++){cfg.fares[i].max_km=Number(document.getElementById('km_'+i).value);cfg.fares[i].fare=Number(document.getElementById('fare_'+i).value)}cfg.notice=document.getElementById('notice').value;const r=await fetch('/api/admin/config',{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(cfg)});if(!r.ok){alert('Save failed');return}cfg=await r.json();render();alert('Saved. New config version: '+cfg.version)}
getCfg();
</script></body></html>'''
