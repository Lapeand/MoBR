import io
import math
import os
import secrets
from contextlib import asynccontextmanager
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from uuid import uuid4
import boto3
import httpx
import jwt
from fastapi import FastAPI, Depends, HTTPException, UploadFile, File, Query, Response
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from PIL import Image, UnidentifiedImageError
from pwdlib import PasswordHash
from sqlalchemy import select, update, func
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session as DBSession
from .models import Base, engine, Session, User, Entry, Message, Media, RefreshToken, Food
from .schemas import Credentials, Profile, EntryCreate, AgentRequest
from .nutrition import CATALOG, KEYS, targets, totals
from .agent import recognize

SECRET = os.getenv('JWT_SECRET', '')
if os.getenv('ENVIRONMENT') == 'production' and (len(SECRET) < 32 or SECRET.startswith('replace-')):
    raise RuntimeError('Set a random JWT_SECRET of at least 32 characters')
if not SECRET:
    if os.getenv('ENVIRONMENT') == 'production':
        raise RuntimeError('JWT_SECRET is required in production')
    SECRET = secrets.token_hex(32)
HASH = PasswordHash.recommended()
DUMMY_PASSWORD_HASH = HASH.hash(secrets.token_hex(32))
UPLOADS = Path(os.getenv('UPLOAD_DIR', './uploads'))
UPLOADS.mkdir(parents=True, exist_ok=True)

def uid():
    return str(uuid4())

@asynccontextmanager
async def lifespan(app):
    if os.getenv('ENVIRONMENT') != 'production':
        Base.metadata.create_all(engine)
    with Session() as db:
        for idx, (name, aliases, values) in enumerate(CATALOG):
            if not db.get(Food, str(idx)):
                db.add(Food(id=str(idx), name=name, aliases=aliases, nutrients=dict(zip(KEYS, values)), source='reference-estimate'))
        db.commit()
    yield

app = FastAPI(title='NutriAI API', version='1.0.0', lifespan=lifespan)
bearer = HTTPBearer(auto_error=False)

def database():
    with Session() as db:
        yield db

def current_user(auth: HTTPAuthorizationCredentials | None = Depends(bearer), db: DBSession = Depends(database)):
    try:
        payload = jwt.decode(auth.credentials if auth else '', SECRET, algorithms=['HS256'])
        if payload.get('type') != 'access':
            raise ValueError()
        user = db.get(User, payload['sub'])
        if not user:
            raise ValueError()
        return user
    except (jwt.PyJWTError, ValueError, KeyError):
        raise HTTPException(401, 'Сессия истекла. Войдите снова.')

def issue_tokens(user, db):
    now = datetime.now(timezone.utc)
    refresh_id = uid()
    db.add(RefreshToken(id=refresh_id, user_id=user.id, expires=now+timedelta(days=30)))
    db.commit()
    def token(kind, expiry, **extra):
        return jwt.encode(dict(sub=user.id, type=kind, exp=now+expiry, **extra), SECRET, algorithm='HS256')
    return dict(access_token=token('access', timedelta(minutes=30)), refresh_token=token('refresh', timedelta(days=30), jti=refresh_id))

def profile_response(user):
    profile = Profile(**(user.profile or {}))
    return dict(**profile.model_dump(), plan=user.plan, targets=targets(profile))

def entry_response(entry):
    return dict(id=entry.id, request_id=entry.request_id, date=entry.date, meal=entry.meal, items=entry.items, totals=totals(entry.items))

def entries(db, user, start, end):
    if start > end or (end-start).days > 366:
        raise HTTPException(422, 'Диапазон дат: от 0 до 366 дней')
    return db.scalars(select(Entry).where(Entry.user_id == user.id, Entry.date >= start.isoformat(), Entry.date <= end.isoformat()).order_by(Entry.date, Entry.id)).all()

@app.get('/health')
def health():
    return {'status': 'ok', 'ai_configured': bool(os.getenv('OPENAI_API_KEY'))}

