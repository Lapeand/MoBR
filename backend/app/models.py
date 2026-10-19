import os
from datetime import datetime, timezone
from sqlalchemy import create_engine, String, ForeignKey, JSON, DateTime, UniqueConstraint
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, sessionmaker

engine = create_engine(os.getenv('DATABASE_URL', 'sqlite:///./nutriai.db'),
                       **({'connect_args': {'check_same_thread': False}} if os.getenv('DATABASE_URL', 'sqlite:').startswith('sqlite:') else {}))
Session = sessionmaker(engine, expire_on_commit=False)

class Base(DeclarativeBase):
    pass

class User(Base):
    __tablename__ = 'users'
    id: Mapped[str] = mapped_column(String, primary_key=True)
    email: Mapped[str] = mapped_column(String, unique=True, index=True)
    password: Mapped[str]
    name: Mapped[str]
    profile: Mapped[dict] = mapped_column(JSON, default=dict)
    plan: Mapped[str] = mapped_column(default='free')

class RefreshToken(Base):
    __tablename__ = 'refresh_tokens'
    id: Mapped[str] = mapped_column(String, primary_key=True)
    user_id: Mapped[str] = mapped_column(ForeignKey('users.id'))
    expires: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    used: Mapped[bool] = mapped_column(default=False)

class Entry(Base):
    __tablename__ = 'diary_entries'
    __table_args__ = (UniqueConstraint('user_id', 'request_id'),)
    id: Mapped[str] = mapped_column(String, primary_key=True)
    user_id: Mapped[str] = mapped_column(ForeignKey('users.id'), index=True)
    request_id: Mapped[str]
    date: Mapped[str] = mapped_column(index=True)
    meal: Mapped[str]
    items: Mapped[list] = mapped_column(JSON)

class Message(Base):
    __tablename__ = 'chat_messages'
    id: Mapped[str] = mapped_column(String, primary_key=True)
    user_id: Mapped[str] = mapped_column(ForeignKey('users.id'), index=True)
    date: Mapped[str]
    is_user: Mapped[bool]
    text: Mapped[str]
    created: Mapped[datetime] = mapped_column(default=lambda: datetime.now(timezone.utc))

class Media(Base):
    __tablename__ = 'media_objects'
    id: Mapped[str] = mapped_column(String, primary_key=True)
    user_id: Mapped[str] = mapped_column(ForeignKey('users.id'))
    path: Mapped[str]
    mime: Mapped[str]

class Food(Base):
    __tablename__ = 'food_items'
    id: Mapped[str] = mapped_column(String, primary_key=True)
    name: Mapped[str]
    aliases: Mapped[list] = mapped_column(JSON)
    nutrients: Mapped[dict] = mapped_column(JSON)
    source: Mapped[str]
