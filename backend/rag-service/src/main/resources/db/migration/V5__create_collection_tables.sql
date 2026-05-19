-- Create rag_collection table
CREATE TABLE rag_collection (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    tags TEXT,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    document_count INT NOT NULL DEFAULT 0,
    ready_document_count INT NOT NULL DEFAULT 0,
    chunk_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

-- Add collection_id to rag_document
ALTER TABLE rag_document
ADD COLUMN collection_id BIGINT REFERENCES rag_collection(id);

-- Create index on collection_id for faster queries
CREATE INDEX idx_rag_document_collection_id
ON rag_document(collection_id);

-- Create default collection
INSERT INTO rag_collection (name, description, document_count, ready_document_count)
VALUES ('Default', 'Default collection for ungrouped documents', 0, 0);

-- Migrate existing documents to default collection
UPDATE rag_document
SET collection_id = (SELECT id FROM rag_collection WHERE name = 'Default')
WHERE collection_id IS NULL;

-- Update collection counts
UPDATE rag_collection c
SET document_count = (
    SELECT COUNT(*)
    FROM rag_document d
    WHERE d.collection_id = c.id
),
ready_document_count = (
    SELECT COUNT(*)
    FROM rag_document d
    WHERE d.collection_id = c.id AND d.status = 'READY'
),
chunk_count = (
    SELECT COUNT(*)
    FROM rag_document_chunk chunk
    JOIN rag_document doc ON chunk.document_id = doc.id
    WHERE doc.collection_id = c.id
);

-- Add updated_at trigger for rag_collection
CREATE OR REPLACE FUNCTION update_rag_collection_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_rag_collection_updated_at
BEFORE UPDATE ON rag_collection
FOR EACH ROW
EXECUTE FUNCTION update_rag_collection_updated_at();
