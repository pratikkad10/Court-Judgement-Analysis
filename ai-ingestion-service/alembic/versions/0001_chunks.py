"""Creates the ai-owned hybrid retrieval index."""

from alembic import op
import sqlalchemy as sa

revision = "0001_chunks"
down_revision = None
branch_labels = None
depends_on = None


def upgrade():
    op.execute("CREATE SCHEMA IF NOT EXISTS ai")
    op.execute("CREATE EXTENSION IF NOT EXISTS vector")
    op.create_table(
        "chunks",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("document_id", sa.String(36), nullable=False),
        sa.Column("document_title", sa.String(500), nullable=False),
        sa.Column("chunk_index", sa.Integer, nullable=False),
        sa.Column("text", sa.Text, nullable=False),
        sa.Column("section_type", sa.String(30), nullable=False),
        sa.Column("page_start", sa.Integer, nullable=False),
        sa.Column("page_end", sa.Integer, nullable=False),
        sa.Column("token_count", sa.Integer, nullable=False),
        sa.Column("embedding", sa.Text, nullable=False),
        sa.Column("embedding_model", sa.String(255), nullable=False),
        sa.Column("tsv", sa.Text, nullable=False),
        schema="ai",
    )
    op.execute(
        "ALTER TABLE ai.chunks ALTER COLUMN embedding TYPE vector(384) USING embedding::vector"
    )
    op.execute(
        "ALTER TABLE ai.chunks ALTER COLUMN tsv TYPE tsvector USING to_tsvector('english', tsv)"
    )
    op.execute("CREATE INDEX chunks_document_idx ON ai.chunks(document_id)")
    op.execute(
        "CREATE INDEX chunks_embedding_hnsw ON ai.chunks USING hnsw (embedding vector_cosine_ops)"
    )
    op.execute("CREATE INDEX chunks_tsv_gin ON ai.chunks USING gin (tsv)")


def downgrade():
    op.drop_table("chunks", schema="ai")