@app.post('/auth/register', status_code=201)
def register(body: Credentials, db: DBSession = Depends(database)):
    user = User(id=uid(), email=body.email.lower(), password=HASH.hash(body.password), name=body.name,
                profile=Profile(name=body.name).model_dump())
    db.add(user)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(409, 'Этот email уже зарегистрирован')
    return issue_tokens(user, db)

@app.post('/auth/login')
def login(body: Credentials, db: DBSession = Depends(database)):
    user = db.scalar(select(User).where(User.email == body.email.lower()))
    # Run password hash even for missing accounts to reduce account timing leakage.
    password_hash = user.password if user else DUMMY_PASSWORD_HASH
    if not HASH.verify(body.password, password_hash) or not user:
        raise HTTPException(401, 'Неверный email или пароль')
    return issue_tokens(user, db)

from pydantic import BaseModel
class RefreshRequest(BaseModel):
    refresh_token: str

def consume_refresh(body, db):
    try:
        payload = jwt.decode(body.refresh_token, SECRET, algorithms=['HS256'])
        if payload.get('type') != 'refresh':
            raise ValueError()
        changed = db.execute(update(RefreshToken).where(RefreshToken.id == payload['jti'], RefreshToken.user_id == payload['sub'], RefreshToken.used == False).values(used=True))
        if changed.rowcount != 1:
            raise ValueError()
        db.commit()
        user = db.get(User, payload['sub'])
        if not user:
            raise ValueError()
        return user
    except (jwt.PyJWTError, ValueError, KeyError):
        db.rollback()
        raise HTTPException(401, 'Недействительная сессия')

@app.post('/auth/refresh')
def refresh(body: RefreshRequest, db: DBSession = Depends(database)):
    return issue_tokens(consume_refresh(body, db), db)

@app.post('/auth/logout', status_code=204)
def logout(body: RefreshRequest, db: DBSession = Depends(database)):
    consume_refresh(body, db)
    return Response(status_code=204)

@app.get('/users/me')
def me(user: User = Depends(current_user)):
    return profile_response(user)

@app.patch('/users/me')
def patch_profile(body: Profile, user: User = Depends(current_user), db: DBSession = Depends(database)):
    user.profile = body.model_dump()
    user.name = body.name
    db.commit()
    return profile_response(user)

@app.get('/diary')
def diary(from_: date = Query(alias='from'), to: date = Query(), user: User = Depends(current_user), db: DBSession = Depends(database)):
    return [entry_response(e) for e in entries(db, user, from_, to)]

@app.post('/diary/entries', status_code=201)
def create_entry(body: EntryCreate, user: User = Depends(current_user), db: DBSession = Depends(database)):
    existing = db.scalar(select(Entry).where(Entry.user_id == user.id, Entry.request_id == body.request_id))
    if existing:
        return entry_response(existing)
    entry = Entry(id=uid(), user_id=user.id, request_id=body.request_id, date=body.date.isoformat(), meal=body.meal, items=[i.model_dump() for i in body.items])
    db.add(entry)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        entry = db.scalar(select(Entry).where(Entry.user_id == user.id, Entry.request_id == body.request_id))
        if not entry:
            raise
    return entry_response(entry)

@app.delete('/diary/entries/{entry_id}', status_code=204)
def delete_entry(entry_id: str, user: User = Depends(current_user), db: DBSession = Depends(database)):
    entry = db.get(Entry, entry_id)
    if not entry or entry.user_id != user.id:
        raise HTTPException(404, 'Запись не найдена')
    db.delete(entry)
    db.commit()
    return Response(status_code=204)

@app.get('/stats/daily')
def daily(date: date, user: User = Depends(current_user), db: DBSession = Depends(database)):
    data = entries(db, user, date, date)
    return dict(date=date.isoformat(), totals=totals([i for e in data for i in e.items]), targets=targets(Profile(**user.profile)))

@app.get('/stats/weekly')
def weekly(date: date, user: User = Depends(current_user), db: DBSession = Depends(database)):
    start = date-timedelta(days=6)
    data = entries(db, user, start, date)
    return [dict(date=(day := start+timedelta(days=n)).isoformat(), totals=totals([i for e in data if e.date == day.isoformat() for i in e.items])) for n in range(7)]

