"""Initial NutriAI schema."""
from alembic import op
import sqlalchemy as sa

revision = '001'
down_revision = None
branch_labels = None
depends_on = None

def upgrade():
    op.create_table('users', sa.Column('id', sa.String(), primary_key=True), sa.Column('email', sa.String(), nullable=False), sa.Column('password', sa.String(), nullable=False), sa.Column('name', sa.String(), nullable=False), sa.Column('profile', sa.JSON(), nullable=False), sa.Column('plan', sa.String(), nullable=False))
    op.create_index('ix_users_email', 'users', ['email'], unique=True)
    op.create_table('refresh_tokens', sa.Column('id', sa.String(), primary_key=True), sa.Column('user_id', sa.String(), sa.ForeignKey('users.id'), nullable=False), sa.Column('expires', sa.DateTime(timezone=True), nullable=False), sa.Column('used', sa.Boolean(), nullable=False))
    op.create_table('diary_entries', sa.Column('id', sa.String(), primary_key=True), sa.Column('user_id', sa.String(), sa.ForeignKey('users.id'), nullable=False), sa.Column('request_id', sa.String(), nullable=False), sa.Column('date', sa.String(), nullable=False), sa.Column('meal', sa.String(), nullable=False), sa.Column('items', sa.JSON(), nullable=False), sa.UniqueConstraint('user_id', 'request_id'))
    op.create_index('ix_diary_entries_user_id', 'diary_entries', ['user_id'])
    op.create_index('ix_diary_entries_date', 'diary_entries', ['date'])
    op.create_table('chat_messages', sa.Column('id', sa.String(), primary_key=True), sa.Column('user_id', sa.String(), sa.ForeignKey('users.id'), nullable=False), sa.Column('date', sa.String(), nullable=False), sa.Column('is_user', sa.Boolean(), nullable=False), sa.Column('text', sa.String(), nullable=False), sa.Column('created', sa.DateTime(), nullable=False))
    op.create_index('ix_chat_messages_user_id', 'chat_messages', ['user_id'])
    op.create_table('media_objects', sa.Column('id', sa.String(), primary_key=True), sa.Column('user_id', sa.String(), sa.ForeignKey('users.id'), nullable=False), sa.Column('path', sa.String(), nullable=False), sa.Column('mime', sa.String(), nullable=False))
    op.create_table('food_items', sa.Column('id', sa.String(), primary_key=True), sa.Column('name', sa.String(), nullable=False), sa.Column('aliases', sa.JSON(), nullable=False), sa.Column('nutrients', sa.JSON(), nullable=False), sa.Column('source', sa.String(), nullable=False))

def downgrade():
    for table in ['food_items', 'media_objects', 'chat_messages', 'diary_entries', 'refresh_tokens', 'users']:
        op.drop_table(table)
