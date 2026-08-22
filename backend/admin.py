import hmac
import os

from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import HTMLResponse
from fastapi.security import HTTPBasic, HTTPBasicCredentials

from store import load_config, save_config, history

router = APIRouter()
security = HTTPBasic()
ADMIN_USER = os.getenv('ADMIN_USER', '').strip()
ADMIN_PASSWORD = os.getenv('ADMIN_PASSWORD', '')


def require_admin(credentials: HTTPBasicCredentials = Depends(security)):
    if not ADMIN_USER or not ADMIN_PASSWORD or ADMIN_PASSWORD == 'change-me':
        raise HTTPException(status_code=503, detail='Admin credentials are not configured')
    valid_user = hmac.compare_digest(credentials.username, ADMIN_USER)
    valid_password = hmac.compare_digest(credentials.password, ADMIN_PASSWORD)
    if not (valid_user and valid_password):
        raise HTTPException(status_code=401, detail='Invalid admin credentials', headers={'WWW-Authenticate': 'Basic realm="Namma Metro Admin"'})
    return True


@router.get('/admin', response_class=HTMLResponse)
async def admin_page(_: bool = Depends(require_admin)) -> str:
    return '''<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="color-scheme" content="light"><title>Namma Metro Admin</title>
<style>
:root{font-family:Inter,system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;color:#17202a;background:#f4f6f8;line-height:1.45}*{box-sizing:border-box}body{margin:0}.shell{max-width:980px;margin:auto;padding:20px 16px 48px}.top{display:flex;justify-content:space-between;align-items:end;gap:16px;margin:8px 0 20px}.eyebrow{font-size:12px;font-weight:800;letter-spacing:.09em;text-transform:uppercase;color:#667085}.top h1{margin:3px 0;font-size:28px}.status{background:#fff;border:1px solid #e4e7ec;border-radius:999px;padding:8px 12px;font-size:13px;white-space:nowrap}.card{background:#fff;border:1px solid #e4e7ec;border-radius:18px;padding:20px;margin:14px 0;box-shadow:0 4px 18px #1018280a}.card h2{font-size:18px;margin:0 0 4px}.muted{color:#667085;font-size:14px;margin:0 0 16px}.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.field label{display:block;font-size:12px;font-weight:750;margin-bottom:6px;color:#475467}.field input,.field textarea{width:100%;border:1px solid #d0d5dd;border-radius:11px;padding:11px 12px;font:inherit;background:#fff}.field input:focus,.field textarea:focus{outline:3px solid #dbeafe;border-color:#2563eb}.field textarea{min-height:90px;resize:vertical}.line{padding:14px;border:1px solid #eaecf0;border-radius:14px;margin-top:12px}.line-head{display:flex;justify-content:space-between;align-items:center;gap:10px;margin-bottom:10px}.badge{font-size:12px;font-weight:800;padding:5px 9px;border-radius:999px;background:#f2f4f7}.actions{position:sticky;bottom:12px;display:flex;justify-content:flex-end;gap:10px;background:#ffffffee;backdrop-filter:blur(10px);border:1px solid #e4e7ec;border-radius:16px;padding:10px;margin-top:16px}.button{border:0;border-radius:11px;padding:11px 16px;font:inherit;font-weight:750;cursor:pointer}.primary{background:#111827;color:#fff}.secondary{background:#f2f4f7;color:#344054}.danger{background:#fef3f2;color:#b42318}.button:disabled{opacity:.5;cursor:not-allowed}.history{display:grid;gap:8px}.history-item{display:flex;justify-content:space-between;gap:10px;padding:10px 0;border-bottom:1px solid #eaecf0;font-size:13px}.toast{position:fixed;right:16px;bottom:16px;max-width:360px;background:#101828;color:#fff;padding:12px 14px;border-radius:12px;display:none;box-shadow:0 10px 30px #0003}.error{color:#b42318;font-size:13px;margin-top:6px}.notice{border-left:4px solid #2563eb;padding-left:12px}@media(max-width:700px){.shell{padding:14px 12px 40px}.top{align-items:flex-start;flex-direction:column}.grid{grid-template-columns:1fr}.actions{justify-content:stretch}.actions .button{flex:1}}
</style></head><body><main class="shell">
<header class="top"><div><div class="eyebrow">Namma Metro · Admin</div><h1>Service configuration</h1><p class="muted">One controlled place to publish fares, service times and passenger notices.</p></div><div class="status" id="status">Loading…</div></header>
<div id="app"><div class="card">Loading configuration…</div></div><div id="toast" class="toast"></div>
<script>
let cfg=null,dirty=false;
const esc=s=>String(s??'').replace(/[&<>\"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;'}[c]));
const setDirty=()=>{dirty=true;document.getElementById('status').textContent='Unsaved changes'};
async function getCfg(){try{const r=await fetch('/api/config',{cache:'no-store'});if(!r.ok)throw new Error('Could not load configuration');cfg=await r.json();dirty=false;render()}catch(e){document.getElementById('app').innerHTML='<div class="card"><b>Could not load configuration.</b><p class="muted">Check the backend connection and reload.</p></div>'}}
function render(){document.getElementById('status').textContent='Version '+cfg.version;let h='<section class="card"><h2>Passenger notice</h2><p class="muted">Shown prominently in the app when non-empty.</p><div class="field"><textarea id="notice" oninput="setDirty()">'+esc(cfg.notice||'')+'</textarea></div></section>';
h+='<section class="card"><h2>Line service times</h2><p class="muted">Use 24-hour HH:MM. These are scheduled/approximate values, not live train telemetry.</p>';for(const [key,line] of Object.entries(cfg.lines||{})){h+='<div class="line"><div class="line-head"><b>'+esc(line.name)+'</b><span class="badge">'+esc(line.stations.length)+' stations</span></div><div class="grid"><div class="field"><label>FIRST TRAIN</label><input id="first_'+key+'" value="'+esc(line.first_train)+'" oninput="setDirty()"></div><div class="field"><label>LAST TRAIN</label><input id="last_'+key+'" value="'+esc(line.last_train)+'" oninput="setDirty()"></div><div class="field"><label>HEADWAY · MINUTES</label><input id="head_'+key+'" type="number" min="1" max="120" value="'+esc(line.headway_min)+'" oninput="setDirty()"></div></div></div>'}h+='</section>';
h+='<section class="card"><h2>Fare slabs</h2><p class="muted">Distance thresholds must increase. Fare values cannot decrease.</p>';for(let i=0;i<cfg.fares.length;i++){const f=cfg.fares[i];h+='<div class="grid"><div class="field"><label>MAX DISTANCE · KM</label><input id="km_'+i+'" type="number" min="0.1" step="0.1" value="'+esc(f.max_km)+'" oninput="setDirty()"></div><div class="field"><label>FARE · ₹</label><input id="fare_'+i+'" type="number" min="0" step="1" value="'+esc(f.fare)+'" oninput="setDirty()"></div></div>'}h+='</section>';
h+='<section class="card"><h2>Publish history</h2><p class="muted">Previous versions are retained for audit and rollback tooling.</p><div id="history" class="history">Loading history…</div></section><div class="actions"><button class="button secondary" onclick="getCfg()">Discard</button><button class="button primary" id="save" onclick="save()">Publish changes</button></div>';document.getElementById('app').innerHTML=h;loadHistory()}
async function loadHistory(){try{const r=await fetch('/api/admin/history',{cache:'no-store'});if(!r.ok)throw 0;const data=await r.json();document.getElementById('history').innerHTML=data.items.length?data.items.map(x=>'<div class="history-item"><span><b>v'+esc(x.version)+'</b> · '+esc(x.changed_by)+'</span><span>'+esc(x.updated_at)+'</span></div>').join(''):'No published changes yet.'}catch(e){document.getElementById('history').textContent='History unavailable.'}}
function collect(){const next=structuredClone(cfg);next.notice=document.getElementById('notice').value;for(const [key,line] of Object.entries(next.lines)){line.first_train=document.getElementById('first_'+key).value.trim();line.last_train=document.getElementById('last_'+key).value.trim();line.headway_min=Number(document.getElementById('head_'+key).value)}for(let i=0;i<next.fares.length;i++){next.fares[i].max_km=Number(document.getElementById('km_'+i).value);next.fares[i].fare=Number(document.getElementById('fare_'+i).value)}return next}
async function save(){if(!dirty)return toast('No changes to publish');const next=collect();if(!confirm('Publish these changes? All app users will receive the new configuration on their next sync.'))return;const b=document.getElementById('save');b.disabled=true;b.textContent='Publishing…';try{const r=await fetch('/api/admin/config',{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(next)});const data=await r.json().catch(()=>({}));if(!r.ok)throw new Error(data.detail||'Save failed');cfg=data;dirty=false;render();toast('Published configuration v'+cfg.version)}catch(e){toast(e.message)}finally{b.disabled=false;b.textContent='Publish changes'}}
function toast(msg){const t=document.getElementById('toast');t.textContent=msg;t.style.display='block';setTimeout(()=>t.style.display='none',3500)}
window.addEventListener('beforeunload',e=>{if(dirty){e.preventDefault();e.returnValue=''}});getCfg();
</script></main></body></html>'''
