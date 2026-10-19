import io
import os
import tempfile
from pathlib import Path

TEST_DIR = tempfile.TemporaryDirectory()
os.environ['DATABASE_URL'] = 'sqlite:///' + str(Path(TEST_DIR.name)/'test.db')
os.environ['UPLOAD_DIR'] = str(Path(TEST_DIR.name)/'uploads')
os.environ['JWT_SECRET'] = 'tests-only-secret-with-at-least-32-characters'
os.environ.pop('OPENAI_API_KEY', None)
from fastapi.testclient import TestClient
from PIL import Image
from app.main import app


def credentials(email='student@example.com'):
    return dict(email=email, password='SecureTest123', name='Тест')


def register(client, email):
    r = client.post('/auth/register', json=credentials(email))
    assert r.status_code == 201, r.text
    return r.json()


def auth(tokens):
    return {'Authorization': 'Bearer '+tokens['access_token']}


def test_end_to_end_and_privacy():
    with TestClient(app) as client:
        first = register(client, 'first@example.com')
        second = register(client, 'second@example.com')
        headers = auth(first)
        assert client.get('/users/me').status_code == 401
        assert client.post('/auth/register', json=credentials('first@example.com')).status_code == 409
        assert client.post('/auth/login', json=dict(email='first@example.com', password='wrongpass')).status_code == 401
        assert client.post('/auth/login', json=credentials('first@example.com')).status_code == 200
        profile = client.get('/users/me', headers=headers).json()
        assert profile['targets']['kcal'] > 0
        profile.update(weight=82, height=178, age=25, goal='lose', onboarded=True)
        response = client.patch('/users/me', json=profile, headers=headers)
        assert response.status_code == 200, response.text
        assert response.json()['targets']['kcal'] == round((10*82+6.25*178-5*25+5)*1.375*.85)
        invalid = dict(profile, weight=-1)
        assert client.patch('/users/me', json=invalid, headers=headers).status_code == 422
        result = client.post('/agent/message', headers=headers, json=dict(text='борщ 300 г, хлеб 40 г', date='2026-10-19'))
        assert result.status_code == 200, result.text
        parsed = result.json()
        assert len(parsed['items']) == 2
        assert parsed['items'][0]['kcal'] == 147
        params = {'from': '2026-10-19', 'to': '2026-10-19'}
        assert client.get('/diary', params=params, headers=headers).json() == []
        entry = dict(request_id='confirm-once', date='2026-10-19', meal='Обед', items=parsed['items'])
        saved = client.post('/diary/entries', headers=headers, json=entry)
        assert saved.status_code == 201, saved.text
        assert client.post('/diary/entries', headers=headers, json=entry).json()['id'] == saved.json()['id']
        diary = client.get('/diary', params=params, headers=headers).json()
        assert len(diary) == 1
        assert diary[0]['totals']['kcal'] == 247
        assert client.get('/diary', params=params, headers=auth(second)).json() == []
        assert client.delete('/diary/entries/'+saved.json()['id'], headers=auth(second)).status_code == 404
        daily = client.get('/stats/daily', params={'date':'2026-10-19'}, headers=headers).json()
        assert daily['totals']['kcal'] == 247
        weekly = client.get('/stats/weekly', params={'date':'2026-10-19'}, headers=headers).json()
        assert len(weekly) == 7 and weekly[-1]['totals']['kcal'] == 247
        recs = client.get('/recommendations/meals', params={'date':'2026-10-19'}, headers=headers).json()
        assert len(recs) == 2
        assert all(r['totals']['kcal'] <= profile['targets']['kcal']-247 for r in recs)
        exported = client.get('/diary/export', params=params, headers=headers)
        assert exported.status_code == 200 and 'Борщ' in exported.text
        assert len(client.get('/chat', headers=headers).json()) == 2
        assert client.get('/chat', headers=auth(second)).json() == []
        assert client.delete('/diary/entries/'+saved.json()['id'], headers=headers).status_code == 204
        assert client.get('/stats/daily', params={'date':'2026-10-19'}, headers=headers).json()['totals']['kcal'] == 0


def test_refresh_rotation_and_logout():
    with TestClient(app) as client:
        tokens = register(client, 'rotate@example.com')
        refreshed = client.post('/auth/refresh', json={'refresh_token':tokens['refresh_token']})
        assert refreshed.status_code == 200
        assert client.post('/auth/refresh', json={'refresh_token':tokens['refresh_token']}).status_code == 401
        current = refreshed.json()
        assert client.post('/auth/logout', json={'refresh_token':current['refresh_token']}).status_code == 204
        assert client.post('/auth/refresh', json={'refresh_token':current['refresh_token']}).status_code == 401
        assert client.post('/auth/refresh', json={'refresh_token':tokens['access_token']}).status_code == 401


def test_media_validation_and_ownership():
    with TestClient(app) as client:
        owner = register(client, 'photo@example.com')
        stranger = register(client, 'stranger@example.com')
        assert client.post('/media/upload', files={'file':('bad.jpg', b'not image', 'image/jpeg')}, headers=auth(owner)).status_code == 422
        image = io.BytesIO()
        Image.new('RGB', (10,10)).save(image, format='PNG')
        uploaded = client.post('/media/upload', files={'file':('photo.png', image.getvalue(), 'image/png')}, headers=auth(owner))
        assert uploaded.status_code == 201, uploaded.text
        request = dict(image_id=uploaded.json()['image_id'], date='2026-10-19')
        assert client.post('/agent/message', json=request, headers=auth(stranger)).status_code == 404
        assert client.post('/agent/message', json=request, headers=auth(owner)).status_code == 503
        assert client.post('/agent/message', json={'date':'2026-10-19'}, headers=auth(owner)).status_code == 422


def test_no_fabrication_and_ranges():
    with TestClient(app) as client:
        tokens = register(client, 'unknown@example.com')
        headers = auth(tokens)
        for text in ['несуществующий продукт', 'борщ 300 г, неизвестная еда', 'рис 9000 г']:
            result = client.post('/agent/message', json={'text':text,'date':'2026-10-19'}, headers=headers)
            assert result.status_code == 200, result.text
            assert result.json()['items'] == []
        assert client.get('/diary', params={'from':'2026-10-20','to':'2026-10-19'}, headers=headers).status_code == 422


def test_free_limit_and_debug_plan(monkeypatch):
    with TestClient(app) as client:
        headers = auth(register(client, 'quota@example.com'))
        payload = dict(text='борщ 100 г', date='2026-10-19')
        for _ in range(20):
            assert client.post('/agent/message', json=payload, headers=headers).status_code == 200
        assert client.post('/agent/message', json=dict(payload, date='2000-01-01'), headers=headers).status_code == 429
        assert client.post('/debug/plan', json={'premium':True}, headers=headers).status_code == 404
        monkeypatch.setenv('ENABLE_DEMO_PLAN', 'true')
        assert client.post('/debug/plan', json={'premium':True}, headers=headers).json()['plan'] == 'premium'
        assert client.post('/agent/message', json=payload, headers=headers).status_code == 200
        monkeypatch.setenv('ENVIRONMENT', 'production')
        assert client.post('/debug/plan', json={'premium':False}, headers=headers).status_code == 404
