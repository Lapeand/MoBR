from datetime import date
from typing import Literal
from pydantic import BaseModel, Field, EmailStr, model_validator

class Credentials(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    name: str = Field(default='Пользователь', min_length=1, max_length=60)

class Profile(BaseModel):
    name: str = Field(default='Пользователь', min_length=1, max_length=60)
    sex: Literal['male', 'female'] = 'male'
    age: int = Field(default=25, ge=18, le=100)
    weight: float = Field(default=75, ge=30, le=300)
    height: float = Field(default=175, ge=120, le=230)
    goal: Literal['lose', 'maintain', 'gain'] = 'maintain'
    activity: Literal['low', 'moderate', 'high'] = 'moderate'
    onboarded: bool = False

class FoodItem(BaseModel):
    name: str = Field(min_length=1, max_length=120)
    weight_g: float = Field(gt=0, le=3000)
    kcal: float = Field(ge=0, le=30000)
    protein: float = Field(ge=0, le=3000)
    fat: float = Field(ge=0, le=3000)
    carbs: float = Field(ge=0, le=3000)
    source: str = Field(default='manual', max_length=200)

class EntryCreate(BaseModel):
    request_id: str = Field(min_length=1, max_length=80)
    date: date
    meal: str = Field(default='Приём пищи', min_length=1, max_length=60)
    items: list[FoodItem] = Field(min_length=1, max_length=30)

class AgentRequest(BaseModel):
    text: str = Field(default='', max_length=2000)
    image_id: str | None = None
    date: date
    @model_validator(mode='after')
    def content_required(self):
        if not self.text.strip() and not self.image_id:
            raise ValueError('Введите текст или приложите фото')
        return self

class ParsedMeal(BaseModel):
    reply: str = Field(max_length=1500)
    items: list[FoodItem] = Field(default_factory=list, max_length=30)
