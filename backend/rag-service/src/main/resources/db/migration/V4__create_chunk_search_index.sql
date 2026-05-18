CREATE TABLE rag_chunk_search_index (
    id BIGSERIAL PRIMARY KEY,
    chunk_id BIGINT NOT NULL REFERENCES rag_document_chunk(id) ON DELETE CASCADE,
    document_id BIGINT NOT NULL REFERENCES rag_document(id) ON DELETE CASCADE,
    search_text TEXT NOT NULL,
    search_vector tsvector NOT NULL,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    UNIQUE (chunk_id)
);

CREATE INDEX idx_rag_chunk_search_index_vector
ON rag_chunk_search_index
USING gin(search_vector);

CREATE INDEX idx_rag_chunk_search_index_document_id
ON rag_chunk_search_index(document_id);