@app.get('/chat')
def chat(user: User = Depends(current_user), db: DBSession = Depends(database)):
    data = db.scalars(select(Message).where(Message.user_id == user.id).order_by(Message.created.desc()).limit(100)).all()
    return [dict(is_user=m.is_user, text=m.text) for m in reversed(data)]

def s3():
    return boto3.client('s3', endpoint_url=os.getenv('S3_ENDPOINT'), aws_access_key_id=os.getenv('S3_ACCESS_KEY'), aws_secret_access_key=os.getenv('S3_SECRET_KEY'), region_name='us-east-1')

def media_data(media):
    if os.getenv('S3_ENDPOINT'):
        return s3().get_object(Bucket=os.getenv('S3_BUCKET', 'nutriai'), Key=media.path)['Body'].read()
    return (UPLOADS/media.path).read_bytes()

@app.post('/media/upload', status_code=201)
async def upload(file: UploadFile = File(), user: User = Depends(current_user), db: DBSession = Depends(database)):
    content = await file.read(8*1024*1024+1)
    if len(content) > 8*1024*1024:
        raise HTTPException(413, 'Фото должно быть меньше 8 МБ')
    try:
        Image.MAX_IMAGE_PIXELS = 25_000_000
        image = Image.open(io.BytesIO(content))
        if image.width * image.height > 25_000_000:
            raise HTTPException(422, 'Слишком большое разрешение фото: максимум 25 мегапикселей')
        image.load()
        image.thumbnail((1600, 1600))
        output = io.BytesIO()
        image.convert('RGB').save(output, format='JPEG', quality=85)
        content = output.getvalue()  # Strip EXIF metadata, normalize format.
    except (UnidentifiedImageError, OSError, Image.DecompressionBombError):
        raise HTTPException(422, 'Файл не является поддерживаемым фото')
    media = Media(id=uid(), user_id=user.id, path=uid()+'.jpg', mime='image/jpeg')
    if os.getenv('S3_ENDPOINT'):
        client = s3()
        bucket = os.getenv('S3_BUCKET', 'nutriai')
        try:
            client.head_bucket(Bucket=bucket)
        except client.exceptions.ClientError:
            client.create_bucket(Bucket=bucket)
        client.put_object(Bucket=bucket, Key=media.path, Body=content, ContentType=media.mime)
    else:
        (UPLOADS/media.path).write_bytes(content)
    db.add(media)
    db.commit()
    return {'image_id': media.id}

@app.post('/agent/message')
async def message(body: AgentRequest, user: User = Depends(current_user), db: DBSession = Depends(database)):
    # Count against the server UTC day, independent of user-supplied diary date.
    start = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
    count = db.scalar(select(func.count()).select_from(Message).where(Message.user_id == user.id, Message.is_user == True, Message.created >= start)) or 0
    if user.plan == 'free' and count >= 20:
        raise HTTPException(429, 'Дневной лимит: 20 сообщений. Ручной дневник доступен без ограничений.')
    photo = None
    if body.image_id:
        photo = db.get(Media, body.image_id)
        if not photo or photo.user_id != user.id:
            raise HTTPException(404, 'Фото не найдено')
    result = await recognize(body.text, media_data(photo) if photo else None, photo.mime if photo else None, db.scalars(select(Food)).all())
    for is_user, text in [(True, body.text or '[Фото блюда]'), (False, result.reply)]:
        db.add(Message(id=uid(), user_id=user.id, date=body.date.isoformat(), is_user=is_user, text=text))
    db.commit()
    return result.model_dump()  # No automatic diary write: explicit confirmation required.

