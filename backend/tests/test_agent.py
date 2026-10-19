import asyncio
import json
from types import SimpleNamespace
import httpx
from fastapi import HTTPException
from app.agent import recognize, output_schema


def test_provider_contract_and_reference_override(monkeypatch):
    monkeypatch.setenv('OPENAI_API_KEY', 'test-key-not-real')
    captured = {}
    response_body = {'choices': [{'message': {'content': json.dumps({'reply':'Проверьте порцию', 'items':[
        dict(name='Борщ', weight_g=200, kcal=999, protein=99, fat=99, carbs=99, source='invented'),
        dict(name='Неизвестное блюдо', weight_g=100, kcal=80, protein=2, fat=2, carbs=8, source='fake-off')
    ]})}}]}
    class FakeClient:
        def __init__(self, **kwargs): pass
        async def __aenter__(self): return self
        async def __aexit__(self, *args): pass
        async def post(self, url, **kwargs):
            captured.update(kwargs['json'])
            return httpx.Response(200, json=response_body, request=httpx.Request('POST', url))
    monkeypatch.setattr('app.agent.httpx.AsyncClient', FakeClient)
    food = SimpleNamespace(name='Борщ', nutrients=dict(kcal=49, protein=2, fat=2, carbs=6), source='reference-estimate')
    result = asyncio.run(recognize('борщ', b'image-data', 'image/jpeg', [food]))
    assert result.items[0].kcal == 98
    assert result.items[0].source == 'reference-estimate'
    assert result.items[1].source == 'ai-estimate'
    assert captured['response_format']['json_schema']['strict'] is True
    assert captured['messages'][1]['content'][1]['image_url']['url'].startswith('data:image/jpeg;base64,')
    schema = output_schema()
    assert set(schema['required']) == {'reply','items'}
    assert schema['additionalProperties'] is False
    assert schema['$defs']['FoodItem']['additionalProperties'] is False
    assert 'source' in schema['$defs']['FoodItem']['required']
