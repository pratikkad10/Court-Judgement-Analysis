CREATE TABLE chat_sessions (
  id UUID PRIMARY KEY,
  owner_id UUID NOT NULL,
  title VARCHAR(500) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chat_messages (
  id UUID PRIMARY KEY,
  session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
  role VARCHAR(20) NOT NULL,
  content TEXT NOT NULL,
  citations JSONB NOT NULL DEFAULT '[]',
  provider_used VARCHAR(100),
  latency_ms BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE analyses (
  id UUID PRIMARY KEY,
  owner_id UUID NOT NULL,
  type VARCHAR(30) NOT NULL,
  document_ids JSONB NOT NULL,
  result JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
  id UUID PRIMARY KEY,
  owner_id UUID,
  action VARCHAR(100) NOT NULL,
  correlation_id VARCHAR(100),
  details JSONB NOT NULL DEFAULT '{}',
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX chat_sessions_owner_idx ON chat_sessions(owner_id, updated_at DESC);
CREATE INDEX analyses_owner_idx ON analyses(owner_id, created_at DESC);
