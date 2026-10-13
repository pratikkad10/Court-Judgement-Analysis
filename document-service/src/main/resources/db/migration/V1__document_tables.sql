CREATE TABLE documents (
  id UUID PRIMARY KEY,
  owner_id UUID NOT NULL,
  title VARCHAR(500) NOT NULL,
  filename VARCHAR(255) NOT NULL,
  storage_path VARCHAR(1000) NOT NULL,
  file_hash CHAR(64) NOT NULL,
  page_count INTEGER NOT NULL,
  status VARCHAR(20) NOT NULL,
  court VARCHAR(500),
  case_number VARCHAR(500),
  judgment_date DATE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(owner_id, file_hash)
);

CREATE INDEX documents_owner_created_idx ON documents(owner_id, created_at DESC);
