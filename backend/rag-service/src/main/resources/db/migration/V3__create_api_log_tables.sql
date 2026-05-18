CREATE TABLE rag_llm_api_log (
    id BIGSERIAL PRIMARY KEY,
    chat_log_id BIGINT,
    model VARCHAR(100) NOT NULL,
    request TEXT NOT NULL,
    response TEXT,
    error_message TEXT,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    latency_ms BIGINT NOT NULL,
    is_success BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_rag_llm_api_log_chat_log_id ON rag_llm_api_log(chat_log_id);
CREATE INDEX idx_rag_llm_api_log_model ON rag_llm_api_log(model);
CREATE INDEX idx_rag_llm_api_log_created_at ON rag_llm_api_log(created_at DESC);

CREATE TABLE rag_embedding_api_log (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT,
    chunk_id BIGINT,
    model VARCHAR(100) NOT NULL,
    input_text TEXT,
    input_length INT,
    response TEXT,
    error_message TEXT,
    input_tokens INT,
    total_tokens INT,
    latency_ms BIGINT NOT NULL,
    is_success BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_rag_embedding_api_log_document_id ON rag_embedding_api_log(document_id);
CREATE INDEX idx_rag_embedding_api_log_chunk_id ON rag_embedding_api_log(chunk_id);
CREATE INDEX idx_rag_embedding_api_log_model ON rag_embedding_api_log(model);
CREATE INDEX idx_rag_embedding_api_log_created_at ON rag_embedding_api_log(created_at DESC);
