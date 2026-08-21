import os
from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from admin import router as admin_router, require_admin
from store import init_db, load_config, save_config

app = FastAPI(title='Namma Metro Admin API', version='7.0.0')
app.add_middleware(CORSMiddleware, allow_origins=['*'], allow_credentials=True, allow_methods=['*'], allow_headers=['*'])
app.include_router(admin_router)

class ConfigPayload(BaseModel):
    version: int
    updated_at: str
    notice: str = ''
    fares: list[dict]
    lines: dict

@app.on_event('startup')
async def startup():
    init_db()

@app.get('/')
async def root():
    return {'service': 'Namma Metro Admin API', 'version': '7.0.0', 'admin': '/admin'}

@app.get('/api/health')
async def health():
    cfg = load_config()
    return {'ok': True, 'config_version': cfg['version']}

@app.get('/api/config')
async def public_config():
    return load_config()

@app.put('/api/admin/config')
async def update_config(payload: ConfigPayload, _: bool = Depends(require_admin)):
    return save_config(payload.model_dump())
