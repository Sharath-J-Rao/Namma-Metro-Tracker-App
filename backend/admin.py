from fastapi import APIRouter, Request, HTTPException
from fastapi.responses import HTMLResponse

router = APIRouter()

@router.get('/admin', response_class=HTMLResponse)
async def admin_page() -> str:
    return '<!doctype html><html><body><h1>Namma Metro Admin</h1><p>Admin panel placeholder.</p></body></html>'
