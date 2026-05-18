export interface DocumentSummary {
  documentId: number;
  title: string;
  fileName: string;
  fileType: string;
  status: string;
}

export interface DocumentStatus {
  documentId: number;
  status: string;
  errorMessage?: string | null;
}

export interface DocumentUploadResponse {
  documentId: number;
  title: string;
  fileName: string;
  fileType: string;
  status: string;
  duplicate: boolean;
}

export interface DocumentIndexResponse {
  documentId: number;
  status: string;
  message: string;
}

export interface DocumentEmbeddingEstimate {
  documentId: number;
  chunkCount: number;
  estimatedTokens: number;
  pricePer1kTokens: number;
  estimatedCostCny: number;
  model: string;
  dimension: number;
}

export interface DocumentChunk {
  chunkId: number;
  documentId: number;
  chapterTitle?: string | null;
  chunkIndex: number;
  startParagraph?: number | null;
  endParagraph?: number | null;
  content: string;
  contentPreview?: string | null;
  tokenCount?: number | null;
}

export interface DocumentChunkListResponse {
  documentId: number;
  chunks: DocumentChunk[];
}
