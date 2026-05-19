export interface ChatRequest {
  question: string;
  documentIds?: number[];
  collectionIds?: number[];
  topK?: number;
  scoreThreshold?: number;
}

export interface ChatSource {
  documentId: number;
  documentTitle: string;
  chapterTitle: string;
  chunkId: number;
  chunkIndex: number;
  score: number;
}

export interface ChatResponse {
  answer: string;
  noAnswer: boolean;
  sources: ChatSource[];
}

export interface ChatLogSummary {
  id: number;
  question: string;
  answerPreview: string;
  createdAt: string;
}

export interface ChatLogDetail {
  id: number;
  question: string;
  answer: string;
  documentIds: string;
  retrievedChunkIds: string[];
  topK: number | null;
  minScore: number | null;
  latencyMs: number | null;
  createdAt: string;
}

