CREATE TABLE rag_document (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    author VARCHAR(200),
    file_name VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT,
    file_hash VARCHAR(64) NOT NULL,
    source_path TEXT NOT NULL,
    language VARCHAR(50) DEFAULT 'zh',
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    UNIQUE (file_hash)
);

CREATE TABLE rag_document_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES rag_document(id),
    chapter_title VARCHAR(500),
    chunk_index INT NOT NULL,
    start_paragraph INT,
    end_paragraph INT,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    token_count INT,
    created_at TIMESTAMP DEFAULT now(),
    UNIQUE (document_id, chunk_index),
    UNIQUE (document_id, content_hash)
);

CREATE INDEX idx_rag_chunk_document_id
ON rag_document_chunk(document_id);

CREATE TABLE rag_chunk_embedding (
    id BIGSERIAL PRIMARY KEY,
    chunk_id BIGINT NOT NULL REFERENCES rag_document_chunk(id),
    embedding vector(1024) NOT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT now(),
    UNIQUE (chunk_id, embedding_model)
);

CREATE INDEX idx_rag_embedding_vector
ON rag_chunk_embedding
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

CREATE TABLE rag_chat_log (
    id BIGSERIAL PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT,
    document_ids TEXT,
    retrieved_chunk_ids TEXT,
    top_k INT,
    min_score DOUBLE PRECISION,
    latency_ms BIGINT,
    created_at TIMESTAMP DEFAULT now()
);