@app.get('/recommendations/meals')
def recommendations(date: date, user: User = Depends(current_user), db: DBSession = Depends(database)):
    target = targets(Profile(**user.profile))
    eaten = totals([i for e in entries(db, user, date, date) for i in e.items])
    remaining = max(0, target['kcal'] - eaten['kcal'])
    if remaining < 100:
        return []
    recipes = [('Обед', 'Курица с гречкой', [('Куриная грудка', 120), ('Гречка варёная', 150), ('Салат овощной', 150)]),
               ('Ужин', 'Творог с бананом', [('Творог 5%', 200), ('Банан', 100)])]
    foods = {f.name: f for f in db.scalars(select(Food)).all()}
    result = []
    for label, title, portions in recipes:
        base = {k: sum(foods[name].nutrients[k]*grams/100 for name, grams in portions) for k in KEYS}
        scale = min([1.0] + [max(0, target[k]-eaten[k])/base[k] for k in KEYS if base[k] > 0])
        if scale < .15:
            continue
        items = [dict(name=name, weight_g=round(grams*scale), source=foods[name].source,
                      **{k: round(v*grams*scale/100, 1) for k, v in foods[name].nutrients.items()}) for name, grams in portions]
        result.append(dict(title=title, meal=label, description=f'Остаток на день: {round(remaining)} ккал. Порции подстроены под остаток.', items=items, totals=totals(items)))
    return result

@app.get('/foods/barcode/{barcode}')
async def barcode(barcode: str, user: User = Depends(current_user), db: DBSession = Depends(database)):
    if not barcode.isdigit() or not 8 <= len(barcode) <= 14:
        raise HTTPException(422, 'Неверный штрихкод')
    existing = db.get(Food, 'off:'+barcode)
    if not existing:
        try:
            async with httpx.AsyncClient(timeout=15, headers={'User-Agent': 'NutriAI-VKR/1.0 (educational diary)'}) as client:
                r = await client.get(f'https://world.openfoodfacts.org/api/v2/product/{barcode}.json', params={'fields': 'product_name,nutriments'})
                r.raise_for_status()
                product = r.json().get('product', {})
                n = product.get('nutriments', {})
                nutrient_keys = ['energy-kcal_100g', 'proteins_100g', 'fat_100g', 'carbohydrates_100g']
                if not product.get('product_name') or any(k not in n for k in nutrient_keys):
                    raise HTTPException(404, 'Нет полного состава продукта в Open Food Facts')
                values = dict(zip(KEYS, [float(n[k]) for k in nutrient_keys]))
                if any(not math.isfinite(v) or v < 0 or v > 1000 for v in values.values()):
                    raise HTTPException(422, 'Некорректные данные справочника')
                existing = Food(id='off:'+barcode, name=product['product_name'], aliases=[product['product_name'].lower()], nutrients=values, source='Open Food Facts')
                db.add(existing)
                db.commit()
        except httpx.HTTPError:
            raise HTTPException(502, 'Справочник временно недоступен')
    return dict(name=existing.name, weight_g=100, source=existing.source, **existing.nutrients)

@app.get('/diary/export')
def export(from_: date = Query(alias='from'), to: date = Query(), user: User = Depends(current_user), db: DBSession = Depends(database)):
    import csv
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(['date', 'meal', 'food', 'grams', *KEYS])
    for e in entries(db, user, from_, to):
        for i in e.items:
            # Prevent formula interpretation in spreadsheet software.
            safe = lambda s: "'"+s if s.startswith(('=', '+', '-', '@')) else s
            writer.writerow([e.date, safe(e.meal), safe(i['name']), i['weight_g'], *[i[k] for k in KEYS]])
    return Response('\ufeff'+output.getvalue(), media_type='text/csv', headers={'Content-Disposition': 'attachment; filename=nutriai.csv'})

class DemoPlan(BaseModel):
    premium: bool

@app.post('/debug/plan')
def demo_plan(body: DemoPlan, user: User = Depends(current_user), db: DBSession = Depends(database)):
    if os.getenv('ENABLE_DEMO_PLAN') != 'true' or os.getenv('ENVIRONMENT') == 'production':
        raise HTTPException(404, 'Демо-переключатель выключен')
    user.plan = 'premium' if body.premium else 'free'
    db.commit()
    return profile_response(user)
