import os
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from admin import router as admin_router, require_admin
from store import init_db, load_config, save_config, history

@asynccontextmanager
async def lifespan(_app):
    init_db()
    yield

app = FastAPI(title='Namma Metro Admin API', version='7.2.0', lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=[o.strip() for o in os.getenv('CORS_ORIGINS', '').split(',') if o.strip()] or ['https://localhost'],
    allow_credentials=False,
    allow_methods=['GET', 'PUT'],
    allow_headers=['Authorization', 'Content-Type'],
)
app.include_router(admin_router)

class ConfigPayload(BaseModel):
    version: int = 1
    updated_at: str = ''
    notice: str = ''
    fares: list[dict]
    lines: dict

@app.get('/')
async def root():
    return {'service': 'Namma Metro Admin API', 'version': '7.2.0', 'admin': '/admin'}

@app.get('/api/health')
async def health():
    cfg = load_config()
    return {'ok': True, 'config_version': cfg['version']}

@app.get('/api/config')
async def public_config():
    response = JSONResponse(load_config())
    response.headers['Cache-Control'] = 'no-store, max-age=0'
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['Referrer-Policy'] = 'no-referrer'
    return response

@app.get('/api/admin/history')
async def admin_history(_: bool = Depends(require_admin)):
    return {'items': history()}

@app.put('/api/admin/config')
async def update_config(payload: ConfigPayload, _: bool = Depends(require_admin)):
    return save_config(payload.model_dump(), changed_by='admin')
